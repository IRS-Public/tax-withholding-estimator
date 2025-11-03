**Table of Contents**

- [Setup](#setup)
  - [Configure commit email address](#configure-commit-email-address)
  - [Developer Environment Setup](#developer-environment-setup)
    - [Dependencies](#dependencies)
      - [Java and Scala Setup](#java-and-scala-setup)
      - [Fact Graph](#fact-graph)
  - [Development](#development)
    - [Commands](#commands)
    - [Get up and running](#get-up-and-running)
    - [Tips](#tips)
    - [IDE Support](#ide-support)
      - [IntelliJ](#intellij)
      - [VSCode](#vscode)
      - [Vim / Nvim](#vim--nvim)
    - [Pre-commit](#pre-commit)
  - [How to follow the code](#how-to-follow-the-code)


# Setup
## Configure commit email address

All commits from IRS staff on this project must be authored using your GitHub-provided `noreply` email address.

To achieve this, you'll need to make sure that commits authored via the GitHub web UI as well as local `git` use the GitHub-provided `noreply` email address:

1. Enable private email address
    1. Go to [your GitHub email settings](https://github.com/settings/emails)
    2. Enable `Keep my email address private`
    3. Note and copy the GitHub email address that is displayed. It looks like `{ID}+{USERNAME}@users.noreply.github.com` ([reference](https://docs.github.com/en/account-and-profile/reference/email-addresses-reference#your-noreply-email-address))
    4. Enable `Block command line pushes that expose my email` (Optional)
2. Set your local `git` email address for authoring commits
    1. Configure your local `git` `user.email` setting
        * If you would like to use your no-reply email address for _all_ local development, run
           ```shell
           git config --global user.email "YOUR_NO_REPLY_EMAIL"
           ```
        * If you prefer to only use the no-reply email address for development in your Tax Withholding Estimator working copy, run
          ```shell
          # Ensure you're in the directory for this git project
          cd ./path/to/tax-withholding-estimator/
          # Set your git email config for this repository only
          git config user.email "YOUR_NO_REPLY_EMAIL"
          ```
    2. Verify the configuration by having `git` echo the `user.email` configuration
       ```shell
       $ git config --global user.email
       YOUR_ID+YOUR_USERNAME@users.noreply.github.com
       ```

For more details, see:
* [Setting your commit email address](https://docs.github.com/en/account-and-profile/how-tos/setting-up-and-managing-your-personal-account-on-github/managing-email-preferences/setting-your-commit-email-address#setting-your-email-address-for-a-single-repository).
* [Your no-reply email address](https://docs.github.com/en/account-and-profile/reference/email-addresses-reference#your-noreply-email-address)

## Developer Environment Setup

### Dependencies

#### Java and Scala Setup
If you don't already have java and sbt installed, you can do so using coursier.

1. [Install coursier](https://get-coursier.io/docs/cli-installation)
```bash

# Linux x86-64 (aka AMD64)
curl -fL "https://github.com/coursier/launchers/raw/master/cs-x86_64-pc-linux.gz" | gzip -d > cs
# Linux ARM64
curl -fL "https://github.com/VirtusLab/coursier-m1/releases/latest/download/cs-aarch64-pc-linux.gz" | gzip -d > cs

# Once downloaded for your system, run setup
chmod +x cs
./cs setup

# macOS Apple Silicon (M1, M2, ...):
$ curl -fL https://github.com/coursier/coursier/releases/latest/download/cs-aarch64-apple-darwin.gz | gzip -d > cs
# macOS Intel:
$ curl -fL https://github.com/coursier/launchers/raw/master/cs-x86_64-apple-darwin.gz | gzip -d > cs

# Once downloaded for your system, run setup
$ chmod +x cs
$ ./cs setup

# Alternatively you can use Homebrew on macOS:
$ brew install coursier/formulas/coursier
$ cs setup

```

2. Install Java v21.0.5
```bash
cs java install openjdk:21.0.5
```

3. Install sbt
```bash
cs install sbt
```

4. Install scalafmt
```bash
cs install scalafmt
```

5. Install xmllint _(part of libxml2)_
```bash

# Linux - xmllint is part of the libxml2-utils package.
sudo apt install -y libxml2-utils

# macOS - xmllint should be installed as part of Command Line Tools for Xcode, but can be downloaded through homebrew if necessary
brew install libxml2
```

#### Fact Graph
TWE depends on the [Fact Graph](https://github.com/IRSDigitalService/fact-graph), which you will need to have published
to a local repository.

Simply download the Fact Graph and run `sbt compile publishLocal` in the fact-graph repo


## Development

### Commands

* `make` (or `make dev`) - Same as above, but automatically recompile and re-run on code changes
* `make twe` - Output TWE to the `/out` directory
* `make clean` - Clean all the build artifacts
* `make format` - Format the Scala and XML code
* `make ci` - Run CI checks

### Get up and running:
1. Run `make dev`

### Tips

You can access the current Fact Graph in the developer console, as `factGraph`.

<details>
<summary>If you are working on some fact-graph changes and want to test them here:</summary>

* Ensure you have published the latest scala files by running `sbt compile publishLocal` mentioned in the setup command.
* You should also run the `sbt fastOptJS` in the fact-graph repo.
* Then in this repo you should run `make copy-fg` to ensure that the updated main.mjs file is copied over. (This assumes
  that the fact-graph repo is located in the same directory as this repo and is called `fact-graph`.)

</details>

### IDE Support

#### IntelliJ

You will want to mark `/out` as excluded.

Open the Project/File Explorer tab, right click on the `/out` directory then select `Mark Directory as` -> `Excluded`

With the scala extension installed, IntelliJ will give you a note to enable "nightly" mode to take advantage of the
latest features.
You should do this.

If you run into issues, running `sbt compile` from "Run Anything" and then clicking "Sync all sbt Projects" typically
resolves things.

To enable format on save:
1. Open Preferences and search for `scalafmt`. Go to `Editor` -> `Code Style` -> `Scala`.
2. Select `Scalafmt` in the `Formatter` menu.
3. Within the same Preferences window, search for `actions on save`. Go to `Tools` -> `Actions on Save`.
4. Ensure `Reformat code` is selected.

#### VSCode

1. Install Metals extension. Search for and install _Metals_ from the _Extensions Marketplace_.
2. To enable format on save: Open the Command Palette using `Ctrl/Cmd + Shift + P` ->
   `Preferences: Open Workspace Settings (JSON)` and add the following block:

_Note 1: there can be only one global JSON object, if you already have settings, you will just be adding the rules
inside of the global object to the existing global object._

_Note 2: the editorconfig.* settings are in here because Editor Config is a suggested extension for this project._

  ```json
    {
  "[scala]": {
    "editor.formatOnSave": true,
    "editor.defaultFormatter": "scalameta.metals",
    "editor.formatOnSaveMode": "file"
  },
  "metals.enableScalafmt": true,
  "editorconfig.enable": true,
  "editorconfig.exclude": [
    "**/*.scala"
  ]
}
  ```

#### Vim / Nvim

1. Install Coursier

```bash
brew install coursier
eval "$(coursier java --jvm 21 --env)" # you may not need this line
```

2. Use Coursier to install the following tools

```bash
# Install scalafmt
coursier install scalafmt

# Install the Metals language server
coursier install metals
```

3. Vim - add the following to your `~/.vimrc`:
```bash
autocmd BufWritePre *.scala call s:scalafmt()

function! s:scalafmt()
  let l:cmd = 'scalafmt ' . shellescape(expand('%:p'))
  let l:output = system(l:cmd)
  if v:shell_error
    echohl ErrorMsg | echo "Scalafmt failed:" l:output | echohl None
  else
    # Reload the file silently to avoid W11 warnings
    silent! edit!
  endif
endfunction

```
4. Nvim - add the following to your `init.lua`:

```bash
vim.api.nvim_create_autocmd("BufWritePre", {
  pattern = "*.scala",
  callback = function()
    local file = vim.fn.expand("%:p")
    local result = vim.system({ "scalafmt", file }):wait()

    if result.code ~= 0 then
      vim.notify("Scalafmt failed:\n" .. result.stderr, vim.log.levels.ERROR)
    else
      -- Reload the file silently to avoid W11 warnings
      vim.cmd("silent! edit!")
    end
  end,
})
```

### Pre-commit

Install and initialize pre-commit with

```bash
brew install pre-commit
pre-commit install
```

## How to follow the code

The entry point of this code is `main.scala`. This reads xml files and parses them. This then constructs an HTML page
that is saved as `index.html` in `/out`. You should be able to follow the logic just reading the code. (If there are
areas that seem confusing please reach out!)

`Website.scala` is where all of the parsing logic starts for the xml files. We effectively read all of the xml and then
process that data and eventually pass the processed data to functions that return raw HTML.

`fg-components.js` is where all of our Web Components are written and is the core js for our app outside of the
factgraph.js. This is also where we mount the factGraph object to the browser/window.

