#!/usr/bin/env bash
mvn clean install docker:build
docker rm -f fog-controller
docker run -ti -p 8082:8082 --name fog-controller -v /usr/bin/docker:/usr/bin/docker -v /var/run/docker.sock:/var/run/docker.sock --link redisFogController fog-controller