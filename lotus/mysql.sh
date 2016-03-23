#!/bin/bash

if [ -f /usr/bin/mysql ] ; then
	BIN=/usr/bin/mysql
else
	BIN=/usr/local/mysql/bin/mysql 
fi

if [ "$1" = "-schema" ] ; then
	$BIN -uroot mysql < schema.sql

else
	$BIN -ulotus -plotus lotus
fi
