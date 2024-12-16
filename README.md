# junit-support-plugin

- 下記でまとめているものが初版として作成した仕様
  - 現状の問題点
    - メソッドの終了時の区切り位置が決まっていないため、上からコメントを書いていく。
      - そのため一つのメソッドに対して複数のjavadocができる
    - 改行がうまくいっていない？
    - 環境起因？
- 上記不具合が改善されないとプレリリースも無理
---
# IntelliJプラグイン仕様概要（最終まとめ）

本プラグインは既存のJUnitテストコードを解析し、外部ルールファイル (`rules.md`) に基づいて以下のサポート機能を提供します。  
完全自動化は目指さず、可能な範囲で補助情報やTODOコメントを挿入することで、ユーザーが後から詳細を補足・調整しやすい環境を整えます。

## 主な処理の流れ

1. **rules.mdの読み込み**  
   プラグインは`rules.md`から以下を取得します。
   - テストメソッド名やJavadocテンプレート中のプレースホルダー（`${TEST_CONDITIONS}`, `${TEST_RESULTS}`など）
   - Javadocテンプレート  
     テンプレート例：  
     ```
     /**
      * // TODO テストの概要を書いてください。
      * 
      * <p>条件</p>
      * <ul>
      *   <li>${TEST_CONDITIONS}</li>
      * </ul>
      *
      * <p>結果</p>
      * <ul>
      *   <li>${TEST_RESULTS}</li>
      * </ul>
      *
      */
     ```
   - 各種fallbackコメント（conditions-empty、results-empty、placeholder-unresolved、method-conflictなど）

2. **テストクラス・主対象メソッドの特定**  
   テストクラス名から対象クラス名を推測 (例：`XxxTest → Xxx`)  
   テストメソッド名が`test`で始まると仮定 (`testXxx`→`Xxx`)し、対象クラス内の該当メソッドを探索。  
   見つからなければTODOコメントを挿入してユーザーに修正を促す。

3. **条件（TEST_CONDITIONS）と結果（TEST_RESULTS）の抽出**  
   - 条件抽出：  
     `testedObject.setMail("aa@aa.com")`など、`setXxx(...)`形式の前処理呼び出しを解析し、  
     `mail："aa@aa.com"`のような簡潔な表記に変換する。  
     `setAge("30")`なら`age："30"`,  
     引数が複雑な場合は原文＋TODOコメントでユーザーに補足を委ねる。

   - 結果抽出：  
     `assertEquals`, `assertTrue`, `assertNotNull`などのアサーション呼び出しを解析し、  
     例えば`assertEquals(input, result)`なら`result の値が input と Equalsであること // TODO 値の説明を追記してください`  
     のような簡易日本語表記に変換。  
     また、`verify(...)`があれば  
     `verify(...)：検証すること // TODO 確認する内容を追記してください`  
     といった行を追加。

   - conditions/resultsが1件もない場合は  
     `// TODO 条件を特定できませんでした`  
     `// TODO 結果を特定できませんでした`  
     を挿入する。

4. **既存Javadocの扱いと新規Javadoc挿入**  
   - Javadocテンプレートを用いて新規Javadocを生成。  
   - 既存Javadocがある場合は、既存Javadocを「そのまま残し」、その直後に新規Javadocを追加。  
   - 新規Javadocには`// TODO テストの概要`行も含め、conditions/resultsを挿入した完全形が並ぶ。

5. **クラス変数へのアサート漏れ検知**  
   - 前処理で`setXxx`して取得したフィールドに対して、結果抽出でフィールド名が含まれるアサートが見当たらなければ  
     `// TODO: 'xxx'フィールドへのアサートが抜けています。適切なアサートを追加してください。`  
     をテストメソッド末尾に挿入。  
   - 全ての`setXxx`で取得したフィールドに対しアサート状況を確認する。

6. **解析不能・曖昧な箇所への対応**  
   - 解析が難しい場合は原文出力＋`// TODO`コメントでユーザーに補足を促す。  
   - プレースホルダー（`${...}`）が未解決で残っている場合は  
     `// TODO ここに適切な説明を記載してください`  
     を挿入。

7. **メソッド名衝突（method-conflict）**  
   メソッド名変更（`renameMethodIfPossible`）が失敗した場合にのみ  
   `// TODO 同名メソッドが存在します。メソッド名を修正してください。`  
   を挿入。

8. **その他微調整**  
   - 不要な空行、余分なスペース、連続した空行などを整理する。  
   - coverage-hintやJUnit4警告は今回対象外として削除。

## 結果イメージ

最終的に変換後のテストコードには、  
- 新規・既存Javadocが並び、  
- conditions/resultsが挿入されたJavadoc  
- アサート漏れフィールドへのTODOコメント  
- 不明瞭な箇所へのTODOコメント  
が必要な場合にのみ出力され、ユーザーはこのTODOコメントやJavadocを手がかりに詳細を補足・修正できます。
