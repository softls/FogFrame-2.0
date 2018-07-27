#!/usr/bin/env bash
sleep 601
echo "Stopping FC1"
ssh pirate@192.168.1.110 -i 'bash -s' < stop_fc.sh &
sleep 30

date
log_dir=/home/olena/FogFrame-2/logs
echo "Starting FC1"
name="$log_dir/fogcell1101.log"
echo $name
ssh pirate@192.168.1.110 -i 'bash -s' < run_fd.sh &> $name &
sleep 90

sleep 125
echo "Stopping FC1"
ssh pirate@192.168.1.110 -i 'bash -s' < stop_fc.sh &
sleep 30

date
echo "Starting FC1"
name="$log_dir/fogcell1102.log"
echo $name
ssh pirate@192.168.1.110 -i 'bash -s' < run_fd.sh &> $name &
sleep 90

