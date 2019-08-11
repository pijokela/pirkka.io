#! /bin/bash -e
ssh ubuntu@pirkka.io redis-cli SAVE
ssh ubuntu@pirkka.io "sudo mv /var/lib/redis/dump.rdb ~/redis.dump-$(date +'%Y-%m-%dT%H:%M:%S').rdb"
ssh ubuntu@pirkka.io "sudo chown ubuntu:ubuntu ~/redis.dump*"
rsync ubuntu@pirkka.io:redis.dump* .
