package com.kalixia.grapi;

import javax.inject.Inject;
import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/cookies")
@SuppressWarnings("PMD.UnnecessaryConstructor")
public class CookiesResource {

    @Inject
    public CookiesResource() {
    }

    @GET
    public String echoCookie(@CookieParam("my-cookie") String cookie) {
        return cookie;
    }

}
