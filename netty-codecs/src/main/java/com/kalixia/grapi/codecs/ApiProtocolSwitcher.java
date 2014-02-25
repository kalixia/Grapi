package com.kalixia.grapi.codecs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kalixia.grapi.MDCLogging;
import com.kalixia.grapi.codecs.ajax.CORSCodec;
import com.kalixia.grapi.codecs.rest.RESTCodec;
import com.kalixia.grapi.codecs.websockets.WebSocketsApiRequestDecoder;
import com.kalixia.grapi.codecs.websockets.WebSocketsApiResponseEncoder;
import com.kalixia.grapi.codecs.websockets.WebSocketsServerProtocolUpdater;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.inject.Inject;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;

/**
 * Alters the pipeline in order to cope with both HTTP REST API handlers and WebSockets handlers.
 */
@ChannelHandler.Sharable
public class ApiProtocolSwitcher extends MessageToMessageDecoder<FullHttpRequest> {
    private final ObjectMapper objectMapper;
    private static final ChannelHandler webSocketsServerProtocolUpdater = new WebSocketsServerProtocolUpdater();
    private static final ChannelHandler webSocketsApiResponseEncoder = new WebSocketsApiResponseEncoder();
    private static final CORSCodec corsCodec = new CORSCodec();
    private static final ChannelHandler restCodec = new RESTCodec();
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiProtocolSwitcher.class);

    @Inject
    public ApiProtocolSwitcher(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, FullHttpRequest msg, List<Object> out) throws Exception {
        ChannelPipeline pipeline = ctx.pipeline();

        if (msg.getUri().equals("/websocket")) {
            LOGGER.debug("Switching to WebSockets pipeline...");
            pipeline.addBefore("api-request-logger", "ws-protocol-updater", webSocketsServerProtocolUpdater);
            pipeline.addAfter("ws-protocol-updater", "api-response-encoder-ws", webSocketsApiResponseEncoder);
            pipeline.addAfter("api-response-encoder-ws", "api-request-decoder-ws", new WebSocketsApiRequestDecoder(objectMapper));
            pipeline.remove(this);
        } else {
            LOGGER.debug("Switching to REST pipeline...");
            pipeline.addBefore("api-request-logger", "cors-codec", corsCodec);
            pipeline.addAfter("cors-codec", "rest-codec", restCodec);
            pipeline.remove(this);
        }

        out.add(ReferenceCountUtil.retain(msg));
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        SocketAddress remoteAddress = ctx.channel().remoteAddress();
        String clientAddr;
        if (remoteAddress instanceof InetSocketAddress)
            clientAddr = ((InetSocketAddress) remoteAddress).getHostString();
        else
            clientAddr = remoteAddress.toString();
        MDC.put(MDCLogging.MDC_CLIENT_ADDR, clientAddr);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        MDC.remove(MDCLogging.MDC_CLIENT_ADDR);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.error("Can't setup API pipeline", cause);
        ctx.close();
    }
}
