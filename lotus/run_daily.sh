#!/bin/bash

cd $(dirname $0)

./mysql.sh -report
/usr/local/mysql/bin/mysqldump -ulotus2 -plotus2 lotus2 > dump/dump-$(date +%Y%m%d).sql 2>/dev/null 2>&1
