# Development Guide

This document contains engineering, build, and release process details for Stutter.

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

## Settings

Settings are stored using Jetpack DataStore and include:

- Reading speed and timing multipliers.
- Long word handling and flanker display.
- Language defaults.
- Appearance options like font, size, spacing, and colors.

There is no theme system. Appearance is fully user configurable.

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

## Build and development commands

Use the Makefile wrappers (preferred) or Gradle tasks directly:

- `make doctor`: verify JDK/SDK setup and Gradle wrapper.
- `make setup`: download dependencies and warm Gradle caches.
- `make build`: assemble debug APK.
- `make bundle`: build release AAB.
- `make test`: run local unit tests.
- `make connected`: run instrumented tests on a device/emulator.
- `make lint`: run Android lint.
- `make ci`: clean + setup + check (local CI pipeline).

Note: Instrumented tests currently require JDK 17 due to a UTP/Java 21
compatibility issue. The `make connected` and `make screenshots` targets
use `scripts/gradlew-java17.sh` to select a local JDK 17 if available.

## Distribution

This project is intended to be distributed via F-Droid.

All dependencies must be open source and F-Droid compatible.
No proprietary SDKs, analytics, or trackers are permitted.

Release details are in `RELEASE.md`.
