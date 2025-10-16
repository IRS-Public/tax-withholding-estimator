DIR ?= ./out
PORT ?= 3000

.PHONY: dev
dev:
	sbt "~run --auditMode"

.PHONY: twe
twe:
	sbt run

.PHONY: site
site:
	sbt "runMain smol.runServer --port $(PORT) --dir $(DIR)"

.PHONY: copy-fg
copy-fg:
	cp ../fact-graph/js/target/scala-3.3.6/factgraph-fastopt/main.mjs ./src/main/resources/twe/website-static/js/factgraph-3.1.0.js
	cp ../fact-graph/js/target/scala-3.3.6/factgraph-fastopt/main.mjs.map ./src/main/resources/twe/website-static/js

# Security scanning, formatting, and testing targets

.PHONY: \
    format format_check ci_format_check \
    test ci_test \
		security_scan \
    ci_semgrep_scala ci_html_validate

# --- Formatting ---

XML_FILES := $(shell git ls-files '*.xml')

format-xml:
	@for file in $(XML_FILES); do \
		echo "Formatting $$file"; \
		xmllint --format "$$file" > "$$file.tmp" && mv "$$file.tmp" "$$file"; \
	done

format-xml-check:
	@failed=0; \
	for file in $(XML_FILES); do \
		if ! xmllint --format "$$file" | diff -q "$$file" - >/dev/null; then \
			echo "✗ $$file is not properly formatted"; \
			failed=1; \
		fi; \
	done; \
	exit $$failed

format:
	make format-xml
	sbt scalafmtAll

format_check:
	make format-xml-check
	sbt scalafmtCheckAll

ci_format_check:
	make format-xml-check
	sbt -warn scalafmtCheckAll

# --- Testing ---

test:
	sbt test

ci_test:
	sbt -info 'set Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-oC")' test

# --- Security Scanning ---

ci_security_scan:
	make ci_html_validate ci_semgrep ci_eslint_check

# Scala JVM  static analysis with Semgrep
ci_semgrep:
	semgrep scan --verbose \
		--metrics off \
	  --config p/security-audit \
		--config p/scala \
	  --severity WARNING \
	  --error

# HTML validation with Thymeleaf-aware security profile
ci_html_validate:
	npx html-validate --config src/main/resources/twe/security/.htmlvalidate.json \
	"src/main/resources/twe/templates/fragments/*.html" \
	"out/*.html"

# JavaScript security linting with ESLint
ci_eslint_check:
	npx eslint --config .github/eslint.config.mjs


.PHONY: clean
clean:
	rm -rf ./target/
	find ./project -name target | xargs rm -rf
	rm -rf ./out/
