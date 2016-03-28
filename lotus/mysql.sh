#!/bin/bash

OUT_DIR=/Users/horse/Google\ Drive/Stuff\ from\ Matt/lotus

if [ -f /usr/bin/mysql ] ; then
	BIN=/usr/bin/mysql
else
	BIN=/usr/local/mysql/bin/mysql 
fi

if [ "$1" = "-schema" ] ; then
	$BIN -uroot mysql < schema.sql

elif [ "$1" = "-report" ] ; then
	$BIN -ulotus -plotus -B lotus < trigger_report.sql 2>/dev/null | sed 's/	/,/g' > /Users/horse/Google\ Drive/Stuff\ from\ Matt/lotus/triggers.csv
	$BIN -ulotus -plotus -B lotus < position_report.sql 2>/dev/null | sed 's/	/,/g' > /Users/horse/Google\ Drive/Stuff\ from\ Matt/lotus/positions.csv
else
	$BIN -ulotus -plotus lotus
fi
