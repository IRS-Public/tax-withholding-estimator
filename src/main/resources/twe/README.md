# Resources for building TWE

This directory contains the fact and flow configs, as well as all  the static files that get bundled into the website.

It also contains a `package.json` file for locking down versions of the tools that we use to validate our HTML and JavaScript files.
The node environment is deliberately included down here, rather than at the package root, because node is not required to actually build the application;
it is only required for running various CI checks.
