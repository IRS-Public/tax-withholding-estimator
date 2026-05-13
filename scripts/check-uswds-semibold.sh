#!/bin/sh

set -eu

css_file="./src/main/resources/twe/website-static/vendor/uswds-3.13.0/styles/uswds.min.css"

if [ ! -f "$css_file" ]; then
  echo "Missing vendored USWDS CSS: $css_file" >&2
  exit 1
fi

if ! grep -q 'sourcesanspro-semibold-webfont\.woff2' "$css_file"; then
  echo "Vendored USWDS CSS is missing the Source Sans Pro semibold font face." >&2
  exit 1
fi

if ! grep -q 'text-semibold{font-weight:600}' "$css_file"; then
  echo "Vendored USWDS CSS is missing the semibold utility declaration." >&2
  exit 1
fi

echo "USWDS semibold support is present in $css_file"
