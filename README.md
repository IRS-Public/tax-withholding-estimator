# Tax Witholding Estimator

An online website for a taxpayer to estimate their witholdings.

Its powered by a static site generator that turns FlowXML into multi-page forms.
Right now this static site generator purpose-built for TWE, but I've done what I can to write it as generically as possible.

## Setup

TWE depends on the [Fact Graph](https://github.com/IRSDigitalService/fact-graph), which you will need to have published to a local repository.
Simply download the Fact Graph and run `sbt compile publishLocal`.

### Development Commands

`make twe` - Output TWE to the `/out` directory
`make dev` - Same as above, but automatically recompile and re-run on code changes
`make site` - Serve the static site out of the `/out` directory (requires npm installation)
`make clean` - Clean all the build artifacts

## IDE Support

### IntelliJ

In general, running `sbt compile` from "Run Anything" and then clicking "Sync all sbt Projects" resolves any issues.
