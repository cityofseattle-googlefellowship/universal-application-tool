package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers;
import controllers.CiviFormController;
import forms.BlockForm;
import java.util.Optional;
import javax.inject.Inject;
import org.pac4j.play.java.Secure;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Http.Request;
import play.mvc.Result;
import services.CiviFormError;
import services.ErrorAnd;
import services.program.BlockDefinition;
import services.program.ProgramBlockNotFoundException;
import services.program.ProgramDefinition;
import services.program.ProgramNeedsABlockException;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import services.question.QuestionService;
import services.question.ReadOnlyQuestionService;
import views.admin.programs.ProgramBlockEditView;

public class AdminProgramBlocksController extends CiviFormController {

  private final ProgramService programService;
  private final ProgramBlockEditView editView;
  private final QuestionService questionService;
  private final FormFactory formFactory;

  @Inject
  public AdminProgramBlocksController(
      ProgramService programService,
      QuestionService questionService,
      ProgramBlockEditView editView,
      FormFactory formFactory) {
    this.programService = checkNotNull(programService);
    this.questionService = checkNotNull(questionService);
    this.editView = checkNotNull(editView);
    this.formFactory = checkNotNull(formFactory);
  }

  @Secure(authorizers = Authorizers.Labels.UAT_ADMIN)
  public Result index(long programId) {
    try {
      ProgramDefinition program = programService.getProgramDefinition(programId);
      long blockId = program.getLastBlockDefinition().id();
      return redirect(routes.AdminProgramBlocksController.edit(programId, blockId));
    } catch (ProgramNotFoundException | ProgramNeedsABlockException e) {
      return notFound(e.toString());
    }
  }

  @Secure(authorizers = Authorizers.Labels.UAT_ADMIN)
  public Result create(Request request, long programId, Optional<Long> repeaterId) {
    try {
      ErrorAnd<ProgramDefinition, CiviFormError> result;
      if (repeaterId.isPresent()) {
        result = programService.addRepeatedBlockToProgram(programId, repeaterId.get());
      } else {
        result = programService.addBlockToProgram(programId);
      }
      ProgramDefinition program = result.getResult();
      BlockDefinition block = program.getLastBlockDefinition();
      if (result.isError()) {
        String errorMessage = joinErrors(result.getErrors());
        return renderEditViewWithMessage(request, program, block, errorMessage);
      }
      return redirect(routes.AdminProgramBlocksController.edit(programId, block.id()).url());
    } catch (ProgramNotFoundException | ProgramNeedsABlockException e) {
      return notFound(e.toString());
    }
  }

  @Secure(authorizers = Authorizers.Labels.UAT_ADMIN)
  public Result edit(Request request, long programId, long blockId) {
    try {
      ProgramDefinition program = programService.getProgramDefinition(programId);
      BlockDefinition block = program.getBlockDefinition(blockId);
      return renderEditViewWithMessage(request, program, block, "");
    } catch (ProgramNotFoundException | ProgramBlockNotFoundException e) {
      return notFound(e.toString());
    }
  }

  @Secure(authorizers = Authorizers.Labels.UAT_ADMIN)
  public Result update(Request request, long programId, long blockId) {
    Form<BlockForm> blockFormWrapper = formFactory.form(BlockForm.class);
    BlockForm blockForm = blockFormWrapper.bindFromRequest(request).get();

    try {
      ErrorAnd<ProgramDefinition, CiviFormError> result =
          programService.updateBlock(programId, blockId, blockForm);
      if (result.isError()) {
        String errorMessage = joinErrors(result.getErrors());
        return renderEditViewWithMessage(
            request, result.getResult(), blockId, blockForm, errorMessage);
      }
    } catch (ProgramNotFoundException | ProgramBlockNotFoundException e) {
      return notFound(e.toString());
    }

    return redirect(routes.AdminProgramBlocksController.edit(programId, blockId));
  }

  @Secure(authorizers = Authorizers.Labels.UAT_ADMIN)
  public Result destroy(long programId, long blockId) {
    try {
      programService.deleteBlock(programId, blockId);
    } catch (ProgramNotFoundException | ProgramNeedsABlockException e) {
      return notFound(e.toString());
    }
    return redirect(routes.AdminProgramBlocksController.index(programId));
  }

  private Result renderEditViewWithMessage(
      Request request, ProgramDefinition program, BlockDefinition block, String message) {
    ReadOnlyQuestionService roQuestionService =
        questionService.getReadOnlyQuestionService().toCompletableFuture().join();

    return ok(
        editView.render(
            request, program, block, message, roQuestionService.getUpToDateQuestions()));
  }

  private Result renderEditViewWithMessage(
      Request request,
      ProgramDefinition program,
      long blockId,
      BlockForm blockForm,
      String message) {
    try {
      BlockDefinition blockDefinition = program.getBlockDefinition(blockId);
      ReadOnlyQuestionService roQuestionService =
          questionService.getReadOnlyQuestionService().toCompletableFuture().join();

      return ok(
          editView.render(
              request,
              program,
              blockId,
              blockForm,
              blockDefinition,
              blockDefinition.programQuestionDefinitions(),
              message,
              roQuestionService.getUpToDateQuestions()));
    } catch (ProgramBlockNotFoundException e) {
      return notFound(e.toString());
    }
  }
}
