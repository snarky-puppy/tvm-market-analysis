#!/bin/bash
#java -jar yee-1.0-SNAPSHOT.jar > logs/yee-$(date +%Y%m%d-%H%M%S).log 2>&1
/usr/lib/jvm/java-1.8.0-openjdk-amd64/bin/java -cp "dist/lib/*" com.tvm.YahooEFT $1
