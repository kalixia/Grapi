package com.kalixia.rawsag.codecs.rest;

import com.kalixia.rawsag.ApiRequest;
import com.kalixia.rawsag.ApiResponse;
import com.kalixia.rawsag.MDCLogging;
import io.netty.buffer.MessageBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.net.InetSocketAddress;
import java.util.UUID;

@ChannelHandler.Sharable
public class RESTCodec extends MessageToMessageCodec<FullHttpRequest, ApiResponse> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RESTCodec.class);

    @Override
    protected void decode(ChannelHandlerContext ctx, FullHttpRequest request, MessageBuf<Object> out) throws Exception {
        UUID requestID;
        String requestIDasString = request.headers().get("X-Api-Request-ID");
        if (requestIDasString != null && "".equals(requestIDasString)) {
            requestID = UUID.fromString(requestIDasString);
        } else {
            requestID = UUID.randomUUID();
        }
        MDC.put(MDCLogging.MDC_REQUEST_ID, requestID.toString());

        String contentType = request.headers().get(HttpHeaders.Names.ACCEPT);

        InetSocketAddress clientAddress = (InetSocketAddress) ctx.channel().remoteAddress();
        ApiRequest apiRequest = new ApiRequest(requestID,
                request.getUri(), request.getMethod(),
                request.content(), contentType,
                clientAddress.getHostName());
        LOGGER.debug("About to handle request {}", request);
        out.add(apiRequest);
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ApiResponse apiResponse, MessageBuf<Object> out) throws Exception {
        FullHttpResponse httpResponse = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,       // TODO: reply with the expectations from the request
                apiResponse.status(),
                apiResponse.content());
        // insert usual HTTP headers
        httpResponse.headers().set(HttpHeaders.Names.CONTENT_LENGTH, apiResponse.content().readableBytes());
        httpResponse.headers().set(HttpHeaders.Names.CONTENT_TYPE, apiResponse.contentType());
//        httpResponse.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
        // insert request ID header
        if (apiResponse.id() != null) {
            httpResponse.headers().set("X-Api-Request-ID", apiResponse.id().toString());
        }
        LOGGER.debug("About to return response {}", httpResponse);
        out.add(httpResponse);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        LOGGER.error("REST Codec error", cause);
    }
}
