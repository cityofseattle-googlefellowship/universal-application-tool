package models;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;
import org.junit.Test;
import repository.ProgramRepository;
import repository.WithPostgresContainer;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import services.question.AddressQuestionDefinition;
import services.question.NameQuestionDefinition;
import services.question.QuestionDefinition;
import services.question.TextQuestionDefinition;
import services.question.TranslationNotFoundException;

public class ProgramTest extends WithPostgresContainer {

  @Test
  public void canSaveProgram() {
    ProgramRepository repo = app.injector().instanceOf(ProgramRepository.class);

    QuestionDefinition questionDefinition =
        new TextQuestionDefinition(
            165L,
            2L,
            "question",
            "applicant.name",
            "applicant's name",
            ImmutableMap.of(Locale.US, "What is your name?"),
            Optional.empty());

    BlockDefinition blockDefinition =
        BlockDefinition.builder()
            .setId(1L)
            .setName("First Block")
            .setDescription("basic info")
            .setQuestionDefinitions(ImmutableList.of(questionDefinition))
            .build();

    ProgramDefinition definition =
        ProgramDefinition.builder()
            .setId(1L)
            .setName("ProgramTest")
            .setDescription("desc")
            .setBlockDefinitions(ImmutableList.of(blockDefinition))
            .build();
    Program program = new Program(definition);

    program.save();

    Program found = repo.lookupProgram(program.id).toCompletableFuture().join().get();

    assertThat(found.getProgramDefinition().name()).isEqualTo("ProgramTest");
    assertThat(found.getProgramDefinition().blockDefinitions().get(0).name())
        .isEqualTo("First Block");

    try {
      assertThat(
              found
                  .getProgramDefinition()
                  .blockDefinitions()
                  .get(0)
                  .questionDefinitions()
                  .get(0)
                  .getQuestionText(Locale.US))
          .isEqualTo("What is your name?");
    } catch (TranslationNotFoundException e) {
    }
  }

  @Test
  public void correctlySerializesDifferentQuestionTypes() {
    ProgramRepository repo = app.injector().instanceOf(ProgramRepository.class);

    AddressQuestionDefinition addressQuestionDefinition =
        new AddressQuestionDefinition(
            1L,
            2L,
            "question",
            "applicant.address",
            "applicant's address",
            ImmutableMap.of(Locale.US, "What is your address?"),
            Optional.empty());
    NameQuestionDefinition nameQuestionDefinition =
        new NameQuestionDefinition(
            2L,
            2L,
            "question",
            "applicant.name",
            "applicant's name",
            ImmutableMap.of(Locale.US, "What is your name?"),
            Optional.empty());

    BlockDefinition blockDefinition =
        BlockDefinition.builder()
            .setId(1L)
            .setName("First Block")
            .setDescription("basic info")
            .setQuestionDefinitions(
                ImmutableList.of(addressQuestionDefinition, nameQuestionDefinition))
            .build();

    ProgramDefinition definition =
        ProgramDefinition.builder()
            .setId(1L)
            .setName("ProgramTest")
            .setDescription("desc")
            .setBlockDefinitions(ImmutableList.of(blockDefinition))
            .build();
    Program program = new Program(definition);
    program.save();

    Program found = repo.lookupProgram(program.id).toCompletableFuture().join().get();

    QuestionDefinition addressQuestion =
        found.getProgramDefinition().blockDefinitions().get(0).questionDefinitions().get(0);
    assertThat(addressQuestion).isInstanceOf(AddressQuestionDefinition.class);
    QuestionDefinition nameQuestion =
        found.getProgramDefinition().blockDefinitions().get(0).questionDefinitions().get(1);
    assertThat(nameQuestion).isInstanceOf(NameQuestionDefinition.class);
  }
}
