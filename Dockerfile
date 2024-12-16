FROM gradle:7.6.1-jdk11 AS build-env
# Gradle公式イメージを使用。jdk11指定のタグでJava 11環境が用意される。

WORKDIR /app

# キャッシュを有効活用するため、先にgradleのラッパー・設定ファイルをコピー
COPY gradle/ gradle/
COPY gradlew .
COPY gradle.properties .
COPY settings.gradle .
COPY build.gradle .

RUN gradle --no-daemon dependencies

# プロジェクト全体をコピー
COPY . .

# ビルド実行（必要に応じて）
RUN gradle --no-daemon build

# 実行用ステージ（任意：もし何らかの実行が必要な場合）
# 基本的には開発時にはbuild-envステージでもOKだが、必要なら分ける
FROM openjdk:11-jdk-slim
WORKDIR /app

# ビルド成果物をコピー（必要なら）
COPY --from=build-env /app/build/distributions/*.zip /app/

# エントリーポイント設定（もし必要なら）
# ENTRYPOINT ["/app/entrypoint.sh"]
