rsync -a -e "ssh -i /root/.ssh/id_goc.dsa" /tmp/cache.*.xml goc@internal-l:/usr/local/backup/rsvprocess/${HOSTNAME}/cache
