package com.kalixia.grapi.codecs.shiro

import io.netty.channel.embedded.EmbeddedChannel
import io.netty.handler.codec.http.DefaultFullHttpRequest
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.HttpResponse
import io.netty.handler.codec.http.HttpResponseStatus
import org.apache.shiro.authc.SimpleAccount
import org.apache.shiro.mgt.DefaultSecurityManager
import org.apache.shiro.mgt.SecurityManager
import org.apache.shiro.subject.Subject
import spock.lang.Shared
import spock.lang.Specification

import static io.netty.handler.codec.http.HttpMethod.GET
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION

class ShiroHandlerTest extends Specification {
    @Shared String VALID_OAUTH2_TOKEN = "123456789"
    @Shared String LOCKED_OAUTH2_TOKEN = "234567891"
    @Shared SecurityManager securityManager

    def setupSpec() {
        securityManager =  new DefaultSecurityManager()
        OAuthAuthorizationServer authorizationServer = Mock(OAuthAuthorizationServer)

        def validAccount = new SimpleAccount("test", VALID_OAUTH2_TOKEN, OAuth2Realm.class.getSimpleName())
        def lockedAccount = new SimpleAccount("test", VALID_OAUTH2_TOKEN, OAuth2Realm.class.getSimpleName())
        lockedAccount.locked = true

        authorizationServer.getAccountFromAccessToken(VALID_OAUTH2_TOKEN) >> validAccount
        authorizationServer.getAccountFromAccessToken(LOCKED_OAUTH2_TOKEN) >> lockedAccount

        securityManager.setRealm(new OAuth2Realm(authorizationServer))
    }

    def "check that HTTP request without OAuth2 token is ignored by ShiroHandler"() {
        given: "a channel with the ShiroHandler codec"
        EmbeddedChannel channel = new EmbeddedChannel(new ShiroHandler(securityManager))

        and: "an HTTP request to a fake REST API"
        FullHttpRequest httpRequest = new DefaultFullHttpRequest(HTTP_1_1, GET, "/users/johndoe")

        when:
        def written = channel.writeInbound(httpRequest)

        then: "the HTTP request is propagated as-is in the pipeline"
        written
        def inbound = channel.readInbound()
        inbound != null
        inbound == httpRequest

        then: "close the channel"
        !channel.finish()
    }

    def "check that HTTP request with non OAuth2 token is ignored by ShiroHandler"() {
        given: "a channel with the ShiroHandler codec"
        EmbeddedChannel channel = new EmbeddedChannel(new ShiroHandler(securityManager))

        and: "an HTTP request to a fake REST API"
        FullHttpRequest httpRequest = new DefaultFullHttpRequest(HTTP_1_1, GET, "/users/johndoe")
        httpRequest.headers().set(AUTHORIZATION, "Non OAuth2 token")

        when:
        def written = channel.writeInbound(httpRequest)

        then: "the HTTP request is propagated as-is in the pipeline"
        written
        def inbound = channel.readInbound()
        inbound != null
        inbound == httpRequest

        then: "close the channel"
        !channel.finish()
    }

    def "check that HTTP request with invalid OAuth2 token is replied with a UNAUTHORIZED response"() {
        given: "a channel with the ShiroHandler codec"
        EmbeddedChannel channel = new EmbeddedChannel(new ShiroHandler(securityManager))

        and: "an HTTP request to a fake REST API"
        FullHttpRequest httpRequest = new DefaultFullHttpRequest(HTTP_1_1, GET, "/users/johndoe")
        httpRequest.headers().set(AUTHORIZATION, ShiroHandler.BEARER + "11111111")

        when:
        channel.writeInbound(httpRequest)

        then: "the HTTP request is replied with an UNAUTHORIZED response"
        !channel.readInbound()

        when:
        def response = channel.readOutbound()

        then:
        response != null
        response instanceof HttpResponse
        ((HttpResponse) response).status == HttpResponseStatus.UNAUTHORIZED

        then: "close the channel"
        !channel.finish()
    }

    def "check that HTTP request with OAuth2 token is properly authenticated"() {
        given: "a channel with the ShiroHandler codec"
        EmbeddedChannel channel = new EmbeddedChannel(new ShiroHandler(securityManager))

        and: "an HTTP request to a fake REST API"
        FullHttpRequest httpRequest = new DefaultFullHttpRequest(HTTP_1_1, GET, "/users/johndoe")
        httpRequest.headers().set(AUTHORIZATION, ShiroHandler.BEARER + VALID_OAUTH2_TOKEN)

        when:
        def written = channel.writeInbound(httpRequest)
        Subject subject = channel.attr(ShiroHandler.ATTR_SUBJECT).get()

        then: "the HTTP request is authenticated"
        written
        subject != null
        subject.isAuthenticated()

        and: "given to the next handler in the pipeline"
        def inbound = channel.readInbound()
        inbound != null
        inbound == httpRequest

        then: "close the channel"
        !channel.finish()
    }

    def "check that HTTP request with OAuth2 token for locked account is replied with a UNAUTHORIZED response"() {
        given: "a channel with the ShiroHandler codec"
        EmbeddedChannel channel = new EmbeddedChannel(new ShiroHandler(securityManager))

        and: "an HTTP request to a fake REST API"
        FullHttpRequest httpRequest = new DefaultFullHttpRequest(HTTP_1_1, GET, "/users/johndoe")
        httpRequest.headers().set(AUTHORIZATION, ShiroHandler.BEARER + LOCKED_OAUTH2_TOKEN)

        when:
        channel.writeInbound(httpRequest)
        Subject subject = channel.attr(ShiroHandler.ATTR_SUBJECT).get()

        then: "the request is refused with an UNAUTHORIZED response"
        !channel.readInbound()
        def response = channel.readOutbound()

        then:
        response != null
        response instanceof HttpResponse
        ((HttpResponse) response).status == HttpResponseStatus.UNAUTHORIZED

        then: "close the channel"
        !channel.finish()
    }


}
