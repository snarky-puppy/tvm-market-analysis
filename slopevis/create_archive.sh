#!/bin/bash

VERSION=1.8
NAME=SlopeVis-$VERSION


tmp=$(mktemp -d)
pkgdir=$tmp/$NAME
mkdir $pkgdir

cp lib/* $pkgdir
rm -f $pkgdir/slopevis.json

cp slopevis.bat $pkgdir

(cd $tmp; zip -r -0 $NAME.zip $NAME)

mv $tmp/$NAME.zip .

rm -rf $tmp
