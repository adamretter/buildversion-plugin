#!/usr/bin/env bash

set -e
set +x

echo "Running $0 ..."
TARGET_DIR="$1"

#rm -rf $TARGET_DIR
mkdir -p $TARGET_DIR
cd $TARGET_DIR

git init
git symbolic-ref HEAD refs/heads/master
git config user.email "test@example.com"
git config user.name "Test User"
> README.txt

git add README.txt
git commit -m "Initial commit. Before any tag"

> README-release.txt
> README-master.txt

git add README*
git commit -m "First tagged commit"

git branch develop
git branch release

git checkout develop

git tag -a -m "v1.0.0-SNAPSHOT" "v1.0.0-SNAPSHOT"

echo 'dev commit 1' >>README.txt
git commit -a -m "dev commit 1"

echo 'dev commit 2' >>README.txt
git commit -a -m "dev commit 2"

git checkout -b "feature_XYZ"
sleep .1
echo 'feature XYZ commit 1' >>README.txt
git commit -a -m "feature XYZ commit 1"

git checkout -
git merge --no-ff feature_XYZ

sleep .1
echo 'dev commit 3' >>README.txt
git commit -a -m "dev commit 3"


# Pre-release 1.0.0
git checkout release
sleep .1
git merge --no-ff develop
git tag -a -m "v1.0.0-RC-SNAPSHOT" "v1.0.0-RC-SNAPSHOT"
git checkout -
git tag -a -m "v1.1.0-SNAPSHOT" "v1.1.0-SNAPSHOT"

sleep .1
echo 'dev commit 4' >>README.txt
git commit -a -m "dev commit 4"


# fix on pre-release:
git checkout release
sleep 1
echo 'release commit 1' >>README-release.txt
git commit -a -m "release commit 1"

git checkout develop
sleep .1
git merge --no-ff release

sleep .1
echo 'dev commit 5' >>README.txt
git commit -a -m "dev commit 5"

# Release
git checkout master
sleep 1
git merge --no-ff release
git tag -a -m "v1.0.0" "v1.0.0"

sleep 1
git checkout develop
echo 'dev commit 6' >>README.txt
git commit -a -m "dev commit 6"
echo 'dev commit 7' >>README.txt
git commit -a -m "dev commit 7"
echo 'dev commit 8' >>README.txt
git commit -a -m "dev commit 8"

# another feature
git checkout develop
git checkout -b "feature_WXY"
sleep .1
echo 'feature WXY commit 1' >>README.txt
git commit -a -m "feature WXY commit 1"
git checkout -
git merge --no-ff feature_WXY

sleep .1
echo 'dev commit 9' >>README.txt
git commit -a -m "dev commit 9"

# Pre-release 1.1.0
git checkout release
sleep .1
git merge --no-ff develop
git tag -a -m "v1.1.0-RC-SNAPSHOT" "v1.1.0-RC-SNAPSHOT"
git checkout -
git tag -a -m "v1.2.0-SNAPSHOT" "v1.2.0-SNAPSHOT"


# Release
git checkout master
sleep .1
git merge --no-ff release
git tag -a -m "v1.1.0" "v1.1.0"


sleep .1
git checkout develop
echo 'dev commit 10' >>README.txt
git commit -a -m "dev commit 10"

# hotfix
sleep 1
git checkout master
git checkout -b hotfix
echo 'hotfix commit 1' >>README-master.txt
git commit -a -m "hotfix commit 1"
git checkout master
sleep .1
git merge --no-ff hotfix
git tag -a -m "v1.1.1" "v1.1.1"
sleep .1
git checkout develop
git merge --no-ff master
