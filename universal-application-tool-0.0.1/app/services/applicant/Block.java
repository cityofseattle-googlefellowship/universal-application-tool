package services.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import services.program.BlockDefinition;
import services.program.ProgramQuestionDefinition;

/** Represents a block in the context of a specific user's application. */
public final class Block {

  /**
   * The block's ID. Note this is different from the {@code BlockDefinition}'s ID because
   * BlockDefinitions that repeat expand to multiple Blocks.
   */
  private final long id;

  private final BlockDefinition blockDefinition;
  private final ApplicantData applicantData;
  private Optional<ImmutableList<ApplicantQuestion>> questionsMemo = Optional.empty();

  Block(long id, BlockDefinition blockDefinition, ApplicantData applicantData) {
    this.id = id;
    this.blockDefinition = checkNotNull(blockDefinition);
    this.applicantData = checkNotNull(applicantData);
  }

  public long getId() {
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
                          new ApplicantQuestion(questionDefinition, applicantData))
                  .collect(toImmutableList()));
    }
    return questionsMemo.get();
  }

  public boolean hasErrors() {
    return getQuestions().stream()
        .map(ApplicantQuestion::hasErrors)
        .reduce(Boolean::logicalOr)
        .orElse(false);
  }

  /**
   * Checks whether the block is complete - that is, {@link ApplicantData} has values at all the
   * paths for all required questions in this block and there are no errors. Note: this cannot be
   * memoized, since we need to reflect internal changes to ApplicantData.
   */
  public boolean isCompleteWithoutErrors() {
    // TODO(https://github.com/seattle-uat/civiform/issues/551): Stream only required scalar paths
    // instead of all scalar paths.
    return blockDefinition.scalarPaths().stream()
            .filter(path -> !applicantData.hasValueAtPath(path))
            .findAny()
            .isEmpty()
        && !hasErrors();
  }

  /**
   * Checks whether all questions in this block were completed while applying to the given program.
   *
   * @param programId the program ID to check
   * @return true if all questions were last updated while filling out the program with the given
   *     ID; false otherwise
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
      return this.id == that.id
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
