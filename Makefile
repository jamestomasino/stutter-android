# Makefile, Stutter (native)

GRADLEW := ./gradlew

help: ## list targets
	@echo "targets:"
	@grep -E '^[a-zA-Z0-9_-]+:.*?## .*$$' $(MAKEFILE_LIST) \
	| sed -n 's/^\(.*\): \(.*\)##\(.*\)/  \1|\3/p' \
	| column -t -s '|'

doctor: ## print toolchain expectations
	@echo "Requires:"
	@echo "  - JDK (version per Gradle config)"
	@echo "  - Android SDK with platform tools"
	@echo "  - ANDROID_SDK_ROOT or ANDROID_HOME set"
	@echo ""
	@if [ ! -x "$(GRADLEW)" ]; then \
		echo "ERROR: gradlew not found or not executable."; \
		echo "Run 'make bootstrap' to generate the Gradle wrapper."; \
		exit 1; \
	fi
	@$(GRADLEW) -v

bootstrap: ## generate Gradle wrapper if missing (one time)
	@if [ -x "$(GRADLEW)" ]; then \
		echo "gradlew already present."; \
	else \
		echo "Generating Gradle wrapper..."; \
		gradle wrapper; \
		chmod +x gradlew; \
	fi

setup: doctor ## download dependencies and prime Gradle caches
	@$(GRADLEW) --no-daemon --refresh-dependencies tasks >/dev/null

deps: ## refresh dependencies
	@$(GRADLEW) --no-daemon --refresh-dependencies

build: ## build debug APK
	@$(GRADLEW) --no-daemon assembleDebug

release: ## build release APK
	@$(GRADLEW) --no-daemon assembleRelease

test: ## run unit tests
	@$(GRADLEW) --no-daemon test

check: ## run all verification tasks
	@$(GRADLEW) --no-daemon check

lint: ## run Android lint
	@$(GRADLEW) --no-daemon lint

install: ## install debug build on device
	@$(GRADLEW) --no-daemon installDebug

run: ## install and launch reader activity on device/emulator
	@adb devices | awk 'NR>1 && $$2=="device" {found=1} END {if (!found) {print "ERROR: no connected devices/emulators. Start an AVD or plug in a device."; exit 1}}'
	@$(GRADLEW) --no-daemon installDebug
	@adb shell am start -n org.tomasino.stutter/.ReaderActivity

debug: ## install, launch, and stream logcat for the app
	@adb devices | awk 'NR>1 && $$2=="device" {found=1} END {if (!found) {print "ERROR: no connected devices/emulators. Start an AVD or plug in a device."; exit 1}}'
	@$(GRADLEW) --no-daemon installDebug
	@adb shell am start -n org.tomasino.stutter/.MainActivity >/dev/null
	@pid=$$(adb shell pidof org.tomasino.stutter | tr -d '\r'); \
	if [ -z "$$pid" ]; then \
		for i in 1 2 3 4 5; do \
			sleep 0.5; \
			pid=$$(adb shell pidof org.tomasino.stutter | tr -d '\r'); \
			[ -n "$$pid" ] && break; \
		done; \
	fi; \
	if [ -z "$$pid" ]; then \
		echo "ERROR: app process not found. Launch the app and retry."; \
		exit 1; \
	fi; \
	echo "Streaming logcat for pid $$pid (Ctrl+C to stop)"; \
	adb logcat --pid $$pid

connected: ## run instrumentation tests
	@$(GRADLEW) --no-daemon connectedAndroidTest

clean: ## clean build outputs
	@$(GRADLEW) --no-daemon clean

ci: clean setup check ## local CI pipeline

.PHONY: help doctor bootstrap setup deps build release test check lint install run debug connected clean ci
