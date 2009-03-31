#!/bin/bash

NUM=$(ps -efw | grep "java -jar open-monica.jar" | grep -v grep | wc -l)

if (( $NUM < 1 )); then
  cd ~/open-monica-read-only
  java -jar open-monica.jar >/dev/null 2>/dev/null
fi
