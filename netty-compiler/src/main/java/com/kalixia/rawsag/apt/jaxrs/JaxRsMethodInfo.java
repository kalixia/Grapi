package com.kalixia.rawsag.apt.jaxrs;

import javax.lang.model.element.Element;
import java.util.List;

class JaxRsMethodInfo {
    private final Element element;
    private final String verb;
    private final String uriTemplate;
    private final String methodName;
    private final String returnType;
    private final List<JaxRsParamInfo> parameters;
    private final String[] produces;

    JaxRsMethodInfo(Element element, String verb, String uriTemplate, String methodName, String returnType,
                    List<JaxRsParamInfo> parameters, String[] produces) {
        this.element = element;
        this.verb = verb;
        this.uriTemplate = uriTemplate;
        this.methodName = methodName;
        this.returnType = returnType;
        this.parameters = parameters;
        this.produces = produces;
    }

    Element getElement() {
        return element;
    }

    String getVerb() {
        return verb;
    }

    String getUriTemplate() {
        return uriTemplate;
    }

    String getMethodName() {
        return methodName;
    }

    String getReturnType() {
        return returnType;
    }

    boolean hasReturnType() {
        return !"void".equals(returnType);
    }

    List<JaxRsParamInfo> getParameters() {
        return parameters;
    }

    boolean hasParameters() {
        return getParameters().size() > 0;
    }

    String[] getProduces() {
        return produces;
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
