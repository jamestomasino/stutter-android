# Stutter for Android

Stutter is a native Android reading application focused on precise, controlled word timing.  
It is designed to help users read text one word at a time at high speeds, with accuracy and predictability as primary goals.

This app is a native Android implementation inspired by the Stutter browser extension, but it is not a browser and does not attempt to behave like one.

---

## What this app is

- A dedicated reader for pasted text, shared text, or fetched article content.
- A timing instrument, where word presentation timing is deterministic and configurable.
- A privacy respectful tool that does not track users or store reading history.
- Fully native Android, written in Kotlin.

---

## What this app is not

- Not a web browser.
- Not an overlay that floats over other apps.
- Not a content aggregator or reading history manager.
- Not capable of accessing logged in browser sessions.

---

## Core design principles

### Timing accuracy
Word timing is the core feature.  
The scheduler uses a monotonic clock with drift correction. UI rendering never controls timing.

### Native only
There is no JavaScript, no WebView, and no embedded browser engine.  
All parsing, segmentation, and rendering is done with native libraries.

### Privacy first
- No cookies.
- No shared browser state.
- No analytics.
- No tracking.
- No history or content persistence beyond the current session.

If you close the app, your content is gone.

### International support
- ICU is used for word segmentation.
- Language aware hyphenation is used for long words.
- Language selection follows this order:
  1. Language declared in the content.
  2. User configured default language.
  3. Device locale.

---

## Supported input methods

### Paste text
Paste any plain text directly into the app and start reading.

### Share text
Share selected text from another app to Stutter.

### Share URL
Share a public article URL to Stutter.  
The app will fetch the page and extract the main readable content.

Authenticated or paywalled pages usually cannot be fetched by URL alone.  
In those cases, paste text instead, or share a PDF if supported.

---

## Architecture overview

The app is structured as a clear pipeline:

```

Input
-> Fetcher (optional)
-> Extractor
-> Language Resolver
-> Tokenizer (ICU)
-> Hyphenator
-> Scheduler
-> Custom Reader View

```

Each component is isolated and testable.

- Tokenization and scheduling are independent of the UI.
- Rendering never influences timing.
- Settings are the only persisted state.

---

## Settings

Settings are stored using Jetpack DataStore and include:

- Reading speed and timing multipliers.
- Long word handling and flanker display.
- Language defaults.
- Appearance options like font, size, spacing, and colors.

There is no theme system. Appearance is fully user configurable.

---

## Testing and correctness

This repository includes:

- A formal specification in `SPEC.md`.
- An implementation plan in `PLAN.md`.
- A test plan in `TESTPLAN.md`.

Core logic is covered by unit tests, including:
- Tokenization.
- Hyphenation.
- Language resolution.
- Scheduler timing and drift correction.

Timing regressions are treated as release blocking issues.

---

## Distribution

This project is intended to be distributed via F-Droid.

All dependencies must be open source and F-Droid compatible.  
No proprietary SDKs, analytics, or trackers are permitted.

---

## Status

This project is under active development.  
The current goal is a stable v1 focused on correctness, clarity, and trust.

---

## License

[GPL3](LICENSE)
