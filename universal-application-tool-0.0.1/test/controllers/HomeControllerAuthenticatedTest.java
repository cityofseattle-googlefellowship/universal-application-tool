package controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static play.api.test.Helpers.testServerPort;
import static play.test.Helpers.*;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.pac4j.core.client.Client;
import org.pac4j.core.client.Clients;
import org.pac4j.core.client.direct.AnonymousClient;
import org.pac4j.core.client.finder.ClientFinder;
import org.pac4j.core.config.Config;
import org.pac4j.core.context.HttpConstants;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.engine.DefaultSecurityLogic;
import play.Application;
import play.mvc.Http;
import play.mvc.Result;
import play.test.WithApplication;

public class HomeControllerAuthenticatedTest extends WithApplication {

  // Should be no need for a database here.
  protected Application provideApplication() {
    ImmutableMap<String, String> args =
        new ImmutableMap.Builder<String, String>()
            .putAll(inMemoryDatabase("default", ImmutableMap.of("MODE", "PostgreSQL")))
            .put("play.evolutions.db.default.enabled", "false")
            .build();
    return fakeApplication(args);
  }

  @Before
  public void setUp() {
    // Get the config, and hack it so that all requests appear authorized.
    Config config = app.injector().instanceOf(Config.class);
    AnonymousClient client = AnonymousClient.INSTANCE;
    config.setClients(new Clients(client));

    // The SecurityLogic wants to use some smarts to figure out which client to use, but
    // those smarts are never going to pick this client (since none of the endpoints are
    // configured to use it), so we implement an anonymous client finder which always returns
    // our client.
    DefaultSecurityLogic securityLogic = new DefaultSecurityLogic();
    securityLogic.setClientFinder(
        new ClientFinder() {
          @Override
          public List<Client> find(Clients clients, WebContext context, String clientNames) {
            return ImmutableList.of(client);
          }
        });
    config.setSecurityLogic(securityLogic);
  }

  @Test
  public void testAuthenticatedSecurePage() {
    Http.RequestBuilder request =
        fakeRequest(routes.HomeController.secureIndex())
            .header(Http.HeaderNames.HOST, "localhost:" + testServerPort());
    Result result = route(app, request);
    assertThat(result.status()).isEqualTo(HttpConstants.OK);
  }
}
