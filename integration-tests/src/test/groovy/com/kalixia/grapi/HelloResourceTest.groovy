package com.kalixia.grapi

import com.kalixia.grapi.codecs.rest.RESTCodec
import com.kalixia.grapi.tests.DataHolder
import com.kalixia.grapi.tests.TestModule
import dagger.ObjectGraph
import groovy.util.logging.Slf4j
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelPipeline
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpResponse
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler
import io.reactivex.netty.RxNetty
import io.reactivex.netty.pipeline.PipelineConfigurator
import io.reactivex.netty.pipeline.PipelineConfigurators
import io.reactivex.netty.protocol.http.client.HttpClient
import io.reactivex.netty.protocol.http.client.HttpClientRequest
import io.reactivex.netty.protocol.http.server.HttpServer
import io.reactivex.netty.protocol.http.server.HttpServerRequest
import io.reactivex.netty.protocol.http.server.HttpServerResponse
import io.reactivex.netty.protocol.http.server.RequestHandler
import rx.Observable
import spock.lang.Shared
import spock.lang.Specification

import java.nio.charset.Charset

import static io.netty.handler.codec.http.HttpResponseStatus.OK

@Slf4j("LOGGER")
class HelloResourceTest extends Specification {
    @Shared
    Integer serverPort = 33333

    @Shared
    HttpServer<ApiRequest, ApiResponse> server;

    @Shared
    HttpClient httpClient = RxNetty.createHttpClient("localhost", serverPort)

    @Shared
    DataHolder dataHolder

    def setupSpec() {
        // setup dagger
        ObjectGraph objectGraph = ObjectGraph.create(new TestModule());
        dataHolder = objectGraph.get(DataHolder.class)
        // start Netty server serving 'HelloResource' JAX-RS resource generated code
        startServer()
    }

    def cleanupSpec() {
        // stop Netty server
        if (server != null)
            server.shutdown()
    }

    def "test hello anonymous user"() {
        given:
        def requestURL = '/hello'               // request sent to '/hello' (GET)

        when:
        def response = request(requestURL)
        def content = response
                .flatMap({ resp -> resp.getContent() })
                .map({ ByteBuf buffer -> buffer.toString(Charset.forName("UTF-8")) })
                .toBlockingObservable().single()
        def status = response.toBlockingObservable().first().status

        then:
        status == OK
        content == 'Hello!'
    }

    def "test hello anonymous user with superflous slash"() {
        given:
        def requestURL = '/hello/'              // request sent to '/hello' (GET)

        when:
        def response = request(requestURL)
        def content = response
                .flatMap({ resp -> resp.getContent() })
                .map({ ByteBuf buffer -> buffer.toString(Charset.forName("UTF-8")) })
                .toBlockingObservable().single()
        def status = response.toBlockingObservable().first().status

        then:
        status == OK
        content == 'Hello!'
    }


    def "test hello John Doe"() {
        given:
        def requestURL = '/hello/Doe/John'      // request sent to '/hello/Doe/John' (GET)

        when:
        def response = request(requestURL)
        def content = response
                .flatMap({ resp -> resp.getContent() })
                .map({ ByteBuf buffer -> buffer.toString(Charset.forName("UTF-8")) })
                .toBlockingObservable().single()
        def status = response.toBlockingObservable().first().status

        then:
        status == OK
        content == "Hello John Doe!"
    }

    def request(String requestURL) {
        return httpClient.submit(HttpClientRequest.createGet(requestURL))
    }

    def startServer() {
        PipelineConfigurator<?, ?> pipelineConfigurator =
                PipelineConfigurators.<ByteBuf, ByteBuf> httpServerConfigurator()
        PipelineConfigurator<?, ?> grapiPipelineConfigurator =
                new PipelineConfigurator<HttpRequest, HttpResponse>() {
            @Override
            void configureNewPipeline(ChannelPipeline pipeline) {
//                pipeline.addLast("shiro", new ShiroHandler(securityManager))
                pipeline.addLast("api-protocol-switcher", dataHolder.apiProtocolSwitcher)
                pipeline.addLast("api-request-logger", new LoggingHandler(RESTCodec.class, LogLevel.DEBUG))
                pipeline.addLast("jax-rs-jaxRsHandler", dataHolder.jaxRsHandler)
            }
        }
        pipelineConfigurator = PipelineConfigurators.composeConfigurators(pipelineConfigurator, grapiPipelineConfigurator)
        server = RxNetty.newHttpServerBuilder(serverPort, new RequestHandler<ApiRequest, ApiResponse>() {
            @Override
            Observable<Void> handle(HttpServerRequest<ApiRequest> req, HttpServerResponse<ApiResponse> resp) {
                return resp.close(false)
            }
        }).pipelineConfigurator(pipelineConfigurator).build();
        server.start()
    }

}