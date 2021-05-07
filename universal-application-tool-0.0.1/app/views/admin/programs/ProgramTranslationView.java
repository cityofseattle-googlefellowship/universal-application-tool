package views.admin.programs;

import com.google.common.collect.ImmutableList;
import controllers.admin.routes;
import j2html.tags.ContainerTag;
import java.util.Locale;
import java.util.Optional;
import javax.inject.Inject;
import play.i18n.Langs;
import play.mvc.Http;
import play.twirl.api.Content;
import views.admin.AdminLayout;
import views.admin.TranslationFormView;
import views.components.FieldWithLabel;

/** Renders a list of languages to select from, and a form for updating program information. */
public class ProgramTranslationView extends TranslationFormView {
  private final AdminLayout layout;

  @Inject
  public ProgramTranslationView(AdminLayout layout, Langs langs) {
    super(langs);
    this.layout = layout;
  }

  public Content render(
      Http.Request request,
      Locale locale,
      long programId,
      String localizedName,
      String localizedDescription,
      Optional<String> errors) {
    return render(
        request,
        locale,
        programId,
        Optional.of(localizedName),
        Optional.of(localizedDescription),
        errors);
  }

  public Content render(
      Http.Request request,
      Locale locale,
      long programId,
      Optional<String> localizedName,
      Optional<String> localizedDescription,
      Optional<String> errors) {
    String formAction =
        controllers.admin.routes.AdminProgramTranslationsController.update(
                programId, locale.toLanguageTag())
            .url();
    ContainerTag form =
        renderTranslationForm(
            request, locale, formAction, formFields(localizedName, localizedDescription), errors);

    // TODO: Set relevant titles with i18n support.
    String title = "Manage Program Translations";
    HtmlBundle bundle = new HtmlBundle()
        .setTitle(title)
        .addHeaderContent(renderHeader(title))
        .addMainContent(renderLanguageLinks(programId, locale))
        .addMainContent(form);
      
    return layout.render(bundle);
  }

  @Override
  protected String languageLinkDestination(long programId, Locale locale) {
    return routes.AdminProgramTranslationsController.edit(programId, locale.toLanguageTag()).url();
  }

  private ImmutableList<FieldWithLabel> formFields(
      Optional<String> localizedName, Optional<String> localizedDescription) {
    return ImmutableList.of(
        FieldWithLabel.input()
            .setId("localize-display-name")
            .setFieldName("displayName")
            .setPlaceholderText("Program display name")
            .setValue(localizedName),
        FieldWithLabel.input()
            .setId("localize-display-description")
            .setFieldName("displayDescription")
            .setPlaceholderText("Program description")
            .setValue(localizedDescription));
  }
}
