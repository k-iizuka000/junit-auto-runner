# method-name-template
test_${TARGET_METHOD_NAME}_scenario

# javadoc-template
/**
 * // TODO テストの概要を書いてください。
 * 
 * <li>条件</li>
 * ${TEST_CONDITIONS}
 *
 * <li>結果</li>
 * ${TEST_RESULTS}
 */

# coverage-hint
- method: ${TARGET_METHOD_NAME}
  missingLines:
    - 120
    - 125

# output-assertion-rules
- method: ${TARGET_METHOD_NAME}
  fieldsToAssert:
    - userRole
    - loginState

# junit4-warning
// TODO: このテストはJUnit4を使用しています。JUnit5へ変更してください。

# fallback-comments
conditions-empty: // TODO 条件を特定できませんでした
results-empty: // TODO 結果を特定できませんでした
placeholder-unresolved: // TODO ここに適切な説明を記載してください
method-conflict: // TODO 同名メソッドが存在します。メソッド名を修正してください。
class-missing: // TODO: テストクラスが見つかりません。対象クラスと関連付けを確認してください。
method-target-missing: // TODO: 対象メソッドが見つかりません。メソッド名を修正してください。
field-assert-missing: // TODO: 'userRole'フィールドへのアサートが抜けています。適切なアサートを追加してください。
