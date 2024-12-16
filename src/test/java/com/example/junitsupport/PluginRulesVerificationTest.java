//package com.example.junitsupport;
//
//import com.example.junitsupport.services.RuleParser;
//import com.example.junitsupport.services.TestAnalysisResult;
//import com.example.junitsupport.services.TestCodeRefactorer;
//import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
//import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.Test;
//import java.nio.file.Paths;
//
//public class PluginRulesVerificationTest extends LightJavaCodeInsightFixtureTestCase {
//
//    private TestCodeRefactorer refactorer;
//    private RuleParser parser;
//
//    @Override
//    protected void setUp() throws Exception {
//        super.setUp();
//        myFixture.setTestDataPath(getTestDataPath());
//        var project = getProject();
//        String rulesPath = Paths.get(project.getBasePath(), "rules.md").toString();
//        parser = new RuleParser(rulesPath);
//        refactorer = new TestCodeRefactorer(parser, project);
//
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
//        return "src/test/testData";
//    }
//
//    private TestAnalysisResult runRefactorOn(String className, String code) {
//        var psiFile = myFixture.configureByText(className + ".java", code);
//        return refactorer.analyzeAndRefactor(psiFile);
//    }
//
//    @Test
//    public void testCoverageHintYes() {
//        TestAnalysisResult result = runRefactorOn("CoverageHintYes", """
//            class CoverageHintYes {
//                @org.junit.Test
//                public void testScenario() {
//                    TestedObject to = new TestedObject();
//                    to.setMail("x@example.com");
//                    String r = to.loginScenario("x@example.com");
//                    org.junit.Assert.assertEquals("x@example.com", r);
//                }
//            }
//        """);
//        String newText = myFixture.getFile().getText();
//        Assertions.assertTrue(newText.contains("Add coverage for line 120"));
//        Assertions.assertTrue(newText.contains("Add coverage for line 125"));
//    }
//
//    @Test
//    public void testCoverageHintNo() {
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
//        Assertions.assertFalse(newText.contains("Add coverage for line"));
//    }
//
//    // 他のルール(conditions-empty, results-empty, method-conflict, field-assert-missing)も同様にtestXXXYes/Noを定義
//
//    @Test
//    public void testJavadocExisting() {
//        TestAnalysisResult result = runRefactorOn("JavadocExisting", """
//            /**
//             * 既存Javadoc
//             * このコメントは絶対に残すこと
//             */
//            class JavadocExisting {
//                @org.junit.Test
//                public void testCase() {
//                    TestedObject to = new TestedObject();
//                    to.setMail("exist@example.com");
//                    String r = to.loginScenario("exist@example.com");
//                    org.junit.Assert.assertEquals("exist@example.com", r);
//                }
//            }
//        """);
//        String newText = myFixture.getFile().getText();
//        // 既存Javadocが残る
//        Assertions.assertTrue(newText.contains("既存Javadoc"));
//        // 新規Javadocが既存Javadocの直後に追加される
//        Assertions.assertTrue(newText.contains("TODO テストの概要を書いてください。"));
//        Assertions.assertTrue(newText.contains("mail：\"exist@example.com\""));
//        Assertions.assertTrue(newText.contains("の値が exist@example.com と Equalsであること"));
//    }
//
//    @Test
//    public void testJavadocNoExisting() {
//        TestAnalysisResult result = runRefactorOn("JavadocNoExisting", """
//            class JavadocNoExisting {
//                @org.junit.Test
//                public void testCase() {
//                    TestedObject to = new TestedObject();
//                    to.setMail("noexist@example.com");
//                    String r = to.loginScenario("noexist@example.com");
//                    org.junit.Assert.assertEquals("noexist@example.com", r);
//                }
//            }
//        """);
//        String newText = myFixture.getFile().getText();
//        // 新規Javadocのみ挿入
//        Assertions.assertTrue(newText.contains("TODO テストの概要を書いてください。"));
//        Assertions.assertTrue(newText.contains("mail：\"noexist@example.com\""));
//        Assertions.assertTrue(newText.contains("の値が noexist@example.com と Equalsであること"));
//    }
//}
