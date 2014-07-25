package com.kalixia.grapi

import com.fasterxml.jackson.core.type.TypeReference
import spock.lang.Shared
import spock.lang.Unroll

import static io.netty.handler.codec.http.HttpMethod.GET
import static io.netty.handler.codec.http.HttpMethod.POST
import static io.netty.handler.codec.http.HttpResponseStatus.CREATED
import static io.netty.handler.codec.http.HttpResponseStatus.OK
import static java.util.Collections.emptyList
import static java.util.Collections.singletonList
import static org.hamcrest.CoreMatchers.equalTo
import static spock.util.matcher.HamcrestSupport.that

class ObjectsResourceTest extends JaxRsResourceTest {
    @Shared def test = new ObjectsResource.Test(name: 'my-test')
    @Shared def TYPE_LIST_OF_TESTS = new TypeReference<List<ObjectsResource.Test>>() {}

    @Unroll("#description")
    def "test complete scenario around ObjectsResource"() {
        expect:
        that response.status, equalTo(status)
        that response.getJsonContent(resultClass), equalTo(result)

        where:
        description                                | url              | method | body | status  | resultClass        | result
        '1. retrieve tests count'                  | '/objects/count' | GET    | null | OK      | Integer.class      | 0
        '2. retrieve tests'                        | '/objects'       | GET    | null | OK      | List.class         | emptyList()
        '3. add a test'                            | '/objects'       | POST   | test | CREATED | String.class       | ""
        '4. retrieve tests count after test added' | '/objects/count' | GET    | null | OK      | Integer.class      | 1
        '5. retrieve tests after test added'       | '/objects'       | GET    | null | OK      | TYPE_LIST_OF_TESTS | singletonList(test)

        response = requestWithJacksonBody(url, method, body)
    }

}