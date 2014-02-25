package com.kalixia.grapi.codecs.rest

import com.kalixia.grapi.ApiRequest
import com.kalixia.grapi.ApiResponse
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled
import io.netty.channel.embedded.EmbeddedChannel
import io.netty.handler.codec.http.DefaultFullHttpRequest
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.FullHttpResponse
import io.netty.handler.codec.http.HttpHeaders
import io.netty.handler.codec.http.HttpResponseStatus
import org.glassfish.jersey.internal.util.collection.ImmutableMultivaluedMap
import spock.lang.Specification

import javax.ws.rs.core.MultivaluedHashMap
import javax.ws.rs.core.MultivaluedMap

import static io.netty.handler.codec.http.HttpMethod.GET
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1

class RESTCodecTest extends Specification {

    def "check that HTTP request is transformed to ApiRequest"() {
        given: "a channel with the REST codec"
        EmbeddedChannel channel = new EmbeddedChannel(new RESTCodec())

        and: "an HTTP request to a fake REST API"
        FullHttpRequest httpRequest = new DefaultFullHttpRequest(HTTP_1_1, GET, "/users/johndoe")

        when:
        channel.writeInbound(httpRequest)
        channel.finish()

        then: "the ApiRequest is propagated in the pipeline"
        def inbound = channel.readInbound()
        inbound instanceof ApiRequest
        def ApiRequest apiRequest = inbound as ApiRequest
        apiRequest.uri() == '/users/johndoe'
        apiRequest.method() == GET
        apiRequest.id() != null
    }

    def "check that HTTP request with headers is transformed to ApiRequest"() {
        given: "a channel with the REST codec"
        EmbeddedChannel channel = new EmbeddedChannel(new RESTCodec())
        UUID requestID = UUID.randomUUID()

        and: "an HTTP request to a fake REST API"
        FullHttpRequest httpRequest = new DefaultFullHttpRequest(HTTP_1_1, GET, "/users/johndoe")
        HttpHeaders.addHeader(httpRequest, HttpHeaders.Names.ACCEPT, "application/json")
        HttpHeaders.addHeader(httpRequest, HttpHeaders.newEntity(RESTCodec.HEADER_REQUEST_ID), requestID)

        when:
        channel.writeInbound(httpRequest)
        channel.finish()

        then: "the ApiRequest is propagated in the pipeline"
        def inbound = channel.readInbound()
        inbound instanceof ApiRequest
        def ApiRequest apiRequest = inbound as ApiRequest
        apiRequest.uri() == '/users/johndoe'
        apiRequest.method() == GET
        apiRequest.contentType() == "application/json"
        apiRequest.id() == requestID
    }

    def "check that ApiResponse is transformed to HTTP response"() {
        given: "a channel with the REST codec"
        EmbeddedChannel channel = new EmbeddedChannel(new RESTCodec())
        UUID requestID = UUID.randomUUID()

        and: "an API response"
        def MultivaluedMap<String, String> headers = new MultivaluedHashMap<>()
        headers.put(HttpHeaders.Names.ACCEPT_LANGUAGE.toString(), ["fr"] as List<String>)
        ApiResponse apiResponse = new ApiResponse(requestID, HttpResponseStatus.OK,
                Unpooled.EMPTY_BUFFER, "application/json", headers)

        when:
        channel.writeOutbound(apiResponse)
        channel.finish()

        then: "the HTTP response is propagated in the pipeline"
        def outbound = channel.readOutbound()
        outbound instanceof FullHttpResponse
        def FullHttpResponse httpResponse = outbound as FullHttpResponse
        httpResponse.headers().get(RESTCodec.HEADER_REQUEST_ID) == requestID.toString()
        httpResponse.headers().get(HttpHeaders.Names.CONTENT_TYPE) == "application/json"
    }

}
