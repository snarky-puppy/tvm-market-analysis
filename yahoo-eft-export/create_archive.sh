#!/bin/bash

VERSION=1.0
NAME=Yahoo_EFT_Export-$VERSION


tmp=$(mktemp -d)
pkgdir=$tmp/$NAME
mkdir $pkgdir

cp -r dist/* $pkgdir

# clean up any possible contamination
rm -f $pkgdir/yahoo.db
rm -f $pkgdir/data

(cd $tmp; zip -r -0 $NAME.zip $NAME)

mv $tmp/$NAME.zip .

rm -rf $tmp
