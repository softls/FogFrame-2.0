#!/usr/bin/env bash
# fog control nodes
# --------------------------------------------------------------------------------------------------------------------
scp ./fognode/target/fognode-2.0.1-SNAPSHOT.jar pirate@fog-control-node-1:~/bin
#scp ./hostmonitor/target/hostmonitor-2.0.1-SNAPSHOT.jar pirate@fog-control-node-1:~/bin

scp ./fognode/target/fognode-2.0.1-SNAPSHOT.jar pirate@fog-control-node-2:~/bin
#scp ./hostmonitor/target/hostmonitor-2.0.1-SNAPSHOT.jar pirate@fog-control-node-2:~/bin

scp ./fognode/target/fognode-2.0.1-SNAPSHOT.jar pirate@fog-control-node-3:~/bin
#scp ./hostmonitor/target/hostmonitor-2.0.1-SNAPSHOT.jar pirate@fog-control-node-3:~/bin