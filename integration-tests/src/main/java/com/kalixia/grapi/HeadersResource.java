package com.kalixia.grapi;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

@Path("/headers")
public class HeadersResource {

    @GET
    public String echoHeader(@HeaderParam("X-CUSTOM-HEADER") String header) {
        return header;
    }

}
