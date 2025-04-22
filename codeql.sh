#!/bin/bash
set -euo pipefail

# === Конфигурация ===
CODEQL_BIN="$HOME/Desktop/codeql/codeql"
DB_DIR="codeql-db"
RESULT_FILE="codeql-results.sarif"
QUERY_PACK="codeql/java-queries"
BUILD_CMD="./gradlew assembleInternal --build-cache"

# === Проверка CodeQL ===
if [[ ! -x "$CODEQL_BIN" ]]; then
  echo "❌ CodeQL CLI не найден по пути: $CODEQL_BIN"
  echo "Убедись, что ты распаковал архив и указал путь правильно"
  exit 1
fi

# === Проверка Rosetta (только для ARM Mac) ===
if [[ "$(uname -m)" == "arm64" ]]; then
  if ! /usr/bin/pgrep oahd &>/dev/null; then
    echo "❌ Rosetta 2 не установлена. Устанавливаю..."
    softwareupdate --install-rosetta --agree-to-license
  fi
fi

export CODEQL_ALLOW_INSTALLATION_ANYWHERE=true

# === Установка query паков ===
echo "📦 Устанавливаем пакеты..."
arch -x86_64 "$CODEQL_BIN" pack install "$QUERY_PACK"

# === Удаление старой базы ===
rm -rf "$DB_DIR"

# === Создание базы ===
echo "🛠 Создаём базу..."
arch -x86_64 "$CODEQL_BIN" database create "$DB_DIR" \
  --language=java \
  --command="$BUILD_CMD"

# === Анализ ===
echo "🔍 Анализ кода..."
arch -x86_64 "$CODEQL_BIN" database analyze "$DB_DIR" \
  "$QUERY_PACK" \
  --format=sarifv2.1.0 \
  --output="$RESULT_FILE"

echo "✅ Готово! Результаты: $RESULT_FILE"
