package com.kalixia.grapi

import spock.lang.Unroll

import static io.netty.handler.codec.http.HttpMethod.GET
import static io.netty.handler.codec.http.HttpResponseStatus.OK
import static org.hamcrest.CoreMatchers.equalTo
import static spock.util.matcher.HamcrestSupport.that

class CookiesResourceTest extends JaxRsResourceTest {

    @Unroll
    def "test cookie #cookie_name"() {
        expect:
        that response.status, equalTo(status)
        that response.content, equalTo(expected_response)

        where:
        url        | cookie_name    | cookie_value | status    | expected_response
        '/cookies' | 'my-cookie'    | 'test'       | OK        | 'test'

        response = requestWithCookies(url, GET, ["$cookie_name": cookie_value])
    }

}