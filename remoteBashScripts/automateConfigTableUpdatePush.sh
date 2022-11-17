#!/bin/bash

#HOW TO USE
  #1. Add new config tables to "SOMEDIR" directory
  #2. Pass in the ticket number of the table changes on mantis as command line arguments to the bash script

#CONCERNS:
  #You will need your gitlab email and username to run the git config commands. Update these values in git_info.sh
  #To avoid automatically typing in user password for every git pull/push, update the ~./git-credentials file or run 'git config credential.helper store' and perform git pull in a git repo

source git_info.sh
git config --global user.email $EMAILFORAUTOPUSH  > /dev/null 2>&1
git config --global user.name $USERNAMEFORAUTOPUSH  > /dev/null 2>&1

if [ $# -eq 0 ]; then
  echo "no arguments given, exiting"
  exit 1
fi

echo "Switching to config warnings repo directory"
cd "directory" || exit 1 

git checkout develop -q
echo "Pulling from git to get latest version of repo"
git pull

REMOTEDIR="feature/general/-$1"

echo "Verifying if another config update is taking place..."
#checks to see if there is a outgoing mr for config table change. If yes, exit
if [ "$(git ls-remote origin ${REMOTEDIR})" ]; then
  echo "Currently there is a outgoing merge request for a previous config table change.
  Please wait for it to be merged or closed before you can merge your changes"
  exit 1
fi
echo "OK"

echo "Checking if feature branch for table update already exists"
if [ $(git rev-parse -q --verify "feature/general/$1") > /dev/null 2>&1 ]; then
  git branch -D "feature/general/$1"
fi

git checkout -b "feature/general/$1"

# Three directories where all config table files can be replaced

DIR1=""
DIR2="/"

NEWTABLEDIR="/"

# updates config files in their respective directories
echo "Updating config files in git repo..."

for file in $NEWTABLEDIR*; do
        #echo ${DIR1}${file##*/}
        if [ -f "${DIR1}${file##*/}" ]; then
                echo "Replacing file $file in directory $DIR1${file##*/}"
                rsync -u "$file" $DIR1
        elif [ -f "${DIR2}${file##*/}" ]; then
                echo "Replacing file $file in directory $DIR2${file##*/}"
                rsync -u "$file" $DIR2
        fi
done

rm "DIR"


MRMESSAGE="SOME MESSAGE"

echo "Beginning pushing process of table update"
git add "".
git status
git commit -m "issue $1: Config table update"
#git push origin -f feature/general/DMS-31728
git push -o merge_request.create -o merge_request.remove_source_branch -o merge_request.description="$MRMESSAGE" --set-upstream origin "feature/general/$1"
git checkout develop
git branch -D "feature/general/$1"


