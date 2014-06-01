package com.kalixia.grapi.codecs.shiro;

import org.apache.shiro.authc.AuthenticationToken;

public class OAuth2Token implements AuthenticationToken {
    private final String token;

    public OAuth2Token(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    @Override
    public Object getPrincipal() {
        return token;
    }

    @Override
    public Object getCredentials() {
        return token;
    }
}
