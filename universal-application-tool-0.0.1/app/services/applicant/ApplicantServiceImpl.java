package services.applicant;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.time.Clock;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.Applicant;
import models.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.concurrent.HttpExecutionContext;
import repository.ApplicantRepository;
import services.ErrorAnd;
import services.Path;
import services.WellKnownPaths;
import services.program.BlockDefinition;
import services.program.PathNotInBlockException;
import services.program.ProgramBlockNotFoundException;
import services.program.ProgramDefinition;
import services.program.ProgramService;
import services.question.QuestionDefinition;
import services.question.ScalarType;
import services.question.UnsupportedScalarTypeException;

public class ApplicantServiceImpl implements ApplicantService {
  private static final ImmutableSet<String> RESERVED_SCALAR_KEYS =
      ImmutableSet.of(
          QuestionDefinition.METADATA_UPDATE_TIME_KEY,
          QuestionDefinition.METADATA_UPDATE_PROGRAM_ID_KEY);

  private final ApplicantRepository applicantRepository;
  private final ProgramService programService;
  private final Clock clock;
  private final HttpExecutionContext httpExecutionContext;
  private final Logger log = LoggerFactory.getLogger(ApplicantService.class);

  @Inject
  public ApplicantServiceImpl(
      ApplicantRepository applicantRepository,
      ProgramService programService,
      Clock clock,
      HttpExecutionContext httpExecutionContext) {
    this.applicantRepository = checkNotNull(applicantRepository);
    this.programService = checkNotNull(programService);
    this.clock = checkNotNull(clock);
    this.httpExecutionContext = checkNotNull(httpExecutionContext);
  }

  @Override
  public CompletionStage<Applicant> createApplicant(long userId) {
    Applicant applicant = new Applicant();
    return applicantRepository.insertApplicant(applicant).thenApply((unused) -> applicant);
  }

  @Override
  public CompletionStage<ReadOnlyApplicantProgramService> getReadOnlyApplicantProgramService(
      long applicantId, long programId) {
    CompletableFuture<Optional<Applicant>> applicantCompletableFuture =
        applicantRepository.lookupApplicant(applicantId).toCompletableFuture();
    CompletableFuture<ProgramDefinition> programDefinitionCompletableFuture =
        programService.getProgramDefinitionAsync(programId).toCompletableFuture();

    return CompletableFuture.allOf(applicantCompletableFuture, programDefinitionCompletableFuture)
        .thenApplyAsync(
            (v) -> {
              Applicant applicant = applicantCompletableFuture.join().get();
              ProgramDefinition programDefinition = programDefinitionCompletableFuture.join();

              return new ReadOnlyApplicantProgramServiceImpl(
                  applicant.getApplicantData(), programDefinition);
            },
            httpExecutionContext.current());
  }

  @Override
  public CompletionStage<ErrorAnd<ReadOnlyApplicantProgramService, Exception>>
      stageAndUpdateIfValid(
          long applicantId, long programId, String blockId, ImmutableMap<String, String> updateMap) {
    ImmutableSet<Update> updates =
        updateMap.entrySet().stream()
            .map(entry -> Update.create(Path.create(entry.getKey()), entry.getValue()))
            .collect(ImmutableSet.toImmutableSet());

    boolean updatePathsContainReservedKeys =
        updates.stream().anyMatch(u -> RESERVED_SCALAR_KEYS.contains(u.path().keyName()));
    if (updatePathsContainReservedKeys) {
      return CompletableFuture.failedFuture(
          new IllegalArgumentException("Path contained reserved scalar key"));
    }

    return stageAndUpdateIfValid(applicantId, programId, blockId, updates);
  }

  protected CompletionStage<ErrorAnd<ReadOnlyApplicantProgramService, Exception>>
      stageAndUpdateIfValid(
          long applicantId, long programId, String blockId, ImmutableSet<Update> updates) {
    CompletableFuture<Optional<Applicant>> applicantCompletableFuture =
        applicantRepository.lookupApplicant(applicantId).toCompletableFuture();

    CompletableFuture<ProgramDefinition> programDefinitionCompletableFuture =
        programService.getProgramDefinitionAsync(programId).toCompletableFuture();

    return CompletableFuture.allOf(applicantCompletableFuture, programDefinitionCompletableFuture)
        .thenComposeAsync(
            (v) -> {
              Optional<Applicant> applicantMaybe = applicantCompletableFuture.join();
              if (applicantMaybe.isEmpty()) {
                return CompletableFuture.completedFuture(
                    ErrorAnd.error(ImmutableSet.of(new ApplicantNotFoundException(applicantId))));
              }
              Applicant applicant = applicantMaybe.get();

              ProgramDefinition programDefinition = programDefinitionCompletableFuture.join();

              try {
                stageUpdates(applicant, programDefinition, blockId, updates);
              } catch (ProgramBlockNotFoundException
                  | UnsupportedScalarTypeException
                  | PathNotInBlockException e) {
                return CompletableFuture.completedFuture(ErrorAnd.error(ImmutableSet.of(e)));
              }

              ReadOnlyApplicantProgramService roApplicantProgramService =
                  new ReadOnlyApplicantProgramServiceImpl(
                      applicant.getApplicantData(), programDefinition);

              Optional<Block> blockMaybe = roApplicantProgramService.getBlock(blockId);
              if (blockMaybe.isPresent() && !blockMaybe.get().hasErrors()) {
                return applicantRepository
                    .updateApplicant(applicant)
                    .thenApplyAsync(
                        (finishedSaving) -> ErrorAnd.of(roApplicantProgramService),
                        httpExecutionContext.current());
              }

              return CompletableFuture.completedFuture(ErrorAnd.of(roApplicantProgramService));
            },
            httpExecutionContext.current());
  }

  @Override
  public String applicantName(Application application) {
    try {
      String firstName =
          application.getApplicantData().readString(WellKnownPaths.APPLICANT_FIRST_NAME).get();
      String lastName =
          application.getApplicantData().readString(WellKnownPaths.APPLICANT_LAST_NAME).get();
      return String.format("%s, %s", lastName, firstName);
    } catch (NoSuchElementException e) {
      log.error("Application {} does not include an applicant name.");
      return "<Anonymous Applicant>";
    }
  }

  @Override
  public CompletionStage<ImmutableList<ProgramDefinition>> relevantPrograms(long applicantId) {
    return applicantRepository.programsForApplicant(applicantId);
  }

  /** In-place update of {@link Applicant}'s data. */
  private void stageUpdates(
      Applicant applicant,
      ProgramDefinition programDefinition,
      String blockId,
      ImmutableSet<Update> updates)
      throws ProgramBlockNotFoundException, UnsupportedScalarTypeException,
          PathNotInBlockException {

    BlockDefinition blockDefinition = programDefinition.getBlockDefinition(blockId);
    stageUpdates(applicant.getApplicantData(), blockDefinition, programDefinition.id(), updates);
  }

  /** In-place update of {@link ApplicantData}. */
  private void stageUpdates(
      ApplicantData applicantData,
      BlockDefinition blockDefinition,
      long programId,
      ImmutableSet<Update> updates)
      throws UnsupportedScalarTypeException, PathNotInBlockException {
    ImmutableSet.Builder<Path> questionPaths = ImmutableSet.builder();
    for (Update update : updates) {
      ScalarType type =
          blockDefinition
              .getScalarType(update.path()) // applicant.children[3].name.first
              .orElseThrow(() -> new PathNotInBlockException(blockDefinition, update.path()));
      questionPaths.add(update.path().parentPath());
      switch (type) {
        case STRING:
          applicantData.putString(update.path(), update.value());
          break;
        case LONG:
          applicantData.putLong(update.path(), update.value());
          break;
        default:
          throw new UnsupportedScalarTypeException(type);
      }
    }

    questionPaths.build().forEach(path -> writeMetadataForPath(path, applicantData, programId));
  }

  private void writeMetadataForPath(Path path, ApplicantData data, long programId) {
    data.putLong(
        path.toBuilder().append(QuestionDefinition.METADATA_UPDATE_PROGRAM_ID_KEY).build(),
        programId);
    data.putLong(
        path.toBuilder().append(QuestionDefinition.METADATA_UPDATE_TIME_KEY).build(),
        clock.millis());
  }
}
