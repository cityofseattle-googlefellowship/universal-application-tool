package services.program.predicate;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.ScalarType;

/**
 * Represents the value on the right side of a JsonPath (https://github.com/json-path/JsonPath)
 * predicate expression. This value is usually a defined constant, such as a number, string, or
 * array.
 */
@AutoValue
public abstract class PredicateValue {

  public static PredicateValue of(long value) {
    return create(String.valueOf(value), ScalarType.LONG);
  }

  public static PredicateValue of(String value) {
    // Escape the string value
    return create(surroundWithQuotes(value), ScalarType.STRING);
  }

  public static PredicateValue of(LocalDate value) {
    return create(
        String.valueOf(value.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()),
        ScalarType.DATE);
  }

  public static PredicateValue of(ImmutableList<String> value) {
    return create(
        value.stream()
            .map(PredicateValue::surroundWithQuotes)
            .collect(toImmutableList())
            .toString(),
        ScalarType.LIST_OF_STRINGS);
  }

  @JsonCreator
  private static PredicateValue create(
      @JsonProperty("value") String value, @JsonProperty("type") ScalarType type) {
    return new AutoValue_PredicateValue(value, type);
  }

  @JsonProperty("value")
  public abstract String value();

  @JsonProperty("type")
  public abstract ScalarType type();

  public String toDisplayString(Optional<QuestionDefinition> question) {
    // Convert to a human-readable date.
    if (type() == ScalarType.DATE) {
      return Instant.ofEpochMilli(Long.parseLong(value()))
          .atZone(ZoneId.systemDefault())
          .toLocalDate()
          .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    // We store the multi-option IDs, rather than the human-readable option text.
    if (type() == ScalarType.LIST_OF_STRINGS
        && question.isPresent()
        && question.get().getQuestionType().isMultiOptionType()) {
      MultiOptionQuestionDefinition multiOptionQuestion =
          (MultiOptionQuestionDefinition) question.get();
      // Convert the quote-escaped string IDs to their corresponding default option text.
      // If an ID is not valid, show "<obsolete>". An obsolete ID does not affect evaluation.
      return Splitter.on(", ")
          .splitToStream(value().substring(1, value().length() - 1))
          .map(stringId -> Long.valueOf(stringId.substring(1, stringId.length() - 1)))
          .map(multiOptionQuestion::getDefaultLocaleOptionForId)
          .map(option -> option.orElse("<obsolete>"))
          .collect(toImmutableList())
          .toString();
    }

    return value();
  }

  private static String surroundWithQuotes(String s) {
    return "\"" + s + "\"";
  }
}