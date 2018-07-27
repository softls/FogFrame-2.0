#!/usr/bin/env bash
# fog control nodes
# --------------------------------------------------------------------------------------------------------------------
scp ./fognode/target/fognode-2.0.1-SNAPSHOT.jar pirate@192.168.1.105:~/bin
#scp ./hostmonitor/target/hostmonitor-2.0.1-SNAPSHOT.jar pirate@fog-control-node-1:~/bin

scp ./fognode/target/fognode-2.0.1-SNAPSHOT.jar pirate@192.168.1.106:~/bin
#scp ./hostmonitor/target/hostmonitor-2.0.1-SNAPSHOT.jar pirate@fog-control-node-2:~/bin

scp ./fognode/target/fognode-2.0.1-SNAPSHOT.jar pirate@192.168.1.107:~/bin
#scp ./hostmonitor/target/hostmonitor-2.0.1-SNAPSHOT.jar pirate@fog-control-node-3:~/bin
