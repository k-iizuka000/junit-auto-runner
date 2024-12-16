
package com.example.junitsupport.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class RuleParser {
    private String methodNameTemplate;
    private String javadocTemplate;
    private String junit4Warning;
    private Map<String, String> fallbackComments = new HashMap<>();
    private List<CoverageHintInfo> coverageHintList = new ArrayList<>();
    private List<OutputAssertionRules> outputAssertionRulesList = new ArrayList<>();

    public RuleParser(String rulesFilePath) {
        parseRules(rulesFilePath);
    }

    private void parseRules(String rulesFilePath) {
        try {
            String content = new String(Files.readAllBytes(Paths.get(rulesFilePath)));

            methodNameTemplate = extractBlock(content, "# method-name-template");
            javadocTemplate    = extractBlock(content, "# javadoc-template");
            
            junit4Warning = extractSingleLine(content, "# junit4-warning");
            
            // fallback-comments抽出
            fallbackComments.put("conditions-empty", extractKeyValue(content, "conditions-empty"));
            fallbackComments.put("results-empty", extractKeyValue(content, "results-empty"));
            fallbackComments.put("placeholder-unresolved", extractKeyValue(content, "placeholder-unresolved"));
            fallbackComments.put("method-conflict", extractKeyValue(content, "method-conflict"));
            fallbackComments.put("class-missing", extractKeyValue(content, "class-missing"));
            fallbackComments.put("method-target-missing", extractKeyValue(content, "method-target-missing"));
            fallbackComments.put("field-assert-missing", extractKeyValue(content, "field-assert-missing"));

            // coverage-hint抽出
            // 形式:
            // # coverage-hint
            // - method: ${TARGET_METHOD_NAME}
            //   missingLines:
            //     - 120
            //     - 125
            extractCoverageHint(content);

            // output-assertion-rules抽出
            // 形式:
            // # output-assertion-rules
            // - method: ${TARGET_METHOD_NAME}
            //   fieldsToAssert:
            //     - userRole
            //     - loginState
            extractOutputAssertionRules(content);

        } catch (IOException e) {
            // ファイルなしならデフォルト
            methodNameTemplate = "test_${TARGET_METHOD_NAME}_scenario";
            StringBuilder sb = new StringBuilder();
            sb.append("/**");
            sb.append("* // TODO テストの概要を書いてください。");
            sb.append("*");
            sb.append("*<li>条件</li>");
            sb.append("*${TEST_CONDITIONS}");
            sb.append("*");
            sb.append("*<li>結果</li>");
            sb.append("*${TEST_RESULTS}");
            sb.append("*");
            sb.append("*/");


            javadocTemplate = sb.toString();
            junit4Warning = "// TODO: このテストはJUnit4を使用しています。JUnit5へ変更してください。";
            // fallbackはデフォルト値
            fallbackComments.put("conditions-empty", "// TODO 条件を特定できませんでした");
            fallbackComments.put("results-empty", "// TODO 結果を特定できませんでした");
            fallbackComments.put("placeholder-unresolved", "// TODO ここに適切な説明を記載してください");
            fallbackComments.put("method-conflict", "// TODO 同名メソッドが存在します。メソッド名を修正してください。");
            fallbackComments.put("class-missing", "// TODO: テストクラスが見つかりません。対象クラスと関連付けを確認してください。");
            fallbackComments.put("method-target-missing", "// TODO: 対象メソッドが見つかりません。メソッド名を修正してください。");
            fallbackComments.put("field-assert-missing", "// TODO: 'userRole'フィールドへのアサートが抜けています。適切なアサートを追加してください。");
        }
    }

    // 以下、簡易的な抽出メソッド群(詳細実装は省略)
    private String extractBlock(String content, String marker) {
        // marker行以降、次の#までを取得
        String[] lines = content.split("\n");
        boolean found = false;
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            if (line.trim().startsWith(marker)) {
                found = true;
                continue;
            }
            if (found) {
                if (line.trim().startsWith("#")) break;
                sb.append(line).append("\n");
            }
        }
        return sb.toString().trim();
    }

    private String extractSingleLine(String content, String marker) {
        String[] lines = content.split("\n");
        boolean found = false;
        for (String line : lines) {
            if (line.trim().startsWith(marker)) {
                found = true;
                continue;
            }
            if (found) {
                if (line.trim().startsWith("#")) break;
                if (!line.trim().isEmpty()) return line.trim();
            }
        }
        return "";
    }

    private String extractKeyValue(String content, String key) {
        // "key: value"形式を想定。fallback-commentsはこういう形式で定義されている前提
        // 例:
        // conditions-empty: // TODO 条件を特定できませんでした
        String[] lines = content.split("\n");
        for (String line : lines) {
            if (line.trim().startsWith(key + ":")) {
                return line.substring(line.indexOf(":")+1).trim();
            }
        }
        return "";
    }

    private void extractCoverageHint(String content) {
        // 簡易実装：coverage-hintセクション内のmethodとmissingLines取得
        // 実際は正規表現やYAMLパーサ利用を推奨
        if (!content.contains("# coverage-hint")) return;
        String block = extractBlock(content, "# coverage-hint");
        // 例:
        // - method: ${TARGET_METHOD_NAME}
        //   missingLines:
        //     - 120
        //     - 125
        String[] lines = block.split("\n");
        String methodNamePattern = "";
        List<Integer> linesList = new ArrayList<>();
        boolean inMissingLines = false;
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("- method:")) {
                methodNamePattern = line.replace("- method:", "").trim();
            } else if (line.startsWith("missingLines:")) {
                inMissingLines = true;
            } else if (inMissingLines && line.startsWith("-")) {
                try {
                    int val = Integer.parseInt(line.replace("-", "").trim());
                    linesList.add(val);
                } catch (NumberFormatException ignored) {}
            } else if (line.isEmpty()) {
                inMissingLines = false;
            }
        }
        if (!methodNamePattern.isEmpty() && !linesList.isEmpty()) {
            coverageHintList.add(new CoverageHintInfo(methodNamePattern, linesList));
        }
    }

    private void extractOutputAssertionRules(String content) {
        // # output-assertion-rules
        // - method: ${TARGET_METHOD_NAME}
        //   fieldsToAssert:
        //     - userRole
        //     - loginState
        if (!content.contains("# output-assertion-rules")) return;
        String block = extractBlock(content, "# output-assertion-rules");
        String[] lines = block.split("\n");
        String methodNamePattern = "";
        List<String> fields = new ArrayList<>();
        boolean inFields = false;
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("- method:")) {
                methodNamePattern = line.replace("- method:", "").trim();
            } else if (line.startsWith("fieldsToAssert:")) {
                inFields = true;
            } else if (inFields && line.startsWith("-")) {
                fields.add(line.replace("-", "").trim());
            } else if (line.isEmpty()) {
                inFields = false;
            }
        }
        if (!methodNamePattern.isEmpty() && !fields.isEmpty()) {
            outputAssertionRulesList.add(new OutputAssertionRules(methodNamePattern, fields));
        }
    }

    public String getMethodNameTemplate() { return methodNameTemplate; }
    public String getJavadocTemplate() { return javadocTemplate; }
    public String getJunit4Warning() { return junit4Warning; }
    public String getFallbackComment(String key) { return fallbackComments.getOrDefault(key, "// TODO 未定義"); }
    public List<CoverageHintInfo> getCoverageHintList() { return coverageHintList; }
    public List<OutputAssertionRules> getOutputAssertionRulesList() { return outputAssertionRulesList; }
}
