# Local tools (agent / developer — not shipped in the app)

These tools live at the **repo root** under `tools/` and are for local research and
automation. They are **not** bundled into the php-usd-converter APK.

## browser-harness

Clone of [browser-use/browser-harness](https://github.com/browser-use/browser-harness):

```text
tools/browser-harness/
```

**Purpose:** CDP control of a real Chrome browser so an agent can reverse-engineer
sites (e.g. hood.dev wallet/passkey UX, DexScreener UI) when plain HTTP is not enough.

**Install (dev machine with Chrome):** see `browser-harness/install.md` and `SKILL.md`.

```bash
# recommended path from upstream
uv tool install --python 3.12 --upgrade --force browser-harness
# enable chrome://inspect/#remote-debugging in Chrome, then:
browser-harness <<'PY'
print(page_info())
PY
```

From the checkout without global install:

```bash
cd tools/browser-harness
./browser-harness <<'PY'
print(page_info())
PY
```

**App code** never imports this package. Blockchain prices use DexScreener HTTP APIs
in `projects/php-usd-converter/blockchain/`.
