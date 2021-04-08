package support;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import javax.persistence.PersistenceException;
import models.LifecycleStage;
import models.Question;
import services.Path;
import services.question.exceptions.InvalidQuestionTypeException;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.AddressQuestionDefinition;
import services.question.types.NameQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.RepeaterQuestionDefinition;
import services.question.types.TextQuestionDefinition;

/**
 * A cached {@link Question} bank for testing.
 *
 * <p>The {@link Question}s in this question bank should be treated as constants, but they need to
 * be persisted in the database for some tests so they are persisted and cached. When used with
 * tests that do not have a database available (see {@link #maybeSave(QuestionDefinition)}), the
 * question IDs may not be reliable since in production, the IDs are set by the database.
 *
 * <p>The properties of these questions (e.g. question path) are not canonical and may not be
 * representative of the properties defined by CiviForm administrators.
 *
 * <p>To add a new {@link Question} to the question bank: create a {@link QuestionEnum} for it,
 * create a private static method to construct the question, and create a public static method to
 * retrieve the cached question.
 */
public class TestQuestionBank {

  private static final long VERSION = 1L;
  private static final Map<QuestionEnum, Question> questionCache = new ConcurrentHashMap<>();
  private static final AtomicLong nextId = new AtomicLong(1L);

  public static void reset() {
    questionCache.clear();
    nextId.set(1L);
  }

  public static Question applicantName() {
    return questionCache.computeIfAbsent(
        QuestionEnum.APPLICANT_NAME, TestQuestionBank::applicantName);
  }

  public static Question applicantAddress() {
    return questionCache.computeIfAbsent(
        QuestionEnum.APPLICANT_ADDRESS, TestQuestionBank::applicantAddress);
  }

  public static Question applicantFavoriteColor() {
    return questionCache.computeIfAbsent(
        QuestionEnum.APPLICANT_FAVORITE_COLOR, TestQuestionBank::applicantFavoriteColor);
  }

  public static Question applicantHouseholdMembers() {
    return questionCache.computeIfAbsent(
        QuestionEnum.APPLICANT_HOUSEHOLD_MEMBERS, TestQuestionBank::applicantHouseholdMembers);
  }

  private static Question applicantName(QuestionEnum ignore) {
    QuestionDefinition definition =
        new NameQuestionDefinition(
            VERSION,
            "applicant name",
            Path.create("applicant.name"),
            Optional.empty(),
            "name of applicant",
            LifecycleStage.ACTIVE,
            ImmutableMap.of(Locale.US, "what is your name?"),
            ImmutableMap.of(Locale.US, "help text"));
    return maybeSave(definition);
  }

  private static Question applicantAddress(QuestionEnum ignore) {
    QuestionDefinition definition =
        new AddressQuestionDefinition(
            VERSION,
            "applicant address",
            Path.create("applicant.address"),
            Optional.empty(),
            "address of applicant",
            LifecycleStage.ACTIVE,
            ImmutableMap.of(Locale.US, "what is your address?"),
            ImmutableMap.of(Locale.US, "help text"));
    return maybeSave(definition);
  }

  private static Question applicantFavoriteColor(QuestionEnum ignore) {
    QuestionDefinition definition =
        new TextQuestionDefinition(
            VERSION,
            "applicant favorite color",
            Path.create("applicant.color"),
            Optional.empty(),
            "favorite color of applicant",
            LifecycleStage.ACTIVE,
            ImmutableMap.of(Locale.US, "what is your favorite color?"),
            ImmutableMap.of(Locale.US, "help text"));
    return maybeSave(definition);
  }

  private static Question applicantHouseholdMembers(QuestionEnum ignore) {
    QuestionDefinition definition =
        new RepeaterQuestionDefinition(
            VERSION,
            "applicant household members",
            Path.create("applicant.household_members[]"),
            Optional.empty(),
            "The applicant's household members",
            LifecycleStage.ACTIVE,
            ImmutableMap.of(Locale.US, "Who are your household members?"),
            ImmutableMap.of(Locale.US, "help text"));

    return maybeSave(definition);
  }

  private static Question maybeSave(QuestionDefinition questionDefinition) {
    Question question = new Question(questionDefinition);
    try {
      question.save();
    } catch (ExceptionInInitializerError | NoClassDefFoundError | PersistenceException ignore) {
      question.id = nextId.getAndIncrement();
      try {
        question.loadQuestionDefinition();
      } catch (InvalidQuestionTypeException | UnsupportedQuestionTypeException e) {
        throw new IllegalArgumentException(
            "Questions in the TestQuestionBank better be supported QuestionTypes.");
      }
    }
    return question;
  }

  private enum QuestionEnum {
    APPLICANT_NAME,
    APPLICANT_ADDRESS,
    APPLICANT_FAVORITE_COLOR,
    APPLICANT_HOUSEHOLD_MEMBERS
  }
}
