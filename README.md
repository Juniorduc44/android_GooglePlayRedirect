# android_GooglePlayRedirect

App meant for redirecting to the browser instead of Google Play — and a
home for related GrapheneOS-first Android tools.

## Active workspace

The GrapheneOS-first master workspace lives here:

```text
android-graphene-workspace/
```

AI tools and humans should treat that folder as the operational root:

1. Read `android-graphene-workspace/AGENTS.md`
2. Read `android-graphene-workspace/VERSIONING.md` **(whole-repo release SOP)**
3. Read `android-graphene-workspace/CONTEXT.md`
4. Read `android-graphene-workspace/REFERENCES.md`
5. Work under `android-graphene-workspace/projects/<name>/`

### Versioning (all apps)

| Bump | Example | Meaning |
|---|---|---|
| Major | `v1.0.0` → `v2.0.0` | Breaking / large redesign |
| Minor | `v1.0.0` → `v1.1.0` | New features |
| Patch | `v1.0.0` → `v1.0.1` | Fixes / small polish |

APK name: `{project}-vMAJOR.MINOR.PATCH.apk` only.  
Full checklist: [`android-graphene-workspace/VERSIONING.md`](android-graphene-workspace/VERSIONING.md).

### Active projects

| Project | Path | Status |
|---|---|---|
| google-play-redirect | `android-graphene-workspace/projects/google-play-redirect/` | **v1.1.1** (crash fix + debug) |
| php-usd-converter | `android-graphene-workspace/projects/php-usd-converter/` | **v1.1.1** shipped |

Scaffold another tool:

```bash
cd android-graphene-workspace
./scripts/new-project.sh <name>
```

## Host tooling

| Tool | Purpose |
|---|---|
| `adb` / `fastboot` | Device tools (`tools/platform-tools/`) |
| `gh` | GitHub CLI (releases, PRs) |
| OpenJDK 21 | Android builds |
| Android SDK | `tools/android-sdk/` (local, not in git) |

```bash
adb version
gh --version
```

## Identity

Git author for this repo: `juniorduc44 <juniorduc44@gmail.com>`.
