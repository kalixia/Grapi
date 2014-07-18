package com.kalixia.grapi;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/")
public class LoginResource {

    @GET
    @Path("login")
    public String login(@FormParam("login") String login, @FormParam("password") String password) {
        return String.format("Should login with %s %s!", login, password);
    }

}
