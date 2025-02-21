#!/bin/bash

symbols=/tvm/symbols.csv

set -e

home=$(dirname $0)

source $home/functions.sh
APP=$(basename $0)

tmp=$(mktemp)
trap "rm -f $tmp" EXIT

gsutil cp gs://lotus/symbols.csv $tmp

size=$(stat -c%s $tmp)

if [ $size -le 0 ] ; then
	log err "new file size 0"
    exit 1
fi

file_type=$(file $tmp | sed 's/.*: //')
log info "file type: $file_type"
if [[ $file_type != ASCII* ]]; then
	log err "Unexpected file type: $file_type"
    exit 1
fi

if [ ! -f $symbols ] ; then
    log info "No pre-existing $symbols found, OK to copy"
    cp $tmp $symbols
    exit 0
fi 

new_cksum=$(cksum $tmp | awk '{print $1}')
old_cksum=$(cksum $symbols | awk '{print $1}')

if [ "$new_cksum" = "$old_cksum" ] ; then
    log info "Checksums match, no update"
    exit 0
fi

log info "Update detected, copying new file"
cp $tmp $symbols

# vim: set ts=4 sw=4 et :
