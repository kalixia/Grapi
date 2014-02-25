package com.kalixia.grapi.codecs.websockets

import groovy.util.logging.Slf4j
import io.netty.channel.embedded.EmbeddedChannel
import io.netty.handler.codec.http.DefaultFullHttpRequest
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.HttpHeaders
import spock.lang.Specification

import static com.kalixia.grapi.codecs.websockets.WebSocketsServerProtocolUpdater.WEBSOCKET_PATH
import static io.netty.handler.codec.http.HttpMethod.GET
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1

@Slf4j("logger")
class WebSocketsServerProtocolUpdaterTest extends Specification {

    def "test protocol updater"() {
        given:
        EmbeddedChannel channel = new EmbeddedChannel(new WebSocketsServerProtocolUpdater())

        and:
        UUID requestID = UUID.randomUUID()
        FullHttpRequest httpRequest = new DefaultFullHttpRequest(HTTP_1_1, GET, WEBSOCKET_PATH)
        HttpHeaders.addHeader(httpRequest, HttpHeaders.Names.HOST, "localhost")
        HttpHeaders.addHeader(httpRequest, HttpHeaders.Names.UPGRADE, "websocket")
        HttpHeaders.addHeader(httpRequest, HttpHeaders.Names.CONNECTION, "Upgrade")
        HttpHeaders.addHeader(httpRequest, HttpHeaders.Names.SEC_WEBSOCKET_VERSION, "13")
        HttpHeaders.addHeader(httpRequest, HttpHeaders.Names.SEC_WEBSOCKET_KEY, requestID)

        when:
        channel.writeInbound(httpRequest)

        then:
        !channel.readInbound()
    }

}
