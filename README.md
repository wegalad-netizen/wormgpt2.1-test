# WormGPT Terminal UI

A full-screen terminal UI for the Gemini `generateContent` API, designed for Termux.

## Install

```sh
mkdir -p "$HOME/wormgpt"
cp wormgpt.py wormgpt_welcome.wav "$HOME/wormgpt/"
pkg install python
pip install aiohttp
```

Store your key outside the repo (the app never embeds one):

```sh
printf '%s' 'your-key-here' > "$HOME/.wormgpt_key"
chmod 600 "$HOME/.wormgpt_key"
```

Launch from anywhere:

```sh
cp bin/wormgpt "$PREFIX/bin/wormgpt"
chmod +x "$PREFIX/bin/wormgpt"
wormgpt
```

Or set `WORMGPT_API_KEY` in your environment and run `python3 wormgpt.py` directly.

Optional ordered API profiles go in `~/.wormgpt_profiles`, one per line:

```text
gemini | another-api-key | gemini-2.5-flash
openai | sk-... | gpt-4o-mini
groq | gsk_... | llama-3.3-70b-versatile
openrouter | sk-or-... | openai/gpt-4o-mini
anthropic | sk-ant-... | claude-3-5-haiku-latest
```

Supported providers: `gemini`, `openai` / `chatgpt`, `openrouter`, `groq`, `together`, `anthropic` / `claude`, and `opencode` / `zen`. OpenCode's free models only work inside OpenCode and return HTTP 429 from direct API calls; to use OpenCode here you need an OpenCode Console API key **and** credits for paid models (default `gpt-6-luna`). A key with no balance returns HTTP 402. Shorthands: `KEY | MODEL` defaults to Gemini, a model-only line reuses the primary key, and a key-only line uses that provider's default model. Keys are never mistaken for model names. Keep this file private with `chmod 600 ~/.wormgpt_profiles`. `/model` lists configured providers and models without showing keys. Gemini profiles query the model list at runtime and automatically pick currently available `generateContent` models; 503/404 responses trigger a refreshed model list, so retired model names are never used.

The Android app uses the same profile format in Settings → **API PROFILES / FALLBACKS**.

For a home-screen shortcut, copy `termux-widget/wormgpt` into `~/.shortcuts/` and add a Termux:Widget widget.

## Native Android app

A Kotlin/Android Studio project lives in [`android/`](android/README.md). You can build the APK in the cloud with the included GitHub Actions workflow — no local Android SDK needed — or open the folder in Android Studio. The app uses the same Master/Ruler styling, oni art, Termux UI startup sounds, and Gemini prompt, with the API key entered inside the app instead of embedded in source.

### Android chat features

The chat screen includes:

- Animated message bubbles with slide/scale entrance per role
- Bouncing three-dot “SENTINAL” typing indicator
- Status line with live progress bar and message count (`SECURE CHANNEL · READY · N MSG`)
- Quick-prompt chips on a fresh chat: **CHECK LINK**, **REVIEW CODE**, **THREAT BRIEF**
- Cyan signal-line sweep when a reply arrives
- Send-button pulse feedback
- Long-press any bubble to copy that message
- **RETRY** chip to resend the last prompt
- Animated **LATEST ↓** jump button when scrolled up
- Persistent chat history across restarts
- Styled fenced/inline code inside replies
- Conversation context sent to the model for follow-up questions
- Reduced-motion settings respected throughout

### Auto improvement research

Both the Android app and the terminal UI can run an auto-improvement research pass:

- Collects non-sensitive local signals such as build/version, configured profile count, and message count
- Fetches public guidance from Android developer docs, OWASP Mobile App Security, and the project README
- Sends that context to Gemini with a defensive quality-auditor instruction
- Returns a prioritized improvement plan plus quick wins
- Android saves the report to `sentinal_auto_improvement.txt`; the terminal stores it in the session history

Android: use **AUTO IMPROVEMENT RESEARCH** in Tools / Security Tools, or enable the Settings switch to run automatically at most once per day. Terminal: run `improve`.

### Dashboard and navigation polish

- Animated protection-score gauge with tick marks, breathing glow, and scanner sweep
- Score counts up and changes colour as guards/permissions/setup improve
- Sliding bottom-navigation indicator between Home / Chat / Tools
- Staggered dashboard card entrance and reduced-motion support

### System UI upgrades

- Edge-to-edge layout with transparent status and navigation bars
- Light system-bar icons matched to the dark Sentinal theme
- Branded Android 12+ splash screen with a smooth scale/fade exit
- Activity open/close transition animations
- Keyboard composer lifts with a smooth animated inset instead of jumping
- Page transitions use a soft scale/slide entrance with reduced-motion fallbacks
- Launch seal rotates into view and the launch overlay exits with a scale/fade
- All dialogs inherit the Sentinal scale/fade window animation
- Crypto chart line draws itself with a progressive reveal when new data arrives

### Chat graphics refresh

- Redesigned message bubbles with gradient fills, accent borders, and directional "tails"
- Role headers with coloured status dot for Master / Sentinal / system reports
- Animated aurora backdrop behind chat messages
- Richer reply formatting: accent bars on code blocks, bold headings, coloured bullets, and underlined links
- Depth-settle animation on new bubbles and staggered reveal on restored conversations
- Typing dots now bounce and scale together

### Tool and profile dialogs

- Security tool results now open in a rich, animated card with a scanning line
- Security tools menu uses staggered sliding rows instead of a plain list
- Owner profile tool buttons have press feedback and the dialog keeps its staggered reveal

### avast-inspired defensive additions

Inspired by common commercial-suite features, implemented as local, privacy-preserving checks:

- **Smart Scan** — animated multi-step local configuration review covering device lock, call/SMS guards, notifications, blocklist, quarantine, AI profile, Wi-Fi, and app cache, with a 0–100 score
- **Wi-Fi Inspector** — read-only report of SSID, band, signal, link speed, IP, and internet validation, plus WPA2/WPA3 guidance
- **Storage / App Cache** — measures and clears only Sentinal Security's own cache
- Ransomware-readiness check folded into Smart Scan via Android's secure-lock state

These are local checks only. The app does not scan for malware, run a firewall/VPN, or upload device data.

## Commands

- `exit`, `quit`, `q` — leave the app
- `/help` — show available commands
- `clear`, `cls` — clear the current conversation view
- `copy`, `cp` — copy the last response to the clipboard and `~/wormgpt_last_response.txt`
- `save` / `export txt|md|json` — export the transcript to `~/wormgpt_history.*`
- `search <words>` — find text in the current conversation
- `sessions`, `resume <ID|latest>`, `new` — list, resume, or start saved conversations
- `blocks`, `copycode N`, `savecode N` — inspect, copy, or save a code block from the last reply
- `test`, `t` — check supported code syntax in the last reply
- `retry`, `r` — resend the last user prompt
- `status` — show session id and message count
- `quick [link|code|threat]` — list or send a quick prompt
- `copymsg N` — copy a specific message by turn number
- `improve` — research public guidance online and suggest app improvements
- `prev`, `up`, `pgup` / `next`, `down`, `pgdn` — scroll through older/newer replies
- `top`, `home` / `bottom`, `end`, `latest` — jump to the oldest/newest reply

Arrow keys, Home/End, and backspace work in the `Master ❯` prompt.

### Code testing

Code blocks are not executed automatically. The `test` command checks Python and shell syntax only; `copycode N` and `savecode N` let you review or edit a selected block yourself. Conversations are saved locally in `~/wormgpt_sessions.json` (up to 20 sessions), without API keys.

## Files

- `wormgpt.py` — application
- `wormgpt_welcome.wav` — startup greeting audio
- `bin/wormgpt` — command launcher
- `termux-widget/wormgpt` — Termux:Widget shortcut
