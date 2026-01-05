# Developer Tips

You can access the current Fact Graph in the developer console, as `factGraph`.

## IDE Support

### IntelliJ

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

### VSCode

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

### Vim / Nvim

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

```vimrc
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

```lua
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

## Running on a Windows VM (Parallels)

`crypto.randomUUID` has wide support but requires secure contexts in some browsers or operating systems ([more on crypto.randomUUID](https://developer.mozilla.org/en-US/docs/Web/API/Crypto/randomUUID)). This is the case with doing local development with Windows on a virtual machine.

Running the app in Windows on a VM also requires a few extra steps involving a `socat` relay.

**1. Install socat with homebrew**
```bash
brew install socat
```

**2. Run the app**
In a terminal window, run the app as you normally would with `make`

**3. Set up the proxy in project root folder in another terminal window**
```bash
socat TCP-LISTEN:3000,bind=10.211.55.2,fork TCP:127.0.0.1:3000
```

**4. Visit this URL in Windows on Parallels**
```
http://10.211.55.2:3000/twe
```

## Pre-commit Hooks

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

