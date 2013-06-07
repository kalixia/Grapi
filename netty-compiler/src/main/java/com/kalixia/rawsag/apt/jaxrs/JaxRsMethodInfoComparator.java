package com.kalixia.rawsag.apt.jaxrs;

import io.netty.handler.codec.http.HttpMethod;
import java.util.Comparator;

class JaxRsMethodInfoComparator implements Comparator<JaxRsMethodInfo> {
    private final UriTemplatePrecedenceComparator templateComparator = new UriTemplatePrecedenceComparator();

    @Override
    public int compare(JaxRsMethodInfo method1, JaxRsMethodInfo method2) {
        // compare verbs first
        HttpMethod verb1 = HttpMethod.valueOf(method1.getVerb());
        HttpMethod verb2 = HttpMethod.valueOf(method2.getVerb());
        if (!verb1.equals(verb2)) {
            return verb1.compareTo(verb2);
        }

        // finally compare URI templates
        return templateComparator.compare(method1.getUriTemplate(), method2.getUriTemplate());
    }

}
