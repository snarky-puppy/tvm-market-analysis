#!/bin/bash
java -jar yee-1.0-SNAPSHOT.jar > logs/yee-$(date +%Y%m%d-%H%M%S).log 2>&1
