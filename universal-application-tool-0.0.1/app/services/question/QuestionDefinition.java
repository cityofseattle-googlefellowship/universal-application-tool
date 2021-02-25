package services.question;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;

/** Defines a single question. */
public abstract class QuestionDefinition {
  private final OptionalLong id;
  private final long version;
  private final String name;
  private final String path;
  private final String description;
  private final ImmutableMap<Locale, String> questionText;
  private final Optional<ImmutableMap<Locale, String>> questionHelpText;

  public QuestionDefinition(
      OptionalLong id,
      long version,
      String name,
      String path,
      String description,
      ImmutableMap<Locale, String> questionText,
      Optional<ImmutableMap<Locale, String>> questionHelpText) {
    this.id = id;
    this.version = version;
    this.name = checkNotNull(name);
    this.path = checkNotNull(path);
    this.description = checkNotNull(description);
    this.questionText = checkNotNull(questionText);
    this.questionHelpText = checkNotNull(questionHelpText);
  }

  public QuestionDefinition(
      long version,
      String name,
      String path,
      String description,
      ImmutableMap<Locale, String> questionText,
      Optional<ImmutableMap<Locale, String>> questionHelpText) {
    this(OptionalLong.empty(), version, name, path, description, questionText, questionHelpText);
  }

  /** Return true if the question is persisted and has an unique identifier. */
  @JsonIgnore
  public boolean isPersisted() {
    return this.id.isPresent();
  }

  /** Get the unique identifier for this question. */
  public long getId() {
    return this.id.getAsLong();
  }

  /** Get the system version this question is pinned to. */
  public long getVersion() {
    return this.version;
  }

  /**
   * Get the name of this question.
   *
   * <p>NOTE: This field will not be localized as it is for admin use only.
   */
  public String getName() {
    return this.name;
  }

  /** Get the full path of this question, in JSON notation. */
  public String getPath() {
    return this.path;
  }

  /**
   * Get a human-readable description for the data this question collects.
   *
   * <p>NOTE: This field will not be localized as it is for admin use only.
   */
  public String getDescription() {
    return this.description;
  }

  /** Get the question text for the given locale. */
  public String getQuestionText(Locale locale) throws TranslationNotFoundException {
    if (this.questionText.containsKey(locale)) {
      return this.questionText.get(locale);
    }
    throw new TranslationNotFoundException(this.getPath(), locale);
  }

  /** Get the question tests for all locales. This is used for serialization. */
  public ImmutableMap<Locale, String> getQuestionText() {
    return questionText;
  }

  /** Get the question help text for the given locale. */
  public String getQuestionHelpText(Locale locale) throws TranslationNotFoundException {
    if (!this.questionHelpText.isPresent()) {
      return "";
    }

    if (this.questionHelpText.get().containsKey(locale)) {
      return this.questionHelpText.get().get(locale);
    }

    throw new TranslationNotFoundException(this.getPath(), locale);
  }

  /** Get the question help text for all locales. This is used for serialization. */
  public Optional<ImmutableMap<Locale, String>> getQuestionHelpText() {
    return this.questionHelpText;
  }

  /** Get the type of this question. Implemented methods should use @JsonIgnore. */
  public abstract QuestionType getQuestionType();

  /**
   * Get a map of scalars stored by this question definition. Implemented methods should
   * use @JsonIgnore.
   */
  public abstract ImmutableMap<String, ScalarType> getScalars();

  /** Get a map of scalars stored by this question definition. */
  public ImmutableMap<String, ScalarType> getFullyQualifiedScalars() {
    ImmutableMap<String, ScalarType> scalars = this.getScalars();
    ImmutableMap.Builder<String, ScalarType> ret = new ImmutableMap.Builder<String, ScalarType>();
    scalars.entrySet().stream()
        .forEach(e -> ret.put(this.getPath() + "." + e.getKey(), e.getValue()));
    return ret.build();
  }

  public Optional<ScalarType> getScalarType(String key) {
    return Optional.ofNullable(this.getScalars().get(key));
  }
}
