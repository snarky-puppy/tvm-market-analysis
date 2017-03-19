#!/bin/bash

for symbol in $(cat ../input/symbols-MS-6.csv); do
    [ $symbol = "Symbol" ] && continue
    echo "Parsing $symbol"
    ./parse.pl $symbol
done


