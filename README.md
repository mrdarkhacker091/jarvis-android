# JARVIS — Android voice assistant

Kotlin + Jetpack Compose. No backend. LLM runs on your configured endpoint.

## Features

- Wake word "jarvis" (customizable) with continuous listening service
- Speech in / speech out using Android TTS and SpeechRecognizer
- Chat transcript UI with text fallback
- Reads notifications from any app, replies via RemoteInput
- Tool calling: alarms, timers, torch, volume, brightness, battery, app launching, web search, quick settings toggle
- Remembers user facts via tool calls
- Settings screen for model endpoint, API key, permissions

## Install

1. Download `app-debug.apk` from the latest release
2. Allow "Install unknown apps" for your browser/downloader
3. Install and open
4. Grant permissions in order: mic → notifications → accessibility → write settings → battery exemption
5. Paste your API key in Settings, pick a model preset, save

## Model endpoints

Any OpenAI-compatible endpoint works. Presets include:
- OpenRouter: https://openrouter.ai/api/v1
- Gemini AI: https://generativelanguage.googleapis.com/v1beta/openai
- Groq: https://api.groq.com/openai/v1

## What this does NOT do

- Full WhatsApp/Telegram chat history (Android doesn't expose this)
- Bluetooth/hotspot toggle through public APIs (uses accessibility service to tap tiles instead)
- Play Store distribution (requires different review process)

## Build locally

```bash
./gradlew assembleDebug
```
