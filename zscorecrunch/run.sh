ulimit -S -n 65536
GRADLE_OPTS="-Xmx12g -Xms4g"
DEFAULT_JVM_OPTS=${GRADLE_OPTS}
export GRADLE_OPTS
export DEFAULT_JVM_OPTS
caffeinate -sim ./gradlew run -DmainClass=com.tvminvestments.zscore.app.ZScore >zscore.out 2>&1 #| tee zscore.out | grep progress
