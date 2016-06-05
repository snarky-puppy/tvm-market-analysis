#!/bin/bash

OUT_DIR=/lotus

cd $(dirname $0)

if [ -f /usr/local/mysql/bin/mysql ] ; then
	BIN=/usr/local/mysql/bin/mysql 
else
	BIN=/usr/bin/mysql
fi

if [ "$1" = "-schema" ] ; then
	$BIN -uroot mysql < schema.sql

elif [ "$1" = "-report" ] ; then
	$BIN -ulotus -plotus -B lotus < trigger_report.sql 2>/dev/null | sed 's/	/,/g' > $OUT_DIR/triggers.csv
	$BIN -ulotus -plotus -B lotus < position_report.sql 2>/dev/null | sed 's/	/,/g' > $OUT_DIR/positions.csv
else
	$BIN -ulotus2 -plotus2 lotus2
fi
