#!/bin/bash

VERSION=1.1
NAME=SlopeVis-$VERSION


tmp=$(mktemp -d)
pkgdir=$tmp/$NAME
mkdir $pkgdir

cp lib/* $pkgdir
rm -f $pkgdir/slopevis.json

(cd $tmp; zip -r -0 $NAME.zip $NAME)

mv $tmp/$NAME.zip .

rm -rf $tmp
