package com.kalixia.grapi;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;

@Path("/headers")
public class HeadersResource {

    @Inject
    public HeadersResource() {
    }

    @GET
    public String echoHeader(@HeaderParam("X-CUSTOM-HEADER") String header) {
        return header;
    }

}
