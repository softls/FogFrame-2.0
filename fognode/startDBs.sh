#!/usr/bin/env bash
docker run -d -p 6380:6379 --name redisFN redis:latest
docker start redisFN

docker run -d -p 6383:6379 --name redisShared redis:latest
docker start redisShared