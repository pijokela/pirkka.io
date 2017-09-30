#! /bin/bash

sbt dist
ssh ubuntu@pirkka.io sudo rm -rf /tmp/pirkka-io-1.0-SNAPSHOT.zip
scp target/universal/pirkka-io-1.0-SNAPSHOT.zip ubuntu@pirkka.io:/tmp/

ssh ubuntu@pirkka.io sudo rm -rf /opt/pirkka.io
ssh ubuntu@pirkka.io sudo rm -rf /opt/pirkka-io-1.0-SNAPSHOT
ssh ubuntu@pirkka.io sudo unzip /tmp/pirkka-io-1.0-SNAPSHOT.zip -d /opt/
ssh ubuntu@pirkka.io sudo chown -R ubuntu:ubuntu /opt/pirkka-io-1.0-SNAPSHOT
ssh ubuntu@pirkka.io sudo ln -s /opt/pirkka-io-1.0-SNAPSHOT /opt/pirkka.io

ssh ubuntu@pirkka.io sudo systemctl restart pirkka-io
