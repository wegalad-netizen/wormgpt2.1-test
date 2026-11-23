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

Optional ordered fallback profiles go in `~/.wormgpt_profiles`, one per line:

```text
another-api-key | gemini-2.5-flash
gemini-2.0-flash
```

The first line supplies its own key and model. A model-only line reuses the primary `WORMGPT_API_KEY`. Keep this file private with `chmod 600 ~/.wormgpt_profiles`. The app tries profiles in order after connection or API errors, and `/model` lists the configured model order without showing keys.

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
