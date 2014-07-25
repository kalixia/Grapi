package com.kalixia.grapi

import static io.netty.handler.codec.http.HttpMethod.GET
import static io.netty.handler.codec.http.HttpResponseStatus.OK
import static org.hamcrest.CoreMatchers.equalTo
import static spock.util.matcher.HamcrestSupport.that

class HeadersResourceTest extends JaxRsResourceTest {

    def "test headers"() {
        expect:
        that status(response), equalTo(OK)
        that content(response), equalTo(expected_response)

        where:
        url        | header_param_name | header_param_value | expected_response
        '/headers' | 'X-CUSTOM-HEADER' | 'test'             | 'test'

        response = requestWithHeaders(url, GET, ["$header_param_name": header_param_value])
    }

}