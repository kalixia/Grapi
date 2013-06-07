package com.kalixia.rawsag.apt.jaxrs.model;

import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

public class JaxRsParamInfo {
    private final String name;
    private final TypeMirror type;
    private final VariableElement element;

    public JaxRsParamInfo(String name, TypeMirror type, VariableElement element) {
        this.name = name;
        this.type = type;
        this.element = element;
    }

    public String getName() {
        return name;
    }

    public TypeMirror getType() {
        return type;
    }

    public VariableElement getElement() {
        return element;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("JaxRsParamInfo{");
        sb.append("name='").append(name).append('\'');
        sb.append(", type=").append(type);
        sb.append(", element=").append(element);
        sb.append('}');
        return sb.toString();
    }
}
