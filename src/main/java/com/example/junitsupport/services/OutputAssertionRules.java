package com.example.junitsupport.services;

import java.util.List;

public class OutputAssertionRules {
    private final String methodPattern;
    private final List<String> fieldsToAssert;

    public OutputAssertionRules(String methodPattern, List<String> fieldsToAssert) {
        this.methodPattern = methodPattern;
        this.fieldsToAssert = fieldsToAssert;
    }

    public String getMethodPattern() { return methodPattern; }
    public List<String> getFieldsToAssert() { return fieldsToAssert; }
}
