package com.kalixia.rawsag.codecs.websockets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kalixia.rawsag.ApiRequest;
import com.kalixia.rawsag.MDCLogging;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.MessageBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.ws.rs.core.MediaType;
import java.net.InetSocketAddress;
import java.util.UUID;

/**
 * Transforms {@link WebSocketFrame} objects to {@link ApiRequest} ones.
 */
public class WebSocketsApiRequestDecoder extends MessageToMessageDecoder<TextWebSocketFrame> {
    private final ObjectMapper mapper;
    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketsApiRequestDecoder.class);

    public WebSocketsApiRequestDecoder(ObjectMapper mapper) {
        super(TextWebSocketFrame.class);
        this.mapper = mapper;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, TextWebSocketFrame frame, MessageBuf<Object> out) throws Exception {
        WebSocketRequest wsRequest = mapper.readValue(frame.text(), WebSocketRequest.class);

        // extract entity, if available
        ByteBuf content;
        if (wsRequest.getEntity() != null)
            content = Unpooled.copiedBuffer(wsRequest.getEntity().getBytes(CharsetUtil.UTF_8));
        else
            content = Unpooled.EMPTY_BUFFER;

        UUID requestID = wsRequest.getId() != null ? wsRequest.getId() : UUID.randomUUID();
        MDC.put(MDCLogging.MDC_REQUEST_ID, requestID.toString());
        InetSocketAddress clientAddress = (InetSocketAddress) ctx.channel().remoteAddress();
        out.add(new ApiRequest(requestID, wsRequest.getPath(),
                HttpMethod.valueOf(wsRequest.getMethod()), content, MediaType.APPLICATION_JSON,
                clientAddress.getHostName()));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.error("Can't convert WebSockets request to API request", cause);
        ctx.close();
    }
}
