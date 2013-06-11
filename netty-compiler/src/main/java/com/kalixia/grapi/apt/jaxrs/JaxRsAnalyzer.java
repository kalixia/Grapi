package com.kalixia.grapi.apt.jaxrs;

import com.kalixia.grapi.apt.jaxrs.model.JaxRsMethodInfo;
import com.kalixia.grapi.apt.jaxrs.model.JaxRsParamInfo;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class JaxRsAnalyzer {

    String extractVerb(Element elem) {
        Annotation annotation;

        // check for GET method
        annotation = elem.getAnnotation(GET.class);
        if (annotation != null)
            return HttpMethod.GET;

        // check for POST method
        annotation = elem.getAnnotation(POST.class);
        if (annotation != null)
            return HttpMethod.POST;

        // check for PUT method
        annotation = elem.getAnnotation(PUT.class);
        if (annotation != null)
            return HttpMethod.PUT;

        // check for DELETE method
        annotation = elem.getAnnotation(DELETE.class);
        if (annotation != null)
            return HttpMethod.DELETE;

        // check for HEAD method
        annotation = elem.getAnnotation(HEAD.class);
        if (annotation != null)
            return HttpMethod.HEAD;

        // check for OPTIONS method
        annotation = elem.getAnnotation(OPTIONS.class);
        if (annotation != null)
            return HttpMethod.OPTIONS;

        return null;
    }

    String extractUriTemplate(Element resource, Element element) {
        Path resourcePath = resource.getAnnotation(Path.class);
        Path elementPath = element.getAnnotation(Path.class);
        if (resourcePath == null) {
            return elementPath == null ? "" : elementPath.value();
        } else {
            if (elementPath == null) {
                return resourcePath.value();
            } else {
                String uriTemplate = resourcePath.value() + '/' + elementPath.value();
                return uriTemplate.replace("//", "/");
            }
        }
    }

    List<JaxRsParamInfo> extractParameters(ExecutableElement method) {
        List<? extends VariableElement> parameters = method.getParameters();
        List<JaxRsParamInfo> parametersInfo = new ArrayList<>();
        for (VariableElement parameter : parameters) {
            String name = parameter.getSimpleName().toString();
            TypeMirror type = parameter.asType();
            JaxRsParamInfo paramInfo = new JaxRsParamInfo(name, type, parameter);
            parametersInfo.add(paramInfo);
        }
        return parametersInfo;
    }

    /**
     * Build a map whose key is the name of the parameter of the JAX-RS resource and whose value is the @PathParam value
     * @param methodInfo the metamodel for the method
     * @return the map of associated parameters
     */
    Map<String, String> analyzePathParamAnnotations(JaxRsMethodInfo methodInfo) {
        Map<String, String> parametersToUriTemplateParameter = new HashMap<>();
        for (JaxRsParamInfo paramInfo : methodInfo.getParameters()) {
            PathParam pathParam = paramInfo.getElement().getAnnotation(PathParam.class);
            if (pathParam != null) {
                parametersToUriTemplateParameter.put(paramInfo.getName(), pathParam.value());
            }
        }
        return parametersToUriTemplateParameter;
    }

}
