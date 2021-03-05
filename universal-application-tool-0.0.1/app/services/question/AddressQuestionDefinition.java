package services.question;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.OptionalLong;
import services.Path;

public class AddressQuestionDefinition extends QuestionDefinition {

  public AddressQuestionDefinition(
      OptionalLong id,
      long version,
      String name,
      Path path,
      String description,
      ImmutableMap<Locale, String> questionText,
      ImmutableMap<Locale, String> questionHelpText) {
    super(id, version, name, path, description, questionText, questionHelpText);
  }

  public AddressQuestionDefinition(
      long version,
      String name,
      Path path,
      String description,
      ImmutableMap<Locale, String> questionText,
      ImmutableMap<Locale, String> questionHelpText) {
    super(version, name, path, description, questionText, questionHelpText);
  }

  @Override
  public QuestionType getQuestionType() {
    return QuestionType.ADDRESS;
  }

  @Override
  public ImmutableMap<Path, ScalarType> getScalars() {
    return ImmutableMap.of(
        getStreetPath(),
        getStreetType(),
        getCityPath(),
        getCityType(),
        getStatePath(),
        getStateType(),
        getZipPath(),
        getZipType());
  }

  public Path getStreetPath() {
    return getPath().toBuilder().append(".street").build();
  }

  public ScalarType getStreetType() {
    return ScalarType.STRING;
  }

  public Path getCityPath() {
    return getPath().toBuilder().append(".city").build();
  }

  public ScalarType getCityType() {
    return ScalarType.STRING;
  }

  public Path getStatePath() {
    return getPath().toBuilder().append(".state").build();
  }

  public ScalarType getStateType() {
    return ScalarType.STRING;
  }

  public Path getZipPath() {
    return getPath().toBuilder().append(".zip").build();
  }

  public ScalarType getZipType() {
    return ScalarType.STRING;
  }
}
