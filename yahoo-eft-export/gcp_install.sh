#!/bin/bash

set -e

DST=/tvm/yahoo-eft-export

#rm -f $DST/dependency-jars

mvn package

cp target/yee-*.jar $DST
#cp -a target/dependency-jars $DST
cp yee.sh $DST
