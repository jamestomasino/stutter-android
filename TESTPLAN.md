# TESTPLAN.md
## Stutter Android, Native Reader App, Test and Verification Plan v1

This document defines the tests and checks required to verify correctness.
Any behavior not covered here is allowed to change.
Any behavior covered here must not regress.

Tests are divided into unit tests (JVM), integration tests, and manual verification.

---

## 1. Core principles

- Core logic must be testable without Android UI.
- Timing correctness is more important than visual polish.
- Tests should prefer deterministic clocks and fixtures.
- Fail fast on violations of SPEC.md non negotiables.

---

## 2. Unit tests, required

### 2.1 PlaybackOptions and clamps
Tests:
- Default values exactly match SPEC.md.
- Values below minimum clamp to minimum.
- Values above maximum clamp to maximum.
- Integer coercions behave correctly (`maxWordLength`, `skipCount`).

Acceptance:
- No setter allows an out of range value to persist.
- All defaults are correct on first launch.

---

### 2.2 Language resolution
Tests:
- HTML language tag present overrides user default.
- User default overrides device locale when HTML tag missing.
- Device locale used when both HTML and user default missing.
- Invalid or empty language tags fall back safely.

Acceptance:
- Resolved language tag is deterministic and documented.

---

### 2.3 Tokenization (ICU)
Tests:
- English sentence tokenization produces expected word boundaries.
- German compound words are treated as single tokens before hyphenation.
- French punctuation handling does not merge words incorrectly.
- Japanese or non Latin script tokenization produces non empty tokens.

Tests must verify:
- Sentence ending detection.
- Other punctuation detection.
- Numeric token detection.
- Short vs long word classification.

Acceptance:
- Tokenization output is stable across runs.
- No empty tokens are produced.

---

### 2.4 Hyphenation fallback
Tests:
- Words longer than `maxWordLength` are split.
- No segment exceeds `maxWordLength`.
- No segment is empty.
- Punctuation adjacent to long words does not break splitting.

Acceptance:
- App never crashes or overflows due to long tokens.

---

### 2.5 Hyphenation with patterns
Tests (per supported language):
- Known words split at plausible hyphenation points.
- Chosen split respects `maxWordLength`.
- Multiple split points choose the best fit, not the first blindly.

Fallback tests:
- Unknown language uses fallback splitter.

Acceptance:
- Pattern based hyphenation improves splits without breaking fallback safety.

---

### 2.6 Scheduler timing
Tests use a fake or injected monotonic clock.

Tests:
- Given a fixed token list and options, scheduler emits tokens in order.
- Pause freezes token index and time.
- Resume continues from the same token.
- Restart resets to token zero.
- Skip forward and backward move by `skipCount`.

Drift tests:
- Over a simulated long run (for example 2 minutes), cumulative drift stays within defined tolerance (example 100 ms).

Acceptance:
- Scheduler behavior is deterministic under test.
- No dependence on UI or frame timing.

---

## 3. Integration tests

### 3.1 Text to playback pipeline
Tests:
- Plain text input flows through tokenizer, scheduler, and emits events.
- Long words are split and scheduled correctly.
- Appearance changes do not affect scheduler timing.

Acceptance:
- End to end playback works without network.

---

### 3.2 Fetcher
Tests:
- Redirects stop after configured cap.
- Responses exceeding size cap are rejected.
- Timeouts trigger failure.
- No cookies are sent or stored.

Acceptance:
- Fetcher respects all constraints from SPEC.md.

---

### 3.3 Extractor
Tests using local HTML fixtures:
- Extracts main content text from a typical article.
- Extracts language tag when present.
- Falls back to heuristic extraction when main extractor fails.
- Returns error when content is effectively empty.

Acceptance:
- Extracted text is non empty for known good fixtures.
- Language tag behavior matches language resolution tests.

---

### 3.4 Login and paywall detection
Tests:
- HTML fixture representing a login page is detected.
- App surfaces the correct user facing error message.

Acceptance:
- Logged in pages do not silently fail or display junk.

---

## 4. UI and rendering tests

### 4.1 ReaderView logic
Unit tests or lightweight instrumentation tests:
- Word parts (left, center, remainder, flanker) are computed correctly.
- Text measurement cache is reused across frames.
- No crashes on rapid word updates.

Acceptance:
- Rendering logic is stable under stress.

---

### 4.2 Settings propagation
Tests:
- Changing playback settings updates scheduler behavior.
- Changing appearance settings updates rendering only.
- Changing text handling options affects token preparation.

Acceptance:
- No settings change requires app restart.
- No setting affects unintended subsystems.

---

## 5. Manual verification checklist

Must be verified manually before release:

- Paste text and read works.
- Share text into app works.
- Share public article URL works.
- Share authenticated article URL fails gracefully with message.
- Pause, resume, skip, restart work reliably.
- Screen stays on during playback.
- App has no history or recent list.
- No overlay permissions requested.
- No analytics or tracking libraries present.

---

## 6. Regression policy

- Any failing test blocks progress to the next milestone.
- New features require new tests.
- Timing or tokenization regressions are release blockers.

---

## 7. Definition of test completeness

This test plan is complete when:
- All required unit tests exist and pass.
- Integration tests cover fetch, extract, tokenize, schedule.
- Manual checklist passes on at least one physical device and one emulator.
