package com.kalixia.grapi.apt.jaxrs;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kalixia.grapi.ApiRequest;
import com.kalixia.grapi.ApiResponse;
import com.kalixia.grapi.ObservableApiResponse;
import com.kalixia.grapi.apt.jaxrs.model.JaxRsMethodInfo;
import com.kalixia.grapi.apt.jaxrs.model.JaxRsParamInfo;
import com.kalixia.grapi.codecs.jaxrs.GeneratedJaxRsMethodHandler;
import com.kalixia.grapi.codecs.jaxrs.UriTemplateUtils;
import com.squareup.javawriter.JavaWriter;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresGuest;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.apache.shiro.authz.annotation.RequiresUser;

import javax.annotation.Generated;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.inject.Inject;
import javax.lang.model.element.PackageElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.JavaFileObject;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.ws.rs.CookieParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.squareup.javawriter.JavaWriter.stringLiteral;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.tools.Diagnostic.Kind.ERROR;

@SuppressWarnings({"PMD.DataflowAnomalyAnalysis", "PMD.TooManyStaticImports"})
public class JaxRsMethodGenerator {
    private final Filer filer;
    private final Messager messager;
    private final boolean useDagger;
    private final boolean useMetrics;
    private final boolean useShiro;
    private final boolean useRxJava;
    private final JaxRsAnalyzer analyzer = new JaxRsAnalyzer();

    public JaxRsMethodGenerator(Filer filer, Messager messager, Map<String, String> options) {
        this.filer = filer;
        this.messager = messager;
        this.useDagger = options.containsKey(Options.DAGGER.getValue())
                && "true".equals(options.get(Options.DAGGER.getValue()));
        this.useMetrics = options.containsKey(Options.METRICS.getValue())
                && "true".equals(options.get(Options.METRICS.getValue()));
        this.useShiro = options.containsKey(Options.SHIRO.getValue())
                        && "true".equals(options.get(Options.SHIRO.getValue()));
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
                    .emitImports(ApiRequest.class)
                    .emitImports(ApiResponse.class);
            if (useRxJava) {
                writer.emitImports(ObservableApiResponse.class);
            }
            writer
                    .emitImports(GeneratedJaxRsMethodHandler.class)
                    .emitImports(UriTemplateUtils.class)
                    .emitImports(ObjectMapper.class)
                    .emitImports(JsonMappingException.class)
//                        .emitImports("io.netty.channel.ChannelHandler.Sharable")
                    .emitImports(ChannelHandlerContext.class)
                    .emitImports(Unpooled.class)
                    .emitImports(Charset.class)
                    .emitImports(HttpMethod.class)
                    .emitImports(HttpResponseStatus.class);
            if (useMetrics) {
                writer
                        .emitImports("com.codahale.metrics.Timer")
                        .emitImports("com.codahale.metrics.MetricRegistry")
                        .emitImports("com.codahale.metrics.annotation.Timed");
            }
            if (useShiro) {
                ShiroGenerator.generateImports(writer);
            }

            writer
                    .emitImports("org.slf4j.Logger")
                    .emitImports("org.slf4j.LoggerFactory")
                    // JAX-RS classes
                    .emitImports(Response.class)
                    .emitImports(MediaType.class)
                    .emitImports(WebApplicationException.class)
                    .emitImports(MultivaluedMap.class)
                    .emitImports(MultivaluedHashMap.class)
                    // Bean Validation classes
                    .emitImports(Validator.class)
                    .emitImports(ConstraintViolation.class)
                    // JDK classes
                    .emitImports(Map.class)
                    .emitImports(Set.class)
                    .emitImports(Iterator.class)
                    .emitImports(Method.class)
                    .emitImports(UnsupportedEncodingException.class);

            if (useDagger) {
                writer.emitImports(Inject.class);
            }

            writer
                    .emitImports(Generated.class)
                    .emitEmptyLine()
                            // begin class
                    .emitJavadoc(String.format("Netty handler for JAX-RS resource {@link %s#%s}.", resourceClassName,
                            method.getMethodName()))
                    .emitAnnotation(Generated.class.getSimpleName(), stringLiteral(StaticAnalysisCompiler.GENERATOR_NAME))
//                        .emitAnnotation("Sharable")
                    .beginType(handlerClassName, "class", EnumSet.of(PUBLIC, FINAL), null, "GeneratedJaxRsMethodHandler")
                            // add delegate to underlying JAX-RS resource
                    .emitJavadoc("Delegate for the JAX-RS resource")
                    .emitField(resourceClassName, "delegate", EnumSet.of(PRIVATE, FINAL))
                    .emitEmptyLine();

            writer
                    .emitField("ObjectMapper", "objectMapper", EnumSet.of(PRIVATE, FINAL))
                    .emitField("Validator", "validator", EnumSet.of(PRIVATE, FINAL))
                    .emitField("Method", "delegateMethod", EnumSet.of(PRIVATE, FINAL));

            if (useMetrics) {
                writer.emitField("Timer", "timer", EnumSet.of(PRIVATE, FINAL));
            }
            writer
                    .emitField("String", "URI_TEMPLATE", EnumSet.of(PRIVATE, STATIC, FINAL), stringLiteral(uriTemplate))
                    .emitField("Logger", "LOGGER", EnumSet.of(PRIVATE, STATIC, FINAL),
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
                    messager.printMessage(ERROR, "Can't close generated source file");
                }
            }
        }
    }

    private JavaWriter generateConstructor(JavaWriter writer, String handlerClassName,
                                           String resourceClassName, JaxRsMethodInfo method) throws IOException {
        writer.emitEmptyLine();

        List<String> parameters = new ArrayList<>();
        parameters.addAll(Arrays.asList("ObjectMapper", "objectMapper"));
        parameters.addAll(Arrays.asList("Validator", "validator"));
        if (useDagger) {
            writer.emitAnnotation(Inject.class);
            parameters.addAll(Arrays.asList(resourceClassName, "delegate"));
        }
        if (useMetrics) {
            parameters.addAll(Arrays.asList("MetricRegistry", "registry"));
        }

        writer.beginMethod(null, handlerClassName, EnumSet.of(PUBLIC), parameters.toArray(new String[parameters.size()]));

        if (useDagger) {
            writer.emitStatement("this.delegate = delegate");
        } else {
            writer.emitStatement("this.delegate = new %s()", resourceClassName);
        }

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
                if (paramIterator.hasNext()) {
                    builder.append(", ");
                }
            }
            writer.emitStatement("this.delegateMethod = delegate.getClass().getMethod(%s, %s)",
                    stringLiteral(method.getMethodName()), builder.toString());
        } else {
            writer.emitStatement("this.delegateMethod = delegate.getClass().getMethod(%s)",
                            stringLiteral(method.getMethodName()));
        }
        writer
                .nextControlFlow("catch (NoSuchMethodException e)")
                .emitSingleLineComment("should not happen as Grapi scanned the source code!")
                .emitStatement("throw new RuntimeException(\"Can't find method '%s.%s' through reflection\")",
                        resourceClassName, method.getMethodName())
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
                .beginMethod("boolean", "matches", EnumSet.of(PUBLIC), "ApiRequest", "request");

        // check against HTTP method
        writer.emitStatement("boolean verbMatches = HttpMethod.%s.equals(request.method())", methodInfo.getVerb());

        // check against URI template
        if (UriTemplateUtils.hasParameters(methodInfo.getUriTemplate())) {
            writer.emitStatement("boolean uriMatches = UriTemplateUtils.extractParameters(URI_TEMPLATE, request.uri()).size() > 0");
        } else {
            writer.emitStatement("boolean uriMatches = %s.equals(request.uri()) || %s.equals(request.uri())",
                    stringLiteral(methodInfo.getUriTemplate()), stringLiteral(methodInfo.getUriTemplate() + "/"));
        }

        // return result
        writer.emitStatement("return verbMatches && uriMatches");

        return writer.endMethod();
    }

    @SuppressWarnings({"PMD.EmptyCatchBlock", "PMD.OnlyOneReturn"})
    private JavaWriter generateHandleMethod(JavaWriter writer, JaxRsMethodInfo methodInfo, String resourceClassName)
            throws IOException {
        boolean conversionNeeded = false;

        writer.emitEmptyLine();

        if (useMetrics) {
            writer.emitAnnotation("Timed");         // the annotation is only for "documentation" purpose
        }

        writer
                .emitAnnotation(Override.class)
                .beginMethod("ApiResponse", "handle", EnumSet.of(PUBLIC),
                        "ApiRequest", "request",
                        "ChannelHandlerContext", "ctx"
                );

        if (useMetrics) {
            // initialize Timer
            writer
                    .emitStatement("final Timer.Context context = timer.time()")
                    .beginControlFlow("try");
        }

        writer.emitSingleLineComment("TODO: extract expected charset from the API request instead of using the default charset");
        writer.emitStatement("Charset charset = Charset.forName(\"UTF-8\")");

        if (useShiro) {
            List<Annotation> shiroAnnotations = methodInfo.getShiroAnnotations();
            if (shiroAnnotations != null && shiroAnnotations.size() > 0) {
                ShiroGenerator.beginSubject(writer);
                for (Annotation shiroAnnotation : shiroAnnotations) {
                    Class<? extends Annotation> annotationType = shiroAnnotation.annotationType();
                    if (annotationType.isAssignableFrom(RequiresPermissions.class)) {
                        ShiroGenerator.generateShiroCodeForRequiresPermissionsCheck(writer, (RequiresPermissions) shiroAnnotation);
                    } else if (annotationType.isAssignableFrom(RequiresRoles.class)) {
                        ShiroGenerator.generateShiroCodeForRequiresRolesCheck(writer, (RequiresRoles) shiroAnnotation);
                    } else if (annotationType.isAssignableFrom(RequiresGuest.class)) {
                        ShiroGenerator.generateShiroCodeForRequiresGuestCheck(writer, (RequiresGuest) shiroAnnotation);
                    } else if (annotationType.isAssignableFrom(RequiresUser.class)) {
                        ShiroGenerator.generateShiroCodeForRequiresUserCheck(writer, (RequiresUser) shiroAnnotation);
                    } else if (annotationType.isAssignableFrom(RequiresAuthentication.class)) {
                        ShiroGenerator.generateShiroCodeForRequiresAuthenticationCheck(writer, (RequiresAuthentication) shiroAnnotation);
                    } else {
                        messager.printMessage(ERROR, "Can't process annotation of type " + annotationType.getSimpleName());
                        return writer;
                    }
                }
                ShiroGenerator.endSubject(writer);
            }
        }

        // analyze @PathParam annotations
        Map<String, String> parametersMap = analyzer.analyzePathParamAnnotations(methodInfo);

        writer.beginControlFlow("try");

        // check if JAX-RS resource method has parameters; if so extract them from URI
        if (methodInfo.hasParameters()) {
            writer.emitSingleLineComment("Extract parameters from request");
            writer.emitStatement("Map<String, String> parameters = UriTemplateUtils.extractParameters(URI_TEMPLATE, request.uri())");
            // extract each parameter
            for (JaxRsParamInfo parameter : methodInfo.getParameters()) {
                String parameterValueSource;
                if (parameter.getElement().getAnnotation(FormParam.class) != null) {
                    FormParam formParam = parameter.getElement().getAnnotation(FormParam.class);
                    writer.emitSingleLineComment("Extract form param '%s'", formParam.value());
                    parameterValueSource = String.format("request.formParameter(\"%s\")", formParam.value());
                } else if (parameter.getElement().getAnnotation(QueryParam.class) != null) {
                    QueryParam queryParam = parameter.getElement().getAnnotation(QueryParam.class);
                    writer.emitSingleLineComment("Extract query param '%s'", queryParam.value());
                    parameterValueSource = String.format("request.queryParameter(\"%s\")", queryParam.value());
                } else if (parameter.getElement().getAnnotation(HeaderParam.class) != null) {
                    HeaderParam headerParam = parameter.getElement().getAnnotation(HeaderParam.class);
                    writer.emitSingleLineComment("Extract header param '%s'", headerParam.value());
                    parameterValueSource = String.format("request.headerParameter(\"%s\")", headerParam.value());
                } else if (parameter.getElement().getAnnotation(CookieParam.class) != null) {
                    CookieParam cookieParam = parameter.getElement().getAnnotation(CookieParam.class);
                    writer.emitSingleLineComment("Extract cookie param '%s'", cookieParam.value());
                    parameterValueSource = String.format("request.cookieParameter(\"%s\")", cookieParam.value());
                } else {
                    String uriTemplateParameter = parametersMap.get(parameter.getName());
                    if (uriTemplateParameter == null) {
                        // consider this is actually content to be converted to an object
                        parameterValueSource = "request.content().toString(Charset.forName(\"UTF-8\"))";
                    } else {
                        // otherwise this is extracted parameterValueSource URI
                        parameterValueSource = String.format("parameters.get(\"%s\")", uriTemplateParameter);
                    }
                }

                String typeClassName = parameter.getElement().asType().toString();
                Method typeValueOfMethod = null;
                Constructor<?> typeConstructorFromString = null;
                try {
                    Class<?> typeClass = Class.forName(typeClassName);
                    typeValueOfMethod = typeClass.getMethod("valueOf", String.class);
                    typeConstructorFromString = typeClass.getConstructor(String.class);
                } catch (Exception e) {
                    // ignore
                }

                if (typeClassName.startsWith("java.lang.")) {
                    typeClassName = typeClassName.substring("java.lang.".length());
                }
                TypeMirror type = parameter.getType();
                if (String.class.getSimpleName().equals(typeClassName)) {
                    writer.emitStatement("String %s = %s", parameter.getName(), parameterValueSource);
                } else if (type.getKind().isPrimitive()) {
                    char firstChar = type.toString().charAt(0);
                    String shortName = Character.toUpperCase(firstChar) + type.toString().substring(1);
                    switch (type.getKind()) {
                        case INT:
                            writer.emitStatement("%s %s = %s.parse%s(%s)", type, parameter.getName(),
                                    Integer.class.getSimpleName(), shortName,
                                    parameterValueSource);
                            break;
                        default:
                            writer.emitStatement("%s %s = %s.parse%s(%s)", type, parameter.getName(),
                                    shortName, shortName, parameterValueSource);
                    }
                } else if (typeValueOfMethod != null) {
                    writer.emitStatement("%s %s = %s.valueOf(%s)",
                            typeClassName, parameter.getName(), typeClassName, parameterValueSource);
                } else if (typeConstructorFromString != null) {
                    writer.emitStatement("%s %s = new %s(%s)",
                        typeClassName, parameter.getName(), typeClassName, parameterValueSource);
                } else {
                    conversionNeeded = true;
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
                if (iterator.hasNext()) {
                    builder.append(", ");
                }
            }
            writer.emitSingleLineComment("Validate parameters");
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
                                    " Unpooled.copiedBuffer(builder.toString(), charset), MediaType.TEXT_PLAIN)")
                    .endControlFlow();
        }

        // call JAX-RS resource method
        writer.emitSingleLineComment("Call JAX-RS resource");
        if (methodInfo.hasParameters()) {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < methodInfo.getParameters().size(); i++) {
                JaxRsParamInfo paramInfo = methodInfo.getParameters().get(i);
                builder.append(paramInfo.getName());
                if (i + 1 < methodInfo.getParameters().size()) {
                    builder.append(", ");
                }
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
            writer.emitSingleLineComment("Validate result returned");
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
                                                    " Unpooled.copiedBuffer(builder.toString(), charset), MediaType.TEXT_PLAIN)")
                                    .endControlFlow();
        }

        writer.emitSingleLineComment("Build API response object");
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
        } else if (String.class.getName().equals(methodInfo.getReturnType())) {
                    writer
                            .emitStatement("byte[] content = result == null ? new byte[] {} : result.getBytes(%s)",  stringLiteral("UTF-8"))
                            .emitStatement("return new ApiResponse(request.id(), HttpResponseStatus.OK, " +
                                    "Unpooled.wrappedBuffer(content), %s)", stringLiteral(produces));
        } else if (methodInfo.hasReturnType()) {            // convert result only if there is one
            conversionNeeded = true;
            writer.emitStatement("byte[] content = objectMapper.writeValueAsBytes(result)")
                    .emitStatement("return new ApiResponse(request.id(), HttpResponseStatus.OK, " +
                            "Unpooled.wrappedBuffer(content), %s)", stringLiteral(produces));
        } else {
            writer.emitStatement("return new ApiResponse(request.id(), HttpResponseStatus.NO_CONTENT, " +
                    "Unpooled.EMPTY_BUFFER, %s)", stringLiteral(produces));
        }

        // conversion is only needed if returnType is not string and if at least one parameter need Json conversion
        if (conversionNeeded) {
            writer.nextControlFlow("catch (IllegalArgumentException|JsonMappingException e)");
        } else {
            writer.nextControlFlow("catch (IllegalArgumentException e)");
        }
        writer
                    .emitStatement("LOGGER.error(\"Bad request\", e)")
                    .emitStatement("return new ApiResponse(request.id(), HttpResponseStatus.BAD_REQUEST, " +
                            "Unpooled.copiedBuffer(e.getMessage(), charset), MediaType.TEXT_PLAIN)")
                .nextControlFlow("catch (WebApplicationException e)")
                    .emitStatement("Response response = e.getResponse()")
                    .emitStatement("return new ApiResponse(request.id(), HttpResponseStatus.valueOf(response.getStatus()), " +
                                            "Unpooled.copiedBuffer(e.getMessage(), charset), MediaType.TEXT_PLAIN)")
                .nextControlFlow("catch (Exception e)")
                    .emitStatement("e.printStackTrace()")
                    .beginControlFlow("if (e.getMessage() != null)")
                        .emitStatement("return new ApiResponse(request.id(), HttpResponseStatus.INTERNAL_SERVER_ERROR, " +
                                "Unpooled.copiedBuffer(e.getMessage(), charset), MediaType.TEXT_PLAIN)")
                    .nextControlFlow("else")
                        .emitStatement("return new ApiResponse(request.id(), HttpResponseStatus.INTERNAL_SERVER_ERROR, " +
                                "Unpooled.copiedBuffer(e.toString(), charset), MediaType.TEXT_PLAIN)")
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
