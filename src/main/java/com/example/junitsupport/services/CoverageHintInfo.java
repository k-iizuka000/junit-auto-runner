package com.example.junitsupport.services;

import java.util.List;

public class CoverageHintInfo {
    private final String methodPattern;
    private final List<Integer> missingLines;

    public CoverageHintInfo(String methodPattern, List<Integer> missingLines) {
        this.methodPattern = methodPattern;
        this.missingLines = missingLines;
    }

    public String getMethodPattern() { return methodPattern; }
    public List<Integer> getMissingLines() { return missingLines; }
}
