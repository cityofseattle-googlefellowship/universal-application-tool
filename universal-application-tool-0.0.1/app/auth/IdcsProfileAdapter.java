package auth;

import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.OidcConfiguration;
import org.pac4j.oidc.profile.OidcProfile;

/**
 * This class takes an existing UAT profile and augments it with the information from an IDCS
 * profile. Right now this is only extracting the email address, as a proof that this works - it
 * needs to be built out. TODO(nathan): decide what can be extracted and extract it.
 */
public class IdcsProfileAdapter extends UatProfileAdapter {

  public IdcsProfileAdapter(
      OidcConfiguration configuration, OidcClient client, ProfileFactory profileFactory) {
    super(configuration, client, profileFactory);
  }

  @Override
  public UatProfileData uatProfileFromOidcProfile(OidcProfile profile) {
    return mergeUatProfile(
        profileFactory.wrapProfileData(profileFactory.createNewApplicant()), profile);
  }

  @Override
  public UatProfileData mergeUatProfile(UatProfile uatProfile, OidcProfile oidcProfile) {
    // This key is the email address in IDCS.  This isn't combined with AdfsProfileAdapter
    // because the two systems hold totally different amounts of data - IDCS holds almost
    // nothing while AD holds a lot - even though at time of writing they're identical except
    // for this line.
    // TODO(nathan): what if there's already an email address?
    uatProfile.setEmailAddress(oidcProfile.getAttribute("user_emailid", String.class)).join();
    return uatProfile.getProfileData();
  }
}
