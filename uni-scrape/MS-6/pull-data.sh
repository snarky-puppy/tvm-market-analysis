#!/bin/bash

echo "Logging in..."

curl -c cookies.txt -o /dev/null https://www.gurufocus.com/forum/login.php

curl -b cookies.txt -c cookies.txt -o /dev/null -d 'username=vinniek&password=tvmtvmtvm1&forum_id=0&redir=http%3A%2F%2Fwww.gurufocus.com%2Fforum%2Findex.php' https://www.gurufocus.com/forum/login.php 


mkdir data

for symbol in $(cat ../input/symbols-MS-6.csv); do
    [ $symbol = "Symbol" ] && continue

    [ -f data/$symbol ] && { echo "Already got $symbol"; continue; }

    echo "Downloading $symbol"

    curl -s -b cookies.txt -o data/$symbol http://www.gurufocus.com/ownership/$symbol
done


