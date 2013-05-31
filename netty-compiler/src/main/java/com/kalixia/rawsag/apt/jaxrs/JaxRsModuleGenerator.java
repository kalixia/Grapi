package com.kalixia.rawsag.apt.jaxrs;

import com.squareup.java.JavaWriter;

import javax.annotation.Generated;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.inject.Inject;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.squareup.java.JavaWriter.stringLiteral;
import static java.lang.reflect.Modifier.PRIVATE;
import static java.lang.reflect.Modifier.PROTECTED;
import static java.lang.reflect.Modifier.PUBLIC;
import static java.lang.reflect.Modifier.FINAL;
import static java.lang.reflect.Modifier.STATIC;

public class JaxRsModuleGenerator {
    private final Filer filer;
    private final Messager messager;
    private final boolean useDagger;
    public static final String MODULE_HANDLER = "GeneratedJaxRsModuleHandler";

    public JaxRsModuleGenerator(Filer filer, Messager messager, Map<String, String> options) {
        this.filer = filer;
        this.messager = messager;
        this.useDagger = options.containsKey("dagger") && "true".equals(options.get("dagger"));
    }

    public void generateModuleClass(String destPackage, List<String> generatedHandlers) {
        Writer handlerWriter = null;
        try {
            // TODO: only uppercase the first character
            String handlerClassName = destPackage + '.' + MODULE_HANDLER;
            JavaFileObject handlerFile = filer.createSourceFile(handlerClassName);
            handlerWriter = handlerFile.openWriter();
            JavaWriter writer = new JavaWriter(handlerWriter);
            writer
                    .emitPackage(destPackage.toString())
                            // add imports
                    .emitImports("com.kalixia.rawsag.ApiRequest")
                    .emitImports("com.kalixia.rawsag.ApiResponse")
                    .emitImports("com.kalixia.rawsag.codecs.jaxrs.JaxRsPipeline")
                    .emitImports("com.kalixia.rawsag.codecs.jaxrs.GeneratedJaxRsMethodHandler")
                    .emitImports("io.netty.buffer.ByteBuf")
                    .emitImports("io.netty.buffer.MessageBuf")
                    .emitImports("io.netty.buffer.Unpooled")
                    .emitImports("io.netty.channel.ChannelHandlerContext")
                    .emitImports("io.netty.channel.ChannelHandler.Sharable")
                    .emitImports("io.netty.handler.codec.MessageToMessageDecoder")
                    .emitImports("io.netty.handler.codec.http.HttpResponseStatus")
                    .emitImports("com.fasterxml.jackson.databind.ObjectMapper")
                    .emitImports("org.slf4j.Logger")
                    .emitImports("org.slf4j.LoggerFactory")
                    .emitImports("java.nio.charset.Charset")
                    .emitImports("java.util.List")
                    .emitImports("java.util.Arrays")
                    .emitImports("javax.ws.rs.core.MediaType")
                    .emitImports(Generated.class.getName())
                    .emitEmptyLine()
                            // begin class
                    .emitJavadoc("Netty handler collections all JAX-RS resources.")
                    .emitAnnotation(Generated.class.getSimpleName(), stringLiteral(StaticAnalysisCompiler.GENERATOR_NAME))
                    .emitAnnotation("Sharable")
                    .beginType(handlerClassName, "class", PUBLIC | FINAL, "MessageToMessageDecoder<ApiRequest>", "JaxRsPipeline")
            // add set of handlers
                    .emitField("List<? extends GeneratedJaxRsMethodHandler>", "handlers", PRIVATE | FINAL)
                    .emitField("ByteBuf", "ERROR_WRONG_URL", PRIVATE | STATIC | FINAL, "Unpooled.copiedBuffer(\"Wrong URL\", Charset.forName(\"UTF-8\"))")
                    .emitField("ByteBuf", "ERROR_INTERNAL_ERROR", PRIVATE | STATIC | FINAL, "Unpooled.copiedBuffer(\"Unexpected error\", Charset.forName(\"UTF-8\"))")
                    .emitField("Logger", "LOGGER", PRIVATE | STATIC | FINAL, "LoggerFactory.getLogger(" + handlerClassName + ".class)")
            ;
            generateConstructor(writer, handlerClassName, generatedHandlers);
            generateDecodeMethod(writer);
            // end class
            writer.endType();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (handlerWriter != null) {
                try {
                    handlerWriter.close();
                } catch (IOException e) {
                    messager.printMessage(Diagnostic.Kind.ERROR, "Can't close generated source file");
                }
            }
        }
    }

    private JavaWriter generateConstructor(JavaWriter writer, String handlerClassName, List<String> generatedHandlers)
            throws IOException {
        writer.emitEmptyLine();

        if (!useDagger) {
            writer.beginMethod(null, handlerClassName, PUBLIC, "ObjectMapper", "objectMapper");
        } else {
            writer.emitAnnotation(Inject.class.getName());
            List<String> args = new ArrayList<>();
            args.add("ObjectMapper");
            args.add("objectMapper");
            for (int i = 1; i <= generatedHandlers.size(); i++) {
                args.add(generatedHandlers.get(i - 1));
                args.add(String.format("handler%d", i));
            }
            writer.beginMethod(null, handlerClassName, PUBLIC, args.toArray(new String[args.size()]));
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < generatedHandlers.size(); i++) {
            if (useDagger)
                builder.append(String.format("handler%d", i + 1));
            else
                builder.append(String.format("new %s(objectMapper)", generatedHandlers.get(i)));
            if (i + 1 < generatedHandlers.size())
                builder.append(",\n");
        }

        return writer.
                emitStatement("this.handlers = Arrays.asList(\n" + builder.toString() + "\n)")
                .endMethod();
    }

    private JavaWriter generateDecodeMethod(JavaWriter writer)
            throws IOException {
        return writer
                .emitEmptyLine()
                .emitAnnotation(Override.class)
                .beginMethod("void", "decode", PROTECTED,
                        "ChannelHandlerContext", "ctx", "ApiRequest", "request", "MessageBuf<Object>", "out")
                    .beginControlFlow("for (GeneratedJaxRsMethodHandler handler : handlers)")
                        .beginControlFlow("if (handler.matches(request))")
                            .beginControlFlow("try")
                                .emitStatement("ApiResponse response = handler.handle(request)")
                                .emitStatement("ctx.write(response)")
                                .emitStatement("return")
                            .nextControlFlow("catch (Exception e)")
                                .emitStatement("LOGGER.error(\"Can't invoke JAX-RS resource\", e)")
                                .emitStatement("ctx.write(new ApiResponse(request.id(), HttpResponseStatus.INTERNAL_SERVER_ERROR,\n" +
                                        "ERROR_INTERNAL_ERROR, MediaType.TEXT_PLAIN))")
                                .emitStatement("return")
                            .endControlFlow()
                        .endControlFlow()
                    .endControlFlow()
                    .emitEndOfLineComment("no matching handler found -- issue a 404 error")
                    .emitStatement("LOGGER.info(\"Could not locate a JAX-RS resource for path '{}' and method {}\", " +
                            "request.uri(), request.method());")
                    .emitEmptyLine()
                    .emitStatement("ctx.write(new ApiResponse(request.id(), HttpResponseStatus.NOT_FOUND, " +
                            "ERROR_WRONG_URL, MediaType.TEXT_PLAIN))")
                .endMethod();
    }

}
