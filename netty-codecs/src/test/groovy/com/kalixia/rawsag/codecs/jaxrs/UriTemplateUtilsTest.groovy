package com.kalixia.rawsag.codecs.jaxrs

import spock.lang.Unroll

class UriTemplateUtilsTest extends spock.lang.Specification {

    @Unroll
    def "test of parameters in uri template #uri_template"() {
        expect:
        UriTemplateUtils.hasParameters(uri_template) == result

        where:
        uri_template                     || result
        "/hello"                         || false
        "/echo/{message}"                || true
        "/users/{user}/devices/{device}" || true
    }

    @Unroll
    def "extraction of parameters from #uri with template #uri_template"() {
        given:
        def parameters = UriTemplateUtils.extractParameters(uri_template, uri)

        expect:
        parameters.size() == parametersMap.size()

        where:
        uri                                | uri_template                     || parametersMap
        "/hello"                           | "/hello"                         || []
        "/echo/test"                       | "/echo/{message}"                || [message: "test"]
        "/users/johndoe/devices/my_device" | "/users/{user}/devices/{device}" || [user: 'johndoe', device: 'my_device']
        "/jeje/devices/"                   | "/{username}/devices"            || [username: 'jeje']
    }

    @Unroll
    def "build uri #uri from #uri_template and parameters map"() {
        expect:
        uri == UriTemplateUtils.createURI(uri_template, uri_parameters)

        where:
        uri_template                     | uri_parameters                       || uri
        "/{username}/devices"            | [username: 'jeje']                   || "/jeje/devices"
        "/{username}/devices/{deviceID}" | [username: 'jeje', deviceID: '1234'] || "/jeje/devices/1234"
    }

    @Unroll
    def "build uri #uri from #uri_template and parameters list"() {
        expect:
        uri == UriTemplateUtils.createURI(uri_template, uri_parameters)

        where:
        uri_template                     | uri_parameters               || uri
        "/{username}/devices"            | ['jeje'] as String[]         || "/jeje/devices"
        "/{username}/devices/{deviceID}" | ['jeje', '1234'] as String[] || "/jeje/devices/1234"
    }


}