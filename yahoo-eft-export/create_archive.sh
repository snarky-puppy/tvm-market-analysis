#!/bin/bash

VERSION=$(cat version.txt)
NAME=Yahoo_EFT_Export-$VERSION


tmp=$(mktemp -d)
pkgdir=$tmp/$NAME
mkdir $pkgdir

cp -r dist/* $pkgdir

# clean up any possible contamination
rm -f $pkgdir/yahoo.db
rm -f $pkgdir/data

(cd $tmp; zip -r -9 $NAME.zip $NAME)

mv $tmp/$NAME.zip .

rm -rf $tmp
