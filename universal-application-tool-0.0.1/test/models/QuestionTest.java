package models;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;
import org.junit.Test;
import repository.QuestionRepository;
import repository.WithResettingPostgresContainer;
import services.question.QuestionDefinition;

public class QuestionTest extends WithResettingPostgresContainer {

  @Test
  public void canSaveQuestion() {
    QuestionRepository repo = app.injector().instanceOf(QuestionRepository.class);
    QuestionDefinition definition =
        new QuestionDefinition(1L, 1L, "test", "my.path", "", ImmutableMap.of(), Optional.empty());
    Question question = new Question(definition);

    question.save();

    Question found = repo.lookupQuestion(definition.getId()).toCompletableFuture().join().get();

    assertThat(found.getQuestionDefinition().getId()).isEqualTo(definition.getId());
    assertThat(found.getQuestionDefinition().getVersion()).isEqualTo(definition.getVersion());
    assertThat(found.getQuestionDefinition().getName()).isEqualTo(definition.getName());
    assertThat(found.getQuestionDefinition().getPath()).isEqualTo(definition.getPath());
    assertThat(found.getQuestionDefinition().getDescription())
        .isEqualTo(definition.getDescription());
    assertThat(found.getQuestionDefinition().getQuestionText())
        .isEqualTo(definition.getQuestionText());
    assertThat(found.getQuestionDefinition().getQuestionHelpText())
        .isEqualTo(definition.getQuestionHelpText());
  }

  @Test
  public void canSerializeLocalizationMaps() {
    QuestionRepository repo = app.injector().instanceOf(QuestionRepository.class);
    QuestionDefinition definition =
        new QuestionDefinition(
            1L,
            1L,
            "",
            "",
            "",
            ImmutableMap.of(Locale.ENGLISH, "hello"),
            Optional.of(ImmutableMap.of(Locale.ENGLISH, "help")));
    Question question = new Question(definition);

    question.save();

    Question found = repo.lookupQuestion(definition.getId()).toCompletableFuture().join().get();

    assertThat(found.getQuestionDefinition().getQuestionText())
        .isEqualTo(definition.getQuestionText());
    assertThat(found.getQuestionDefinition().getQuestionHelpText())
        .isEqualTo(definition.getQuestionHelpText());
  }
}
