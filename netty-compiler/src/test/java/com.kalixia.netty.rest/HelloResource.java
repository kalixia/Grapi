package com.kalixia.ha.api.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

@Path("/hello")
public class HelloResource {

    @GET
    public String hello() {
        return "Hello!";
    }

    @GET
    @Path("{lastName}/{firstName}")
    public String hello2(@PathParam("firstName") String first, @PathParam("lastName") String last) {
        return String.format("Hello %s %s!", first, last);
    }

}
