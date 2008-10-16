#!/bin/bash

NUM=$(ps -efw | grep "java -jar monica-server" | grep -v grep | wc -l)

if (( $NUM < 1 )); then
  cd ~/open-monica-read-only
  java -jar monica-server.jar >/dev/null 2>/dev/null
fi
