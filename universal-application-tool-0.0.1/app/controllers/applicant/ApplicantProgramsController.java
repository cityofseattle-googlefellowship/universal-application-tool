package controllers.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.CompletableFuture.supplyAsync;

import auth.ProfileUtils;
import com.google.common.collect.ImmutableList;
import controllers.CiviFormController;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import javax.inject.Inject;
import models.LifecycleStage;
import org.pac4j.play.java.Secure;
import play.i18n.MessagesApi;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Http.Request;
import play.mvc.Result;
import services.applicant.ApplicantService;
import services.applicant.Block;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import views.applicant.ApplicantProgramInfoView;
import views.applicant.ProgramIndexView;

/**
 * Controller for handling methods for an applicant applying to programs. CAUTION: you must
 * explicitly check the current profile so that an unauthorized user cannot access another
 * applicant's data!
 */
public class ApplicantProgramsController extends CiviFormController {

  private final HttpExecutionContext httpContext;
  private final ApplicantService applicantService;
  private final MessagesApi messagesApi;
  private final ProgramIndexView programIndexView;
  private final ApplicantProgramInfoView programInfoView;
  private final ProfileUtils profileUtils;

  @Inject
  public ApplicantProgramsController(
      HttpExecutionContext httpContext,
      ApplicantService applicantService,
      MessagesApi messagesApi,
      ProgramIndexView programIndexView,
      ApplicantProgramInfoView programInfoView,
      ProfileUtils profileUtils) {
    this.httpContext = httpContext;
    this.applicantService = applicantService;
    this.messagesApi = checkNotNull(messagesApi);
    this.programIndexView = checkNotNull(programIndexView);
    this.programInfoView = checkNotNull(programInfoView);
    this.profileUtils = checkNotNull(profileUtils);
  }

  @Secure
  public CompletionStage<Result> index(Request request, long applicantId) {
    Optional<String> banner = request.flash().get("banner");
    CompletionStage<String> applicantStage = this.applicantService.getName(applicantId);

    return applicantStage
        .thenComposeAsync(v -> checkApplicantAuthorization(profileUtils, request, applicantId))
        .thenComposeAsync(
            v -> applicantService.relevantPrograms(applicantId), httpContext.current())
        .thenApplyAsync(
            allPrograms -> {
              Set<String> programsWithDraftApplication =
                  allPrograms.get(LifecycleStage.DRAFT).stream()
                      .map(programDefinition -> programDefinition.adminName())
                      .collect(Collectors.toSet());
              ImmutableList<ProgramDefinition> dedupedActivePrograms =
                  allPrograms.get(LifecycleStage.ACTIVE).stream()
                      .filter(
                          programDefinition ->
                              !programsWithDraftApplication.contains(programDefinition.adminName()))
                      .collect(ImmutableList.toImmutableList());
              return ok(
                  programIndexView.render(
                      messagesApi.preferred(request),
                      request,
                      applicantId,
                      applicantStage.toCompletableFuture().join(),
                      allPrograms.get(LifecycleStage.DRAFT),
                      dedupedActivePrograms,
                      banner));
            },
            httpContext.current())
        .exceptionally(
            ex -> {
              if (ex instanceof CompletionException) {
                if (ex.getCause() instanceof SecurityException) {
                  return unauthorized();
                }
              }
              throw new RuntimeException(ex);
            });
  }

  @Secure
  public CompletionStage<Result> view(Request request, long applicantId, long programId) {
    CompletionStage<String> applicantStage = this.applicantService.getName(applicantId);

    return applicantStage
        .thenComposeAsync(v -> checkApplicantAuthorization(profileUtils, request, applicantId))
        .thenComposeAsync(
            v -> applicantService.relevantPrograms(applicantId), httpContext.current())
        .thenApplyAsync(
            allPrograms -> {
              Optional<ProgramDefinition> programDefinition =
                  allPrograms.values().stream()
                      .flatMap(programs -> programs.stream())
                      .filter(program -> program.id() == programId)
                      .findFirst();
              if (programDefinition.isPresent()) {
                return ok(
                    programInfoView.render(
                        messagesApi.preferred(request),
                        programDefinition.get(),
                        request,
                        applicantId,
                        applicantStage.toCompletableFuture().join()));
              }
              return badRequest();
            },
            httpContext.current())
        .exceptionally(
            ex -> {
              if (ex instanceof CompletionException) {
                if (ex.getCause() instanceof SecurityException) {
                  return unauthorized();
                }
              }
              throw new RuntimeException(ex);
            });
  }

  @Secure
  public CompletionStage<Result> edit(Request request, long applicantId, long programId) {

    // Determine first incomplete block, then redirect to other edit.
    return checkApplicantAuthorization(profileUtils, request, applicantId)
        .thenComposeAsync(
            v -> applicantService.getReadOnlyApplicantProgramService(applicantId, programId))
        .thenApplyAsync(
            roApplicantService -> {
              Optional<Block> blockMaybe = roApplicantService.getFirstIncompleteBlock();
              return blockMaybe.flatMap(
                  block ->
                      Optional.of(
                          found(
                              routes.ApplicantProgramBlocksController.edit(
                                  applicantId, programId, block.getId()))));
            },
            httpContext.current())
        .thenComposeAsync(
            resultMaybe -> {
              if (resultMaybe.isEmpty()) {
                return supplyAsync(
                    () ->
                        redirect(
                            routes.ApplicantProgramReviewController.review(
                                applicantId, programId)));
              }
              return supplyAsync(resultMaybe::get);
            },
            httpContext.current())
        .exceptionally(
            ex -> {
              if (ex instanceof CompletionException) {
                Throwable cause = ex.getCause();
                if (cause instanceof SecurityException) {
                  return unauthorized();
                }
                if (cause instanceof ProgramNotFoundException) {
                  return badRequest(cause.toString());
                }
                throw new RuntimeException(cause);
              }
              throw new RuntimeException(ex);
            });
  }
}
