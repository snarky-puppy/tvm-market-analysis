#!/bin/bash

URL=file:///Users/horse/Projects/repo/HComp/
backup="HComp-`date +%Y%m%d`"
backup_dir="/Users/horse/Google Drive/Stuff from Matt/code/"

cd /tmp
rm -rf $backup
svn co $URL $backup
tar -czvf $backup.tar.gz $backup
mv $backup.tar.gz "$backup_dir"

