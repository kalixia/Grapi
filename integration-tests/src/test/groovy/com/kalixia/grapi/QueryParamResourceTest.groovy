package com.kalixia.grapi

import spock.lang.Unroll

import static io.netty.handler.codec.http.HttpMethod.GET
import static io.netty.handler.codec.http.HttpResponseStatus.OK
import static org.hamcrest.CoreMatchers.equalTo
import static spock.util.matcher.HamcrestSupport.that

class QueryParamResourceTest extends JaxRsResourceTest {

    @Unroll
    def "test query with params for '#url'"() {
        expect:
        that response.status, equalTo(OK)
        that response.getJsonContent(resultClass), equalTo(expected_response)

        where:
        url                       | query_param_name | query_param_value | resultClass   | expected_response
        '/query'                  | 'message'        | 'test'            | String.class  | 'test'
        '/query/number'           | 'number'         | '123'             | Integer.class | 123
        '/query/number-primitive' | 'number'         | '456'             | Integer.class | 456

        response = requestWithQueryParams(url, GET, ["$query_param_name": query_param_value])
    }

}