# android_GooglePlayRedirect

App meant for redirecting to the browser instead of Google Play.

## Active workspace

The GrapheneOS-first master workspace lives here:

```text
android-graphene-workspace/
```

AI tools and humans should treat that folder as the operational root:

1. Read `android-graphene-workspace/AGENTS.md`
2. Read `android-graphene-workspace/CONTEXT.md`
3. Read `android-graphene-workspace/REFERENCES.md`
4. Work under `android-graphene-workspace/projects/<name>/`

### Active projects

| Project | Path | Status |
|---|---|---|
| php-usd-converter | `android-graphene-workspace/projects/php-usd-converter/` | Working / testing |
| google-play-redirect | `android-graphene-workspace/projects/google-play-redirect/` | In progress |

Scaffold another tool:

```bash
cd android-graphene-workspace
./scripts/new-project.sh <name>
```

## Host tooling

Android platform-tools (ADB / fastboot) are installed at:

```text
tools/platform-tools/
```

`~/.bashrc` adds that directory to `PATH`. Verify:

```bash
adb version
adb devices
```

## Identity

Git author for this repo: `juniorduc44 <juniorduc44@gmail.com>`.
