package com.kalixia.grapi.apt.jaxrs.model;

import javax.lang.model.element.Element;
import javax.ws.rs.QueryParam;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JaxRsMethodInfo {
    private final Element element;
    private final String verb;
    private final String uriTemplate;
    private final String methodName;
    private final String returnType;
    private final List<JaxRsParamInfo> parameters;
    private final String[] produces;
    private final List<Annotation> shiroAnnotations;

    public JaxRsMethodInfo(Element element, String verb, String uriTemplate, String methodName, String returnType,
                    List<JaxRsParamInfo> parameters, String[] produces, List<Annotation> shiroAnnotations) {
        this.element = element;
        this.verb = verb;
        this.uriTemplate = uriTemplate;
        this.methodName = methodName;
        this.returnType = returnType;
        this.parameters = new ArrayList<>(parameters);
        this.produces = Arrays.copyOf(produces, produces.length);
        this.shiroAnnotations = new ArrayList<>(shiroAnnotations);
    }

    public Element getElement() {
        return element;
    }

    public String getVerb() {
        return verb;
    }

    public String getUriTemplate() {
        return uriTemplate;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getReturnType() {
        return returnType;
    }

    public boolean hasReturnType() {
        return !"void".equals(returnType);
    }

    public List<JaxRsParamInfo> getParameters() {
        return new ArrayList<>(parameters);
    }

    public boolean hasParameters() {
        return parameters.size() > 0;
    }

    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    public boolean hasQueryParameters() {
        List<JaxRsParamInfo> methodInfoParameters = getParameters();
        boolean hasQueryParam = false;
        for (JaxRsParamInfo paramInfo : methodInfoParameters) {
            QueryParam queryParam = paramInfo.getElement().getAnnotation(QueryParam.class);
            if (queryParam != null) {
                hasQueryParam = true;
            }
        }
        return hasQueryParam;
    }

    public String[] getProduces() {
        return Arrays.copyOf(produces, produces.length);
    }

    public List<Annotation> getShiroAnnotations() {
        return new ArrayList<>(shiroAnnotations);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("JaxRsMethodInfo{");
        sb.append("element=").append(element);
        sb.append(", verb='").append(verb).append('\'');
        sb.append(", uriTemplate='").append(uriTemplate).append('\'');
        sb.append(", methodName='").append(methodName).append('\'');
        sb.append(", returnType='").append(returnType).append('\'');
        sb.append(", parameters=").append(parameters);
        sb.append('}');
        return sb.toString();
    }
}
