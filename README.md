# RaWSAG: REST and WebSockets APIs Generator

RaWSAG is a Java source code generator based on APT (Javac plugin).

RaWSAG analyzes your JAX-RS source code and generate some code in order to expose your JAX-RS resources through
[Netty](http://netty.io).

Additionally, RaWSAG can generate a [Dagger](https://github.com/square/dagger) module in order to simplify even more
the use of the generated code.

Finally if your JAX-RS resources return some [RxJava](https://github.com/Netflix/RxJava) ``` Observable ``` then the
generated Netty code with use HTTP Transfer-Encoding and emit an HTTP chunk for each of data element.


## How To Use?

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
``