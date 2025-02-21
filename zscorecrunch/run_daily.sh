#!/bin/bash
cd /Users/horse/Projects/ZScoreCrunch
export PATH=/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin
ulimit -S -n 12000
GRADLE_OPTS="-Xmx12g -Xms4g"
DEFAULT_JVM_OPTS=${GRADLE_OPTS}
export GRADLE_OPTS
export DEFAULT_JVM_OPTS

if [ -n "$(VBoxManage startvm 'Windows 7' 2>&1 | egrep error\|aborted)" ] ; then
	echo "Windows 7 failed to start"
	exit 1
fi
sleep 5
while [ -n "$(ps -ef | grep VirtualBoxVM | grep 'Windows 7')" ]; do sleep 60; done

caffeinate -sim ./gradlew run -DmainClass=com.tvminvestments.zscore.app.DailyTriggerReport >zscore.out 2>&1 #| tee zscore.out | grep progress
#caffeinate -sim ./gradlew run -DmainClass=com.tvminvestments.zscore.app.DailyTriggerReport -PappProp="$*" >zscore.out 2>&1 #| tee zscore.out | grep progress


if [ $(date +%Y%m%d) -ge 20160701  ] ; then
	mv /lotus/daily_archive/*.csv /lotus/triggers 
else
	mv /lotus/daily_archive/*.csv /lotus/old_triggers 
fi
