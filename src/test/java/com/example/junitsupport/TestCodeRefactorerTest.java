//package com.example.junitsupport;
//
//import com.example.junitsupport.services.RuleParser;
//import com.example.junitsupport.services.TestAnalysisResult;
//import com.example.junitsupport.services.TestCodeRefactorer;
//import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
//import org.junit.Assert;
//
//import java.nio.file.Paths;
//
//public class TestCodeRefactorerTest extends LightJavaCodeInsightFixtureTestCase {
//
//    private TestCodeRefactorer refactorer;
//    private RuleParser parser;
//
//    @Override
//    protected void setUp() throws Exception {
//        super.setUp();
//        myFixture.setTestDataPath(getTestDataPath());
//
//        var project = getProject();
//        // rules.mdがプロジェクトルート直下にある想定
//        String rulesPath = Paths.get(project.getBasePath(), "rules.md").toString();
//        parser = new RuleParser(rulesPath);
//        refactorer = new TestCodeRefactorer(parser, project);
//
//        // TestedObjectをプロジェクトに配置
//        myFixture.addFileToProject("TestedObject.java", """
//            public class TestedObject {
//                private String mail;
//                public void setMail(String email) { this.mail = email; }
//                public String loginScenario(String input) { return input; }
//            }
//        """);
//    }
//
//    @Override
//    protected String getTestDataPath() {
//        return "src/test/testData"; // 実際にtestData使わないが要定義
//    }
//
//    private TestAnalysisResult runRefactorOn(String className, String code) {
//        var psiFile = myFixture.configureByText(className + ".java", code);
//        return refactorer.analyzeAndRefactor(psiFile);
//    }
//
//    /** CoverageHintのTODO発生ケース */
//    public void testCoverageHintYes() {
//        // ターゲットメソッド名が${TARGET_METHOD_NAME}と一致するように
//        // 例えばmethod: ${TARGET_METHOD_NAME}が coverage hint発生条件なら
//        // testScenario → scenarioにrenameされscenarioとpattern一致でTODO発生
//        // ここはルールに合わせてtargetMethodNameが一致するようにする
//        TestAnalysisResult result = runRefactorOn("CoverageHintYes", """
//            class CoverageHintYes {
//                @org.junit.Test
//                public void testScenario() { // "testScenario" → targetMethodName="scenario"
//                    TestedObject to = new TestedObject();
//                    to.setMail("x@example.com");
//                    String r = to.loginScenario("x@example.com");
//                    org.junit.Assert.assertEquals("x@example.com", r);
//                }
//            }
//        """);
//        String newText = myFixture.getFile().getText();
//        // Coverage hintが追加されているはず
//        Assert.assertTrue(newText.contains("Add coverage for line 120"));
//        Assert.assertTrue(newText.contains("Add coverage for line 125"));
//    }
//
//    /** CoverageHintのTODO不発ケース */
//    public void testCoverageHintNo() {
//        // method名をtestOtherにして、targetMethodName="other"
//        // rules.mdのcoverage-hintは${TARGET_METHOD_NAME}がscenarioでしか発生しない想定
//        TestAnalysisResult result = runRefactorOn("CoverageHintNo", """
//            class CoverageHintNo {
//                @org.junit.Test
//                public void testOther() {
//                    TestedObject to = new TestedObject();
//                    to.setMail("y@example.com");
//                    String r = to.loginScenario("y@example.com");
//                    org.junit.Assert.assertEquals("y@example.com", r);
//                }
//            }
//        """);
//        String newText = myFixture.getFile().getText();
//        Assert.assertFalse(newText.contains("Add coverage for line"));
//    }
//
//    /** OutputAssertionRules発生ケース */
//    public void testOutputAssertionYes() {
//        // これも同様、method名を合わせてfieldsToAssertが一致するようにする
//        // ここでは${TARGET_METHOD_NAME}とマッチするため testUserLogin → userLogin
//        // ルールでfieldsToAssert:userRoleがあるならTODO発生
//        TestAnalysisResult result = runRefactorOn("OutputAssertionYes", """
//            class OutputAssertionYes {
//                @org.junit.Test
//                public void testUserLogin() {
//                    TestedObject to = new TestedObject();
//                    to.setMail("z@example.com");
//                    String r = to.loginScenario("z@example.com");
//                    org.junit.Assert.assertEquals("z@example.com", r);
//                }
//            }
//        """);
//        String newText = myFixture.getFile().getText();
//        Assert.assertTrue(newText.contains("fieldへのアサートが抜けています"));
//    }
//
//    /** OutputAssertionRules不発ケース */
//    public void testOutputAssertionNo() {
//        // 不一致メソッド名でfieldsToAssertが発動しない
//        TestAnalysisResult result = runRefactorOn("OutputAssertionNo", """
//            class OutputAssertionNo {
//                @org.junit.Test
//                public void testSomethingElse() {
//                    TestedObject to = new TestedObject();
//                    to.setMail("noAssert@example.com");
//                    String r = to.loginScenario("noAssert@example.com");
//                    org.junit.Assert.assertEquals("noAssert@example.com", r);
//                }
//            }
//        """);
//        String newText = myFixture.getFile().getText();
//        Assert.assertFalse(newText.contains("fieldへのアサートが抜けています"));
//    }
//
//    /** JUnit4警告発生ケース */
//    public void testJUnit4WarningYes() {
//        // @org.junit.Test使用
//        TestAnalysisResult result = runRefactorOn("JUnit4Yes", """
//            class JUnit4Yes {
//                @org.junit.Test
//                public void testCase() {
//                    TestedObject to = new TestedObject();
//                    to.setMail("4@example.com");
//                    String r = to.loginScenario("4@example.com");
//                    org.junit.Assert.assertEquals("4@example.com", r);
//                }
//            }
//        """);
//        String newText = myFixture.getFile().getText();
//        Assert.assertTrue(newText.contains("JUnit4を使用しています"));
//    }
//
//    /** JUnit4警告不発ケース（JUnit5使用） */
//    public void testJUnit4WarningNo() {
//        TestAnalysisResult result = runRefactorOn("JUnit4No", """
//            import org.junit.jupiter.api.Test;
//            class JUnit4No {
//                @Test
//                public void testCase() {
//                    TestedObject to = new TestedObject();
//                    to.setMail("5@example.com");
//                    String r = to.loginScenario("5@example.com");
//                    org.junit.jupiter.api.Assertions.assertEquals("5@example.com", r);
//                }
//            }
//        """);
//        String newText = myFixture.getFile().getText();
//        Assert.assertFalse(newText.contains("JUnit4を使用しています"));
//    }
//
//    /** conditions-empty発生ケース（条件呼び出し無し） */
//    public void testConditionsEmptyYes() {
//        TestAnalysisResult result = runRefactorOn("CondEmptyYes", """
//            class CondEmptyYes {
//                @org.junit.Test
//                public void testNoCondition() {
//                    TestedObject to = new TestedObject();
//                    // no setMail call
//                    String r = to.loginScenario("cond@example.com");
//                    org.junit.Assert.assertEquals("cond@example.com", r);
//                }
//            }
//        """);
//        String newText = myFixture.getFile().getText();
//        Assert.assertTrue(newText.contains("TODO 条件を特定できませんでした"));
//    }
//
//    /** conditions-empty不発ケース（setMailあり） */
//    public void testConditionsEmptyNo() {
//        TestAnalysisResult result = runRefactorOn("CondEmptyNo", """
//            class CondEmptyNo {
//                @org.junit.Test
//                public void testWithCondition() {
//                    TestedObject to = new TestedObject();
//                    to.setMail("condok@example.com");
//                    String r = to.loginScenario("condok@example.com");
//                    org.junit.Assert.assertEquals("condok@example.com", r);
//                }
//            }
//        """);
//        String newText = myFixture.getFile().getText();
//        Assert.assertFalse(newText.contains("条件を特定できませんでした"));
//    }
//
//    /** results-empty発生ケース（アサーションなし） */
//    public void testResultsEmptyYes() {
//        TestAnalysisResult result = runRefactorOn("ResEmptyYes", """
//            class ResEmptyYes {
//                @org.junit.Test
//                public void testNoResults() {
//                    TestedObject to = new TestedObject();
//                    to.setMail("res@example.com");
//                    // no assert call
//                    String r = to.loginScenario("res@example.com");
//                }
//            }
//        """);
//        String newText = myFixture.getFile().getText();
//        Assert.assertTrue(newText.contains("TODO 結果を特定できませんでした"));
//    }
//
//    /** results-empty不発ケース（assertあり） */
//    public void testResultsEmptyNo() {
//        TestAnalysisResult result = runRefactorOn("ResEmptyNo", """
//            class ResEmptyNo {
//                @org.junit.Test
//                public void testWithResults() {
//                    TestedObject to = new TestedObject();
//                    to.setMail("resok@example.com");
//                    String r = to.loginScenario("resok@example.com");
//                    org.junit.Assert.assertEquals("resok@example.com", r);
//                }
//            }
//        """);
//        String newText = myFixture.getFile().getText();
//        Assert.assertFalse(newText.contains("結果を特定できませんでした"));
//    }
//
//    /** MethodConflict発生ケース（同名メソッド存在でrename失敗） */
//    public void testMethodConflictYes() {
//        // ここで同名メソッドを2つ定義し、renameできずにTODO発生させる
//        TestAnalysisResult result = runRefactorOn("MethodConflictYes", """
//            class MethodConflictYes {
//                @org.junit.Test
//                public void testDuplicate() {}
//                @org.junit.Test
//                public void testDuplicate() {
//                    TestedObject to = new TestedObject();
//                    to.setMail("dup@example.com");
//                    String r = to.loginScenario("dup@example.com");
//                    org.junit.Assert.assertEquals("dup@example.com", r);
//                }
//            }
//        """);
//        String newText = myFixture.getFile().getText();
//        Assert.assertTrue(newText.contains("同名メソッドが存在します。メソッド名を修正してください。"));
//    }
//
//    /** MethodConflict不発ケース（同名なし） */
//    public void testMethodConflictNo() {
//        TestAnalysisResult result = runRefactorOn("MethodConflictNo", """
//            class MethodConflictNo {
//                @org.junit.Test
//                public void testUnique() {
//                    TestedObject to = new TestedObject();
//                    to.setMail("unique@example.com");
//                    String r = to.loginScenario("unique@example.com");
//                    org.junit.Assert.assertEquals("unique@example.com", r);
//                }
//            }
//        """);
//        String newText = myFixture.getFile().getText();
//        Assert.assertFalse(newText.contains("同名メソッドが存在します"));
//    }
//
//    // Javadoc既存あり/なしテスト
//
//    /** 既存JavadocありでTODO概要挿入をスキップ（NoTODOシナリオ） */
//    public void testJavadocNoTodoScenario() {
//        // Javadocが既にあり、そこに条件・結果行を挿入するロジック
//        // これでTODO概要行なし
//        TestAnalysisResult result = runRefactorOn("JavadocNoTodo", """
//            /**
//             * 既存Javadocです。
//             * <li>条件</li>
//             * (ここに条件挿入予定)
//             *
//             * <li>結果</li>
//             * (ここに結果挿入予定)
//             */
//            class JavadocNoTodo {
//                @org.junit.Test
//                public void testNoTodoJavadoc() {
//                    TestedObject to = new TestedObject();
//                    to.setMail("noTodoJavadoc@example.com");
//                    String r = to.loginScenario("noTodoJavadoc@example.com");
//                    org.junit.Assert.assertEquals("noTodoJavadoc@example.com", r);
//                }
//            }
//        """);
//        String newText = myFixture.getFile().getText();
//
//        // // TODO テストの概要はない
//        Assert.assertFalse(newText.contains("TODO テストの概要"));
//        // conditions/resultsが挿入されている
//        Assert.assertTrue(newText.contains("mail：\"noTodoJavadoc@example.com\""));
//        Assert.assertTrue(newText.contains("の値が noTodoJavadoc@example.com と Equalsであること"));
//        // coverage/field/junit4などは発生せず、TODO最小
//        Assert.assertFalse(newText.contains("Add coverage for line"));
//        Assert.assertFalse(newText.contains("fieldへのアサートが抜けています"));
//        Assert.assertFalse(newText.contains("JUnit4を使用しています"));
//    }
//
//    /** 既存JavadocなしでTODO概要が入るケース */
//    public void testJavadocYesTodo() {
//        TestAnalysisResult result = runRefactorOn("JavadocYesTodo", """
//            class JavadocYesTodo {
//                @org.junit.Test
//                public void testJavadocTodo() {
//                    TestedObject to = new TestedObject();
//                    to.setMail("todoJavadoc@example.com");
//                    String r = to.loginScenario("todoJavadoc@example.com");
//                    org.junit.Assert.assertEquals("todoJavadoc@example.com", r);
//                }
//            }
//        """);
//        String newText = myFixture.getFile().getText();
//        // TODO テストの概要が入る
//        Assert.assertTrue(newText.contains("TODO テストの概要を書いてください。"));
//    }
//}
