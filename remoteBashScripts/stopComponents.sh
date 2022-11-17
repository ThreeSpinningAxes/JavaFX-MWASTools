#!/bin/bash
echo "dms" | sudo -S service dms-decoder-cap-warp3-ec stop
sudo service dms-decoder-mt-warp3-ec stop
sudo service dms-decoder-ninjo-alert-warp3-ec stop
sudo service dms-importer-bulletin-ec stop
sudo service dms-manager-alert-warp3-ec stop
sudo service dms-pg-active-alert-summary-warp3-ec stop
sudo service dms-pg-cap-warp3-ec stop
sudo service dms-pg-ecam-twitteralertgenerator-warp3-ec stop
sudo service dms-pg-mfile-warp3-ec stop
sudo service dms-pg-mt-warp3-ec stop
sudo service dms-pg-ninjo-rawbulletin-ec stop
sudo service dms-pg-reports-warp3-ec stop
sudo service dms-pg-warnings-warp3-ec stop
sudo service dms-pg-wxapp-ec stop
sudo service dms-pg-wxr-ec stop
sudo service dms-publisher-ec stop
sudo service dms-decoder-translation-ec stop
sudo service dms-pg-translation-ec stop
sudo service dms-manager-metnote-ec stop
echo finished;
echo ;
