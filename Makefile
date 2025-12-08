PORT ?= 3000
TWE_RESOURCES_DIR := ./src/main/resources/twe

FLOW_DIR := $(TWE_RESOURCES_DIR)/flow
FLOW_CONFIG := $(FLOW_DIR)/FlowConfig.rng

FACTS_DIR := $(TWE_RESOURCES_DIR)/facts
FACTS_CONFIG := $(FACTS_DIR)/FactDictionaryModule.rng

FG_SOURCE_DIR := ../fact-graph/js/target/scala-3.3.6/factgraph-fastopt
FG_TARGET_DIR := ./src/main/resources/twe/website-static/vendor/fact-graph

# Build and run development server, watching for changes
.PHONY: dev
dev:
	sbt -Dsmol.port=$(PORT) '~run --serve --auditMode'

# Build site for production
.PHONY: twe
twe:
	sbt run

# Copy compiled Fact Graph from sibling reposiorty
.PHONY: copy-fg
copy-fg:
	cp $(FG_SOURCE_DIR)/main.mjs $(FG_TARGET_DIR)/factgraph-3.1.0.js
	cp $(FG_SOURCE_DIR)/main.mjs.map $(FG_TARGET_DIR)

.PHONY: test
test:
	# This only prints the tests that fail
	sbt -info 'set Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-oC")' test

.PHONY: format
format:
	make format-xml
	sbt scalafmtAll

# Run most of the CI checks locally
.PHONY: ci
ci:
	make validate-xml
	make ci_validate-format
	make ci_validate-html
	make ci_validate-js
	# Skipping semgrep (locally) for now

.PHONY: clean
clean:
	rm -rf ./target/
	find ./project -name target | xargs rm -rf
	rm -rf ./out/

.PHONY: validate-xml
validate-xml:
	find $(FLOW_DIR) -name '*xml' | xargs xmllint --relaxng $(FLOW_CONFIG) > /dev/null
	find $(FACTS_DIR) -name '*xml' | xargs xmllint --relaxng $(FACTS_CONFIG) > /dev/null

.PHONY: format-xml
format-xml:
	find $(TWE_RESOURCES_DIR) -name '*xml' | xargs -I {} xmllint --format {} --output {}

.PHONY: ci_setup
ci_setup:
	npm --prefix $(TWE_RESOURCES_DIR) install

.PHONY: ci_validate-format
ci_validate-format:
	find $(TWE_RESOURCES_DIR) -name '*xml' | \
		xargs -I {} bash -c "diff {} <(xmllint --format {})"
	sbt -warn scalafmtCheckAll

.PHONY: ci_validate-html
ci_validate-html:
	npm --prefix $(TWE_RESOURCES_DIR) run html-validate

.PHONY: ci_validate-js
ci_validate-js:
	npm --prefix $(TWE_RESOURCES_DIR) run lint

.PHONY: ci_semgrep
ci_semgrep:
	semgrep scan --verbose --metrics off --severity WARNING --error \
		--config p/security-audit --config p/scala
