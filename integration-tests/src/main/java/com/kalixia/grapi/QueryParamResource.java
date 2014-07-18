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

    @GET
    @Path("/number")
    String echoNumber(@QueryParam("number") Integer number) {
        return number.toString();
    }

    @GET
    @Path("/number-primitive")
    String echoNumberPrimitive(@QueryParam("number") int number) {
        return Integer.toString(number);
    }

}
