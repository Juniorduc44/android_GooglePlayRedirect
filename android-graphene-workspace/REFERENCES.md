# References

Background material, links, and prior decisions. The AI should read this
for context but generally shouldn't need to act on it directly — it's the
"why" and "what we already decided," not the "what to do right now."

## Official GrapheneOS resources

- Main site / features overview: https://grapheneos.org/features
- FAQ (covers Play Services sandboxing, root stance, device support):
  https://grapheneos.org/faq
- Usage guide: https://grapheneos.org/usage
- Source code / releases: https://github.com/GrapheneOS

## Android / AOSP developer references

- Android Developers docs: https://developer.android.com/docs
- ADB reference: https://developer.android.com/tools/adb
- AOSP source: https://source.android.com/
- Storage Access Framework: https://developer.android.com/guide/topics/providers/document-provider

## FOSS alternatives to GMS-dependent services

- Push notifications without FCM: **UnifiedPush** — https://unifiedpush.org/
- Maps without Google Maps SDK: **MapLibre** / **OSMDroid**
- App distribution: **F-Droid** (https://f-droid.org/), **Accrescent**
  (https://accrescent.app/), **Obtainium** (sideload updater for GitHub
  releases/APKs), **Aurora Store** (anonymous Play Store frontend)
- Optional Google Play compatibility (sandboxed, user-installed):
  GrapheneOS's own sandboxed Play services — see the FAQ link above for how
  this differs from a normal GMS install.

## Examples of good work

[Paste an example tool, script, or output you liked and want to replicate
the style/quality of.]

## Decisions log

Keep a running log here so an AI reading this later understands *why*
something is the way it is, instead of re-litigating it.

| Date | Decision | Why |
|---|---|---|
| 2026-07-26 | First real project is `google-play-redirect` (on-device app) | Matches repo purpose: open Play links in browser on GrapheneOS / no-Play setups |
| 2026-07-26 | Host platform-tools under repo `tools/platform-tools/` + Debian `adb` package | Self-contained ADB/fastboot in the workspace; PATH prefers workspace copy (v37) |
| 2026-07-26 | No root; no GMS/Play Integrity for this app | GrapheneOS defaults; tool exists specifically to avoid Play Store dependency |
| 2026-07-26 | Host tool `php-usd-converter` uses CustomTkinter + requests | User-provided GUI; live rate from exchangerate-api.com with offline fallback 0.0175 |
| 2026-07-26 | Single APK name: `php-usd-converter-vX.Y.Z.apk` only | Drop short `vX.Y.Z.apk` aliases; SemVer major/minor/patch per project `VERSIONING.md` |


## Notes

- This git repo root is `android_GooglePlayRedirect/`; the operational AI
  workspace lives in `android-graphene-workspace/` (AGENTS.md, projects/, etc.).
- ADB is installed; as of last check `adb devices` listed no attached device.
- Google official platform-tools zip was unpacked to
  `../tools/platform-tools/` (sibling of this folder under the repo root).
