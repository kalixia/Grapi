package com.kalixia.ha.api.rest;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

@Path("/nothing")
public class DoNothingResource {
    @POST
    public void doNothing() {
    }

    @GET
    @Path("with-long/{aLong}")
    public Long doNothingWithALong(@PathParam("aLong") Long aLong) {
        return aLong;
    }

    @GET
    @Path("with-long-primitive/{aLong}")
    public long doNothingWithALongPrimitive(@PathParam("aLong") long aLong) {
        return aLong;
    }

//    @GET
//    @Path("with-integer-primitive/{anInteger}")
//    public int doNothingWithAnIntegerPrimitive(@PathParam("anInteger") int anInteger) {
//        return anInteger;
//    }
}
