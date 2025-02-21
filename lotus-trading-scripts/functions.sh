#!/bin/bash

function log {
    [ -z "$APP" ] && { echo "log: \$APP not set!"; exit 1; }
    [ $# -ne 2 ] && { echo "Usage: log LEVEL MSG"; exit 1; }
    echo "$1: $2"
    logger -p local0.$1 -t $APP "$2"
}


# vim: set ts=4 sw=4 et :
