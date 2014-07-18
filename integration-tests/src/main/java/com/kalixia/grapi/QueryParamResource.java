package com.kalixia.grapi;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

@Path("/query")
public class QueryParamResource {

    @GET
    String echoQueryParam(@QueryParam("message") String msg) {
        return msg;
    }

}
