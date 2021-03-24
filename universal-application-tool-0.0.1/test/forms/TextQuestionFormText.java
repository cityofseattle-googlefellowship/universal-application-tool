package forms;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import services.Path;
import services.question.QuestionDefinition;
import services.question.QuestionDefinitionBuilder;
import services.question.TextQuestionDefinition;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

public class TextQuestionFormTest {

  @Test
  public void getBuilder_returnsCompleteBuilder() throws Exception {
    TextQuestionForm form = new TextQuestionForm();
    form.setQuestionName("name");
    form.setQuestionDescription("description");
    form.setQuestionPath("my.question.path");
    form.setQuestionText("What is the question text?");
    form.setQuestionHelpText("");
    form.setTextMinLength(4);
    form.setTextMaxLength(6);
    QuestionDefinitionBuilder builder = form.getBuilder();

    // The QuestionForm does not set version, which is needed in order to build the
    // QuestionDefinition. How we get this value hasn't been determined.
    builder.setVersion(1L);

    TextQuestionDefinition expected =
            new TextQuestionDefinition(
                    1L,
                    "name",
                    Path.create("my.question.path"),
                    "description",
                    ImmutableMap.of(Locale.US, "What is the question text?"),
                    ImmutableMap.of(),
                    TextQuestionDefinition.TextValidationPredicates.create(4, 6));

    QuestionDefinition actual = builder.build();

    assertThat(actual).isEqualTo(expected);
  }
}
