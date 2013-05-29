package com.kalixia.ha.api.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import java.util.UUID;

@Path("/objects")

public class ObjectsResource {

    @GET
    @Path("{id}")
    public UUID echoUUID(@PathParam("id") UUID id) {
        return id;
    }

}
