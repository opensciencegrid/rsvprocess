date=`date +%Y%m%d.%H`_$RANDOM

trunk='https://osg-svn.rtinfo.indiana.edu/goc-internal/rsvprocess/trunk'
SVN_BRANCHES=https://osg-svn.rtinfo.indiana.edu/goc-internal/rsvprocess/branches
echo "-------------------------------------------------------"
echo "Existing branches"
svn --non-interactive --trust-server-cert list $SVN_BRANCHES
echo "-------------------------------------------------------"
echo -n "Please name your new branch (you can override if you want)> "
read -e BRANCH

echo "removing old branch (if exist)"
svn rm -m "removing old branch (if exist)" ${SVN_BRANCHES}/${BRANCH}
echo "creating new branch"
svn cp -m "creating new branch via branch script" $trunk ${SVN_BRANCHES}/${BRANCH}

