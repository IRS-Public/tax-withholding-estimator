# Non-Dev TWE 2.0 Local Setup
**Table of Contents**
- [Local Setup for Mac](#local-setup-for-mac)
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
### Getting started
* You must be local admin. Check this by opening your User & Groups settings and confirming if your user is 'Admin' or 'Standard'
* Use spotlight to find the Terminal application in your Utilties folder 

### Install Homebrew
* Open terminal, and paste in the following command: `/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"`
* Once that completes, run: `brew install node`
* Each should start a process to download and install homebrew & node which gives you some tools you'll need further ahead

### Download Repos & Install Dependencies
* Download Github Desktop from here & login using your IRS associated account
* In GitHub Desktop, clone the fact-graph & tax_withholding_estimator repos to your ~/Documents/GitHub folder
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

* Extract and install package the relevant package from https://github.com/coursier/coursier/releases/
* In terminal run cs setup
* Download and install node.js: https://nodejs.org/en/download/
* Download Github Desktop from here & login using your IRS associated account
* In GitHub Desktop, clone the fact-graph & tax_withholding_estimator repos to your /Documents/GitHub folder
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
    * Open terminal and nav to your twe repo: `cd ~/Documents/GitHub/tax_withholding_estimator`
    * Once you're in the twe repo, use this series of commands to run the application
        * In one terminal window: `make twe` (this will run and complete)
        * In the same terminal window: `make dev` (this will run and keep running)
        * In a new terminal window (`cmd+t` on Mac): `make site` (this will run and keep running)
* With twe running in the background, you can open your browser and navigate to: http://localhost:3000/
* Note: you'll need to do this every time you want to run TWE (or just leave it running in the background)

### Kill TWE
* While TWE is running you'll have 2 terminal windows with active processes 
* On each of those windows use the relevant keyboard command to kill the process (`ctrl+c` on Mac)
* From here you can quit terminal

### Update TWE
* In GitHub desktop, make sure you're on the main branch.
* Click the 'Fetch origin' button (top right corner) and wait for it to load
* This can be done while TWE is running in the background. It should update automatically 
