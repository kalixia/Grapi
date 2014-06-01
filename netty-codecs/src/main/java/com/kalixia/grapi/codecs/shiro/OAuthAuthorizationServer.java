package com.kalixia.grapi.codecs.shiro;

import org.apache.shiro.authc.SimpleAccount;

public interface OAuthAuthorizationServer {
    SimpleAccount getAccountFromAccessToken(String token);
}
