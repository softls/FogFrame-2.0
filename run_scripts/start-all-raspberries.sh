#!/usr/bin/env bash
# run all fog devices
# --------------------------------------------------------------------------------------------------------------------
# fog control nodes
date
log_dir=/home/olena/FogFrame-2/logs
echo "starting FCN1"
ssh pirate@192.168.1.105 -i 'bash -s' < run_fd.sh &> $log_dir/fogcontrolnode105.log &
#echo "starting FCN3"
#ssh pirate@192.168.1.107 -i 'bash -s' < run_fd.sh &> $log_dir/fogcontrolnode107.log &

#sleep 90
echo "starting FCN2"
ssh pirate@192.168.1.106 -i 'bash -s' < run_fd.sh &> $log_dir/fogcontrolnode106.log &

sleep 150

#fog cells
echo "starting FC3"
ssh pirate@192.168.1.112 -i 'bash -s' < run_fd.sh &> $log_dir/fogcell112.log &

#sleep 60

echo "starting FC1 and FC2"
ssh pirate@192.168.1.110 -i 'bash -s' < run_fd.sh &> $log_dir/fogcell110.log &
ssh pirate@192.168.1.111 -i 'bash -s' < run_fd.sh &> $log_dir/fogcell111.log &

