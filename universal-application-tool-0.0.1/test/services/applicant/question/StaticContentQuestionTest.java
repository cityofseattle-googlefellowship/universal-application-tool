package services.applicant.question;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import models.Applicant;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import repository.WithPostgresContainer;
import services.LocalizedStrings;
import services.applicant.ApplicantData;
import services.question.types.StaticContentQuestionDefinition;

@RunWith(JUnitParamsRunner.class)
public class StaticContentQuestionTest extends WithPostgresContainer {

  private static final StaticContentQuestionDefinition questionDefinition =
      new StaticContentQuestionDefinition(
          "name",
          Optional.empty(),
          "description",
          LocalizedStrings.of(Locale.US, "Some text. Not an actual question."),
          LocalizedStrings.empty());

  private Applicant applicant;
  private ApplicantData applicantData;

  @Before
  public void setUp() {
    applicant = new Applicant();
    applicantData = applicant.getApplicantData();
  }

  @Test
  public void defaultState() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(questionDefinition, applicantData, Optional.empty());

    StaticContentQuestion question = new StaticContentQuestion(applicantQuestion);

    assertThat(question.hasQuestionErrors()).isFalse();
    assertThat(question.hasTypeSpecificErrors()).isFalse();
  }
}
