#!/bin/bash

sleep 500 &
temppid=$!
tail -f -n0 --pid $temppid /apps/dms/dms-decoder-ninjo-alert-warp3/logs/msc-dms-decoder-ninjo-alert-warp3.log >> /apps/dms/configTableScripts/data/droptestlogs.log &

cd /apps/dms/mwas-autoissuer/ 
sh single-run.sh 2>&1 &
p2=$!
wait $p2

#kill $p1
#wait $! 2>/dev/null

sleep 15

kill $temppid
wait $temppid 2>/dev/null

cat /apps/dms/configTableScripts/data/droptestlogs.log ;
> /apps/dms/configTableScripts/data/droptestlogs.log

