package com.kalixia.grapi;

import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/cookies")
public class CookiesResource {

    @GET
    public String echoHeader(@CookieParam("my-cookie") String cookie) {
        return cookie;
    }

}
