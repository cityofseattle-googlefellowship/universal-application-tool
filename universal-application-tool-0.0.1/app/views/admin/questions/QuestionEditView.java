package views.admin.questions;

import static j2html.TagCreator.body;
import static j2html.TagCreator.br;
import static j2html.TagCreator.div;
import static j2html.TagCreator.form;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import forms.QuestionForm;
import j2html.tags.ContainerTag;
import j2html.tags.DomContent;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import play.mvc.Http.Request;
import play.twirl.api.Content;
import services.question.QuestionDefinition;
import services.question.QuestionType;
import views.BaseHtmlLayout;
import views.BaseHtmlView;

public final class QuestionEditView extends BaseHtmlView {
  private final BaseHtmlLayout layout;

  @Inject
  public QuestionEditView(BaseHtmlLayout layout) {
    this.layout = layout;
  }

  public Content renderNewQuestionForm(Request request) {
    return layout.htmlContent(
        body(
            renderHeader("New Question"),
            buildNewQuestionForm().with(makeCsrfTokenInputTag(request))));
  }

  public Content renderEditQuestionForm(Request request, QuestionDefinition question) {
    return layout.htmlContent(
        body(
            renderHeader("Edit Question"),
            buildEditQuestionForm(question).with(makeCsrfTokenInputTag(request))));
  }

  private ContainerTag buildNewQuestionForm() {
    QuestionForm questionForm = new QuestionForm();
    ContainerTag formTag = buildQuestionForm(questionForm);
    formTag
        .withAction(controllers.admin.routes.QuestionController.create().url())
        .with(submitButton("Create"));

    return formTag;
  }

  private ContainerTag buildEditQuestionForm(QuestionDefinition definition) {
    QuestionForm questionForm = new QuestionForm(definition);
    ContainerTag formTag = buildQuestionForm(questionForm);
    formTag.with(
        div("id: " + definition.getId()), div("version: " + definition.getVersion()), br());
    formTag
        .withAction(controllers.admin.routes.QuestionController.update(definition.getId()).url())
        .with(submitButton("Update"));

    return formTag;
  }

  private ContainerTag buildQuestionForm(QuestionForm questionForm) {
    ContainerTag formTag = form().withMethod("POST");
    formTag
        .with(textInputWithLabel("Name: ", "questionName", questionForm.getQuestionName()))
        .with(
            textInputWithLabel(
                "Description: ", "questionDescription", questionForm.getQuestionDescription()))
        .with(textInputWithLabel("Path: ", "questionPath", questionForm.getQuestionPath()))
        .with(textAreaWithLabel("Question Text: ", "questionText", questionForm.getQuestionText()))
        .with(
            textAreaWithLabel(
                "Question Help Text: ", "questionHelpText", questionForm.getQuestionHelpText()))
        .with(formQuestionTypeSelect(QuestionType.valueOf(questionForm.getQuestionType())));

    return formTag;
  }

  private ImmutableList<DomContent> formQuestionTypeSelect(QuestionType selectedType) {
    ImmutableList<SimpleEntry<String, String>> options =
        Arrays.stream(QuestionType.values())
            .map(item -> new SimpleEntry<String, String>(item.toString(), item.name()))
            .collect(ImmutableList.toImmutableList());

    return formSelect("Question type: ", "questionType", options, selectedType.name());
  }
}
