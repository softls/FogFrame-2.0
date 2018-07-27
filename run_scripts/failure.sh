#!/usr/bin/env bash
sleep 600
f=0
while true
do
    declare -a arr=(1 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0  0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0) # 1% og probability of failure event occurrence
    echo "Starting to generate failure of FC1 with 0.05% rate per second"
    x=0
    n=0
    while [ $x -lt 1 ]
    do
        rand=$[$RANDOM % ${#arr[@]}]
        echo $(date)
        echo "iteration=${n}"
        x=${arr[$rand]}
        echo "isFailure=${x}"
        n=$((n+1))
        sleep 1
    done
    f=$((f+1))
    echo "Stopping FC1"
    ssh pirate@192.168.1.110 -i 'bash -s' < stop_fc.sh &
    sleep 30

    date
    log_dir=/home/olena/FogFrame-2/logs

    echo "Starting FC1"
    name="$log_dir/fogcell110$f.log"
    echo $name
    ssh pirate@192.168.1.110 -i 'bash -s' < run_fd.sh &> $name &
    sleep 90
done


