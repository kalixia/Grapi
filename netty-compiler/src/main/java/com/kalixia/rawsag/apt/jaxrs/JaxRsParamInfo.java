package com.kalixia.rawsag.apt.jaxrs;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.util.List;

class JaxRsParamInfo {
    private final String name;
    private final TypeMirror type;
    private final VariableElement element;

    JaxRsParamInfo(String name, TypeMirror type, VariableElement element) {
        this.name = name;
        this.type = type;
        this.element = element;
    }

    String getName() {
        return name;
    }

    TypeMirror getType() {
        return type;
    }

    VariableElement getElement() {
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
