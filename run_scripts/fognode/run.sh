#!/usr/bin/env bash
docker rm -f redisFCN
docker run -d -p 6380:6379 --name redisFN hypriot/rpi-redis:latest

docker run -d -p 6383:6379 --name redisShared hypriot/rpi-redis:latest
docker start redisShared

sh stopMonitor.sh
rm hostmonitor.log
nohup java -jar hostmonitor-2.0.1-SNAPSHOT.jar > hostmonitor.log &

docker build -t fognode .
docker rm -f fognode
docker run -ti -p 8080:8080 --name fognode -v /usr/bin/docker:/usr/bin/docker -v /var/run/docker.sock:/var/run/docker.sock --link redisFN fognode
