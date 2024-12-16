#!/bin/sh
# エントリーポイントでビルド等を実行したい場合の例
gradle --no-daemon build
exec "$@"
