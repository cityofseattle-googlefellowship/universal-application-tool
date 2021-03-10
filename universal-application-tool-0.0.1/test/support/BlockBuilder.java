package support;

import com.google.common.collect.ImmutableList;
import services.program.BlockDefinition;
import services.program.Predicate;
import services.program.ProgramDefinition;
import services.program.ProgramQuestionDefinition;
import services.question.QuestionDefinition;

public class BlockBuilder {

  private ProgramBuilder programBuilder;
  private BlockDefinition.Builder blockDefBuilder;

  private BlockBuilder(ProgramBuilder programBuilder) {
    this.programBuilder = programBuilder;
    blockDefBuilder = BlockDefinition.builder();
  }

  private BlockBuilder(ProgramBuilder programBuilder, BlockDefinition.Builder blockDefBuilder) {
    this.programBuilder = programBuilder;
    this.blockDefBuilder = blockDefBuilder;
  }

  static BlockBuilder newBlock(ProgramBuilder programBuilder, long id) {
    BlockBuilder blockBuilder = new BlockBuilder(programBuilder);
    blockBuilder.blockDefBuilder = BlockDefinition.builder().setId(id);
    return blockBuilder;
  }

  static BlockBuilder newBlock(
      ProgramBuilder programBuilder, long id, String name, String description) {
    BlockBuilder blockBuilder = new BlockBuilder(programBuilder);
    blockBuilder.blockDefBuilder =
        BlockDefinition.builder().setId(id).setName(name).setDescription(description);
    return blockBuilder;
  }

  public BlockBuilder withName(String name) {
    blockDefBuilder.setName(name);
    return this;
  }

  public BlockBuilder withDescription(String description) {
    blockDefBuilder.setDescription(description);
    return this;
  }

  public BlockBuilder withHidePredicate(Predicate predicate) {
    blockDefBuilder.setHidePredicate(predicate);
    return this;
  }

  public BlockBuilder withHidePredicate(String predicate) {
    blockDefBuilder.setHidePredicate(Predicate.create(predicate));
    return this;
  }

  public BlockBuilder withOptionalPredicate(Predicate predicate) {
    blockDefBuilder.setOptionalPredicate(predicate);
    return this;
  }

  public BlockBuilder withOptionalPredicate(String predicate) {
    blockDefBuilder.setOptionalPredicate(Predicate.create(predicate));
    return this;
  }

  public BlockBuilder withQuestion(QuestionDefinition question) {
    blockDefBuilder.addQuestion(ProgramQuestionDefinition.create(question));
    return this;
  }

  public BlockBuilder withQuestions(ImmutableList<QuestionDefinition> questions) {
    ImmutableList<ProgramQuestionDefinition> pqds =
        questions.stream()
            .map(ProgramQuestionDefinition::create)
            .collect(ImmutableList.toImmutableList());
    blockDefBuilder.setProgramQuestionDefinitions(pqds);
    return this;
  }

  public BlockBuilder withBlock() {
    programBuilder.builder.addBlockDefinition(blockDefBuilder.build());
    return programBuilder.withBlock();
  }

  public BlockBuilder withBlock(String name, String description) {
    programBuilder.builder.addBlockDefinition(blockDefBuilder.build());
    return programBuilder.withBlock(name, description);
  }

  public ProgramDefinition build() {
    return programBuilder.builder.addBlockDefinition(blockDefBuilder.build()).build();
  }
}
