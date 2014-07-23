package com.kalixia.grapi;

import javax.inject.Inject;
import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/cookies")
public class CookiesResource {

    @Inject
    public CookiesResource() {
    }

    @GET
    public String echoHeader(@CookieParam("my-cookie") String cookie) {
        return cookie;
    }

}
