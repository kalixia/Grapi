package com.kalixia.grapi.codecs.shiro;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.ExpiredCredentialsException;
import org.apache.shiro.authc.LockedAccountException;
import org.apache.shiro.authc.SimpleAccount;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;

/**
 * Realm authenticating {@link OAuth2Token}s.
 */
public class OAuth2Realm extends AuthorizingRealm {
    private final OAuthAuthorizationServer authorizationServer;

    public OAuth2Realm(OAuthAuthorizationServer authorizationServer) {
        super();
        this.authorizationServer = authorizationServer;
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authToken) throws AuthenticationException {
        OAuth2Token token = (OAuth2Token) authToken;

        SimpleAccount account = authorizationServer.getAccountFromAccessToken(token.getToken());

        if (account != null) {
            if (account.isLocked()) {
                throw new LockedAccountException("Account [" + account + "] is locked.");
            }
            if (account.isCredentialsExpired()) {
                String msg = "The credentials for account [" + account + "] are expired";
                throw new ExpiredCredentialsException(msg);
            }
        }

        return account;
    }

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        String username = getAvailablePrincipal(principals).toString();
        return authorizationServer.getAccountFromAccessToken(username);

    }

    @Override
    public boolean supports(AuthenticationToken token) {
        return token instanceof OAuth2Token;
    }

}
