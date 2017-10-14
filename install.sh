#! /bin/bash

SERVICE=pirkka-io
APPNAME=pirkka.io
APPNAME_PACKAGE=${SERVICE}-1.0-SNAPSHOT
TARGET_USER=ubuntu
TARGET_SERVER=ubuntu@pirkka.io

sbt dist
rm -rf /tmp/${APPNAME_PACKAGE}
mv target/universal/${APPNAME_PACKAGE}.zip /tmp/
unzip /tmp/${APPNAME_PACKAGE}.zip -d /tmp/
rsync -avz --delete /tmp/${APPNAME_PACKAGE} ${TARGET_SERVER}:/tmp/

ssh ${TARGET_SERVER} sudo rm -rf /opt/${APPNAME}
ssh ${TARGET_SERVER} sudo rm -rf /opt/${APPNAME_PACKAGE}
ssh ${TARGET_SERVER} sudo cp -a /tmp/${APPNAME_PACKAGE} /opt/${APPNAME_PACKAGE}
ssh ${TARGET_SERVER} sudo chown -R ${TARGET_USER}:${TARGET_USER} /opt/${APPNAME_PACKAGE}
ssh ${TARGET_SERVER} sudo ln -s /opt/${APPNAME_PACKAGE} /opt/${APPNAME}

# UPSTART:
# ssh ${TARGET_SERVER} sudo restart ${SERVICE}

# SYSTEMD:
ssh ${TARGET_SERVER} sudo systemctl restart ${SERVICE}
