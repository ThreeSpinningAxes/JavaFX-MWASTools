#!/bin/bash

DIR1="src%2Fmain%2Fconfig%2Fwarp3%2F"
DIR2="src%2Fmain%2Fconfig%2Fwarp3%2Falert-categories%2F"
BASEURL="https://dms-gitlab.cmc.ec.gc.ca/api/v4/projects/864/repository/files/"
REF="/raw?ref=develop"

DOCKERDIR1="/apps/dms/dms-config/warnings/conf/warp3/"
DOCKERDIR2="/apps/dms/dms-config/warnings/conf/warp3/alert-categories/"

TOKEN=$(cat token.txt)

for table in "$@"
do
	curl -s -f --header "PRIVATE-TOKEN: ${TOKEN}" "$BASEURL$DIR1$table$REF" > "$DOCKERDIR1$table" ||
	curl -s -f --header "PRIVATE-TOKEN: ${TOKEN}" "$BASEURL$DIR2$table$REF" > "$DOCKERDIR2$table"
done

sh apps/dms/stop_components.sh
sleep 5m
sh apps/dms/start_components.sh
