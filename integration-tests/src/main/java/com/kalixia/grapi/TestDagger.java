package com.kalixia.grapi;

import dagger.ObjectGraph;

import javax.inject.Inject;
import javax.validation.Validator;
import javax.validation.executable.ExecutableValidator;

public class TestDagger {
    @Inject Validator validator;

    public void doIt() {
        ExecutableValidator executableValidator = validator.forExecutables();
    }

    public static void main(String[] args) {
        ObjectGraph graph = ObjectGraph.create(new TestModule());
        TestDagger test = graph.get(TestDagger.class);
        test.doIt();
    }
}
