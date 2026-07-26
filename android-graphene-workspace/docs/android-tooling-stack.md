# Android Tooling Stack Reference

What's assumed to be installed/available when an AI proposes a build or run
command. Update this as your actual setup changes.

## Core tools

| Tool | Purpose | Notes |
|---|---|---|
| Android Studio | IDE, emulator management, Gradle builds | Optional if you're doing CLI/Claude-Code-driven work |
| `adb` | Device communication | Part of Android SDK Platform-Tools; needed for anything device-facing |
| `fastboot` | Bootloader-level flashing | Only relevant for flashing/OS-level work, not typical app tooling |
| Kotlin / Android Gradle Plugin | App-side code | Default language for on-device app code |
| Python 3 | Host-side scripting, ADB automation | Default for CLI tools that orchestrate ADB |
| Node.js | Only if a tool specifically needs it (e.g. Claude Code itself) | Not a default assumption for Android tooling |

## Recommended default stack for new tools

- **Device-interaction CLI tools** → Python + `subprocess` wrapping `adb`,
  or a dedicated ADB library if the task warrants it.
- **On-device apps** → Kotlin, standard Android Jetpack libraries, minimum
  SDK matched to what GrapheneOS's supported Pixel range can run (check
  current GrapheneOS supported-device page before assuming a floor).
- **Automation/scripts** → Bash for simple ADB one-liners, Python once
  there's real logic (parsing `dumpsys` output, retry logic, etc.).

## Environment checklist (mirrors Lesson 1.1's "What You Need")

- [x] Claude account (or other AI) — free tier is enough to start
- [x] Code editor (VS Code / Cursor / code-server + Grok)
- [ ] Node.js — only if using Claude Code
- [x] Android SDK Platform-Tools (`adb`, `fastboot`) on PATH
- [ ] USB debugging enabled on the GrapheneOS device, or wireless ADB paired
- [ ] (Optional) Android Studio, if building a full app rather than CLI tooling

### This environment (as of 2026-07-26)

| Item | Location / version |
|---|---|
| `adb` (preferred) | `android_GooglePlayRedirect/tools/platform-tools/adb` — 37.0.0 |
| `fastboot` | same directory |
| `adb` (system) | `/usr/bin/adb` — Debian 34.0.5 |
| PATH | `~/.bashrc` prepends `tools/platform-tools` |
| Python 3 | 3.13.x available for host-side scripts |
| Device | none attached (`adb devices` empty) |

## Verifying ADB sees the device

```bash
adb devices
# should list your device's serial with "device" (not "unauthorized")
```

If it says `unauthorized`, check the device screen for the RSA key
confirmation prompt — GrapheneOS behaves the same as stock AOSP here.
