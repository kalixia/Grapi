package com.kalixia.grapi;

import javax.validation.constraints.NotNull;
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
    public String echo(@PathParam("message") @NotNull String message) {
        return message;
    }

}
