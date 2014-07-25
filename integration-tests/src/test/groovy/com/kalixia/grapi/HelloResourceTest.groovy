package com.kalixia.grapi

import spock.lang.Unroll

import static io.netty.handler.codec.http.HttpResponseStatus.OK
import static org.hamcrest.CoreMatchers.equalTo
import static spock.util.matcher.HamcrestSupport.that

class HelloResourceTest extends JaxRsResourceTest {

    @Unroll
    def "test hello #description"() {
        expect:
        that status(response), equalTo(status)
        that content(response), equalTo(content)

        where:
        description                             | url               | status | content
        'anonymous user'                        | '/hello'          | OK     | 'Hello!'
        'anonymous user with superfluous slash' | '/hello/'         | OK     | 'Hello!'
        'John Doe'                              | '/hello/Doe/John' | OK     | 'Hello John Doe!'

        response = request(url)
    }

}