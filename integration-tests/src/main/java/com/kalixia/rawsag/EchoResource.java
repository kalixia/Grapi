package com.kalixia.rawsag;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/echo")
@Produces(MediaType.TEXT_PLAIN)
public class EchoResource {

    @Path("{message}")
    @GET
    public String echo(@PathParam("message") String message) {
        return message;
    }

}
