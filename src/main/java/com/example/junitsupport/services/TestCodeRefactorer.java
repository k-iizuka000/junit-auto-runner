package com.example.junitsupport.services;

import com.example.junitsupport.utils.PlaceholderReplacer;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.javadoc.PsiDocComment;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class TestCodeRefactorer {

    private final RuleParser ruleParser;
    private final Project project;

    // conditionsで取得したフィールド名リスト
    private List<String> extractConditionsFields = new ArrayList<>();

    public TestCodeRefactorer(RuleParser ruleParser, Project project) {
        this.ruleParser = ruleParser;
        this.project = project;
    }

    public TestAnalysisResult analyzeAndRefactor(PsiFile psiFile) {
        TestAnalysisResult result = new TestAnalysisResult();
        PsiClass testClass = findTestClass(psiFile);
        if (testClass == null) {
            insertTodoComment(psiFile, ruleParser.getFallbackComment("class-missing"), result);
            return result;
        }

        for (PsiMethod method : testClass.getMethods()) {
            if (isTestMethod(method)) {
                String targetMethodName = extractTargetMethodName(method.getName());
                boolean targetFound = checkTargetMethodExists(targetMethodName);
                if (!targetFound) {
                    insertTodoComment(method, ruleParser.getFallbackComment("method-target-missing"), result);
                }

                List<String> conditions = extractConditions(method);
                ResultInfo resInfo = extractResultsInfo(method);
                List<String> results = resInfo.lines;
                boolean verifyFound = resInfo.verifyFound;

                if (conditions.isEmpty()) {
                    conditions.add(ruleParser.getFallbackComment("conditions-empty"));
                }
                if (results.isEmpty()) {
                    results.add(ruleParser.getFallbackComment("results-empty"));
                }

                boolean renamed = renameMethodIfPossible(method, ruleParser.getMethodNameTemplate().replace("${TARGET_METHOD_NAME}", targetMethodName));
                if (!renamed) {
                    insertTodoComment(method, ruleParser.getFallbackComment("method-conflict"), result);
                }

                updateMethodJavadoc(method, ruleParser.getJavadocTemplate(), conditions, results, result);

                checkAndInsertClassFieldTodo(method, targetMethodName, result, verifyFound, results);

                // coverage-hint対象外→削除済み
                // JUnit4警告削除済み

                handleUnresolvedPlaceholders(method, result);
            }
        }

        return result;
    }

    private void handleUnresolvedPlaceholders(PsiMethod method, TestAnalysisResult result) {
        String text = method.getContainingFile().getText();
        if (text.contains("${")) {
            insertTodoComment(method, ruleParser.getFallbackComment("placeholder-unresolved"), result);
        }
    }

    private boolean renameMethodIfPossible(PsiMethod method, String newName) {
        try {
            WriteCommandAction.runWriteCommandAction(project, () -> {
                method.setName(newName);
            });
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void checkAndInsertClassFieldTodo(PsiMethod method, String targetMethodName, TestAnalysisResult result, boolean verifyFound, List<String> resultsLines) {
        // conditionsで取得した全フィールドをresultsで確認
        for (String field : extractConditionsFields) {
            boolean asserted = false;
            for (String line : resultsLines) {
                if (line.contains(field)) {
                    asserted = true;
                    break;
                }
            }
            if (!asserted) {
                insertTodoComment(method, "// TODO: '" + field + "'フィールドへのアサートが抜けています。適切なアサートを追加してください。", result);
            }
        }
    }

    private void updateMethodJavadoc(PsiMethod method, String template, List<String> conditions, List<String> results, TestAnalysisResult result) {
        WriteCommandAction.runWriteCommandAction(project, () -> {
            PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);
            PsiDocComment oldComment = method.getDocComment();

            String joinedConditions = String.join("\n", conditions);
            String joinedResults = String.join("\n", results);

            // 新規Javadoc
            // 新しいフォーマット:
            // /**
            //  * // TODO テストの概要を書いてください。
            //  *
            //  * <p>条件</p>
            //  * <ul>
            //  *   <li>${TEST_CONDITIONS}</li>
            //  * </ul>
            //  *
            //  * <p>結果</p>
            //  * <ul>
            //  *   <li>${TEST_RESULTS}</li>
            //  * </ul>
            //  *
            //  */
            String newDocText = template
                    .replace("${TEST_CONDITIONS}", joinedConditions)
                    .replace("${TEST_RESULTS}", joinedResults);
            PsiDocComment newDocComment = factory.createDocCommentFromText(newDocText);

            if (oldComment != null) {
                // 既存Javadocあり：既存を残し、その直後にnewDocComment追加
                method.addAfter(newDocComment, oldComment);
            } else {
                // 既存Javadocなし
                method.addBefore(newDocComment, method.getFirstChild());
            }
        });
    }

    private static class ResultInfo {
        List<String> lines = new ArrayList<>();
        boolean verifyFound = false;
    }

    private ResultInfo extractResultsInfo(PsiMethod method) {
        ResultInfo info = new ResultInfo();
        PsiCodeBlock body = method.getBody();
        if (body == null) return info;

        for (PsiStatement stmt : body.getStatements()) {
            if (stmt instanceof PsiExpressionStatement) {
                PsiExpression expr = ((PsiExpressionStatement) stmt).getExpression();
                if (expr instanceof PsiMethodCallExpression) {
                    PsiMethodCallExpression call = (PsiMethodCallExpression) expr;
                    String name = getCalledMethodName(call);
                    if (name == null) continue;
                    if (name.equals("assertEquals")) {
                        handleAssertEquals(call, info.lines);
                    } else if (name.equals("assertTrue")) {
                        handleAssertTrue(call, info.lines);
                    } else if (name.equals("assertNotNull")) {
                        handleAssertNotNull(call, info.lines);
                    } else if (name.equals("verify")) {
                        info.verifyFound = true;
                        String text = exprToString(call);
                        info.lines.add(text + "：検証すること // TODO 確認する内容を追記してください");
                    }
                }
            }
        }
        return info;
    }

    private String exprToString(PsiExpression expr) {
        if (expr instanceof PsiLiteralExpression) {
            Object val = ((PsiLiteralExpression)expr).getValue();
            return val != null ? val.toString() : "null";
        } else if (expr instanceof PsiMethodCallExpression) {
            return ((PsiMethodCallExpression)expr).getMethodExpression().getReferenceName() + "(...)";
        }
        return expr.getText();
    }

    private void handleAssertEquals(PsiMethodCallExpression call, List<String> results) {
        PsiExpression[] args = call.getArgumentList().getExpressions();
        if (args.length == 2) {
            String expected = exprToString(args[0]);
            String actual = exprToString(args[1]);
            results.add(actual + " の値が " + expected + " と Equalsであること // TODO 値の説明を追記してください");
        } else {
            results.add(call.getText() + " // TODO 引数が2つでないassertEqualsは未対応");
        }
    }

    private void handleAssertTrue(PsiMethodCallExpression call, List<String> results) {
        PsiExpression[] args = call.getArgumentList().getExpressions();
        if (args.length == 1) {
            String condition = exprToString(args[0]);
            results.add(condition + "：trueであること // TODO 値の説明を追記してください");
        } else {
            results.add(call.getText() + " // TODO 引数数不正");
        }
    }

    private void handleAssertNotNull(PsiMethodCallExpression call, List<String> results) {
        PsiExpression[] args = call.getArgumentList().getExpressions();
        if (args.length == 1) {
            String val = exprToString(args[0]);
            results.add(val + "：nullでないこと // TODO 値の説明を追記してください");
        } else {
            results.add(call.getText() + " // TODO 引数数不正");
        }
    }

    private List<String> extractConditions(PsiMethod method) {
        List<String> conditions = new ArrayList<>();
        extractConditionsFields.clear();
        PsiCodeBlock body = method.getBody();
        if (body == null) return conditions;

        for (PsiStatement stmt : body.getStatements()) {
            if (stmt instanceof PsiExpressionStatement) {
                PsiExpression expr = ((PsiExpressionStatement) stmt).getExpression();
                if (expr instanceof PsiMethodCallExpression) {
                    PsiMethodCallExpression call = (PsiMethodCallExpression) expr;
                    String calledName = getCalledMethodName(call);
                    if (calledName != null && calledName.startsWith("set")) {
                        String prop = calledName.substring(3);
                        String propLower = prop.substring(0,1).toLowerCase() + prop.substring(1);
                        PsiExpression[] args = call.getArgumentList().getExpressions();
                        if (args.length == 1 && args[0] instanceof PsiLiteralExpression) {
                            Object val = ((PsiLiteralExpression) args[0]).getValue();
                            if (val instanceof String) {
                                conditions.add(propLower + "：\"" + val + "\"");
                                extractConditionsFields.add(propLower);
                            } else {
                                conditions.add(call.getText() + " // TODO 変換が難しいので原文出力");
                                extractConditionsFields.add(propLower);
                            }
                        } else {
                            conditions.add(call.getText() + " // TODO 複雑な引数");
                            extractConditionsFields.add(propLower);
                        }
                    }
                }
            }
        }

        return conditions;
    }

    @Nullable
    private PsiClass findTestClass(PsiFile psiFile) {
        if (!(psiFile instanceof PsiJavaFile)) return null;
        PsiClass[] classes = ((PsiJavaFile) psiFile).getClasses();
        return classes.length > 0 ? classes[0] : null;
    }

    private boolean isTestMethod(PsiMethod method) {
        return method.getModifierList().findAnnotation("org.junit.Test") != null
                || method.getModifierList().findAnnotation("org.junit.jupiter.api.Test") != null;
    }

    private String extractTargetMethodName(String testMethodName) {
        if (testMethodName.startsWith("test") && testMethodName.length() > 4) {
            String name = testMethodName.substring(4);
            return name.substring(0,1).toLowerCase() + name.substring(1);
        }
        return "";
    }

    private boolean checkTargetMethodExists(String targetMethodName) {
        // ダミーtrue
        return !targetMethodName.isEmpty();
    }

    private String getCalledMethodName(PsiMethodCallExpression call) {
        return call.getMethodExpression().getReferenceName();
    }

    private void insertTodoComment(PsiElement element, String comment, TestAnalysisResult result) {
        WriteCommandAction.runWriteCommandAction(project, () -> {
            PsiElementFactory factory = JavaPsiFacade.getInstance(project).getElementFactory();
            PsiComment psiComment = factory.createCommentFromText(comment, null);
            element.getParent().addBefore(psiComment, element);
            result.incrementTodoCount();
        });
    }
}
