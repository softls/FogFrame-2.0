#!/usr/bin/env bash
docker run -d -p 6382:6379 --name redisFogController redis:latest
docker start redisFogController