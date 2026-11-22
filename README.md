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

For a home-screen shortcut, copy `termux-widget/wormgpt` into `~/.shortcuts/` and add a Termux:Widget widget.

## Native Android app

A Kotlin/Android Studio project lives in [`android/`](android/README.md). You can build the APK in the cloud with the included GitHub Actions workflow — no local Android SDK needed — or open the folder in Android Studio. The app uses the same Master/Ruler styling, oni art, Termux UI startup sounds, and Gemini prompt, with the API key entered inside the app instead of embedded in source.

## Commands

- `exit`, `quit`, `q` — leave the app
- `clear`, `cls` — clear the response area
- `copy`, `cp` — copy the last response to the clipboard and `~/wormgpt_last_response.txt`
- `save`, `export` — write the full transcript to `~/wormgpt_history.txt`
- `test`, `t`, `run` — re-run code tests on the last reply
- `prev`, `up`, `pgup` / `next`, `down`, `pgdn` — scroll through older/newer replies
- `top`, `home` / `bottom`, `end` — jump to the oldest/newest reply

Arrow keys, Home/End, and backspace work in the `Master ❯` prompt.

### Code testing

Fenced code blocks in replies are tested automatically. Python blocks are run, shell blocks get a syntax check, and other languages are skipped. Results appear as a `TEST` entry in the chat and in the saved transcript. Set `WORMGPT_AUTOTEST=0` to disable automatic testing and use the `test` command manually instead.

## Files

- `wormgpt.py` — application
- `wormgpt_welcome.wav` — startup greeting audio
- `bin/wormgpt` — command launcher
- `termux-widget/wormgpt` — Termux:Widget shortcut
