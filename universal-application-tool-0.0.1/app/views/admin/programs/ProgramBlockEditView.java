package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.a;
import static j2html.TagCreator.div;
import static j2html.TagCreator.form;
import static j2html.TagCreator.input;
import static j2html.TagCreator.p;
import static j2html.TagCreator.text;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import controllers.admin.routes;
import forms.BlockForm;
import j2html.TagCreator;
import j2html.attributes.Attr;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import java.util.OptionalLong;
import play.mvc.Http.HttpVerbs;
import play.mvc.Http.Request;
import play.twirl.api.Content;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import services.program.ProgramDefinition.Direction;
import services.program.ProgramQuestionDefinition;
import services.question.types.QuestionDefinition;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.components.FieldWithLabel;
import views.components.Icons;
import views.components.Modal;
import views.components.QuestionBank;
import views.components.ToastMessage;
import views.style.AdminStyles;
import views.style.ReferenceClasses;
import views.style.StyleUtils;
import views.style.Styles;

public class ProgramBlockEditView extends BaseHtmlView {

  private final AdminLayout layout;

  public static final String ENUMERATOR_ID_FORM_FIELD = "enumeratorId";
  private static final String CREATE_BLOCK_FORM_ID = "block-create-form";
  private static final String CREATE_REPEATED_BLOCK_FORM_ID = "repeated-block-create-form";
  private static final String DELETE_BLOCK_FORM_ID = "block-delete-form";

  @Inject
  public ProgramBlockEditView(AdminLayout layout) {
    this.layout = checkNotNull(layout);
  }

  public Content render(
      Request request,
      ProgramDefinition program,
      BlockDefinition blockDefinition,
      String message,
      ImmutableList<QuestionDefinition> questions) {
    return render(
        request,
        program,
        blockDefinition.id(),
        new BlockForm(blockDefinition.name(), blockDefinition.description()),
        blockDefinition,
        blockDefinition.programQuestionDefinitions(),
        message,
        questions);
  }

  public Content render(
      Request request,
      ProgramDefinition programDefinition,
      long blockId,
      BlockForm blockForm,
      BlockDefinition blockDefinition,
      ImmutableList<ProgramQuestionDefinition> blockQuestions,
      String message,
      ImmutableList<QuestionDefinition> questions) {
    Tag csrfTag = makeCsrfTokenInputTag(request);
    String title = "Block edit view";

    String blockUpdateAction =
        controllers.admin.routes.AdminProgramBlocksController.update(
                programDefinition.id(), blockId)
            .url();
    Modal blockDescriptionEditModal = blockDescriptionModal(csrfTag, blockForm, blockUpdateAction);

    HtmlBundle htmlBundle =
        layout
            .getBundle()
            .setTitle(title)
            .addMainStyles(Styles.FLEX, Styles.FLEX_COL)
            .addMainContent(
                addFormEndpoints(csrfTag, programDefinition.id(), blockId),
                layout.renderProgramInfo(programDefinition),
                div()
                    .withId("program-block-info")
                    .withClasses(Styles.FLEX, Styles.FLEX_GROW, Styles._MX_2)
                    .with(blockOrderPanel(request, programDefinition, blockId))
                    .with(
                        blockEditPanel(
                            programDefinition,
                            blockId,
                            blockForm,
                            blockQuestions,
                            blockDefinition.isEnumerator(),
                            csrfTag,
                            blockDescriptionEditModal.getButton()))
                    .with(
                        questionBankPanel(questions, programDefinition, blockDefinition, csrfTag)))
            .addModals(blockDescriptionEditModal);

    if (message.length() > 0) {
      htmlBundle.addToastMessages(ToastMessage.error(message).setDismissible(false));
    }

    return layout.renderCentered(htmlBundle);
  }

  private Tag addFormEndpoints(Tag csrfTag, long programId, long blockId) {
    String blockCreateAction =
        controllers.admin.routes.AdminProgramBlocksController.create(programId).url();
    ContainerTag createBlockForm =
        form(csrfTag)
            .withId(CREATE_BLOCK_FORM_ID)
            .withMethod(HttpVerbs.POST)
            .withAction(blockCreateAction);

    ContainerTag createRepeatedBlockForm =
        form(csrfTag)
            .withId(CREATE_REPEATED_BLOCK_FORM_ID)
            .withMethod(HttpVerbs.POST)
            .withAction(blockCreateAction)
            .with(
                FieldWithLabel.number()
                    .setFieldName(ENUMERATOR_ID_FORM_FIELD)
                    .setValue(OptionalLong.of(blockId))
                    .getContainer());

    String blockDeleteAction =
        controllers.admin.routes.AdminProgramBlocksController.destroy(programId, blockId).url();
    ContainerTag deleteBlockForm =
        form(csrfTag)
            .withId(DELETE_BLOCK_FORM_ID)
            .withMethod(HttpVerbs.POST)
            .withAction(blockDeleteAction);

    return div(createBlockForm, createRepeatedBlockForm, deleteBlockForm)
        .withClasses(Styles.HIDDEN);
  }

  private ContainerTag blockOrderPanel(
      Request request, ProgramDefinition program, long focusedBlockId) {
    ContainerTag ret =
        div()
            .withClasses(
                Styles.SHADOW_LG,
                Styles.PT_6,
                Styles.W_1_5,
                Styles.BORDER_R,
                Styles.BORDER_GRAY_200);
    ret.with(
        renderBlockList(
            request, program, program.getNonRepeatedBlockDefinitions(), focusedBlockId, 0));
    ret.with(
        submitButton("Add Block")
            .withId("add-block-button")
            .attr(Attr.FORM, CREATE_BLOCK_FORM_ID)
            .withClasses(Styles.M_4));
    return ret;
  }

  private ContainerTag renderBlockList(
      Request request,
      ProgramDefinition programDefinition,
      ImmutableList<BlockDefinition> blockDefinitions,
      long focusedBlockId,
      int level) {
    ContainerTag container = div().withClass("pl-" + level * 2);
    for (BlockDefinition blockDefinition : blockDefinitions) {
      String editBlockLink =
          controllers.admin.routes.AdminProgramBlocksController.edit(
                  programDefinition.id(), blockDefinition.id())
              .url();

      // TODO: Not i18n safe.
      int numQuestions = blockDefinition.getQuestionCount();
      String questionCountText = String.format("Question count: %d", numQuestions);
      String blockName = blockDefinition.name();

      ContainerTag moveButtons =
          blockMoveButtons(request, programDefinition.id(), blockDefinitions, blockDefinition);
      String selectedClasses = blockDefinition.id() == focusedBlockId ? Styles.BG_GRAY_100 : "";
      ContainerTag blockTag =
          div()
              .withClasses(
                  Styles.FLEX,
                  Styles.FLEX_ROW,
                  Styles.GAP_2,
                  Styles.PY_2,
                  Styles.PX_4,
                  Styles.BORDER,
                  Styles.BORDER_WHITE,
                  StyleUtils.hover(Styles.BORDER_GRAY_300),
                  selectedClasses)
              .with(
                  a().withClasses(Styles.FLEX_GROW, Styles.OVERFLOW_HIDDEN)
                      .withHref(editBlockLink)
                      .with(p(blockName), p(questionCountText).withClasses(Styles.TEXT_SM)))
              .with(moveButtons);

      container.with(blockTag);

      // Recursively add repeated blocks indented under their enumerator block
      if (blockDefinition.isEnumerator()) {
        container.with(
            renderBlockList(
                request,
                programDefinition,
                programDefinition.getBlockDefinitionsForEnumerator(blockDefinition.id()),
                focusedBlockId,
                level + 1));
      }
    }
    return container;
  }

  private ContainerTag blockMoveButtons(
      Request request,
      long programId,
      ImmutableList<BlockDefinition> blockDefinitions,
      BlockDefinition blockDefinition) {
    String moveUpFormAction =
        routes.AdminProgramBlocksController.move(programId, blockDefinition.id()).url();
    // Move up button is invisible for the first block
    String moveUpInvisible =
        blockDefinition.id() == blockDefinitions.get(0).id() ? Styles.INVISIBLE : "";
    Tag moveUp =
        div()
            .withClass(moveUpInvisible)
            .with(
                form()
                    .withAction(moveUpFormAction)
                    .withMethod(HttpVerbs.POST)
                    .with(makeCsrfTokenInputTag(request))
                    .with(input().isHidden().withName("direction").withValue(Direction.UP.name()))
                    .with(submitButton("^").withClasses(AdminStyles.MOVE_BLOCK_BUTTON)));

    String moveDownFormAction =
        routes.AdminProgramBlocksController.move(programId, blockDefinition.id()).url();
    // Move down button is invisible for the last block
    String moveDownInvisible =
        blockDefinition.id() == blockDefinitions.get(blockDefinitions.size() - 1).id()
            ? Styles.INVISIBLE
            : "";
    Tag moveDown =
        div()
            .withClasses(Styles.TRANSFORM, Styles.ROTATE_180, moveDownInvisible)
            .with(
                form()
                    .withAction(moveDownFormAction)
                    .withMethod(HttpVerbs.POST)
                    .with(makeCsrfTokenInputTag(request))
                    .with(input().isHidden().withName("direction").withValue(Direction.DOWN.name()))
                    .with(submitButton("^").withClasses(AdminStyles.MOVE_BLOCK_BUTTON)));
    ContainerTag moveButtons =
        div().withClasses(Styles.FLEX, Styles.FLEX_COL, Styles.SELF_CENTER).with(moveUp, moveDown);
    return moveButtons;
  }

  private ContainerTag blockEditPanel(
      ProgramDefinition program,
      long blockId,
      BlockForm blockForm,
      ImmutableList<ProgramQuestionDefinition> blockQuestions,
      boolean blockDefinitionIsEnumerator,
      Tag csrfTag,
      Tag blockDescriptionModalButton) {
    // A block can only be deleted when it has no repeated blocks. Same is true for removing the
    // enumerator question from the block.
    final boolean canDelete = !blockDefinitionIsEnumerator || hasNoRepeatedBlocks(program, blockId);

    ContainerTag blockInfoDisplay =
        div()
            .with(
                div(blockForm.getName()).withClasses(Styles.TEXT_XL, Styles.FONT_BOLD, Styles.PY_2))
            .with(div(blockForm.getDescription()).withClasses(Styles.TEXT_LG, Styles.MAX_W_PROSE))
            .withClasses(Styles.M_4);

    // Add buttons to change the block.
    ContainerTag buttons =
        div().withClasses(Styles.MX_4, Styles.FLEX, Styles.FLEX_ROW, Styles.GAP_4);
    buttons.with(blockDescriptionModalButton);
    if (blockDefinitionIsEnumerator) {
      buttons.with(
          submitButton("Create Repeated Block")
              .withId("create-repeated-block-button")
              .attr(Attr.FORM, CREATE_REPEATED_BLOCK_FORM_ID));
    }
    // TODO: Maybe add alpha variants to button color on hover over so we do not have
    //  to hard-code what the color will be when button is in hover state?
    if (program.blockDefinitions().size() > 1) {
      buttons.with(div().withClass(Styles.FLEX_GROW));
      buttons.with(
          submitButton("Delete Block")
              .withId("delete-block-button")
              .attr(Attr.FORM, DELETE_BLOCK_FORM_ID)
              .condAttr(!canDelete, Attr.DISABLED, "")
              .condAttr(
                  !canDelete,
                  Attr.TITLE,
                  "A block can only be deleted when it has no repeated blocks.")
              .withClasses(
                  Styles.MX_4,
                  Styles.MY_1,
                  Styles.BG_RED_500,
                  StyleUtils.hover(Styles.BG_RED_700),
                  Styles.INLINE,
                  StyleUtils.disabled(Styles.OPACITY_50)));
    }

    String deleteQuestionAction =
        controllers.admin.routes.AdminProgramBlockQuestionsController.destroy(program.id(), blockId)
            .url();
    ContainerTag questionDeleteForm =
        form(csrfTag)
            .withId("block-questions-form")
            .withMethod(HttpVerbs.POST)
            .withAction(deleteQuestionAction);
    blockQuestions.forEach(
        pqd -> questionDeleteForm.with(renderQuestion(pqd.getQuestionDefinition(), canDelete)));

    return div()
        .withClasses(Styles.FLEX_AUTO, Styles.PY_6)
        .with(blockInfoDisplay, buttons, questionDeleteForm);
  }

  private ContainerTag renderQuestion(QuestionDefinition definition, boolean canRemove) {
    ContainerTag ret =
        div()
            .withClasses(
                Styles.RELATIVE,
                Styles.MX_4,
                Styles.MY_2,
                Styles.BORDER,
                Styles.BORDER_GRAY_200,
                Styles.PX_4,
                Styles.PY_2,
                Styles.FLEX,
                Styles.ITEMS_START,
                canRemove ? "" : Styles.OPACITY_50,
                StyleUtils.hover(Styles.TEXT_GRAY_800, Styles.BG_GRAY_100));

    Tag removeButton =
        TagCreator.button(text(definition.getName()))
            .withType("submit")
            .withId("block-question-" + definition.getId())
            .withName("block-question-" + definition.getId())
            .withValue(definition.getId() + "")
            .condAttr(!canRemove, Attr.DISABLED, "")
            .condAttr(
                !canRemove,
                Attr.TITLE,
                "An enumerator question can only be removed from the block when the block has no"
                    + " repeated blocks.")
            .withClasses(ReferenceClasses.REMOVE_QUESTION_BUTTON, AdminStyles.CLICK_TARGET_BUTTON);

    ContainerTag icon =
        Icons.questionTypeSvg(definition.getQuestionType(), 24)
            .withClasses(Styles.FLEX_SHRINK_0, Styles.H_12, Styles.W_6);
    ContainerTag content =
        div()
            .withClasses(Styles.ML_4)
            .with(
                p(definition.getName()),
                p(definition.getDescription()).withClasses(Styles.MT_1, Styles.TEXT_SM),
                removeButton);
    return ret.with(icon, content);
  }

  private ContainerTag questionBankPanel(
      ImmutableList<QuestionDefinition> questionDefinitions,
      ProgramDefinition program,
      BlockDefinition blockDefinition,
      Tag csrfTag) {
    String addQuestionAction =
        controllers.admin.routes.AdminProgramBlockQuestionsController.create(
                program.id(), blockDefinition.id())
            .url();

    QuestionBank qb =
        new QuestionBank()
            .setQuestionAction(addQuestionAction)
            .setCsrfTag(csrfTag)
            .setQuestions(questionDefinitions)
            .setProgram(program)
            .setBlockDefinition(blockDefinition);
    return qb.getContainer();
  }

  private Modal blockDescriptionModal(Tag csrfTag, BlockForm blockForm, String blockUpdateAction) {
    String modalTitle = "Block Name and Description";
    String modalButtonText = "Edit Name and Description";
    ContainerTag blockDescriptionForm =
        form(csrfTag).withMethod(HttpVerbs.POST).withAction(blockUpdateAction);
    blockDescriptionForm
        .withId("block-edit-form")
        .with(
            div(
                    FieldWithLabel.input()
                        .setId("block-name-input")
                        .setFieldName("name")
                        .setLabelText("Block name")
                        .setValue(blockForm.getName())
                        .getContainer(),
                    FieldWithLabel.textArea()
                        .setId("block-description-textarea")
                        .setFieldName("description")
                        .setLabelText("Block description")
                        .setValue(blockForm.getDescription())
                        .getContainer())
                .withClasses(Styles.MX_4),
            submitButton("Save")
                .withId("update-block-button")
                .withClasses(
                    Styles.MX_4,
                    Styles.MY_1,
                    Styles.INLINE,
                    Styles.OPACITY_100,
                    StyleUtils.disabled(Styles.OPACITY_50))
                .attr("disabled", ""));
    return Modal.builder("block-description-modal", blockDescriptionForm)
        .setModalTitle(modalTitle)
        .setButtonText(modalButtonText)
        .build();
  }

  private boolean hasNoRepeatedBlocks(ProgramDefinition programDefinition, long blockId) {
    return programDefinition.getBlockDefinitionsForEnumerator(blockId).isEmpty();
  }
}
