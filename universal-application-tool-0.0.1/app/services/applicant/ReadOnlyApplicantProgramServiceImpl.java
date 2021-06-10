package services.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import services.LocalizedStrings;
import services.Path;
import services.applicant.predicate.JsonPathPredicateGenerator;
import services.applicant.predicate.PredicateEvaluator;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.FileUploadQuestion;
import services.applicant.question.Scalar;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import services.program.predicate.PredicateDefinition;
import services.question.LocalizedQuestionOption;
import services.question.types.EnumeratorQuestionDefinition;

public class ReadOnlyApplicantProgramServiceImpl implements ReadOnlyApplicantProgramService {

  /**
   * Note that even though {@link ApplicantData} is mutable, we can consider it immutable at this
   * point since there is no shared state between requests. In fact, we call {@link
   * ApplicantData#lock()} in the constructor so no changes can occur. This means that we can
   * memoize attributes based on ApplicantData without concern that the data will change.
   */
  private final ApplicantData applicantData;

  private final ProgramDefinition programDefinition;
  private ImmutableList<Block> allBlockList;
  private ImmutableList<Block> currentBlockList;

  protected ReadOnlyApplicantProgramServiceImpl(
      ApplicantData applicantData, ProgramDefinition programDefinition) {
    this.applicantData = new ApplicantData(checkNotNull(applicantData).asJsonString());
    this.applicantData.setPreferredLocale(applicantData.preferredLocale());
    this.applicantData.lock();
    this.programDefinition = checkNotNull(programDefinition);
  }

  @Override
  public String getProgramTitle() {
    return programDefinition.localizedName().getOrDefault(applicantData.preferredLocale());
  }

  @Override
  public ImmutableList<Block> getAllBlocks() {
    if (allBlockList == null) {
      allBlockList = getBlocks(this::showBlock);
    }
    return allBlockList;
  }

  @Override
  public ImmutableList<Block> getInProgressBlocks() {
    if (currentBlockList == null) {
      currentBlockList =
          getBlocks(
              block ->
                  (!block.isCompleteWithoutErrors()
                          || block.wasCompletedInProgram(programDefinition.id()))
                      && showBlock(block));
    }
    return currentBlockList;
  }

  @Override
  public Optional<Block> getBlock(String blockId) {
    return getAllBlocks().stream().filter((block) -> block.getId().equals(blockId)).findFirst();
  }

  @Override
  public Optional<Block> getInProgressBlockAfter(String blockId) {
    ImmutableList<Block> blocks = getInProgressBlocks();
    for (int i = 0; i < blocks.size() - 1; i++) {
      if (blocks.get(i).getId().equals(blockId)) {
        return Optional.of(blocks.get(i + 1));
      }
    }
    return Optional.empty();
  }

  @Override
  public int getBlockIndex(String blockId) {
    ImmutableList<Block> allBlocks = getAllBlocks();

    for (int i = 0; i < allBlocks.size(); i++) {
      if (allBlocks.get(i).getId().equals(blockId)) return i;
    }

    return -1;
  }

  @Override
  public Optional<Block> getFirstIncompleteBlock() {
    return getInProgressBlocks().stream()
        .filter(block -> !block.isCompleteWithoutErrors())
        .findFirst();
  }

  @Override
  public boolean preferredLanguageSupported() {
    return programDefinition.getSupportedLocales().contains(applicantData.preferredLocale());
  }

  @Override
  public ImmutableList<AnswerData> getSummaryData() {
    // TODO: We need to be able to use this on the admin side with admin-specific l10n.
    ImmutableList.Builder<AnswerData> builder = new ImmutableList.Builder<>();
    ImmutableList<Block> blocks = getAllBlocks();
    for (Block block : blocks) {
      ImmutableList<ApplicantQuestion> questions = block.getQuestions();
      for (int questionIndex = 0; questionIndex < questions.size(); questionIndex++) {
        ApplicantQuestion question = questions.get(questionIndex);
        String questionText = question.getQuestionText();
        String answerText = question.errorsPresenter().getAnswerString();
        Optional<Long> timestamp = question.getLastUpdatedTimeMetadata();
        Optional<Long> updatedProgram = question.getUpdatedInProgramMetadata();
        boolean isPreviousResponse =
            updatedProgram.isPresent() && updatedProgram.get() != programDefinition.id();
        AnswerData data =
            AnswerData.builder()
                .setProgramId(programDefinition.id())
                .setBlockId(block.getId())
                .setQuestionDefinition(question.getQuestionDefinition())
                .setRepeatedEntity(block.getRepeatedEntity())
                .setQuestionIndex(questionIndex)
                .setQuestionText(questionText)
                .setAnswerText(answerText)
                .setFileKey(getFileKey(question))
                .setTimestamp(timestamp.orElse(AnswerData.TIMESTAMP_NOT_SET))
                .setIsPreviousResponse(isPreviousResponse)
                .setScalarAnswersInDefaultLocale(
                    getScalarAnswers(question, LocalizedStrings.DEFAULT_LOCALE))
                .build();
        builder.add(data);
      }
    }
    return builder.build();
  }

  /**
   * Gets {@link Block}s for this program and applicant. If {@code onlyIncludeInProgressBlocks} is
   * true, then only the current blocks will be included in the list. A block is "in progress" if it
   * has yet to be filled out by the applicant, or if it was filled out in the context of this
   * program.
   */
  private ImmutableList<Block> getBlocks(Predicate<Block> includeBlockIfTrue) {
    String emptyBlockIdSuffix = "";
    return getBlocks(
        programDefinition.getNonRepeatedBlockDefinitions(),
        emptyBlockIdSuffix,
        Optional.empty(),
        includeBlockIfTrue);
  }

  /**
   * Recursive helper method for {@link ReadOnlyApplicantProgramServiceImpl#getBlocks(Predicate)}.
   */
  private ImmutableList<Block> getBlocks(
      ImmutableList<BlockDefinition> blockDefinitions,
      String blockIdSuffix,
      Optional<RepeatedEntity> maybeRepeatedEntity,
      Predicate<Block> includeBlockIfTrue) {
    ImmutableList.Builder<Block> blockListBuilder = ImmutableList.builder();

    for (BlockDefinition blockDefinition : blockDefinitions) {
      // Create and maybe include the block for this block definition.
      Block block =
          new Block(
              blockDefinition.id() + blockIdSuffix,
              blockDefinition,
              applicantData,
              maybeRepeatedEntity);
      if (includeBlockIfTrue.test(block)) {
        blockListBuilder.add(block);
      }

      // For an enumeration block definition, build blocks for its repeated questions
      if (blockDefinition.isEnumerator()) {

        // Get all the repeated entities enumerated by this enumerator question.
        EnumeratorQuestionDefinition enumeratorQuestionDefinition =
            blockDefinition.getEnumerationQuestionDefinition();
        ImmutableList<RepeatedEntity> repeatedEntities =
            maybeRepeatedEntity.isPresent()
                ? maybeRepeatedEntity
                    .get()
                    .createNestedRepeatedEntities(enumeratorQuestionDefinition, applicantData)
                : RepeatedEntity.createRepeatedEntities(
                    enumeratorQuestionDefinition, applicantData);

        // For each repeated entity, recursively build blocks for all of the repeated blocks of this
        // enumerator block.
        ImmutableList<BlockDefinition> repeatedBlockDefinitions =
            programDefinition.getBlockDefinitionsForEnumerator(blockDefinition.id());
        for (int i = 0; i < repeatedEntities.size(); i++) {
          String nextBlockIdSuffix = String.format("%s-%d", blockIdSuffix, i);
          blockListBuilder.addAll(
              getBlocks(
                  repeatedBlockDefinitions,
                  nextBlockIdSuffix,
                  Optional.of(repeatedEntities.get(i)),
                  includeBlockIfTrue));
        }
      }
    }

    return blockListBuilder.build();
  }

  private boolean showBlock(Block block) {
    if (block.getVisibilityPredicate().isEmpty()) {
      // Default to show
      return true;
    }

    JsonPathPredicateGenerator predicateGenerator =
        new JsonPathPredicateGenerator(
            this.programDefinition.streamQuestionDefinitions().collect(toImmutableList()),
            block.getRepeatedEntity());
    PredicateEvaluator predicateEvaluator =
        new PredicateEvaluator(this.applicantData, predicateGenerator);
    PredicateDefinition predicate = block.getVisibilityPredicate().get();

    switch (predicate.action()) {
      case HIDE_BLOCK:
        return !predicateEvaluator.evaluate(predicate.rootNode());
      case SHOW_BLOCK:
        return predicateEvaluator.evaluate(predicate.rootNode());
      default:
        return true;
    }
  }

  /** Returns the identifier of uploaded file if applicable. */
  private Optional<String> getFileKey(ApplicantQuestion question) {
    switch (question.getType()) {
      case FILEUPLOAD:
        FileUploadQuestion fileUploadQuestion = question.createFileUploadQuestion();
        if (!fileUploadQuestion.isAnswered()) {
          return Optional.empty();
        }
        return fileUploadQuestion.getFileKeyValue();
      default:
        return Optional.empty();
    }
  }

  /**
   * Returns the {@link Path}s and their corresponding scalar answers to a {@link
   * ApplicantQuestion}. Answers do not include metadata.
   */
  private ImmutableMap<Path, String> getScalarAnswers(ApplicantQuestion question, Locale locale) {
    switch (question.getType()) {
      case DROPDOWN:
      case RADIO_BUTTON:
        return ImmutableMap.of(
            question.getContextualizedPath().join(Scalar.SELECTION),
            question
                .createSingleSelectQuestion()
                .getSelectedOptionValue(locale)
                .map(LocalizedQuestionOption::optionText)
                .orElse(""));
      case CHECKBOX:
        return ImmutableMap.of(
            question.getContextualizedPath().join(Scalar.SELECTION),
            question
                .createMultiSelectQuestion()
                .getSelectedOptionsValue(locale)
                .map(
                    selectedOptions ->
                        selectedOptions.stream()
                            .map(LocalizedQuestionOption::optionText)
                            .collect(Collectors.joining(", ")))
                .orElse(""));
      case FILEUPLOAD:
        return ImmutableMap.of(
            question.getContextualizedPath().join(Scalar.FILE_KEY),
            question
                .createFileUploadQuestion()
                .getFileKeyValue()
                .map(
                    fileKey ->
                        controllers.routes.FileController.adminShow(programDefinition.id(), fileKey)
                            .url())
                .orElse(""));
      case ENUMERATOR:
        return ImmutableMap.of(
            question.getContextualizedPath(),
            question.createEnumeratorQuestion().getAnswerString());
      default:
        return question.getContextualizedScalars().keySet().stream()
            .filter(path -> !Scalar.getMetadataScalarKeys().contains(path.keyName()))
            .collect(
                ImmutableMap.toImmutableMap(
                    path -> path, path -> applicantData.readAsString(path).orElse("")));
    }
  }
}
