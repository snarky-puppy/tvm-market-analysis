#!/bin/bash

newname=lotustest

tmp=$(mktemp)

trap "rm -f $tmp" EXIT

cd $(dirname $0)

if [ -f /usr/local/mysql/bin/mysql ] ; then
	BIN=/usr/local/mysql/bin/mysql 
else
	BIN=/usr/bin/mysql
fi

if [ ! -f schema.sql ] ; then
	echo "no schema found in $PWD"
	exit 1
fi

cp schema.sql $tmp

dbname=$(grep 'DROP DATABASE' $tmp  | awk '{print $5}' | sed 's/;//')

echo "database name is $dbname"

if [ -z "$dbname" ] ; then
	echo "dbname not parsed correctly from schema"
	exit 1
fi

perl -pi -e "s/$dbname/$newname/g" $tmp


if [ "$1" = "-create" ] ; then
	$BIN -uroot mysql < $tmp

elif [ "$1" = "-destroy" ] ; then
	${BIN}admin -f -uroot drop $newname
else
	$BIN -u$newname -p$newname $newname
fi
