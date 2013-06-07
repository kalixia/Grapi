package com.kalixia.rawsag.codecs.rest;

import com.kalixia.rawsag.ApiRequest;
import com.kalixia.rawsag.ApiResponse;
import com.kalixia.rawsag.MDCLogging;
import io.netty.buffer.BufUtil;
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

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static io.netty.handler.codec.http.HttpHeaders.Names.ACCEPT;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaders.Values.KEEP_ALIVE;

@ChannelHandler.Sharable
public class RESTCodec extends MessageToMessageCodec<FullHttpRequest, ApiResponse> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RESTCodec.class);

    @Override
    protected void decode(ChannelHandlerContext ctx, FullHttpRequest request, MessageBuf<Object> out) throws Exception {
        UUID requestID;
        String requestIDasString = request.headers().get("X-Api-Request-ID");
        if (requestIDasString != null && !"".equals(requestIDasString)) {
            requestID = UUID.fromString(requestIDasString);
        } else {
            requestID = UUID.randomUUID();
        }
        MDC.put(MDCLogging.MDC_REQUEST_ID, requestID.toString());

        String contentType = request.headers().get(ACCEPT);

        InetSocketAddress clientAddress = (InetSocketAddress) ctx.channel().remoteAddress();

        // build headers map
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        HttpHeaders nettyHeaders = request.headers();
        Iterator<String> iterator = nettyHeaders.names().iterator();
        while (iterator.hasNext()) {
            String headerName = iterator.next();
            headers.put(headerName, nettyHeaders.getAll(headerName));
        }

        // build ApiRequest object
        ApiRequest apiRequest = new ApiRequest(requestID,
                request.getUri(), request.getMethod(),
                BufUtil.retain(request.content()), contentType,
                headers, clientAddress.getHostName());
        LOGGER.debug("About to handle request {}", request);
        out.add(apiRequest);
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ApiResponse apiResponse, MessageBuf<Object> out) throws Exception {
        FullHttpResponse httpResponse = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                apiResponse.status(),
                apiResponse.content());
        // insert usual HTTP headers
        httpResponse.headers().set(CONTENT_LENGTH, apiResponse.content().readableBytes());
        httpResponse.headers().set(CONTENT_TYPE, apiResponse.contentType());
        httpResponse.headers().set(CONNECTION, KEEP_ALIVE);

        // insert request ID header
        if (apiResponse.id() != null) {
            httpResponse.headers().set("X-Api-Request-ID", apiResponse.id().toString());
        }

        // insert headers from ApiResponse object
        Iterator<Map.Entry<String, List<String>>> iterator = apiResponse.headers().entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, List<String>> header = iterator.next();
            httpResponse.headers().set(header.getKey(), header.getValue());
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
