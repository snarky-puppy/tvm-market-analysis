#!/bin/bash

cd $(dirname $0)
./yahoo_scrape.pl csv daily > run.log || exit 1
dir="scrape-$(date +%Y-%m-%d)"
mkdir $dir
mv *.csv $dir
zip -r -9 $dir.zip $dir
rm -rf $dir
mv $dir.zip /Users/horse/Google\ Drive/Stuff\ from\ Matt/scrapes

