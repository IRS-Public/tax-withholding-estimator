PORT ?= 3000
XML_FILES := $(shell git ls-files '*.xml')
FG_SOURCE_DIR := ../fact-graph/js/target/scala-3.3.6/factgraph-fastopt
FG_TARGET_DIR := ./src/main/resources/twe/website-static/vendor/fact-graph/

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

.PHONY: format-xml
format-xml:
	@for file in $(XML_FILES); do \
		echo "Formatting $$file"; \
		xmllint --format "$$file" > "$$file.tmp" && mv "$$file.tmp" "$$file"; \
	done

.PHONY: format-xml-check
format-xml-check:
	@failed=0; \
	for file in $(XML_FILES); do \
		if ! xmllint --format "$$file" | diff -q "$$file" - >/dev/null; then \
			echo "✗ $$file is not properly formatted"; \
			failed=1; \
		fi; \
	done; \
	exit $$failed

.PHONY: format_check
format_check:
	make format-xml-check
	sbt scalafmtCheckAll

.PHONY: ci_format_check
ci_format_check:
	make format-xml-check
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
	semgrep scan --verbose \
		--metrics off \
	  --config p/security-audit \
		--config p/scala \
	  --severity WARNING \
	  --error

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
