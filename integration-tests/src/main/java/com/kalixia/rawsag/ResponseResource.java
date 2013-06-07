package com.kalixia.rawsag;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;

@Path("/response")
public class ResponseResource {

    @GET
    public Response createSomethingFake() throws URISyntaxException {
        return Response.created(new URI("/some-fake-response")).build();
    }

}
