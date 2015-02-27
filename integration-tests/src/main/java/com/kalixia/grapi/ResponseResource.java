package com.kalixia.grapi;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;

@Path("/response")
@SuppressWarnings("PMD.UnnecessaryConstructor")
public class ResponseResource {

    @Inject
    public ResponseResource() {
    }

    @GET
    public Response createSomethingFake() throws URISyntaxException {
        return Response.created(new URI("/some-fake-response")).build();
    }

}
