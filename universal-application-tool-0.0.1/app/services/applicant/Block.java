package services.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import services.Path;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.PresentsErrors;
import services.program.BlockDefinition;
import services.program.ProgramQuestionDefinition;

/** Represents a block in the context of a specific user's application. */
public final class Block {

  /**
   * The block's ID. Note this is different from the {@code BlockDefinition}'s ID because
   * BlockDefinitions that repeat expand to multiple Blocks.
   *
   * <p>A top level block has a single number, e.g. "1". This could be a REPEATER question (e.g. who
   * are your household members)
   *
   * <p>Repeated blocks, which ask questions about repeated entities, use dash separated integers
   * which reference a block definition followed by repeated entity indices. For example, consider a
   * question about the incomes for jobs for each household member. Each applicant has multiple
   * household members, and each of those can have multiple jobs. Assume there is a {@link
   * BlockDefinition} with ID 8 that is asking for the income for each job. For block id "8-1-2",
   * the "8" refers to the {@link BlockDefinition} ID, and the "1-2" refers to the first household
   * member's second job.
   */
  private final String id;

  private final BlockDefinition blockDefinition;
  private final ApplicantData applicantData;
  private final Path contextualizedPath;
  private Optional<ImmutableList<ApplicantQuestion>> questionsMemo = Optional.empty();

  Block(
      String id,
      BlockDefinition blockDefinition,
      ApplicantData applicantData,
      Path contextualizedPath) {
    this.id = id;
    this.blockDefinition = checkNotNull(blockDefinition);
    this.applicantData = checkNotNull(applicantData);
    this.contextualizedPath = checkNotNull(contextualizedPath);
  }

  // TODO(#783): Remove this constructor.
  Block(long id, BlockDefinition blockDefinition, ApplicantData applicantData) {
    this(String.valueOf(id), blockDefinition, applicantData, Path.create("applicant"));
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return blockDefinition.name();
  }

  public String getDescription() {
    return blockDefinition.description();
  }

  public ImmutableList<ApplicantQuestion> getQuestions() {
    if (questionsMemo.isEmpty()) {
      this.questionsMemo =
          Optional.of(
              blockDefinition.programQuestionDefinitions().stream()
                  .map(ProgramQuestionDefinition::getQuestionDefinition)
                  .map(
                      questionDefinition ->
                          new ApplicantQuestion(
                              questionDefinition, applicantData, contextualizedPath))
                  .collect(toImmutableList()));
    }
    return questionsMemo.get();
  }

  /** A block has errors if any one of its {@link ApplicantQuestion}s has errors. */
  public boolean hasErrors() {
    return getQuestions().stream().anyMatch(ApplicantQuestion::hasErrors);
  }

  /**
   * Checks whether the block is complete - that is, {@link ApplicantData} has values at all the
   * paths for all required questions in this block and there are no errors. Note: this cannot be
   * memoized, since we need to reflect internal changes to ApplicantData.
   */
  public boolean isCompleteWithoutErrors() {
    // TODO(https://github.com/seattle-uat/civiform/issues/551): Stream only required scalar paths
    //  instead of all scalar paths.
    return isComplete() && !hasErrors();
  }

  /**
   * A block is complete if all of its {@link ApplicantQuestion}s {@link
   * PresentsErrors#isAnswered()}.
   */
  private boolean isComplete() {
    return getQuestions().stream()
        .map(ApplicantQuestion::errorsPresenter)
        .allMatch(PresentsErrors::isAnswered);
  }

  /**
   * Checks that this block is complete and that at least one of the questions was answered during
   * the program.
   *
   * @param programId the program ID to check
   * @return true if this block is complete at least one question was updated while filling out the
   *     program with the given ID; false if this block is incomplete; if it is complete with
   *     errors; or it is complete and all questions were answered in a different program.
   */
  public boolean wasCompletedInProgram(long programId) {
    return isCompleteWithoutErrors()
        && getQuestions().stream()
            .anyMatch(
                q -> {
                  Optional<Long> lastUpdatedInProgram = q.getUpdatedInProgramMetadata();
                  return lastUpdatedInProgram.isPresent()
                      && lastUpdatedInProgram.get().equals(programId);
                });
  }

  @Override
  public boolean equals(@Nullable Object object) {
    if (object instanceof Block) {
      Block that = (Block) object;
      return this.id.equals(that.id)
          && this.blockDefinition.equals(that.blockDefinition)
          && this.applicantData.equals(that.applicantData);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, blockDefinition, applicantData);
  }

  @Override
  public String toString() {
    return "Block [id: " + this.id + "]";
  }
}
