#!/bin/bash

#cd /usr/local/rsv-gratia-collector-0.32.1
cd /usr/local/rsv-gratia-collector-1.0
. setup.sh

#get oim data from sonofsam
cat /home/hayashis/dev/rsvprocess/sql/myosg.sql | $VDT_LOCATION/mysql5/bin/mysql -u root
