# Tax Witholding Estimator

An online website for a taxpayer to estimate their witholdings.

Its powered by a static site generator that turns FlowXML into multi-page forms.
Right now this static site generator purpose-built for TWE, but I've done what I can to write it as generically as possible.

## Setup

TWE depends on the [Fact Graph](https://github.com/IRSDigitalService/fact-graph), which you will need to have published to a local repository.
1. Simply download the Fact Graph and run `sbt compile publishLocal` in the fact-graph repo
2. Run `make dev` from this repo
3. Run `make site` from this repo

## Development

### Commands

* `make` (or `make dev`) - Same as above, but automatically recompile and re-run on code changes
* `make twe` - Output TWE to the `/out` directory
* `make site` - Serve the static site out of the `/out` directory (requires npm installation)
* `make clean` - Clean all the build artifacts

### Tips

You can access the current Fact Graph in the developer console, as `factGraph`.

<details>
<summary>If you are working on some fact-graph changes and want to test them here:</summary>

* Ensure you have published the latest scala files by running `sbt compile publishLocal` mentioned in the setup command. 
* You should also run the `sbt fastOptJS` in the fact-graph repo. 
* Then in this repo you should run `make copy-fg` to ensure that the updated main.mjs file is copied over. (This assumes that the fact-graph repo is located in the same directory as this repo and is called `fact-graph`.)

</details>

## IDE Support

### IntelliJ

You will want to mark `/out` as excluded.

With the scala extension installed, IntelliJ will give you a note to enable "nightly" mode to take advantage of the latest features.
You should do this.

If you run into issues, running `sbt compile` from "Run Anything" and then clicking "Sync all sbt Projects" typically resovles things.

## How to follow the code

The entry point of this code is `main.scala`. This reads xml files and parses them. This then constructs an HTML page that is saved as `index.html` in `/out`. You should be able to follow the logic just reading the code. (If there are areas that seem confusing please reach out!)

`Website.scala` is where all of the parsing logic starts for the xml files. We effectively read all of the xml and then process that data and eventually pass the processed data to functions that return raw HTML.

`fg-components.js` is where all of our Web Components are written and is the core js for our app outside of the factgraph.js. This is also where we mount the factGraph object to the browser/window. 
