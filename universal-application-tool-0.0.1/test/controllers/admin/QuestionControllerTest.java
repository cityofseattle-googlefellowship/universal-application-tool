package controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static play.api.test.CSRFTokenHelper.addCSRFToken;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.contentAsString;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import models.Question;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Http.Request;
import play.mvc.Http.RequestBuilder;
import play.mvc.Result;
import play.test.Helpers;
import repository.WithPostgresContainer;
import services.Path;
import services.question.QuestionDefinitionBuilder;
import services.question.QuestionType;
import services.question.UnsupportedQuestionTypeException;
import views.html.helper.CSRF;

public class QuestionControllerTest extends WithPostgresContainer {
  private QuestionController controller;

  @Before
  public void setup() {
    controller = app.injector().instanceOf(QuestionController.class);
  }

  @Test
  public void create_redirectsOnSuccess() throws UnsupportedQuestionTypeException {
    buildQuestionsList();
    ImmutableMap.Builder<String, String> formData = ImmutableMap.builder();
    formData
        .put("questionName", "name")
        .put("questionDescription", "desc")
        .put("questionPath", "my.question.path")
        .put("questionType", "TEXT")
        .put("questionText", "Hi mom!")
        .put("questionHelpText", ":-)");
    RequestBuilder requestBuilder = Helpers.fakeRequest().bodyForm(formData.build());
    controller
        .create(requestBuilder.build())
        .thenAccept(
            result -> {
              assertThat(result.redirectLocation())
                  .hasValue(routes.QuestionController.index("table").url());
              assertThat(result.flash()).isNull();
            })
        .toCompletableFuture()
        .join();
  }

  @Test
  public void create_redirectsWithFlashOnFailure() throws UnsupportedQuestionTypeException {
    buildQuestionsList();
    ImmutableMap.Builder<String, String> formData = ImmutableMap.builder();
    formData.put("questionPath", "#invalid_path!");
    RequestBuilder requestBuilder = Helpers.fakeRequest().bodyForm(formData.build());
    controller
        .create(requestBuilder.build())
        .thenAccept(
            result -> {
              assertThat(result.redirectLocation())
                  .hasValue(routes.QuestionController.index("table").url());
              assertThat(result.flash().get("message").get()).contains("#invalid_path!");
            })
        .toCompletableFuture()
        .join();
  }

  @Test
  public void edit_invalidPathRedirectsToNew() throws UnsupportedQuestionTypeException {
    buildQuestionsList();
    Request request = addCSRFToken(Helpers.fakeRequest()).build();
    // Attempts to go to /admin/questions/edit/invalid.path then redirects to /admin/questions/new
    controller
        .edit(request, "invalid.path")
        .thenAccept(
            result -> {
              assertThat(result.redirectLocation())
                  .hasValue(routes.QuestionController.newOne().url());
            })
        .toCompletableFuture()
        .join();
  }

  @Test
  public void edit_returnsPopulatedForm() throws UnsupportedQuestionTypeException {
    buildQuestionsList();
    Request request = addCSRFToken(Helpers.fakeRequest()).build();
    controller
        .edit(request, "the.ultimate.question")
        .thenAccept(
            result -> {
              assertThat(result.status()).isEqualTo(OK);
              assertThat(contentAsString(result)).contains("Edit Question");
              assertThat(contentAsString(result))
                  .contains(CSRF.getToken(request.asScala()).value());
            })
        .toCompletableFuture()
        .join();
  }

  @Test
  public void index_returnsQuestions() throws UnsupportedQuestionTypeException {
    buildQuestionsList();
    Request request = addCSRFToken(Helpers.fakeRequest()).build();
    controller
        .index(request, "table")
        .thenAccept(
            result -> {
              assertThat(result.status()).isEqualTo(OK);
              assertThat(result.contentType()).hasValue("text/html");
              assertThat(result.charset()).hasValue("utf-8");
              assertThat(contentAsString(result)).contains("Total Questions: 1");
              assertThat(contentAsString(result)).contains("All Questions");
            })
        .toCompletableFuture()
        .join();
  }

  @Test
  public void index_withNoQuestions() {
    Request request = addCSRFToken(Helpers.fakeRequest()).build();
    controller
        .index(request, "table")
        .thenAccept(
            result -> {
              assertThat(result.status()).isEqualTo(OK);
              assertThat(result.contentType()).hasValue("text/html");
              assertThat(result.charset()).hasValue("utf-8");
              assertThat(contentAsString(result)).contains("Total Questions: 0");
              assertThat(contentAsString(result)).contains("All Questions");
            })
        .toCompletableFuture()
        .join();
  }

  @Test
  public void index_showsMessageFlash() {
    Request request = addCSRFToken(Helpers.fakeRequest().flash("message", "has message")).build();
    controller
        .index(request, "table")
        .thenAccept(
            result -> {
              assertThat(result.status()).isEqualTo(OK);
              assertThat(result.contentType()).hasValue("text/html");
              assertThat(result.charset()).hasValue("utf-8");
              assertThat(contentAsString(result)).contains("has message");
            })
        .toCompletableFuture()
        .join();
  }

  @Test
  public void newOne_returnsExpectedForm() {
    Request request = addCSRFToken(Helpers.fakeRequest()).build();
    Result result = controller.newOne(request);

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("New Question");
    assertThat(contentAsString(result)).contains(CSRF.getToken(request.asScala()).value());
  }

  @Test
  public void update_redirectsOnSuccess() {
    Question question = resourceCreator().insertQuestion("my.path");
    ImmutableMap.Builder<String, String> formData = ImmutableMap.builder();
    formData
        .put("questionName", "name")
        .put("questionDescription", "desc")
        .put("questionPath", "my.path")
        .put("questionType", "TEXT")
        .put("questionText", "question text updated!")
        .put("questionHelpText", ":-)");
    RequestBuilder requestBuilder = Helpers.fakeRequest().bodyForm(formData.build());
    controller
        .update(requestBuilder.build(), question.id)
        .thenAccept(
            result -> {
              assertThat(result.redirectLocation())
                  .hasValue(routes.QuestionController.index("table").url());
              assertThat(result.flash()).isNull();
            })
        .toCompletableFuture()
        .join();
  }

  @Test
  public void update_redirectsWithFlashOnFailure() {
    Question question = resourceCreator().insertQuestion("my.path");
    ImmutableMap.Builder<String, String> formData = ImmutableMap.builder();
    formData.put("questionPath", "invalid.path").put("questionText", "question text updated!");
    RequestBuilder requestBuilder = Helpers.fakeRequest().bodyForm(formData.build());
    controller
        .update(requestBuilder.build(), question.id)
        .thenAccept(
            result -> {
              assertThat(result.redirectLocation())
                  .hasValue(routes.QuestionController.index("table").url());
              assertThat(result.flash().get("message").get()).contains("invalid.path");
            })
        .toCompletableFuture()
        .join();
  }

  private void buildQuestionsList() throws UnsupportedQuestionTypeException {
    QuestionDefinitionBuilder builder =
        new QuestionDefinitionBuilder()
            .setVersion(1L)
            .setName("First Question")
            .setDescription("This is the first question.")
            .setPath(Path.create("the.ultimate.question"))
            .setQuestionText(
                ImmutableMap.of(Locale.ENGLISH, "What is the answer to the ultimate question?"))
            .setQuestionHelpText(ImmutableMap.of())
            .setQuestionType(QuestionType.TEXT);
    Question question = new Question(builder.build());
    question.save();
  }
}
