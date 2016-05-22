#!/bin/bash
cd /Users/horse/Projects/Lotus

US_DT=$(TZ=America/New_York date '+%Y-%m-%d %H:%m')
MY_DT=$(date '+%Y-%m-%d %H:%m')

echo "== STARTING RUN == EST: $US_DT / AEST: $MY_DT ==" >> /Users/horse/Google\ Drive/Stuff\ from\ Matt/lotus/lotus.log 

java -jar /Users/horse/Projects/Lotus/out/Lotus.jar >> /Users/horse/Google\ Drive/Stuff\ from\ Matt/lotus/lotus.log 2>&1
/Users/horse/Projects/Lotus/mysql.sh -report
