package com.kalixia.grapi.tests;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.kalixia.grapi.GeneratedJaxRsDaggerModule;
import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;

@Module(injects = DataHolder.class, includes = GeneratedJaxRsDaggerModule.class)
@SuppressWarnings("PMD.DefaultPackage")
public class TestModule {

    @Singleton @Provides ObjectMapper provideObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        return mapper;
    }

}
