package com.kalixia.ha.api.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

@Path("/echo")
public class EchoResource {

    @Path("{message}")
    @GET
    public String echo(@PathParam("message") String message) {
        return message;
    }

}
