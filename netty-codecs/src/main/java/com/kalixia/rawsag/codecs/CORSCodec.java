package com.kalixia.rawsag.codecs;

import io.netty.buffer.BufUtil;
import io.netty.buffer.MessageBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.netty.handler.codec.http.HttpHeaders.Names.ACCESS_CONTROL_ALLOW_HEADERS;
import static io.netty.handler.codec.http.HttpHeaders.Names.ACCESS_CONTROL_ALLOW_METHODS;
import static io.netty.handler.codec.http.HttpHeaders.Names.ACCESS_CONTROL_ALLOW_ORIGIN;
import static io.netty.handler.codec.http.HttpHeaders.Names.ACCESS_CONTROL_MAX_AGE;
import static io.netty.handler.codec.http.HttpHeaders.Names.ACCESS_CONTROL_REQUEST_HEADERS;
import static io.netty.handler.codec.http.HttpHeaders.Names.ORIGIN;

/**
 * Codec adding support for Cross-Origin Resource Sharing (CORS).
 *
 * TODO: security should be improved as by default this handle allows any origin to make requests!
 */
@ChannelHandler.Sharable
public class CORSCodec extends MessageToMessageCodec<FullHttpRequest, HttpResponse> {
    private static final AttributeKey<String> attrOrigin = new AttributeKey<>("corsOrigin");
    private static final Logger LOGGER = LoggerFactory.getLogger(CORSCodec.class);

    @Override
    protected void decode(ChannelHandlerContext ctx, FullHttpRequest request, MessageBuf<Object> out) throws Exception {
        if (request.headers().contains(ORIGIN)) {
            // preflight request?
            if (HttpMethod.OPTIONS.equals(request.getMethod())) {
                handleCorsRequest(ctx, request);
                return;
            }
            String origin = request.headers().get(ORIGIN);
            ctx.channel().attr(attrOrigin).set(origin);
        }
        // otherwise simply forward to the next channel handler as-is
        out.add(BufUtil.retain(request));
    }

    private void handleCorsRequest(ChannelHandlerContext ctx, FullHttpRequest request) {
        LOGGER.debug("Intercepted CORS request {}", request);
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        // TODO: filter allowed CORS domains?
        response.headers().add(ACCESS_CONTROL_ALLOW_ORIGIN, request.headers().get(ORIGIN));
        response.headers().add(ACCESS_CONTROL_ALLOW_HEADERS, request.headers().get(ACCESS_CONTROL_REQUEST_HEADERS));
        response.headers().add(ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, PUT, DELETE, OPTIONS");
        response.headers().add(ACCESS_CONTROL_MAX_AGE, "3628800");
        ctx.write(response);
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, HttpResponse response, MessageBuf<Object> out) throws Exception {
        String origin = ctx.channel().attr(attrOrigin).getAndRemove();
        LOGGER.info("Origin: {}", origin);
        if (origin != null) {
            response.headers().add(ACCESS_CONTROL_ALLOW_ORIGIN, origin);
        }
        LOGGER.info("Response is: {}", response);
        out.add(BufUtil.retain(response));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.error("Can't handle CORS request", cause);
        ctx.close();
    }

}