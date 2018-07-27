#!/usr/bin/env bash
docker build -t fognode .
docker rm -f fognode
docker run -ti -p 8080:8080 --name fognode -v /usr/bin/docker:/usr/bin/docker -v /var/run/docker.sock:/var/run/docker.sock --link redisFN fognode