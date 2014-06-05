#!/bin/bash

echo "existing tags"
git tag

echo -n "enter new tag name> "
read -e NEWTAG

git tag -a $NEWTAG
git push origin --tags

#update latest tag - only do this on production
#git tag -f -a latest

