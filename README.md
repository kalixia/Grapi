# Grapi: Generated REST API

Grapi is a Java source code generator based on APT (Javac plugin).

Grapi analyzes your JAX-RS source code and generate some code in order to expose your JAX-RS resources through
[Netty](http://netty.io) 4.


## Introduction

Basically Grapi generates some optimized Java code for ease of use of JAX-RS resources through Netty.

There is no runtime dependency on any JAX-RS provider (*actually only a small one on Jersey UriTemplate class
which will be removed later on*) and Grapi is not a JAX-RS provider.

Grapi avoids the introspection *crap* and generates Java source code which will work according to the JAX-RS resources.
When some JAX-RS features aren't used, some Java code is dropped from the generated source code in order to both reduce
complexity of the generated source code and improve the performance and maintenance of code.

Additionally, Grapi can generate a [Dagger](https://github.com/square/dagger) module in order to simplify even more
the use of the generated code and also use [Metrics](http://metrics.codahale.com) in order to measure various timings
about your JAX-RS resources.


## Current Status

The project is in early stages. It's being successfully used and tested on other projects.

### Build Status

[![Build Status](https://travis-ci.org/kalixia/Grapi.svg?branch=master)](https://travis-ci.org/kalixia/Grapi)

### Working Features

| Feature                       | Description
|-------------------------------|----------------------------------------------------------------------------------------------------
| @Path                         | Full working with all HTTP verbs, URI templates and path precedence rules
| @QueryParam, @FormParam, @HeaderParam, @CookieParam | Proper extraction from HTTP requests
| @Produces                     | Correctly expose your resources with the proper content-type
| Data conversion               | Uses Jackson in order to convert objects to the appropriate data format (XML, JSon, etc.) -- both for incoming data and outgoing data
| JAX-RS Response               | Can partially cope with JAX-RS ``` Response ``` results.
| WebApplicationException       | Basic support.
| CORS                          | Netty CorsHandler can be properly used.
| Dagger                        | Generate a Dagger module simplifying even more the use of the generated code
| Metrics                       | Expose your JAX-RS resource calls as [Metrics](http://metrics.codahale.com)
| JSR-349 (Bean Validation 1.1) | If your JAX-RS resources are annotated with Bean Validation annotations, the handlers to check the parameters and result


### Current Limitations

They are though some few limitations worth knowing. These limitations will be taken care of and are only
missing features:

* the WS stack is highly experimental at the moment
* no support for UriInfo injection and UriBuilder
* no support for @Consumes
* no support for @MatrixParam
* no support for processing Groovy sources (probably through AST transformations)


## Setup

The project is available from a [Bintray](https://bintray.com/kalixia/Grapi) repository:
```xml
<repository>
    <id>grapi</id>
    <name>Grapi</name>
    <url>http://dl.bintray.com/kalixia/Grapi</url>
    <releases><enabled>true</enabled></releases>
    <snapshots><enabled>false</enabled></snapshots>
</repository>
```

Then you simply need to add two dependencies to your project/module (where your JAX-RS source code is located):
```xml
<dependency>
    <groupId>com.kalixia.grapi</groupId>
    <artifactId>netty-codecs</artifactId>
    <version>0.4.4</version>
</dependency>

<dependency>
    <groupId>com.kalixia.grapi</groupId>
    <artifactId>netty-compiler</artifactId>
    <version>0.4.4</version>
</dependency>
```

## Configuration

There are currently three options available:

| Option  | Description
|---------|------------
| dagger  | Additionally generate a Dagger module, named ``` GeneratedJaxRsDaggerModule ```
| metrics | Additionally use [Metrics](http://metrics.codahale.com) for tracing JAX-RS resource method calls
| shiro   | Additionally analyze [Shiro](http://shiro.apache.org) annotations in JAX-RS resources and ensure they are met

In order to configure a Maven project, you can use something like this:
```xml
<plugin>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.1</version>
    <configuration>
        <source>1.7</source>
        <target>1.7</target>
        <encoding>UTF-8</encoding>
        <compilerArgs>
            <compilerArg>-Adagger=true</compilerArg>
            <compilerArg>-Ametrics=true</compilerArg>
            <compilerArg>-Ashiro=true</compilerArg>
        </compilerArgs>
    </configuration>
</plugin>
```

When you run ``` mvn compile ``` you'll find the generated source code in ``` target/generated-sources/annotations ```.
Each JAX-RS resource's method generates a Java class. There is also one ``` GeneratedJaxRsModuleHandler ``` created
which references all the generated classes.


## Usage

### Netty pipeline

Netty uses a ``` ChannelInitializer ``` in order to setup the *pipeline*.

You can create your own one based on the following code.
This will allow you JAX-RS resources to be reached both by HTTP requests and via WebSockets.

```java
public class ApiServerChannelInitializer extends ChannelInitializer<SocketChannel> {
    private final ObjectMapper objectMapper;
    private final ChannelHandler apiProtocolSwitcher;
    private final ShiroHandler shiroHandler;
    private final GeneratedJaxRsModuleHandler jaxRsHandlers;
    private static final ChannelHandler apiRequestLogger = new LoggingHandler(RESTCodec.class, LogLevel.DEBUG);

    @Inject
    public ApiServerChannelInitializer(ObjectMapper objectMapper,
                                       ApiProtocolSwitcher apiProtocolSwitcher,
                                       SecurityManager securityManager,
                                       GeneratedJaxRsModuleHandler jaxRsModuleHandler) {
        this.apiProtocolSwitcher = apiProtocolSwitcher;
        this.jaxRsHandlers =  jaxRsModuleHandler;
        this.shiroHandler = new ShiroHandler(securityManager);
        SimpleModule nettyModule = new SimpleModule("Netty", PackageVersion.VERSION);
        nettyModule.addSerializer(new ByteBufSerializer());
        objectMapper.registerModule(nettyModule);
    }

    @Override
    public void initChannel(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();

        pipeline.addLast("http-request-decoder", new HttpRequestDecoder());
        pipeline.addLast("http-response-encoder", new HttpResponseEncoder());
        pipeline.addLast("http-object-aggregator", new HttpObjectAggregator(1048576));
        pipeline.addLast("shiro", shiroHandler);

        // Alters the pipeline depending on either REST or WebSockets requests
        pipeline.addLast("api-protocol-switcher", apiProtocolSwitcher);

        // Logging handlers for API requests
        pipeline.addLast("api-request-logger", apiRequestLogger);

        // JAX-RS handlers
        pipeline.addLast("jax-rs-handlers", jaxRsHandlers);
    }
}
```

### Dagger

Grapi will generate a class named ```GeneratedJaxRsDaggerModule ```. You have two different ways to use this module:

1. reference the generated Dagger module from another module of your own,
2. create the ``` ObjectGraph ``` with this module (and eventually other ones too).

For option 1, you can use the ``` @Module ``` annotation like so:
```java
@Module(
    injects = Main.class,
    includes = { GeneratedJaxRsDaggerModule.class }
)
```

For option 2, you can create your Dagger graph like so:
```java
ObjectGraph objectGraph = ObjectGraph.create(new GeneratedJaxRsDaggerModule());
```

### Shiro

When enabled, Grapi will extract OAuth2 bearer tokens and authenticate the user via Shiro and Grapi's classes.
