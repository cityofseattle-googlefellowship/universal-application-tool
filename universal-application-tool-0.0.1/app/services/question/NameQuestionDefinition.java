package services.question;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;

public class NameQuestionDefinition extends QuestionDefinition {

  public NameQuestionDefinition(
      OptionalLong id,
      long version,
      String name,
      String path,
      String description,
      ImmutableMap<Locale, String> questionText,
      Optional<ImmutableMap<Locale, String>> questionHelpText) {
    super(id, version, name, path, description, questionText, questionHelpText);
  }

  public NameQuestionDefinition(
      long version,
      String name,
      String path,
      String description,
      ImmutableMap<Locale, String> questionText,
      Optional<ImmutableMap<Locale, String>> questionHelpText) {
    super(version, name, path, description, questionText, questionHelpText);
  }

  @Override
  public QuestionType getQuestionType() {
    return QuestionType.NAME;
  }

  @Override
  public ImmutableMap<String, ScalarType> getScalars() {
    return ImmutableMap.of(
        "first", ScalarType.STRING, "middle", ScalarType.STRING, "last", ScalarType.STRING);
  }
}
