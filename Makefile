# Makefile, Stutter Android (native)
# Assumes a standard Gradle Android project with ./gradlew at repo root.

GRADLE ?= ./gradlew

help: ## list targets
	@echo "targets:"
	@grep -E '^[a-zA-Z0-9_-]+:.*?## .*$$' $(MAKEFILE_LIST) \
	| sed -n 's/^\(.*\): \(.*\)##\(.*\)/  \1|\3/p' \
	| column -t -s '|'

doctor: ## print toolchain expectations
	@echo "Requires:"
	@echo "  - JDK 17 (or whatever your Gradle config specifies)"
	@echo "  - Android SDK + platform tools (ANDROID_SDK_ROOT or ANDROID_HOME set)"
	@echo "  - Accept Android SDK licenses (sdkmanager --licenses)"
	@echo ""
	@echo "Gradle:"
	@$(GRADLE) -v

setup: ## download dependencies, prime Gradle caches
	@$(GRADLE) --no-daemon --refresh-dependencies tasks >/dev/null

deps: ## refresh dependencies
	@$(GRADLE) --no-daemon --refresh-dependencies

build: ## build debug APK
	@$(GRADLE) --no-daemon assembleDebug

release: ## build release APK (signing required)
	@$(GRADLE) --no-daemon assembleRelease

test: ## run unit tests
	@$(GRADLE) --no-daemon test

check: ## run all verification tasks (tests, lint, etc)
	@$(GRADLE) --no-daemon check

lint: ## run Android lint
	@$(GRADLE) --no-daemon lint

install: ## install debug build on connected device or emulator
	@$(GRADLE) --no-daemon installDebug

connected: ## run instrumentation tests on connected device or emulator
	@$(GRADLE) --no-daemon connectedAndroidTest

clean: ## clean build outputs
	@$(GRADLE) --no-daemon clean

ci: clean setup check ## run a reasonable CI pipeline locally

.PHONY: help doctor setup deps build release test check lint install connected clean ci
