#!/bin/bash

DATA=../data
OUTPUT=minmax.csv

echo "Symbol,Exchange,Min Date,Max Date" > $OUTPUT

function convert_date {
    echo $1 | sed 's?\([0-9][0-9][0-9][0-9]\)\([0-9][0-9]\)\([0-9][0-9]\)?\3/\2/\1?'
}

for exdir in ../data/*; do
    exchange=$(basename $exdir)
    for file in $exdir/*.csv; do
        symbol=$(basename $file | sed 's/.csv//')
        start=$(head -2 $file | tail -1 | cut -d , -f 1)
        start=$(convert_date $start)
        end=$(tail -1 $file | cut -d , -f 1)
        end=$(convert_date $end)
        echo $symbol,$exchange,$start,$end >> $OUTPUT
    done
done
