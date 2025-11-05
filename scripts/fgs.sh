#!/bin/bash
# fgs.sh (pronounced "figs") - a small shell utility for exploring the TWE fact dictionary
#
# Dependencies: libxml2, fzf
# Usage:
#   fgs - Fuzzy-search for definitions
#   fgs defn [PATH] - Print the definition of a specific fact
#   fgs deps [PATH] - Print the facts that depend on a specific fact

set -euo pipefail

# Go to the script source directory
cd -- "$( dirname -- "${BASH_SOURCE[0]}" )"

TWE_FACTS="../src/main/resources/twe/facts"

help() {
  cat << EOF
usage: fgs
       fgs defn [PATH]
       fgs deps [PATH]
EOF
}

all-facts() {
  # Build a valid XML document out of the concatenated fact files
  echo '<?xml version="1.0"?>'
  echo "<AllFacts>"
  find $TWE_FACTS -name '*.xml' -print0 | xargs -0 cat | grep -v '<\?xml'
  echo "</AllFacts>"
}

extract-path() {
  grep -o '/[^"]*'
}

format() {
  xmllint --format - | grep -v '<\?xml'
}

deps() {
  all-facts | xpath -q -e "//Fact[.//Dependency[@path=\"$1\"]]/@path" | extract-path
}

defn() {
  all-facts | xpath -q -e "//Fact[@path=\"$1\"]" | format
}

search() {
  all-facts | xpath -q -e '//Fact/@path' | extract-path | fzf
}

# The default command is search, otherwise run the command specified
if [[ $# == 0 ]]; then
  defn "$(search)"
elif [[ "$1" == "deps" ]] || [[ "$1" == "defn" ]]; then
  "$@"
else
  help
  exit 1
fi
