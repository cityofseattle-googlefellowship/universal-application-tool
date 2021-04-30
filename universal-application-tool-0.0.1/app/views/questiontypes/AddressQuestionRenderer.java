package views.questiontypes;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;

import j2html.tags.Tag;
import play.i18n.Messages;
import services.applicant.question.AddressQuestion;
import services.applicant.question.ApplicantQuestion;
import views.BaseHtmlView;
import views.components.FieldWithLabel;
import views.style.ReferenceClasses;
import views.style.Styles;

public class AddressQuestionRenderer extends BaseHtmlView implements ApplicantQuestionRenderer {

  private final ApplicantQuestion question;

  public AddressQuestionRenderer(ApplicantQuestion question) {
    this.question = checkNotNull(question);
  }

  @Override
  public Tag render(Messages messages) {
    AddressQuestion addressQuestion = question.createAddressQuestion();

    return div()
        .withId(question.getContextualizedPath().toString())
        .withClasses(Styles.MX_AUTO, Styles.PX_16)
        .with(
            div()
                .withClasses(ReferenceClasses.APPLICANT_QUESTION_TEXT)
                .withText(question.getQuestionText()),
            div()
                .withClasses(
                    ReferenceClasses.APPLICANT_QUESTION_HELP_TEXT,
                    Styles.TEXT_BASE,
                    Styles.FONT_THIN,
                    Styles.MB_2)
                .withText(question.getQuestionHelpText()),
            div()
                .withClasses(Styles.ROUNDED, Styles.BG_OPACITY_50, Styles.PT_2, Styles.PB_4)
                .with(
                    /** First line of address entry: Street */
                    FieldWithLabel.input()
                        .setFieldName(addressQuestion.getStreetPath().toString())
                        .setLabelText(messages.at("label.street"))
                        .setPlaceholderText(messages.at("placeholder.street"))
                        .setFloatLabel(true)
                        .setValue(addressQuestion.getStreetValue().orElse(""))
                        .getContainer()
                        .withClasses(Styles.MY_2, Styles.PT_2),
                    /** Second line of address entry: City, State, Zip */
                    div()
                        .withClasses(Styles.FLEX, Styles.FLEX_ROW)
                        .with(
                            FieldWithLabel.input()
                                .setFieldName(addressQuestion.getCityPath().toString())
                                .setLabelText(messages.at("label.city"))
                                .setPlaceholderText(messages.at("placeholder.city"))
                                .setFloatLabel(true)
                                .setValue(addressQuestion.getCityValue().orElse(""))
                                .getContainer()
                                .withClasses(Styles.P_1, Styles.PT_2, Styles.PL_0, Styles.W_1_2),
                            FieldWithLabel.input()
                                .setFieldName(addressQuestion.getStatePath().toString())
                                .setLabelText(messages.at("label.state"))
                                .setPlaceholderText(messages.at("placeholder.state"))
                                .setFloatLabel(true)
                                .setValue(addressQuestion.getStateValue().orElse(""))
                                .getContainer()
                                .withClasses(Styles.P_1, Styles.PT_2, Styles.W_1_4),
                            FieldWithLabel.input()
                                .setFieldName(addressQuestion.getZipPath().toString())
                                .setLabelText(messages.at("label.zipcode"))
                                .setPlaceholderText(messages.at("placeholder.zipcode"))
                                .setFloatLabel(true)
                                .setValue(addressQuestion.getZipValue().orElse(""))
                                .getContainer()
                                .withClasses(Styles.P_1, Styles.PT_2, Styles.PR_0, Styles.W_1_4)),
                    fieldErrors(addressQuestion.getQuestionErrors(messages))
                        .withClasses(
                            Styles.ML_2, Styles.TEXT_XS, Styles.TEXT_RED_600, Styles.FONT_BOLD)));
  }
}
