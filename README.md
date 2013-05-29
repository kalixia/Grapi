# RaWSAG: REST and WebSockets APIs Generator

RaWSAG is a Java source code generator based on APT (Javac plugin).

RaWSAG analyzes your JAX-RS source code and generate some code in order to expose your JAX-RS resources through
[Netty](http://netty.io) 4.

Additionally, RaWSAG can generate a [Dagger](https://github.com/square/dagger) module in order to simplify even more
the use of the generated code.

Finally if your JAX-RS resources return some [RxJava](https://github.com/Netflix/RxJava) ``` Observable ``` then the
generated Netty code with use HTTP Transfer-Encoding and emit an HTTP chunk for each of data element.


## Setup

First, you need to build the project as it is not *yet* available via a Maven repository:
```
./gradlew install
```

Then you simply need to add two dependencies to your project/module (where your JAX-RS source code is located):
```xml
<dependency>
    <groupId>com.kalixia.rawsag</groupId>
    <artifactId>netty-codecs</artifactId>
    <version>0.1-SNAPSHOT</version>
</dependency>

<dependency>
    <groupId>com.kalixia.rawsag</groupId>
    <artifactId>netty-compiler</artifactId>
    <version>0.1-SNAPSHOT</version>
</dependency>
```

## Configuration

There are currently two options available:

| Option | Description
|--------|------------
| dagger | Additionally generate a Dagger module, named ``` GeneratedJaxRsDaggerModule ```
| rxjava | Use HTTP chunks when a JAX-RS method returns an ``` Observable ```

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
            <compilerArg>-Arxjava=true</compilerArg>
        </compilerArgs>
    </configuration>
</plugin>
```

## Usage

### ChannelInitializer

Netty uses a ``` ChannelInitializer ``` in order to setup the *pipeline*.

You can create your own one based on the following code.
This will allow you JAX-RS resources to be reached both by HTTP requests and via WebSockets.

```java
public class ApiServerChannelInitializer extends ChannelInitializer<SocketChannel> {
    private final ObjectMapper objectMapper;
    private final ObservableEncoder rxjavaHandler;  // optional: no need if you don't use RxJava
    private final GeneratedJaxRsModuleHandler jaxRsHandlers;
    private static final ChannelHandler debugger = new MessageLoggingHandler(LogLevel.TRACE);
    private static final ChannelHandler apiRequestLogger = new MessageLoggingHandler(RESTCodec.class, LogLevel.DEBUG);

    @Inject
    public ApiServerChannelInitializer(ObjectMapper objectMapper, ObservableEncoder rxjavaHandler,
                                       GeneratedJaxRsModuleHandler jaxRsModuleHandler) {
        this.objectMapper = objectMapper;
        this.rxjavaHandler = rxjavaHandler;
        this.jaxRsHandlers =  jaxRsModuleHandler;
        SimpleModule nettyModule = new SimpleModule("Netty", PackageVersion.VERSION);
        nettyModule.addSerializer(new ByteBufSerializer());
        objectMapper.registerModule(nettyModule);
    }

    @Override
    public void initChannel(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();

        pipeline.addLast("http-request-decoder", new HttpRequestDecoder());
        pipeline.addLast("deflater", new HttpContentDecompressor());
        pipeline.addLast("http-object-aggregator", new HttpObjectAggregator(1048576));
        pipeline.addLast("http-response-encoder", new HttpResponseEncoder());
        pipeline.addLast("inflater", new HttpContentCompressor());

        // Alters the pipeline depending on either REST or WebSockets requests
        pipeline.addLast("api-protocol-switcher", new ApiProtocolSwitcher(objectMapper));
        pipeline.addLast("debugger", debugger);

        // Logging handlers for API requests
        pipeline.addLast("api-request-logger", apiRequestLogger);

        pipeline.addLast("rxjava-handler", rxjavaHandler);

        // JAX-RS handlers
        pipeline.addLast("jax-rs-handler", jaxRsHandlers);
    }
}
```

### Dagger

RaWSAG will generate a class named GeneratedJaxRsDaggerModule. You have two different ways to use this module:
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