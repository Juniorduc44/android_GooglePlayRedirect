# SOP: Versioning & releases (whole workspace)

This SOP applies to **the entire repository** and **every app/tool under
`projects/`** that ships versioned artifacts (especially Android APKs).
AI assistants and humans must follow it for any release.

Per-project notes may extend this file but **must not contradict** it.
Example extension: `projects/php-usd-converter/VERSIONING.md` (app-specific
paths only).

---

## 1. Semantic versioning (SemVer)

Version form: **`MAJOR.MINOR.PATCH`**

Displayed as **`vMAJOR.MINOR.PATCH`** on tags, GitHub Releases, and APK
filenames. Stored **without** the `v` prefix in `VERSION` files and in
Android `versionName`.

| Bump | Example | When to use |
|---|---|---|
| **MAJOR** | `v1.0.0` → `v2.0.0` | Breaking change, large redesign, incompatible behavior, or a new generation of the product |
| **MINOR** | `v1.0.0` → `v1.1.0` | Backward-compatible features or noticeable improvements |
| **PATCH** | `v1.0.0` → `v1.0.1` | Bug fixes, copy tweaks, small polish, security/dependency fixes with no new feature |

### Decision guide

1. Breaks expectations / incompatible? → **MAJOR**
2. Else, new user-visible capability? → **MINOR**
3. Else → **PATCH**

If unsure between MINOR and PATCH: prefer **PATCH** for tiny fixes, **MINOR**
when the changelog would list a real feature.

Reset rules:

- MAJOR bump → MINOR and PATCH become `0`
- MINOR bump → PATCH becomes `0`
- PATCH bump → only PATCH increases

---

## 2. Single artifact naming scheme (APKs)

**One long name per release.** No short aliases.

| Item | Rule |
|---|---|
| Pattern | `{project-slug}-v{MAJOR}.{MINOR}.{PATCH}.apk` |
| Location | `projects/{project-slug}/dist/{project-slug}-v{MAJOR}.{MINOR}.{PATCH}.apk` |
| Forbidden | `v1.0.0.apk`, unversioned `app-release.apk` in `dist/`, duplicate aliases |

Examples:

- `php-usd-converter-v1.0.0.apk`
- `php-usd-converter-v1.0.1.apk`
- `google-play-redirect-v1.0.0.apk` (when that app ships)

Default: **keep prior versioned APKs** in `dist/` as history unless asked to
prune.

---

## 3. Android `versionName` / `versionCode`

In each app’s `android/app/build.gradle.kts` → `defaultConfig`:

| Field | Format | Purpose |
|---|---|---|
| `versionName` | `"MAJOR.MINOR.PATCH"` (no `v`) | User-visible |
| `versionCode` | Integer, **always increases** | Install/upgrade identity |

### `versionCode` formula (required for new releases)

```text
versionCode = MAJOR * 10000 + MINOR * 100 + PATCH
```

| versionName | versionCode |
|---|---|
| 1.0.0 | 10000 *(or historical exception documented in that app)* |
| 1.0.1 | 10001 |
| 1.1.0 | 10100 |
| 2.0.0 | 20000 |

**Exception:** `php-usd-converter` **v1.0.0** already shipped with
`versionCode = 1`. All **later** versions of that app (and all other apps
from first release) must use the formula and stay strictly greater than the
previous shipped `versionCode`.

---

## 4. Git tags & GitHub Releases

For each shipped app version:

1. Tag format: `{project-slug}-vMAJOR.MINOR.PATCH`  
   - Example: `php-usd-converter-v1.0.0`  
   - Optional repo-wide monorepo tag only if a coordinated multi-app release
     is intentional: `vMAJOR.MINOR.PATCH` (document which apps it covers).
2. Create a **GitHub Release** with that tag via `gh release create`.
3. Attach the long-named APK from `dist/`.
4. Release title: human-readable, e.g. `php-usd-converter v1.0.0`.
5. Body: short changelog (why this was MAJOR / MINOR / PATCH).

### CLI (requires `gh` authenticated as juniorduc44)

```bash
# Example: php-usd-converter v1.0.0
gh release create "php-usd-converter-v1.0.0" \
  --title "php-usd-converter v1.0.0" \
  --notes-file - \
  "android-graphene-workspace/projects/php-usd-converter/dist/php-usd-converter-v1.0.0.apk"
```

---

## 5. Per-project files

Every shippable Android (or versioned) project should maintain:

| File | Purpose |
|---|---|
| `VERSION` | Single line: `MAJOR.MINOR.PATCH` (current) |
| `VERSIONING.md` | Optional app-specific notes; points here as authority |
| `dist/{slug}-vX.Y.Z.apk` | Release binary |
| `README.md` | Install path + changelog bullets |
| `CONTEXT.md` | Current version recorded after each release |

Workspace root also keeps this file as the **global** SOP.

---

## 6. Release checklist (every app)

1. [ ] Classify: MAJOR / MINOR / PATCH  
2. [ ] Compute new `X.Y.Z` and `versionCode`  
3. [ ] Update Gradle `versionName` / `versionCode`  
4. [ ] Update project `VERSION`  
5. [ ] Update project `README.md` + `CONTEXT.md`  
6. [ ] Build release APK  
7. [ ] Copy **once** to  
    `dist/{slug}-vX.Y.Z.apk` (no short alias)  
8. [ ] Verify with `aapt dump badging …`  
9. [ ] Commit as `juniorduc44 <juniorduc44@gmail.com>` when asked  
10. [ ] Push `main` when asked  
11. [ ] `gh release create` with tag + APK asset  

Do **not** commit: keystores, `local.properties`, `**/build/`, `.venv/`, SDK trees.

---

## 7. Tooling

| Tool | Role |
|---|---|
| `git` + SSH | Commit / push as juniorduc44 |
| `gh` | GitHub Releases, PRs, API from CLI |
| `adb` | Optional install testing (not required to ship) |
| Android SDK + Gradle | Build APKs |

Install `gh` on Debian-like systems from [cli.github.com](https://cli.github.com/).
Authenticate with a token or `gh auth login` as **juniorduc44**.

---

## 8. Authority order

1. This file (`android-graphene-workspace/VERSIONING.md`) — global rules  
2. `AGENTS.md` — GrapheneOS / coding constraints  
3. `projects/<name>/VERSIONING.md` — app-specific paths only  
4. `projects/<name>/CONTEXT.md` — current focus, not version policy  

If a request asks for short APK aliases or skipped `versionCode` bumps,
follow this SOP unless the user **explicitly** overrides it.
