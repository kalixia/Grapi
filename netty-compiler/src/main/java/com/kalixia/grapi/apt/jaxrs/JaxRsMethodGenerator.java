package com.kalixia.grapi.apt.jaxrs;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kalixia.grapi.apt.jaxrs.model.JaxRsMethodInfo;
import com.kalixia.grapi.apt.jaxrs.model.JaxRsParamInfo;
import com.kalixia.grapi.codecs.jaxrs.UriTemplateUtils;
import com.squareup.java.JavaWriter;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import javax.annotation.Generated;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.inject.Inject;
import javax.lang.model.element.PackageElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.squareup.java.JavaWriter.stringLiteral;
import static java.lang.reflect.Modifier.FINAL;
import static java.lang.reflect.Modifier.PRIVATE;
import static java.lang.reflect.Modifier.PUBLIC;
import static java.lang.reflect.Modifier.STATIC;

public class JaxRsMethodGenerator {
    private final Filer filer;
    private final Messager messager;
    private final boolean useDagger;
    private final boolean useMetrics;
    private final boolean useRxJava;
    private final JaxRsAnalyzer analyzer = new JaxRsAnalyzer();

    public JaxRsMethodGenerator(Filer filer, Messager messager, Map<String, String> options) {
        this.filer = filer;
        this.messager = messager;
        this.useDagger = options.containsKey(Options.DAGGER.getValue())
                && "true".equals(options.get(Options.DAGGER.getValue()));
        this.useMetrics = options.containsKey(Options.METRICS.getValue())
                && "true".equals(options.get(Options.METRICS.getValue()));
        this.useRxJava = options.containsKey(Options.RXJAVA.getValue())
                && "true".equals(options.get(Options.RXJAVA.getValue()));
    }

    public String generateHandlerClass(String resourceClassName, PackageElement resourcePackage,
                                        String uriTemplate, JaxRsMethodInfo method) {
        Writer handlerWriter = null;
        try {
            // TODO: only uppercase the first character
            String resourceFQN = resourcePackage.toString() + '.' + resourceClassName;
            String methodNameCamel = method.getMethodName()/*.toUpperCase()*/;
            String handlerClassName = String.format("%s_%s_Handler", resourceFQN, methodNameCamel);
            JavaFileObject handlerFile = filer.createSourceFile(handlerClassName);
            handlerWriter = handlerFile.openWriter();
            JavaWriter writer = new JavaWriter(handlerWriter);
            writer
                    .emitPackage(resourcePackage.toString())
                            // add imports
                    .emitImports("com.kalixia.grapi.ApiRequest")
                    .emitImports("com.kalixia.grapi.ApiResponse");
            if (useRxJava)
                writer.emitImports("com.kalixia.grapi.ObservableApiResponse");
            writer
                    .emitImports("com.kalixia.grapi.codecs.jaxrs.GeneratedJaxRsMethodHandler")
                    .emitImports("com.kalixia.grapi.codecs.jaxrs.UriTemplateUtils")
                    .emitImports(ObjectMapper.class.getName())
                    .emitImports(JsonMappingException.class.getName())
//                        .emitImports("io.netty.channel.ChannelHandler.Sharable")
                    .emitImports(Unpooled.class.getName())
                    .emitImports(Charset.class.getName())
                    .emitImports(HttpMethod.class.getName())
                    .emitImports(HttpResponseStatus.class.getName());
            if (useMetrics) {
                writer
                        .emitImports("com.codahale.metrics.Timer")
                        .emitImports("com.codahale.metrics.MetricRegistry")
                        .emitImports("com.codahale.metrics.annotation.Timed");
            }

            writer
                    .emitImports("org.slf4j.Logger")
                    .emitImports("org.slf4j.LoggerFactory")
                    // JAX-RS classes
                    .emitImports(Response.class.getName())
                    .emitImports(MediaType.class.getName())
                    .emitImports(WebApplicationException.class.getName())
                    .emitImports(MultivaluedMap.class.getName())
                    .emitImports(MultivaluedHashMap.class.getName())
                    // Bean Validation classes
                    .emitImports(Validator.class.getName())
                    .emitImports(ConstraintViolation.class.getName())
                    // JDK classes
                    .emitImports(Map.class.getName())
                    .emitImports(Set.class.getName())
                    .emitImports(Iterator.class.getName())
                    .emitImports(Method.class.getName());

            if (useDagger)
                writer.emitImports(Inject.class.getName());

            writer
                    .emitImports(Generated.class.getName())
                    .emitEmptyLine()
                            // begin class
                    .emitJavadoc(String.format("Netty handler for JAX-RS resource {@link %s#%s}.", resourceClassName,
                            method.getMethodName()))
                    .emitAnnotation(Generated.class.getSimpleName(), stringLiteral(StaticAnalysisCompiler.GENERATOR_NAME))
//                        .emitAnnotation("Sharable")
                    .beginType(handlerClassName, "class", PUBLIC | FINAL, null, "GeneratedJaxRsMethodHandler")
                            // add delegate to underlying JAX-RS resource
                    .emitJavadoc("Delegate for the JAX-RS resource")
                    .emitField(resourceClassName, "delegate", PRIVATE | FINAL)
                    .emitEmptyLine();

            writer
                    .emitField("ObjectMapper", "objectMapper", PRIVATE | FINAL)
                    .emitField("Validator", "validator", PRIVATE | FINAL)
                    .emitField("Method", "delegateMethod", PRIVATE | FINAL);

            if (useMetrics) {
                writer.emitField("Timer", "timer", PRIVATE | FINAL);
            }
            writer
                    .emitField("String", "URI_TEMPLATE", PRIVATE | STATIC | FINAL, stringLiteral(uriTemplate))
                    .emitField("Logger", "LOGGER", PRIVATE | STATIC | FINAL,
                            "LoggerFactory.getLogger(" + handlerClassName + ".class)");

            generateConstructor(writer, handlerClassName, resourceClassName, method);
            generateMatchesMethod(writer, method);
            generateHandleMethod(writer, method, resourceClassName);
            // end class
            writer.endType();

            return handlerClassName;
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

    private JavaWriter generateConstructor(JavaWriter writer, String handlerClassName,
                                           String resourceClassName, JaxRsMethodInfo method) throws IOException {
        writer.emitEmptyLine();

        if (useDagger)
            writer.emitAnnotation(Inject.class);

        List<String> parameters = new ArrayList<>();
        if (useDagger)
            parameters.addAll(Arrays.asList(resourceClassName, "delegate"));
        parameters.addAll(Arrays.asList("ObjectMapper", "objectMapper"));
        parameters.addAll(Arrays.asList("Validator", "validator"));
        if (useMetrics)
            parameters.addAll(Arrays.asList("MetricRegistry", "registry"));

        writer.beginMethod(null, handlerClassName, PUBLIC, parameters.toArray(new String[parameters.size()]));

        if (useDagger)
            writer.emitStatement("this.delegate = delegate");
        else
            writer.emitStatement("this.delegate = new %s()", resourceClassName);

        writer
                .emitStatement("this.objectMapper = objectMapper")
                .emitStatement("this.validator = validator");

        // create reflection method used by the validation API
        writer.beginControlFlow("try");
        if (method.hasParameters()) {
            StringBuilder builder = new StringBuilder();
            Iterator<JaxRsParamInfo> paramIterator = method.getParameters().iterator();
            while (paramIterator.hasNext()) {
                JaxRsParamInfo param = paramIterator.next();
                builder.append(param.getType().toString()).append(".class");
                if (paramIterator.hasNext())
                    builder.append(", ");
            }
            writer.emitStatement("this.delegateMethod = delegate.getClass().getMethod(%s, %s)",
                    stringLiteral(method.getMethodName()), builder.toString());
        } else {
            writer.emitStatement("this.delegateMethod = delegate.getClass().getMethod(%s)",
                            stringLiteral(method.getMethodName()));
        }
        writer
                .nextControlFlow("catch (NoSuchMethodException e)")
                .emitEndOfLineComment("should not happen as Grapi scanned the source code!")
                .emitStatement("throw new RuntimeException(%s)", stringLiteral("Can't find method through reflection"))
                .endControlFlow();

        if (useMetrics) {
//            writer.emitStatement("this.registry = registry");
            writer.emitStatement("this.timer = registry.timer(MetricRegistry.name(%s, \"%s\"))",
                    stringLiteral(resourceClassName), method.getMethodName());
        }

        return writer.endMethod();
    }

    private JavaWriter generateMatchesMethod(JavaWriter writer, JaxRsMethodInfo methodInfo)
            throws IOException {
        writer
                .emitEmptyLine()
                .emitAnnotation(Override.class)
                .beginMethod("boolean", "matches", PUBLIC, "ApiRequest", "request");

        // check against HTTP method
        writer.emitStatement("boolean verbMatches = HttpMethod.%s.equals(request.method())", methodInfo.getVerb());

        // check against URI template
        if (UriTemplateUtils.hasParameters(methodInfo.getUriTemplate()))
            writer.emitStatement("boolean uriMatches = UriTemplateUtils.extractParameters(URI_TEMPLATE, request.uri()).size() > 0");
        else
            writer.emitStatement("boolean uriMatches = %s.equals(request.uri()) || %s.equals(request.uri())",
                    stringLiteral(methodInfo.getUriTemplate()), stringLiteral(methodInfo.getUriTemplate() + "/"));

        // return result
        writer.emitStatement("return verbMatches && uriMatches");

        return writer.endMethod();
    }

    private JavaWriter generateHandleMethod(JavaWriter writer, JaxRsMethodInfo methodInfo, String resourceClassName)
            throws IOException {
        writer.emitEmptyLine();

        if (useMetrics)
            writer.emitAnnotation("Timed");         // the annotation is only for "documentation" purpose

        writer
                .emitAnnotation(Override.class)
                .beginMethod("ApiResponse", "handle", PUBLIC, "ApiRequest", "request");

        if (useMetrics) {
            // initialize Timer
            writer
                    .emitStatement("final Timer.Context context = timer.time()")
                    .beginControlFlow("try");
        }

        // analyze @PathParam annotations
        Map<String, String> parametersMap = analyzer.analyzePathParamAnnotations(methodInfo);

        writer.beginControlFlow("try");

        // check if JAX-RS resource method has parameters; if so extract them from URI
        if (methodInfo.hasParameters()) {
            writer.emitEndOfLineComment("Extract parameters from URI");
            writer.emitStatement("Map<String, String> parameters = UriTemplateUtils.extractParameters(URI_TEMPLATE, request.uri())");
            // extract each parameter
            for (JaxRsParamInfo parameter : methodInfo.getParameters()) {
                String uriTemplateParameter = parametersMap.get(parameter.getName());
                String parameterValueSource;
                if (uriTemplateParameter == null) {
                    // consider this is actually content to be converted to an object
                    parameterValueSource = "request.content().toString(Charset.defaultCharset())";
                } else {
                    // otherwise this is extracted parameterValueSource URI
                    parameterValueSource = String.format("parameters.get(\"%s\")", uriTemplateParameter);
                }

                TypeMirror type = parameter.getType();
                if (String.class.getName().equals(type.toString())) {
                    writer.emitStatement("String %s = %s",
                            parameter.getName(), parameterValueSource);
                } else if (type.toString().startsWith("java.lang")) {
                    String shortName = type.toString().substring(type.toString().lastIndexOf('.') + 1);
                    writer.emitStatement("%s %s = %s.parse%s(%s)",
                            shortName, parameter.getName(), shortName, shortName, parameterValueSource);
                } else if (type.getKind().isPrimitive()) {
                    char firstChar = type.toString().charAt(0);
                    String shortName = Character.toUpperCase(firstChar) + type.toString().substring(1);
                    writer.emitStatement("%s %s = %s.parse%s(%s)",
                            type, parameter.getName(), shortName, shortName, parameterValueSource);
                } else {
                    writer.emitStatement("%s %s = objectMapper.readValue(%s, %s.class)",
                            type, parameter.getName(), parameterValueSource, type);
                }
            }
        }

        // validate parameters
        if (methodInfo.hasParameters()) {
            StringBuilder builder = new StringBuilder();
            Iterator<JaxRsParamInfo> iterator = methodInfo.getParameters().iterator();
            while (iterator.hasNext()) {
                JaxRsParamInfo param = iterator.next();
                builder.append(param.getName());
                if (iterator.hasNext())
                    builder.append(", ");
            }
            writer.emitEndOfLineComment("Validate parameters");
            writer.emitStatement("Set<ConstraintViolation<%s>> violations = validator.forExecutables().validateParameters(delegate,%n" +
                    "delegateMethod, new Object[] { %s })", resourceClassName, builder.toString());
            writer
                    .beginControlFlow("if (!violations.isEmpty())")
                    .emitStatement("Iterator<ConstraintViolation<%s>> iterator = violations.iterator()", resourceClassName)
                    .emitStatement("StringBuilder builder = new StringBuilder()")
                    .beginControlFlow("while (iterator.hasNext())")
                    .emitStatement("ConstraintViolation<%s> violation = iterator.next()", resourceClassName)
                    .emitStatement("builder.append(\"Parameter \").append(violation.getMessage()).append('\\n')")
                    .endControlFlow()
                    .emitStatement(
                            "return new ApiResponse(request.id(), HttpResponseStatus.BAD_REQUEST," +
                                    " Unpooled.copiedBuffer(builder.toString().getBytes()), MediaType.TEXT_PLAIN)")
                    .endControlFlow();
        }

        // call JAX-RS resource method
        writer.emitEndOfLineComment("Call JAX-RS resource");
        if (methodInfo.hasParameters()) {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < methodInfo.getParameters().size(); i++) {
                JaxRsParamInfo paramInfo = methodInfo.getParameters().get(i);
                builder.append(paramInfo.getName());
                if (i + 1 < methodInfo.getParameters().size())
                    builder.append(", ");
            }
            if (methodInfo.hasReturnType()) {
                writer.emitStatement("%s result = delegate.%s(%s)",
                        methodInfo.getReturnType(), methodInfo.getMethodName(), builder.toString());
            } else {
                writer.emitStatement("delegate.%s(%s)", methodInfo.getMethodName(), builder.toString());
            }
        } else if (methodInfo.hasReturnType()) {
            writer.emitStatement("%s result = delegate.%s()", methodInfo.getReturnType(), methodInfo.getMethodName());
        } else {
            writer.emitStatement("delegate.%s()", methodInfo.getMethodName());
        }

        // validate result
        if (methodInfo.hasReturnType()) {
            writer.emitEndOfLineComment("Validate result returned");
            writer
                    .emitStatement("Set<ConstraintViolation<%s>> resultViolations = validator.forExecutables().validateReturnValue(delegate,%n" +
                            "delegateMethod, result)", resourceClassName)
                    .beginControlFlow("if (!resultViolations.isEmpty())")
                                    .emitStatement("Iterator<ConstraintViolation<%s>> iterator = resultViolations.iterator()", resourceClassName)
                                    .emitStatement("StringBuilder builder = new StringBuilder()")
                                    .beginControlFlow("while (iterator.hasNext())")
                                    .emitStatement("ConstraintViolation<%s> violation = iterator.next()", resourceClassName)
                                    .emitStatement("builder.append(\"Result \").append(violation.getMessage()).append('\\n')")
                                    .endControlFlow()
                                    .emitStatement(
                                            "return new ApiResponse(request.id(), HttpResponseStatus.BAD_REQUEST," +
                                                    " Unpooled.copiedBuffer(builder.toString().getBytes()), MediaType.TEXT_PLAIN)")
                                    .endControlFlow();
        }

        writer.emitEndOfLineComment("Build API response object");
        String produces = methodInfo.getProduces()[0];
        if (useRxJava && methodInfo.hasReturnType() && methodInfo.getReturnType().startsWith("rx.Observable")) {
            writer.emitStatement("return new ObservableApiResponse(request.id(), HttpResponseStatus.OK, result, %s)",
                    stringLiteral(produces));
        } else if (methodInfo.hasReturnType() && methodInfo.getReturnType().equals(Response.class.getName())) {
            writer.beginControlFlow("if (result.hasEntity())")
                    .emitStatement("byte[] content = objectMapper.writeValueAsBytes(result.getEntity())")
                    .emitStatement("return new ApiResponse(request.id(), " +
                            "HttpResponseStatus.valueOf(result.getStatus()), Unpooled.copiedBuffer(content), %s, " +
                            "result.getStringHeaders())", stringLiteral(produces))
                    .nextControlFlow("else")
                    .emitStatement("return new ApiResponse(request.id(), " +
                            "HttpResponseStatus.valueOf(result.getStatus()), Unpooled.EMPTY_BUFFER, %s, " +
                            "result.getStringHeaders())", stringLiteral(produces))
                    .endControlFlow();
        } else if (methodInfo.hasReturnType()) {            // convert result only if there is one
            writer.emitStatement("byte[] content = objectMapper.writeValueAsBytes(result)")
                    .emitStatement("return new ApiResponse(request.id(), HttpResponseStatus.OK, " +
                            "Unpooled.wrappedBuffer(content), %s)", stringLiteral(produces));
        } else {
            writer.emitStatement("return new ApiResponse(request.id(), HttpResponseStatus.NO_CONTENT, " +
                    "Unpooled.EMPTY_BUFFER, %s)", stringLiteral(produces));
        }

        if (methodInfo.hasReturnType() || methodInfo.hasParameters())
            writer.nextControlFlow("catch (IllegalArgumentException|JsonMappingException e)");
        else
            writer.nextControlFlow("catch (IllegalArgumentException e)");
        writer
                    .emitStatement("return new ApiResponse(request.id(), HttpResponseStatus.BAD_REQUEST, " +
                            "Unpooled.copiedBuffer(e.getMessage().getBytes()), MediaType.TEXT_PLAIN)")
                .nextControlFlow("catch (WebApplicationException e)")
                    .emitStatement("Response response = e.getResponse()")
                    .emitStatement("return new ApiResponse(request.id(), HttpResponseStatus.valueOf(response.getStatus()), " +
                                            "Unpooled.copiedBuffer(e.getMessage().getBytes()), MediaType.TEXT_PLAIN)")
                .nextControlFlow("catch (Exception e)")
                    .emitStatement("e.printStackTrace()")
                    .beginControlFlow("if (e.getMessage() != null)")
                        .emitStatement("return new ApiResponse(request.id(), HttpResponseStatus.INTERNAL_SERVER_ERROR, " +
                                "Unpooled.copiedBuffer(e.getMessage().getBytes()), MediaType.TEXT_PLAIN)")
                    .nextControlFlow("else")
                        .emitStatement("return new ApiResponse(request.id(), HttpResponseStatus.INTERNAL_SERVER_ERROR, " +
                                "Unpooled.copiedBuffer(e.toString().getBytes()), MediaType.TEXT_PLAIN)")
                    .endControlFlow()
                .endControlFlow();

        if (useMetrics) {
            // end Timer measurement
            writer
                    .nextControlFlow("finally")
                    .emitStatement("context.stop()")
                    .endControlFlow();
        }

        return writer.endMethod();
    }
}
