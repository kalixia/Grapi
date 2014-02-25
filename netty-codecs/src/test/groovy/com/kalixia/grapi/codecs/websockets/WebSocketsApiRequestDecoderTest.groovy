package com.kalixia.grapi.codecs.websockets

import com.fasterxml.jackson.databind.ObjectMapper
import com.kalixia.grapi.ApiRequest
import groovy.util.logging.Slf4j
import io.netty.channel.embedded.EmbeddedChannel
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import spock.lang.Specification

import javax.ws.rs.core.MediaType

import static io.netty.handler.codec.http.HttpMethod.GET

@Slf4j("logger")
class WebSocketsApiRequestDecoderTest extends Specification {

    def "decode WS text frame as ApiRequest"() {
        given:
        ObjectMapper objectMapper = new ObjectMapper()
        EmbeddedChannel channel = new EmbeddedChannel(new WebSocketsApiRequestDecoder(objectMapper))
        UUID requestID = UUID.randomUUID()

        when:
        channel.writeInbound(new TextWebSocketFrame('{ "id": "' + requestID.toString() + '", "path": "/users/johndoe", "method": "GET", "entity": "" }'))
        channel.finish()

        then:
        def inbound = channel.readInbound()
        inbound instanceof ApiRequest
        def ApiRequest apiRequest = inbound as ApiRequest
        apiRequest.uri() == '/users/johndoe'
        apiRequest.method() == GET
        apiRequest.contentType() == MediaType.APPLICATION_JSON
        apiRequest.id() == requestID
    }

}
