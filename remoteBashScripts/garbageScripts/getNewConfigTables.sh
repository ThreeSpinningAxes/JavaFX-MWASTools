#!/bin/bash

DIR1="src%2Fmain%2Fconfig%2F"
DIR2="src%2Fmain%2Fconfig%2Fwarp3%2F"
DIR3="src%2Fmain%2Fconfig%2Fwarp3%2Falert-categories%2F"
BASEURL="https://dms-gitlab.cmc.ec.gc.ca/api/v4/projects/864/repository/files/"
REF="/raw?ref=develop"
NEWTABLEDIR="/apps/dms/temp/newConfigTables/"
TOKEN=$(cat token.txt)

for table in "$@"
do
	   curl -s -f --header "PRIVATE-TOKEN: ${TOKEN}" "$BASEURL$DIR1$table$REF" -o "$NEWTABLEDIR$table" ||
		   curl -s -f --header "PRIVATE-TOKEN: ${TOKEN}" "$BASEURL$DIR2$table$REF" -o "$NEWTABLEDIR$table" || 
		   curl -s -f --header "PRIVATE-TOKEN: ${TOKEN}" "$BASEURL$DIR3$table$REF" -o "$NEWTABLEDIR$table"
done


