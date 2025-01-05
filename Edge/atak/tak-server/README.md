TC 2020-12-22 (Tue) --

-------------
# Build
-------------
```
export DOCKER_BUILDKIT=1
export COMPOSE_DOCKER_CLI_BUILD=1
cd <top_level>
```


## TAK Server using CentOS7:
```
docker rmi takserver:centos7
docker build --rm -t takserver:centos7base -f Dockerfile.centos7base .
```

## Customize Docker
### Start instance
```
## Start instance
docker run --name takserver -it --rm --tmpfs /tmp --tmpfs /run -v /sys/fs/cgroup:/sys/fs/cgroup:ro  takserver:centos7base

## attach to instance
docker exec -it takserver bash
```
### Install database (from inside instance)
```
/opt/tak/db-utils/takserver-setup-db.sh
systemctl daemon-reload
```
### Enable TAK server at boot-time
```
systemctl enable takserver
```
### Create keystore and truststore (X.509 certificates)
```
sed --in-place 's|STATE=${STATE}|STATE=Virginia|' /opt/tak/certs/cert-metadata.sh
sed --in-place 's|CITY=${CITY}|CITY=Fairfax|' /opt/tak/certs/cert-metadata.sh
sed --in-place 's|${ORGANIZATION:-TAK}|APOLLO-TAK|' /opt/tak/certs/cert-metadata.sh
sed --in-place '/Please edit cert-metadata.sh/d' /opt/tak/certs/cert-metadata.sh

# Create certificate authority (CA):
cd /opt/tak/certs/
echo APOLLO-CA | ./makeRootCa.sh 

# Create a server certificate:
./makeCert.sh server takserver

# Create a client certificates:
./makeCert.sh client user
./makeCert.sh client user1
./makeCert.sh client user2
./makeCert.sh client user3

# admin to access the admin UI:
./makeCert.sh client admin

# restart 
service takserver restart
sleep 60

# Authorize the admin cert to perform administrative functions using the UI:
java -jar /opt/tak/utils/UserManager.jar certmod -A /opt/tak/certs/files/admin.pem

# Create login credentials for local adminstrative access to the configuration interface:
# needed browser address
# firefox http://localhost:8080 and https://localhost:8446

java -jar /opt/tak/utils/UserManager.jar usermod -A -p atakatak admin
java -jar /opt/tak/utils/UserManager.jar usermod -p atakatak user
java -jar /opt/tak/utils/UserManager.jar usermod -p atakatak user1
java -jar /opt/tak/utils/UserManager.jar usermod -p atakatak user2
java -jar /opt/tak/utils/UserManager.jar usermod -p atakatak user3

```

### Enable ssh server

```

# Install sshserver
yum install -y openssh-server
yum install -y openssh-clients
chkconfig sshd on

# allow tak user to ssh into
echo -e 'atakatak\natakatak' | passwd tak

sed --in-place '/pam_nologin\.so/i \
account [success=1 default=ignore] pam_succeed_if.so quiet uid eq 1491' \
/etc/pam.d/sshd

```

### Configure TAK Server Certificate
```
sed --in-place '/announce/i <input _name="tlsx509" protocol="tls" port="8089" auth="x509"/>' /opt/tak/CoreConfig.xml

sed --in-place 's|<auth>|<auth x509groups="true" x509addAnonymous="true">|' /opt/tak/CoreConfig.xml

service takserver restart
```


### Configure TAK Client Certificate (from outside instance)
```
docker cp takserver:/opt/tak/certs/files/admin.pem .
docker cp takserver:/opt/tak/certs/files/admin.p12 .
docker cp takserver:/opt/tak/certs/files/truststore-root.p12 .

docker cp takserver:/opt/tak/certs/files/user.p12 .
docker cp takserver:/opt/tak/certs/files/user1.p12 .
docker cp takserver:/opt/tak/certs/files/user2.p12 .
docker cp takserver:/opt/tak/certs/files/user3.p12 .

```

## Save (snapshot) Customize Docker (from outside docker)
```
docker commit takserver takserver:centos7

# if need to rename
#docker rmi takserver:centos7
#docker tag takserver:tmp takserver:centos7

# save image as a tarball
docker save takserver:centos7 | pigz > takserver_centos7.tgz

# load tarball into image
# docker load < takserver_centos7.tgz
```

## Setup Client
```
# import client certificate to browser, so that you can access the Admin
admin.pem 
admin.p12

# needed for browser address
#firefox https://localhost:8443

#
```


-------------
# Run
-------------
## TAK Server CentOS7 Container:
```
docker run --name takserver -it --rm -p 8080:8080 -p 8443:8443 -p 8444:8444 -p 8446:8446 -p 8089:8089 -p 8087:8087 -p 8088:8088 -p 9000:9000 -p 9001:9001 -p8822:22 --tmpfs /tmp --tmpfs /run -v /sys/fs/cgroup:/sys/fs/cgroup:ro takserver:centos7

# attach to docker instance
docker exec -it takserver bash

# to stop:
docker stop takserver 

```

## Web Client and WebTak:

- Suppose your IP address is 192.168.1.167, 

  - https://192.168.1.167:8443/    # need certificate
  - https://192.168.1.167:8446/    # don't need certificate (username=admin and password=atakatak)
  - http://192.168.1.167:8080/     # don't need certificate (username=admin and password=atakatak)

- webtak (must use https)

  - https://192.168.1.167:8443/Marti/webtak/index.html # need certificate
  - https://192.168.1.167:8446/Marti/webtak/index.html # (username=admin and password=atakatak)


## ATAK Client
    - Start emulator
### port reverse
```
adb -e reverse tcp:8089 tcp:8089   # need certificate
adb -e reverse tcp:8087 tcp:8087   # don't need certificate
```
### ATAK Server connection setting Using 8089 ssl 
    - Name: Local SSL
    - Address: 127.0.0.1
    - Advanced Options
      - SSL
      - Server Port 8089
      - import Trust Store --> upload 
        - adb shell mkdir /sdcard/download/atak_cert_files/
        - adb -e push truststore-root.p12 /sdcard/download/atak_cert_files/
        - password = atakatak
      - import client certificate --> upload 
        - adb -e push user2.p12 /sdcard/download/atak_cert_files/
        - password = atakatak
### ATAK Server connection setting Using 8087 tcp
    Doesn't seem to work.

# MISC
```
# some useful command inside centOS7
systemctl status takserver
systemctl restart takserver
java -jar /opt/tak/utils/UserManager.jar certmod -A /opt/tak/certs/files/admin.pem 
systemctl daemon-reload
service takserver status
service takserver restart
java -jar /opt/tak/utils/UserManager.jar usermod -A -p atakatak admin
java -jar /opt/tak/utils/UserManager.jar usermod -p atakatak user
journalctl -xe

# some useful outsize docker
docker cp takserver:/opt/tak/certs/files/user3.p12 .
adb -d reverse tcp:8089 tcp:8089
adb -d shell mkdir /sdcard/download/atak_cert_files/
adb -d push user1.p12 /sdcard/download/atak_cert_files/
adb -d push truststore-root.p12 /sdcard/download/atak_cert_files/


adb -e reverse tcp:8089 tcp:8089
adb -e shell mkdir /sdcard/download/atak_cert_files/
adb -e push user2.p12 /sdcard/download/atak_cert_files/
adb -e push truststore-root.p12 /sdcard/download/atak_cert_files/

scrcpy -s 3658324553443498
adb -s 3658324553443498 forward tcp:8024 tcp:8022
ssh -p8024 user@localhost 
adb -s 3658324553443498 reverse tcp:8089 tcp:8089

Setting->all->network->show connection
```

# CoT experiment
```bash
# inject cot message: doesn't seem to works..
nc -w 1 192.168.1.167 8087 <your.xml> 

# output all Cot messages
nc 192.168.1.167 8088

# get missions
curl -s "http://localhost:8080/Marti/api/missions" | python -mjson.tool | grep name

# get single mission
curl -s "http://localhost:8080/Marti/api/missions/new_feed" | python -mjson.tool

# get mission CoT (what does it mean?)
curl -s "http://localhost:8080/Marti/api/missions/new_feed/cot" | tidy -xml -i 

# get mission log
curl -s "http://localhost:8080/Marti/api/missions/new_feed/log" | python -mjson.tool

# upload a file to enterprise sync (with keyword)
curl -v -X POST -H "Content-Type: image/png" \
--data-binary "@map_a.png" \
'http://localhost:8080/Marti/sync/upload?name=map_a.png&keywords=dog'

## got reply:
{"UID":"046de24b-28f7-49db-80d3-8adc1d099caf","SubmissionDateTime":"2020-10-16T17:21:26.388Z","MIMEType":"image\/png","SubmissionUser":"anonymous","PrimaryKey":"48","Hash":"0af786676ce2cc21be8f971f6c5fe7ff694fd7742b724d1df3026abfbab06535","Name":"map_a.png"}

## Search enterprise files by keyword (can be issued by client)
curl -s -X GET http://192.168.1.167:8080/Marti/sync/search?keywords=dog | python -mjson.tool

## Download a file by its hash
curl -v  'http://localhost:8080/Marti/sync/content?hash=0af786676ce2cc21be8f971f6c5fe7ff694fd7742b724d1df3026abfbab06535' --output elme

# put file into mission, by hash (file) -- will triger a data sync feed in webTak, which subscribes the mission.!

curl -X PUT "http://localhost:8080/Marti/api/missions/new_feed/contents" -H "Content-Type: application/json" -d '{"hashes" : ["7c871243624921a872beecf2b4ec7e0be45a6e10e83cddb63b0be645ade4b3a6"]}'


# subscribe client to mission package, using clienet uid ??
deviceUid=ANDROID-R38MA0BQDNH
webtakUid=988edaa9-ae1c-80ff-debd-1cdc66dd0ebb
curl -v -X PUT "http://localhost:8080/Marti/api/missions/new_feed/subscription?uid=$deviceUid"
curl -v -X PUT "http://localhost:8080/Marti/api/missions/new_feed/subscription?uid=$webtakUid"


# unsubscribe client to mission package, using clienet uid (webtak)
curl -v -X DELETE "http://localhost:8080/Marti/api/missions/new_feed/subscription?uid=$deviceUid"

# get all subscriptions
curl -v 'http://localhost:8080/Marti/api/missions/all/subscriptions' 

# download a mission package:
curl -v --output delme.zip 'http://localhost:8080/Marti/api/missions/new_feed/archive' 


```
