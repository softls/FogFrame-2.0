#!/usr/bin/env bash
# run all fog devices
# --------------------------------------------------------------------------------------------------------------------
# fog control nodes
date
log_dir=/home/olena/FogFrame-2.0/logs
echo "starting FN1"
ssh pirate@192.168.1.105 -i 'bash -s' < run_fd.sh &> $log_dir/fognode105.log &
#echo "starting FN3"
#ssh pirate@192.168.1.107 -i 'bash -s' < run_fd.sh &> $log_dir/fognode107.log &

sleep 100
echo "starting FN2"
ssh pirate@192.168.1.106 -i 'bash -s' < run_fd.sh &> $log_dir/fognode106.log &


sleep 150


echo "starting FC1 and FC2"
ssh pirate@192.168.1.110 -i 'bash -s' < run_fd.sh &> $log_dir/fogcell110.log &
ssh pirate@192.168.1.111 -i 'bash -s' < run_fd.sh &> $log_dir/fogcell111.log &


#fog cells
echo "starting FC3"
ssh pirate@192.168.1.112 -i 'bash -s' < run_fd.sh &> $log_dir/fogcell112.log &

