package com.kalixia.grapi;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

@Path("/hello")
public class HelloResource {

    @Inject
    public HelloResource() {
    }

    @GET
    public String hello() {
        return "Hello!";
    }

    @GET
    @Path("{lastName}/{firstName}")
    public String helloFromFirstNameAndLastName(@PathParam("firstName") String first, @PathParam("lastName") String last) {
        return String.format("Hello %s %s!", first, last);
    }

}
