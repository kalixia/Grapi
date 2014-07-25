package com.kalixia.grapi

import static io.netty.handler.codec.http.HttpResponseStatus.CREATED
import static org.hamcrest.CoreMatchers.equalTo
import static spock.util.matcher.HamcrestSupport.that

class ResponseResourceTest extends JaxRsResourceTest {

    def "test response made of JAX-RS Response instance"() {
        when:
        def response = request('/response')

        then:
        that status(response), equalTo(CREATED)
    }

}