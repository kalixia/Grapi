package com.kalixia.grapi.codecs.websockets

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.base.Charsets
import com.kalixia.grapi.ApiResponse
import groovy.util.logging.Slf4j
import io.netty.buffer.Unpooled
import io.netty.channel.embedded.EmbeddedChannel
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import spock.lang.Specification

import static io.netty.handler.codec.http.HttpResponseStatus.OK
import static javax.ws.rs.core.MediaType.APPLICATION_JSON

@Slf4j("logger")
class WebSocketsApiResponseEncoderTest extends Specification {

    def "encore ApiResponse as WS text frame"() {
        given:
        ObjectMapper objectMapper = new ObjectMapper()
        EmbeddedChannel channel = new EmbeddedChannel(new WebSocketsApiResponseEncoder())
        UUID requestID = UUID.randomUUID()
        def content = '{ "id": "' + requestID.toString() + '", "path": "/users/johndoe", "method": "GET", "entity": "" }'
        def byteBuf = Unpooled.copiedBuffer(content, Charsets.UTF_8)
        ApiResponse apiResponse = new ApiResponse(requestID, OK, byteBuf, APPLICATION_JSON)

        when:
        channel.writeOutbound(apiResponse)
        channel.finish()

        then:
        def inbound = channel.readOutbound()
        inbound instanceof TextWebSocketFrame
        def TextWebSocketFrame textFrame = inbound as TextWebSocketFrame
        textFrame.text() == content
    }

}
