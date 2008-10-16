#!/bin/bash

PROCLINE=$(ps -efw | grep "java -jar monica-server" | grep -v grep)

if (( $? )); then
  echo MoniCA server is not running
  exit
fi

PID=$(echo $PROCLINE | awk '{print $2}')
echo MoniCA server running as PID=$PID, terminating now
kill $PID
