#!/bin/bash
sh ./stopComponents.sh
sleep 5m
(cd /apps/dms ; sh start_components.sh)
