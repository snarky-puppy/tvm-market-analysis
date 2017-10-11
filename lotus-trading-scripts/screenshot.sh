#!/bin/bash

set -e

tmp=$(mktemp)

trap "rm -f $tmp" EXIT

cat /tmp/ibfb/Xvfb_screen0 | convert xwd:- $tmp

tstamp=$(date +%Y%m%d%H%M)
gsutil cp $tmp gs://lotus/screenshots/screenshot-${tstamp}.jpg
