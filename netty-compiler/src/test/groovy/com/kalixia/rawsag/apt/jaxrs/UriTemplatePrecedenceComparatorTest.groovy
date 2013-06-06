package com.kalixia.rawsag.apt.jaxrs

import spock.lang.Unroll

class UriTemplatePrecedenceComparatorTest extends spock.lang.Specification {

    @Unroll
    def "test precedence of #uri_template1 vs #uri_template2"() {
        given:
        def comparator = new UriTemplatePrecedenceComparator()

        expect:
        comparator.compare(uri_template1, uri_template2) == result

        where:
        uri_template1         | uri_template2         || result
        "/nothing"            | "/nothing-but-longer" || -1
        "/nothing-but-longer" | "/nothing"            || 1
        "/nothing"            | "/nothing"            || 0
        "/something/abc/{d}"  | "/something/{a}/{b}"  || -1
        "/something/{a}/{b}"  | "/something/abc/{d}"  || 1
    }

}
