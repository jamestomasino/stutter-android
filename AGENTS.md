# Repository Guidelines

## Project Structure & Module Organization
- `app/`: main Android application module (Kotlin + Jetpack Compose).
- `app/src/main/`: production code and resources.
- `app/src/test/`: local JVM unit tests (JUnit).
- `app/src/androidTest/`: instrumented tests (AndroidX test runner).
- Top-level docs: `README.md`, `SPEC.md`, `PLAN.md`, `TESTPLAN.md` define product intent and testing scope.

## Build, Test, and Development Commands
Use the Makefile wrappers (preferred) or Gradle tasks directly.
- `make doctor`: verify JDK/SDK setup and Gradle wrapper.
- `make setup`: download dependencies and warm Gradle caches.
- `make build`: assemble debug APK.
- `make test`: run local unit tests.
- `make connected`: run instrumented tests on a device/emulator.
- `make lint`: run Android lint.
- `make ci`: clean + setup + check (local CI pipeline).

## Coding Style & Naming Conventions
- Kotlin with 4-space indentation; keep files organized by feature.
- Package namespace is `org.tomasino.stutter`.
- Use conventional Kotlin/Android naming: `PascalCase` types, `camelCase` functions/vars.
- Prefer small, testable components (see `README.md` pipeline: tokenizer, scheduler, reader view).

## Testing Guidelines
- Unit tests live in `app/src/test`; instrumented tests in `app/src/androidTest`.
- Name tests with `*Test` and keep behavior-focused test names.
- Run unit tests with `make test` and device tests with `make connected`.
- Timing regressions are release-blocking per `README.md` and `TESTPLAN.md`.

## Commit & Pull Request Guidelines
- Recent commits are short, present-tense statements (e.g., “adds android project”); follow that style.
- Keep commits scoped and describe intent over implementation details.
- PRs should include: summary, testing done (`make test`, `make lint`, etc.), and screenshots for UI changes.

## Configuration Tips
- `local.properties` is machine-specific (SDK path) and should not be committed.
- Set `ANDROID_SDK_ROOT` and `JAVA_HOME` for CLI Gradle usage.
