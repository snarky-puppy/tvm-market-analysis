#!/bin/bash


cd $(dirname $0)

java -jar out/artifacts/DailySlope/DailySlope.jar > slope-$(date +%Y%m%d-%H%M%S).log 2>&1
