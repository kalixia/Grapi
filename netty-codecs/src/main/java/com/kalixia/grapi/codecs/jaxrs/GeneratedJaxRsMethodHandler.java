package com.kalixia.grapi.codecs.jaxrs;

import com.kalixia.grapi.ApiRequest;
import com.kalixia.grapi.ApiResponse;
import io.netty.channel.ChannelHandlerContext;

public interface GeneratedJaxRsMethodHandler {

    /**
     * Indicates if a {@link ApiRequest} should be handled by this handler.
     * @param request the request to be matched against
     * @return returns true if the {@link ApiRequest} should be handled by this handler.
     */
    boolean matches(ApiRequest request);

    /**
     * Handles an {@link ApiRequest}
     * @param request the request to handle
     * @param ctx     the channel context on which the request is made
     * @return the {@link} ApiResponse
     */
    ApiResponse handle(ApiRequest request, ChannelHandlerContext ctx) throws Exception;

}
