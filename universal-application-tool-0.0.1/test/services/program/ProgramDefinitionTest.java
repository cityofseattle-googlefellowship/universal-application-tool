package services.program;

import org.junit.Test;

public class ProgramDefinitionTest {
  @Test
  public void createProgramDefinition() {
    BlockDefinition blockA =
        BlockDefinition.builder().setName("Block Name").setDescription("Block Description").build();
    ProgramDefinition.builder()
        .setId("123")
        .setName("The Program")
        .setDescription("This program is for testing.")
        .addBlockDefinition(blockA)
        .build();
  }
}
