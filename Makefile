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

.PHONY: format
format:
	sbt scalafmtAll

.PHONY: format_check
format_check:
	sbt scalafmtCheckAll

.PHONY: clean
clean:
	rm -rf ./target/
	find ./project -name target | xargs rm -rf
	rm -rf ./out/
