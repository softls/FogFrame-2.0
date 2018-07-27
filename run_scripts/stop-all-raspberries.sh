#!/usr/bin/env bash
# run all fog devices
# --------------------------------------------------------------------------------------------------------------------
# fog control nodes
date
ssh pirate@192.168.1.105 -i 'bash -s' < stop_fcn.sh &
ssh pirate@192.168.1.106 -i 'bash -s' < stop_fcn.sh &
ssh pirate@192.168.1.107 -i 'bash -s' < stop_fcn.sh &

#fog cells
ssh pirate@192.168.1.110 -i 'bash -s' < stop_fc.sh &
ssh pirate@192.168.1.111 -i 'bash -s' < stop_fc.sh &
ssh pirate@192.168.1.112 -i 'bash -s' < stop_fc.sh &