package com.kalixia.grapi.tests;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kalixia.grapi.GeneratedJaxRsModuleHandler;
import com.kalixia.grapi.codecs.ApiProtocolSwitcher;

import javax.inject.Inject;
import javax.validation.Validator;

public class DataHolder {
    private final GeneratedJaxRsModuleHandler jaxRsHandler;
    private final ApiProtocolSwitcher apiProtocolSwitcher;
    private final MetricRegistry metricRegistry;
    private final Validator validator;
    private final ObjectMapper objectMapper;

    @Inject
    public DataHolder(GeneratedJaxRsModuleHandler jaxRsHandler, ApiProtocolSwitcher apiProtocolSwitcher,
                      MetricRegistry metricRegistry, Validator validator, ObjectMapper objectMapper) {
        this.jaxRsHandler = jaxRsHandler;
        this.apiProtocolSwitcher = apiProtocolSwitcher;
        this.metricRegistry = metricRegistry;
        this.validator = validator;
        this.objectMapper = objectMapper;
    }

    public GeneratedJaxRsModuleHandler getJaxRsHandler() {
        return jaxRsHandler;
    }

    public ApiProtocolSwitcher getApiProtocolSwitcher() {
        return apiProtocolSwitcher;
    }

    public MetricRegistry getMetricRegistry() {
        return metricRegistry;
    }

    public Validator getValidator() {
        return validator;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
