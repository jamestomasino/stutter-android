# PLAN.md
## Stutter, Native Reader App, Implementation Plan v1

This plan sequences the work into milestones that produce runnable, testable increments.
After each milestone:
- Run unit tests.
- Ensure SPEC.md non negotiables remain satisfied.
- Do not start the next milestone until the current milestone builds and tests pass.

Repository layout assumptions:
- `app/` contains the Android app module.
- `docs/` is optional, but this repo uses `/SPEC.md` and `/PLAN.md` at the root.

---

## Milestone 0, Repo and build scaffold
Goal: A clean Kotlin Android project with CI friendly test execution.

Deliverables:
- Android app builds in debug.
- Unit tests run from command line.
- No WebView dependency.

Tasks:
- Create a new Android project, Kotlin, minimum SDK chosen for ICU support needs.
- Add Jetpack DataStore dependencies.
- Add Kotlin coroutines dependencies.
- Add test dependencies (JUnit, kotlinx-coroutines-test).
- Add a basic `MainActivity` placeholder.

Acceptance checks:
- `./gradlew test` succeeds.
- Grep check: no `WebView`, no `android.webkit` usage.

---

## Milestone 1, Options model and DataStore
Goal: Implement the settings model as the single persisted state and enforce clamps.

Deliverables:
- Kotlin data classes:
  - `PlaybackOptions`
  - `TextHandlingOptions`
  - `LanguageOptions`
  - `AppearanceOptions`
  - `StutterOptions` (aggregate)
- DataStore repository:
  - `StateFlow<StutterOptions>`
  - setter functions applying clamps
- Defaults exactly match SPEC.md.

Tasks:
- Decide DataStore flavor:
  - Preferences DataStore (fastest)
  - Proto DataStore (stronger typing)
- Implement clamp utilities once, used by all setters.
- Implement a minimal settings screen or placeholder UI hook that reads the options flow.

Tests:
- Unit tests for clamp behavior and defaults.

Acceptance checks:
- Changing out of range values clamps correctly.
- Defaults match SPEC.md exactly.

---

## Milestone 2, Token model and ICU tokenization
Goal: Turn text into tokens accurately, across languages.

Deliverables:
- Token data model with flags required for timing.
- ICU BreakIterator tokenizer implementation.

Tasks:
- Implement `Tokenizer` interface:
  - `fun tokenize(text: String, languageTag: String?): List<Token>`
- Ensure tokens preserve punctuation needed for:
  - sentence ending detection
  - other punctuation detection
  - numeric detection
  - short and long word detection rules

Tests:
- Tokenization tests with multilingual fixture strings:
  - English, German, French, Japanese (at least)
- Sentence boundary and punctuation flag tests.

Acceptance checks:
- Tokenizer output is stable and language aware.
- Unit tests cover at least one non Latin script case.

---

## Milestone 3, Hyphenation interface and fallback splitting
Goal: Make long word handling work end to end, even before adding full hyphenation patterns.

Deliverables:
- `Hyphenator` interface per SPEC.md.
- Fallback implementation that:
  - hard splits long words at safe boundaries
  - respects `maxWordLength`
  - never produces empty segments

Tasks:
- Implement `split(word, languageTag, maxLength)` returning segments.
- Integrate into token preparation stage:
  - if token length exceeds max, replace token with multiple tokens segments
  - preserve timing flags appropriately

Tests:
- Hard split respects max length.
- No segment is empty.
- Edge cases: very long strings, whitespace, punctuation adjacent.

Acceptance checks:
- App can render long words without overflow or crash using fallback splitting.

---

## Milestone 4, Scheduler core with monotonic clock and drift correction
Goal: Accurate timing engine, independent of UI.

Deliverables:
- `Scheduler` implementation using coroutines.
- Controls: play, pause, resume, restart, skip forward/back.
- Emits events with token index and token data.

Tasks:
- Use a monotonic time source (injectable clock for tests).
- Precompute word target timestamps from options and token flags.
- Implement drift correction by scheduling to target timestamps, not chaining delays.
- Implement state machine:
  - Idle, Playing, Paused, Finished

Tests:
- Deterministic scheduler tests using fake clock and coroutines test dispatcher.
- Drift bound test over a simulated long run.
- Pause/resume preserves token index and schedule.

Acceptance checks:
- Scheduler tests pass reliably.
- Scheduler does not depend on any Android UI classes.

---

## Milestone 5, ReaderView custom drawing and integration
Goal: Display pipeline works with accurate timing.

Deliverables:
- `ReaderView`, a custom view drawing left, center, remainder, optional flanker.
- `ReaderActivity` wiring:
  - receives tokens
  - connects scheduler outputs to the view
  - provides controls (play/pause, skip, restart, open settings)

Tasks:
- Implement text measurement caching.
- Implement layout decisions:
  - stable region for word display
  - controls do not shift the word block
- Implement appearance options:
  - sizes, colors, spacing, padding

Tests:
- Basic instrumentation or snapshot style sanity tests are optional.
- Unit tests for word splitting logic (left/center/remainder) strongly recommended.

Acceptance checks:
- App can read pasted text end to end with stable display.
- Appearance changes update rendering without breaking scheduler.

---

## Milestone 6, Fetcher and Extractor (Readability style)
Goal: URL in, readable text out, without JS.

Deliverables:
- `Fetcher` with:
  - redirect cap
  - timeout
  - size cap
  - no cookies
- `Extractor` producing:
  - text
  - title optional
  - languageTag optional

Tasks:
- Implement fetch errors with clear user messages.
- Add extractor fallback heuristic if main extractor yields empty.

Tests:
- Fetcher tests using mocked HTTP or local test server.
- Extractor tests using HTML fixtures in `src/test/resources`.

Acceptance checks:
- Sharing a public article URL results in readable text and playback.
- Login/paywall detection triggers the expected user message.

---

## Milestone 7, Real hyphenation patterns (international)
Goal: Replace fallback splitting with true hyphenation.

Deliverables:
- Hyphenation implementation backed by TeX patterns.
- Pattern assets for a reasonable set of languages.

Tasks:
- Choose a TeX pattern hyphenation library compatible with Android.
- Package patterns in app assets or resources.
- Load patterns lazily and cache per language.
- Keep fallback splitting for unknown languages.

Tests:
- Language specific hyphenation tests, at least:
  - German compound example
  - English example
  - One additional language if patterns available
- Verify segments obey maxWordLength when selecting split points.

Acceptance checks:
- Long words split at plausible hyphenation points when patterns exist.
- Unknown language still safe splits.

---

## Milestone 8, Settings UI polish and accessibility pass
Goal: Make it usable.

Deliverables:
- Settings screens for all v1 options:
  - playback
  - text handling
  - language
  - appearance
- Basic accessibility:
  - large text support
  - contrast sensible defaults
  - talkback labels on controls

Tasks:
- Ensure settings changes propagate immediately.
- Provide a reset to defaults action.

Acceptance checks:
- All options in SPEC.md are user adjustable.
- App remains stable under rapid option changes.

---

## Milestone 9, Packaging and F-Droid friendliness
Goal: Make distribution smooth.

Deliverables:
- No proprietary dependencies.
- Reproducible build settings if feasible.
- Clear README describing privacy and limitations.

Acceptance checks:
- Dependency audit: all open source, F-Droid compatible.
- No analytics, no tracking, no history storage.

---

## Completion criteria
The project is complete for v1 when:
- All milestones through 9 pass acceptance checks.
- SPEC.md definition of done is satisfied.
- Tests cover scheduler, tokenization, hyphenation, extraction, and language resolution.
