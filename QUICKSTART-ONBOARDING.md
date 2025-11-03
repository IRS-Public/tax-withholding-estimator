# Non-Dev TWE 2.0 Local Setup
**Table of Contents**
- [Local Setup for Mac](#local-setup-for-mac)
  - [Configure commit email address](#configure-commit-email-address)
  - [Getting Started](#getting-started)
  - [Install Homebrew](#install-homebrew)
  - [Download Repos & Dependencies](#download-repos--install-dependencies)
  - [Terminal Tips & Tricks](#terminal-tips--tricks)
- [Local Setup for Windows](#local-setup-for-windows)
  - [Download Repos & Dependencies](#download-repos--install-dependencies-1)
- [Run TWE Locally](#run-twe-locally)
  - [Compile FactGraph](#compile-factgraph)
  - [Run TWE](#run-twe)
  - [Kill TWE](#kill-twe)
  - [Update TWE](#update-twe)

## Local Setup for Mac
### Configure commit email address

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
### Getting started
* You must be local admin. Check this by opening your User & Groups settings and confirming if your user is 'Admin' or 'Standard'
* Use spotlight to find the Terminal application in your Utilties folder

### Install Homebrew
* Open terminal, and paste in the following command: `/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"`

### Download Repos & Install Dependencies
* Download Github Desktop from here & login using your IRS associated account
* In GitHub Desktop, clone the `fact-graph` & `tax-withholding-estimator` repos to your ~/Documents/GitHub folder
* In terminal, move to the fact-graph repo & install dependencies
    * Open terminal and move to your fact-graph repo: `cd ~/Documents/GitHub/fact-graph`
    * Once you're in the fact-graph repo, use this command to install SBT: `brew install scala openjdk sbt`
* Once you've completed the above steps you're ready to [compile the fact graph](#compile-factgraph) and [Run TWE Locally](#run-twe)

### Terminal Tips & Tricks:
* Shortcuts
  * Open a new terminal window: `cmd+n`
  * Open a new terminal tab: `cmd+t`
  * Kill a running command: `ctrl+c`
  * Suspend a running command: `ctrl+z`
* Useful Commands
  * Present working directory: `pwd`
  * Change directory: `cd`
  * List items in current directory: `ls`
* Note: `~` refers to the current logged-in user's home directory found in /Users/


## Local Setup for Windows

### Download Repos & Install Dependencies

* Extract and install the relevant package from https://github.com/coursier/coursier/releases/
    * Make sure you are downloading from the version marked "Latest" ([Version 2.1.24](https://github.com/coursier/coursier/releases/tag/v2.1.24) as of 10/10/2025)
    * Look for cs-x86_64-pc-win32.zip
* In terminal run cs setup
* Download Github Desktop from here & login using your IRS associated account
* In GitHub Desktop, clone the fact-graph & tax-withholding-estimator repos to your /Documents/GitHub folder
* In terminal, move to the fact-graph repo & install dependencies
    * Open terminal in Run as Administrator mode (right click on app icon) and move to your fact-graph repo: `cd ~/Documents/GitHub/fact-graph`
    * Once you're in the fact-graph repo, use this command to install Make: `choco install make`
* Once you've completed the above steps you're ready to [compile the fact graph](#compile-factgraph) and [Run TWE Locally](#run-twe)


## Run TWE Locally
### Compile FactGraph
Before you can run TWE you'll need to compile the fact-graph.
* In terminal, move to the fact-graph repo & compile the fact-graph
    * Open terminal and move to your fact-graph repo: `cd ~/Documents/GitHub/fact-graph`
    * Once you're in the fact-graph repo, use this command to compile the fact graph: `make publish`
    * Note: you'll need to do this every time the fact-graph repo updates, but not every time you run TWE

### Run TWE
Once you've compiled the fact-graph and run `make publish` you're ready to run TWE locally
* In terminal, move to the tax-withholding-estimator repo & run it!
    * Open terminal and nav to your twe repo: `cd ~/Documents/GitHub/tax-withholding-estimator`
    * Run `make` (this will run and keep running)
* With twe running in the background, you can open your browser and navigate to: http://localhost:3000/
* Note: you'll need to do this every time you want to run TWE (or just leave it running in the background)

### Kill TWE
* Use `ctrl+c` to kill the process
* From here you can quit terminal

### Update TWE
* In GitHub desktop, make sure you're on the main branch.
* Click the 'Fetch origin' button (top right corner) and wait for it to load
* This can be done while TWE is running in the background. It should update automatically

