package com.kalixia.grapi.codecs.websockets;

import com.kalixia.grapi.ApiResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class WebSocketsApiResponseEncoder extends MessageToMessageEncoder<ApiResponse> {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketsApiResponseEncoder.class);

    public WebSocketsApiResponseEncoder() {
        super(ApiResponse.class);
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ApiResponse msg, List<Object> out) throws Exception {
        out.add(new TextWebSocketFrame(msg.content()));
    }

}
