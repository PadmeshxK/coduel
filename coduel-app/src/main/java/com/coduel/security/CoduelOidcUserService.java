package com.coduel.security;

import com.coduel.api.UserApi;
import com.coduel.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

/**
 * Runs on every Google login: fetch the OIDC profile (via super), then upsert our Users row.
 * First login = signup; later logins refresh the profile. The principal stays a standard OidcUser
 * whose subject (sub) is the googleId — that's how we resolve the app userId when needed.
 */
@Service
public class CoduelOidcUserService extends OidcUserService {

    @Autowired
    private UserApi userApi;

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) {
        OidcUser oidcUser = super.loadUser(userRequest);

        User incoming = new User();
        incoming.setGoogleId(oidcUser.getSubject());
        incoming.setEmail(oidcUser.getEmail());
        incoming.setDisplayName(oidcUser.getFullName());
        incoming.setAvatarUrl(oidcUser.getPicture());
        userApi.upsert(incoming);

        return oidcUser;
    }
}
