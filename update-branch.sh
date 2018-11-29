#!/bin/sh -e 

BRANCH=$1

git submodule update --init --recursive
cd cloudstack
git checkout -b $BRANCH || git checkout $BRANCH
git branch --set-upstream-to=origin/$BRANCH $BRANCH
git reset --hard origin/$BRANCH
git pull
cd ..
git commit -a -m "Update CloudStack submodule to $BRANCH head"
