#!/bin/sh

clientJar=`ls -t vidsync*client.jar|head -1`
ourRmiIp=`hostname -I|head -1`

java -Djava.rmi.server.hostname=$ourRmiIp -jar $clientJar &