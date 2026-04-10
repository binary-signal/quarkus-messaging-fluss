#JAVA_HOME ?= $(HOME)/.sdkman/candidates/java/17.0.18-zulu
#export JAVA_HOME
#export PATH := $(JAVA_HOME)/bin:$(PATH)

MVN := mvnd
MVN_FLAGS := -e

.PHONY: build clean install test verify package tree format format-check help

## Build & Install

build: ## Compile all modules
	$(MVN) $(MVN_FLAGS) compile

install: ## Build and install to local Maven repo
	$(MVN) $(MVN_FLAGS) clean install

install-fast: ## Install skipping tests
	$(MVN) $(MVN_FLAGS) clean install -DskipTests

package: ## Package JARs without installing
	$(MVN) $(MVN_FLAGS) clean package -DskipTests

## Testing

test: ## Run all tests
	$(MVN) $(MVN_FLAGS) test

test-runtime: ## Run runtime module tests only
	$(MVN) $(MVN_FLAGS) test -pl runtime

test-deployment: ## Run deployment module tests only
	$(MVN) $(MVN_FLAGS) test -pl deployment

## Quality

format: ## Format Java sources with Palantir via Spotless
	$(MVN) $(MVN_FLAGS) spotless:apply

format-check: ## Check formatting without modifying files
	$(MVN) $(MVN_FLAGS) spotless:check

verify: ## Run full verify lifecycle (compile, test, format check)
	$(MVN) $(MVN_FLAGS) clean verify

## Cleanup

clean: ## Remove all build artifacts
	$(MVN) $(MVN_FLAGS) clean

## Info

tree: ## Show dependency tree
	$(MVN) $(MVN_FLAGS) dependency:tree

tree-runtime: ## Show runtime module dependency tree
	$(MVN) $(MVN_FLAGS) dependency:tree -pl runtime

versions: ## Show plugin and dependency version updates
	$(MVN) $(MVN_FLAGS) versions:display-dependency-updates versions:display-plugin-updates

java-version: ## Print Java version in use
	java -version

## Help

help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'

.DEFAULT_GOAL := help
