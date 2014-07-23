package com.kalixia.grapi.apt.jaxrs.model;

import io.netty.handler.codec.http.HttpMethod;

import java.io.Serializable;
import java.util.Comparator;

public class JaxRsMethodInfoComparator implements Comparator<JaxRsMethodInfo>, Serializable {
    private final UriTemplatePrecedenceComparator templateComparator = new UriTemplatePrecedenceComparator();

    @Override
    public int compare(JaxRsMethodInfo method1, JaxRsMethodInfo method2) {
        // compare verbs first
        HttpMethod verb1 = HttpMethod.valueOf(method1.getVerb());
        HttpMethod verb2 = HttpMethod.valueOf(method2.getVerb());
        // compare uri templates next
        return verb1.equals(verb2) ?
                templateComparator.compare(method1.getUriTemplate(), method2.getUriTemplate())
                : verb1.compareTo(verb2);
    }

}
