.PHONY: dev
dev:
	sbt ~run

.PHONY: twe
twe:
	sbt run

# This uses the NodeJS toolchain to run "serve", a static site server
.PHONY: site
site:
	npx serve out

.PHONY: copy-fg
copy-fg:
	cp ../fact-graph/js/target/scala-3.3.6/factgraph-fastopt/main.mjs ./src/main/resources/twe/website-static/js/factgraph-3.1.0.js
	cp ../fact-graph/js/target/scala-3.3.6/factgraph-fastopt/main.mjs.map ./src/main/resources/twe/website-static/js

.PHONY: format format-xml format-xml-check format_check ci_format_check

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
	sbt -error scalafmtCheckAll

.PHONY: test ci_test
test:
	sbt test

ci_test:
	 sbt -info 'set Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-oC")' test

.PHONY: clean
clean:
	rm -rf ./target/
	find ./project -name target | xargs rm -rf
	rm -rf ./out/
