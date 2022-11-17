#!/bin/bash

DIR1=
DIR2=
DIR3=

NEWTABLEDIR="/apps/dms/temp/newConfigTables/"

for file in $NEWTABLEDIR*; do
        #echo ${DIR1}${file##*}
        if [ -f "${DIR1}${file##*/}" ]; then

                echo "Replacing file $file in directory $DIR1" "${file##*/}"
                rsync -u "$file" DIR1

        elif [ -f "${DIR2}${file##*/}" ]; then
                
		echo "Replacing file $file in directory $DIR2" "${file##*/}"
                rsync -u "$file" DIR2

        elif [ -f "${DIR3}${file##*/}" ]; then
                
		echo "Replacing file $file in directory $DIR3" "${file##*/}"
                rsync -u "$file" DIR3
        fi

done
