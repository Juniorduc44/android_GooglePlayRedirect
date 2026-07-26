# GrapheneOS Technical Notes

Deeper reference material for anything AGENTS.md summarizes. Point an AI
here when a task needs more detail than the top-level constraints.

## What GrapheneOS is

A privacy/security-hardened Android distribution built from AOSP, officially
supported only on **Google Pixel** hardware (this is a hard constraint —
GrapheneOS does not support non-Pixel devices due to needing verified boot
with user-controlled keys and a strong hardware security module).

## Key differences from stock Android that affect tooling

1. **No Google apps/services by default.** A fresh install has zero GMS.
   Users can optionally install a sandboxed, de-privileged version of
   Google Play — but it runs as a regular app with no special system
   access, unlike stock Android where GMS is a privileged system app.
2. **Hardened memory allocator (hardened_malloc).** Stricter than Bionic's
   default allocator — will abort the process on some memory-safety
   violations that other allocators tolerate. Native/NDK code should be
   tested here specifically, not assumed safe because it works on stock
   Android.
3. **Per-app network toggle.** Every app can have network access revoked
   entirely, independent of any Android permission. Tools that assume
   network availability should handle `UnknownHostException` /
   connection-refused gracefully and explain the likely cause.
4. **Per-app sensors toggle.** Same idea — GrapheneOS exposes a system-level
   toggle to cut off sensor access per app, on top of Android's own runtime
   permissions.
5. **Storage Scopes.** Lets a user restrict what an app sees when it "asks"
   for storage — the app can be given a limited or spoofed view. Assume the
   filesystem you can see is not necessarily the full filesystem.
6. **Contact Scopes.** Same pattern applied to the contacts provider.
7. **Auto-reboot / duress features.** Devices can be configured to
   auto-reboot after inactivity, which drops FBE (file-based encryption)
   keys back to locked state. Long-running background tooling should not
   assume indefinite device uptime or unlocked storage.
8. **Verified boot with user keys.** Users can sign their own OS builds and
   have the bootloader verify against their own key, rather than Google's.
   This has no direct API implication for app tooling but matters if your
   tool touches boot/flashing workflows.
9. **No root, by design.** Rooting is explicitly outside GrapheneOS's threat
   model and disables key protections. Default to root-free approaches
   (ADB shell commands via `pm`, `cmd`, `appops`, etc. cover a lot of
   ground without root).

## Practical ADB commands useful for tooling on GrapheneOS

```bash
# List installed packages
adb shell pm list packages -3

# Check granted runtime permissions for an app
adb shell dumpsys package <package.name> | grep -A5 "runtime permissions"

# Check appops (network, sensors etc. show up here too)
adb shell cmd appops get <package.name>

# Revoke/grant a permission for testing
adb shell pm revoke <package.name> android.permission.CAMERA
adb shell pm grant <package.name> android.permission.CAMERA
```

## Distribution channels (no Play Store assumed)

- **F-Droid** — FOSS app repo, reproducible builds for many apps
- **Accrescent** — newer, security-focused app store built with a threat
  model similar to GrapheneOS's own
- **Obtainium** — tracks GitHub/GitLab releases and auto-updates sideloaded
  APKs
- **Aurora Store** — anonymous/pseudonymous frontend to the Play Store,
  useful when a specific proprietary app is unavoidable

## Testing considerations

- There is no official GrapheneOS emulator/AVD image. Testing is typically
  done on real Pixel hardware. If a task requires an emulator, be explicit
  that behavior may differ from a real GrapheneOS device (especially
  around GMS absence and the hardened allocator).
- When proposing a testing plan, default to "test on the real device over
  ADB" rather than assuming an AVD will catch GrapheneOS-specific issues.
