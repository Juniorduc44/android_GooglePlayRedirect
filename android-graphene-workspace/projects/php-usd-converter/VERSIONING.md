# Versioning notes — php-usd-converter

**Global SOP (source of truth):**  
[`../../VERSIONING.md`](../../VERSIONING.md)

This file only records **app-specific** details. Do not redefine SemVer or
naming rules here.

## App identity

| Item | Value |
|---|---|
| Project slug | `php-usd-converter` |
| APK pattern | `php-usd-converter-vMAJOR.MINOR.PATCH.apk` |
| Dist path | `dist/php-usd-converter-vX.Y.Z.apk` |
| Package ID | `com.juniorduc44.phpusdconverter` |
| Gradle | `android/app/build.gradle.kts` |
| Current `VERSION` file | see [`VERSION`](./VERSION) |

## Historical note

- **v1.0.0** shipped with Android `versionCode = 1` (before the global
  formula was adopted).
- **Next** release must use  
  `versionCode = MAJOR * 10000 + MINOR * 100 + PATCH`  
  and must be **> 1**.

## Release tag / GitHub Release name

```text
php-usd-converter-vMAJOR.MINOR.PATCH
```

Example:

```bash
gh release create "php-usd-converter-v1.0.0" \
  --title "php-usd-converter v1.0.0" \
  --notes "Initial release: PHP → USD converter (live rate + offline fallback)." \
  "dist/php-usd-converter-v1.0.0.apk"
```

(Run from this project directory, or pass the full path to the APK.)
