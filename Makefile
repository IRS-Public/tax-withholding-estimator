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
	sbt test

.PHONY: format
format:
	make format-xml
	sbt scalafmtAll

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

.PHONY: ci-format-check
ci_format_check:
	find $(TWE_RESOURCES_DIR) -name '*xml' | \
		xargs -I {} bash -c "diff {} <(xmllint --format {})"
	sbt -warn scalafmtCheckAll

# These command line flags only output the failed tests, for a simpler CI output
.PHONY: ci_test
ci_test:
	sbt -info 'set Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-oC")' test

.PHONY: ci_test
ci_security_scan:
	make ci_html_validate ci_semgrep ci_eslint_check

# Scala JVM  static analysis with Semgrep
.PHONY: ci_semgrep
ci_semgrep:
	semgrep scan --verbose --metrics off --severity WARNING --error \
		--config p/security-audit --config p/scala


# HTML validation with Thymeleaf-aware security profile
.PHONY: ci_html_validate
ci_html_validate:
	npx html-validate --config src/main/resources/twe/security/.htmlvalidate.json \
	"src/main/resources/twe/templates/fragments/*.html" \
	"out/*.html"

# JavaScript security linting with ESLint
.PHONY: ci_eslint_check
ci_eslint_check:
	npx eslint --config .github/eslint.config.mjs
