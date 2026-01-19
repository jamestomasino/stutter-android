# Makefile, Stutter Android (native)

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

connected: ## run instrumentation tests
	@$(GRADLEW) --no-daemon connectedAndroidTest

clean: ## clean build outputs
	@$(GRADLEW) --no-daemon clean

ci: clean setup check ## local CI pipeline

.PHONY: help doctor bootstrap setup deps build release test check lint install connected clean ci
