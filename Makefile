PORT ?= 3000
TWE_RESOURCES_DIR := ./src/main/resources/twe
TWE_OUTPUT_DIR := ./out
HTML_VALIDATE_TARGET := ../../../../out/**/*.html

FLOW_DIR := $(TWE_RESOURCES_DIR)/flow
FLOW_CONFIG := $(FLOW_DIR)/FlowConfig.rng

FACTS_DIR := $(TWE_RESOURCES_DIR)/facts
FACTS_CONFIG := $(FACTS_DIR)/FactDictionaryModule.rng

FG_SOURCE_DIR := ../fact-graph/js/target/scala-3.3.6/factgraph-fastopt
FG_TARGET_DIR := ./src/main/resources/twe/website-static/vendor/fact-graph

.PHONY: dev
dev: ## Build and run development server, watching for changes (Default)
	sbt -Dsmol.port=$(PORT) '~run --serve --auditMode'

help: ## Print the help documentation
	@grep -E '^[/a-zA-Z0-9_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'

.PHONY: twe
twe: ## Build site for production
	sbt run


.PHONY: copy-fg
copy-fg: ## Copy compiled Fact Graph from sibling repository
	cp $(FG_SOURCE_DIR)/main.mjs $(FG_TARGET_DIR)/factgraph-3.1.0.js
	cp $(FG_SOURCE_DIR)/main.mjs.map $(FG_TARGET_DIR)

.PHONY: test
test: ## Run test suite
	@# This only prints the tests that fail
	sbt -info 'set Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-oC")' test scalafmtCheckAll

.PHONY: test-watch
test-watch: ## Run and watch tests
	sbt -info 'set Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-oC")' '~test'

.PHONY: format
format: ## Format the Scala and XML code
	@# We only automatically format the fact configs
	find $(FACTS_DIR) -name '*xml' | xargs -I {} xmllint --format {} --output {}
	sbt scalafmtAll
	npm --prefix $(TWE_RESOURCES_DIR) run format

.PHONY: clean
clean: ## Clean all the build artifacts
	rm -rf ./target/
	find ./project -name target | xargs rm -rf
	rm -rf $(TWE_OUTPUT_DIR)

.PHONY: ci-setup
ci-setup: ## Install validation and linting tools
	npm --prefix $(TWE_RESOURCES_DIR) install


.PHONY: ci
ci: ## Run most of the CI checks locally
	make validate-xml
	make validate-html
	make validate-js
	# Skip semgrep (locally) for now

.PHONY: validate-xml
validate-xml: ## Validate .xml files
	find $(FACTS_DIR) -name '*xml' | xargs xmllint --quiet --relaxng $(FACTS_CONFIG) > /dev/null

.PHONY: validate-html
validate-html: ## Validate .html files
	npm --prefix $(TWE_RESOURCES_DIR) run html-validate -- "$(HTML_VALIDATE_TARGET)"

.PHONY: validate-js
validate-js: ## Run javascript linter
	npm --prefix $(TWE_RESOURCES_DIR) run lint

# Semgrep setup is not handled by ci-setup, but done separately in the GHA files
.PHONY:
semgrep:
	semgrep scan --verbose --metrics off --severity WARNING --error \
		--config p/security-audit --config p/scala
