package com.kalixia.grapi.codecs.ajax

import groovy.util.logging.Slf4j
import io.netty.channel.embedded.EmbeddedChannel
import io.netty.handler.codec.http.DefaultFullHttpRequest
import io.netty.handler.codec.http.DefaultFullHttpResponse
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.FullHttpResponse
import io.netty.handler.codec.http.HttpHeaders
import spock.lang.Specification

import static io.netty.handler.codec.http.HttpHeaders.Names.ACCESS_CONTROL_ALLOW_ORIGIN
import static io.netty.handler.codec.http.HttpHeaders.Names.ACCESS_CONTROL_REQUEST_HEADERS
import static io.netty.handler.codec.http.HttpHeaders.Names.ORIGIN
import static io.netty.handler.codec.http.HttpMethod.GET
import static io.netty.handler.codec.http.HttpMethod.OPTIONS
import static io.netty.handler.codec.http.HttpResponseStatus.OK
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1

@Slf4j("logger")
class CORSCodecTest extends Specification {

    def "check that non CORS request is not altered"() {
        given: "a channel with the CORS codec"
        EmbeddedChannel channel = new EmbeddedChannel(new CORSCodec())

        and: "an HTTP request"
        FullHttpRequest request = new DefaultFullHttpRequest(HTTP_1_1, GET, "/")

        expect: "the request to be as it was"
        channel.writeInbound(request)
        channel.finish()
        channel.readInbound() == request
    }

    def "check that CORS request with missing access control headers is not intercepted and not altered"() {
        given: "a channel with the CORS codec"
        EmbeddedChannel channel = new EmbeddedChannel(new CORSCodec())

        and: "an HTTP request initiating CORS preflight"
        FullHttpRequest request1 = new DefaultFullHttpRequest(HTTP_1_1, OPTIONS, "/")
        HttpHeaders.setHeader(request1, ORIGIN, 'http://example.com')

        expect: "the request to be as it was"
        channel.writeInbound(request1)
        channel.finish()
    }

    def "check that CORS request with proper headers is intercepted"() {
        given: "a channel with the CORS codec"
        EmbeddedChannel channel = new EmbeddedChannel(new CORSCodec())

        and: "an HTTP request is made initiating CORS preflight"
        FullHttpRequest preflightRequest = new DefaultFullHttpRequest(HTTP_1_1, OPTIONS, "/")
        HttpHeaders.addHeader(preflightRequest, ORIGIN, 'http://example.com')
        HttpHeaders.addHeader(preflightRequest, ACCESS_CONTROL_REQUEST_HEADERS, 'GET, POST, PUT, DELETE, OPTIONS')

        and: "an HTTP request is made using CORS"
        FullHttpRequest corsRequest = new DefaultFullHttpRequest(HTTP_1_1, GET, "/")
        HttpHeaders.setHeader(corsRequest, ORIGIN, 'http://example.com')

        and: "an HTTP response will be sent"
        FullHttpResponse corsResponse = new DefaultFullHttpResponse(HTTP_1_1, OK)

        expect: "the response to be enriched with CORS headers"
        !channel.writeInbound(preflightRequest)
        channel.writeInbound(corsRequest)
        channel.writeOutbound(corsResponse)
        channel.finish()
        channel.readOutbound().headers().get(ACCESS_CONTROL_ALLOW_ORIGIN) == 'http://example.com'
    }

}
