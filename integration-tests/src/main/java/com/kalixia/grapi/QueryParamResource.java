package com.kalixia.grapi;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

@Path("/query")
@SuppressWarnings("PMD.UnnecessaryConstructor")
public class QueryParamResource {

    @Inject
    public QueryParamResource() {
    }

    @GET
    public String echoQueryParam(@QueryParam("message") String msg) {
        return msg;
    }

    @GET
    @Path("/number")
    public String echoNumber(@QueryParam("number") Integer number) {
        return number.toString();
    }

    @GET
    @Path("/number-primitive")
    public String echoNumberPrimitive(@QueryParam("number") int number) {
        return Integer.toString(number);
    }

}
