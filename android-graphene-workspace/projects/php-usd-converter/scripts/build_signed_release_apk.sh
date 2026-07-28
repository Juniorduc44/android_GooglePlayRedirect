#!/usr/bin/env bash
# Build a *signed* release APK into dist/. Fails if signing env is missing.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ANDROID="$ROOT/android"
DEFAULT_KS="$ANDROID/app/release.keystore"

: "${PHP_USD_KEYSTORE:=$DEFAULT_KS}"
: "${PHP_USD_KEY_ALIAS:=php-usd-converter}"

if [[ -z "${PHP_USD_STORE_PASSWORD:-}" || -z "${PHP_USD_KEY_PASSWORD:-}" ]]; then
  echo "ERROR: set PHP_USD_STORE_PASSWORD and PHP_USD_KEY_PASSWORD" >&2
  echo "Optional: PHP_USD_KEYSTORE (default: $DEFAULT_KS), PHP_USD_KEY_ALIAS" >&2
  exit 1
fi
if [[ ! -f "$PHP_USD_KEYSTORE" ]]; then
  echo "ERROR: keystore not found: $PHP_USD_KEYSTORE" >&2
  exit 1
fi

export PHP_USD_KEYSTORE PHP_USD_STORE_PASSWORD PHP_USD_KEY_ALIAS PHP_USD_KEY_PASSWORD
export ANDROID_HOME="${ANDROID_HOME:-/home/coder/android_GooglePlayRedirect/tools/android-sdk}"
export ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$ANDROID_HOME}"

cd "$ANDROID"
./gradlew :app:assembleRelease

VERSION="$(tr -d '[:space:]' < "$ROOT/VERSION")"
APK_SRC="$(find "$ANDROID/app/build/outputs/apk/release" -name 'app-release.apk' | head -1)"
if [[ -z "$APK_SRC" || "$APK_SRC" == *unsigned* ]]; then
  echo "ERROR: expected signed app-release.apk, got: ${APK_SRC:-none}" >&2
  exit 1
fi

mkdir -p "$ROOT/dist"
OUT="$ROOT/dist/php-usd-converter-v${VERSION}.apk"
cp -f "$APK_SRC" "$OUT"

APKSIGNER="$ANDROID_HOME/build-tools/34.0.0/apksigner"
if [[ -x "$APKSIGNER" ]]; then
  "$APKSIGNER" verify --print-certs "$OUT"
  echo "OK: signed APK at $OUT"
else
  echo "WARN: apksigner not found; copied $OUT without verify"
fi
