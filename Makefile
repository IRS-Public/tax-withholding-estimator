.PHONY: twe
twe:
	sbt "run ./flows/about-you-basic.xml"

.PHONY: run
dev:
	sbt "~run ./flows/about-you-basic.xml"

# This uses the NodeJS toolchain to run "serve", a static site server
.PHONY: site
site:
	npx serve out

.PHONY: clean
clean:
	rm -rf ./target/
	find ./project -name target | xargs rm -rf
	rm -rf ./out/
