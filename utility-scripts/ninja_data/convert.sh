#!/bin/bash

DATA=Delisted
OUT=Delisted-ninja

rm -rf $OUT
mkdir $OUT

for symbol in $(cd $DATA; ls -1 | sed 's/-[0-9]*.csv//' | sort | uniq); do 
	inf=$(ls $DATA/${symbol}-* | sort | tail -1)
	outf=$OUT/$(basename $inf | sed 's/-[0-9]*.csv/.Last.txt/')
	cut -d, -f 1-6 $inf | tail +2 | sed 's/,/;/g' > $outf
done

