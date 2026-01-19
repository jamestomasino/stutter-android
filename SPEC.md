# Stutter, Native Reader App

## Specification, v1

This document is the single source of truth for architecture, constraints, and behavior.
All implementation must conform to this spec. When in doubt, this document wins.

---

## 1. Non negotiables

* 100% native Android, Kotlin only.
* No JavaScript, no WebView, no embedded browser.
* Dedicated reader app, not an overlay.
* Plain HTTP client, no cookies, no shared browser state.
* ICU based word segmentation.
* International hyphenation is required for long words.
* Timing accuracy is a primary feature.
* Coroutines based scheduling using a monotonic clock.
* Settings stored with Jetpack DataStore.
* No history, no recents, no content persistence beyond the current session.
* Code under /reference must never be imported, compiled, or depended on.

Explicitly forbidden:

* WebView
* CookieManager
* Browser cookie sharing
* Background services unrelated to reading
* Storing fetched articles
* Timing tied to UI frame callbacks

---

## 2. User flows

### 2.1 Paste text

* User pastes plain text into the app.
* Text is read immediately.

### 2.2 Share text

* App receives `ACTION_SEND` with `text/plain`.
* If content is not a URL, treat as plain text.

### 2.3 Share URL

* App receives URL via share.
* App fetches HTML with a plain HTTP client.
* App extracts readable content using a Readability style extractor.
* App reads extracted text.

### 2.4 Authenticated pages

* URLs requiring login will usually fail.
* App must detect likely login or paywall pages and show a clear message:

  * Suggest pasting text instead.
  * Optionally suggest sharing a PDF if supported later.

This limitation is intentional.

---

## 3. Architecture overview

The system is a pipeline of isolated modules.
Each module is testable in isolation.

```
Input
  -> Fetcher (optional)
  -> Extractor
  -> Language Resolver
  -> Tokenizer (ICU)
  -> Hyphenator
  -> Scheduler
  -> ReaderView
```

---

## 4. Core modules and contracts

### 4.1 Fetcher

Responsibility:

* Fetch HTML from a URL.

Requirements:

* Plain HTTP client.
* No cookies.
* Redirects capped.
* Response size capped.
* Timeouts enforced.

Output:

* Raw HTML string or error.

---

### 4.2 Extractor

Responsibility:

* Convert HTML into readable text.

Requirements:

* Readability style algorithm.
* No JavaScript execution.
* Extract:

  * Main text
  * Optional title
  * Optional language tag from HTML

Output:

```kotlin
data class ExtractedContent(
  val text: String,
  val title: String?,
  val languageTag: String?
)
```

Fallback behavior:

* If extraction fails, attempt heuristic text stripping.
* If still empty, return error.

---

### 4.3 Language resolution

Language selection order:

1. Language declared in extracted HTML.
2. User configured default language.
3. Device locale.

Language tag format:

* BCP 47 (for example `en`, `en-US`, `de`, `fr`).

---

### 4.4 Tokenizer

Responsibility:

* Split text into word tokens.

Requirements:

* Use ICU `BreakIterator`.
* Language aware.
* Preserve punctuation information needed for timing logic.

Output tokens must include:

* Original text
* Flags for sentence ending, punctuation, numeric, short word, long word

---

### 4.5 Hyphenator

Responsibility:

* Split long words exceeding `maxWordLength`.

Requirements:

* International ready.
* TeX hyphenation patterns preferred.
* Language aware.
* Fallback to safe hard split if patterns unavailable.

Contract:

```kotlin
interface Hyphenator {
    fun split(word: String, languageTag: String?, maxLength: Int): List<String>
}
```

---

### 4.6 Scheduler

Responsibility:

* Emit words at precise times.

Requirements:

* Coroutine based.
* Uses a monotonic clock.
* Precomputes target timestamps.
* Corrects for drift.
* Supports pause, resume, skip, restart.

Scheduler must be UI agnostic.

---

### 4.7 ReaderView

Responsibility:

* Render the current word.

Requirements:

* Pure custom view.
* No TextView composition.
* Draw:

  * Left part
  * Center part
  * Remainder
  * Optional flanker
* Cache text measurements.
* Never control timing.

---

## 5. Settings model

Settings are the only persisted state.
Stored via DataStore.

### 5.1 PlaybackOptions

Defaults and clamps are mandatory.

* `wpm`: Int, default 400, clamp 100..1800
* `slowStartCount`: Int, default 5, clamp 1..10
* `sentenceDelay`: Float, default 2.5, clamp 1..10
* `otherPuncDelay`: Float, default 1.5, clamp 1..10
* `shortWordDelay`: Float, default 1.3, clamp 1..10
* `longWordDelay`: Float, default 1.4, clamp 1..10
* `numericDelay`: Float, default 1.8, clamp 1..10
* `skipCount`: Int, default 10, clamp 0..100

### 5.2 TextHandlingOptions

* `maxWordLength`: Int, default 13, clamp 5..50
* `showFlankers`: Boolean, default false

### 5.3 LanguageOptions

* `autoDetectFromHtml`: Boolean, default true
* `defaultLanguageTag`: String?, optional

### 5.4 AppearanceOptions

Purely visual, no effect on timing.

* Base text size
* Center emphasis scale
* Font family
* Colors for background, left, center, remainder, flanker
* Letter spacing
* Padding
* Optional bold center letter

---

## 6. Timing accuracy requirements

* Scheduler timing must be based on a monotonic clock.
* Drift over long sessions must be corrected.
* Pause and resume must not change word order or timing multipliers.
* UI rendering must not influence scheduling.

---

## 7. Definition of done

A build is considered correct only if:

* All non negotiables are satisfied.
* Core logic has unit tests:

  * Scheduler timing
  * Tokenization
  * Hyphenation
  * Language resolution
* App can:

  * Read pasted text
  * Read shared text
  * Fetch, extract, and read a URL
* No WebView or JavaScript exists anywhere in the codebase.
* No history or content persistence exists.

---

## 8. Guiding principle

This app is a timing instrument, not a browser.
Correctness, predictability, and clarity beat cleverness.
