package com.example.junitsupport.action;

import com.example.junitsupport.actions.JUnitSupportAction;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class JUnitSupportActionTest {

    private void setTest(String a){

    }

    /**
     * 既存のテスト
     */ /**
     * // TODO テストの概要を書いてください。
     *
     * <li>条件</li>
     * // TODO 条件を特定できませんでした
     *
     * <li>結果</li>
     * testTrue(...)：trueであること // TODO 値の説明を追記してください
     */
    @Test
    public void test___true_scenario_scenario_scenario() {
        JUnitSupportAction junitSupoot = new JUnitSupportAction();
        assertTrue( junitSupoot.testTrue());

    }


    // TODO: 'test'フィールドへのアサートが抜けています。適切なアサートを追加してください。

    /**
     * // TODO テストの概要を書いてください。
     *
     * <li>条件</li>
     * test："aaa"
     *
     * <li>結果</li>
     * // TODO 結果を特定できませんでした
     */
    @Test
    public void test___false_scenario_scenario_scenario() {
        JUnitSupportAction junitSupoot = new JUnitSupportAction();
        int a = 123;
        setTest("aaa");
//        assertTrue( junitSupoot.testFalse());
    }

    /**
     * // TODO テストの概要を書いてください。
     *
     * <li>条件</li>
     * test："aaa"
     *
     * <li>結果</li>
     * testFalse(...)：trueであること // TODO 値の説明を追記してください
     */
    @Test
    public void test___false_scenario_scenario2_scenario() {
        JUnitSupportAction junitSupoot = new JUnitSupportAction();
        int a = 123;
        setTest("aaa");
        assertTrue( junitSupoot.testFalse());
    }
}
