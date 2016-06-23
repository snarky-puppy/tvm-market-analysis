#!/bin/bash

OUT_DIR=/lotus

DB=lotus2
U=lotus2
P=lotus2

cd $(dirname $0)

if [ -f /usr/local/mysql/bin/mysql ] ; then
	BIN=/usr/local/mysql/bin/mysql 
else
	BIN=/usr/bin/mysql
fi

if [ "$1" = "-schema" ] ; then
	$BIN -uroot mysql < schema.sql

elif [ "$1" = "-report" ] ; then
	$BIN -u$U -p$P -B $DB < trigger_report.sql 2>/dev/null | sed 's/	/,/g' > $OUT_DIR/triggers.csv
	$BIN -u$U -p$P -B $DB < position_report.sql 2>/dev/null | sed 's/	/,/g' > $OUT_DIR/positions.csv
	$BIN -u$U -p$P -B $DB < compounder_report.sql 2>/dev/null | sed 's/	/,/g' > $OUT_DIR/compounder.csv
else
	$BIN -u$U -p$P $DB
fi
