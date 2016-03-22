#!/bin/bash

if [ "$1" = "-schema" ] ; then
	/usr/local/mysql/bin/mysql -uroot mysql < schema.sql

else
	/usr/local/mysql/bin/mysql -ulotus -plotus lotus
fi
