#!/bin/bash

rm -f data.csv

for f in data/*; do
    cat $f | awk -F\t '{print $1","$2","$3","$13}' | sed 's/"//g' | tail -n +7 >> data.csv
done
