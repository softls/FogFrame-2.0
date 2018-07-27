#!/usr/bin/env bash
# fogcells
# --------------------------------------------------------------------------------------------------------------------
scp ./fogcell/target/fogcell-2.0.1-SNAPSHOT.jar pirate@192.168.1.110:~/bin
#scp ./hostmonitor/target/hostmonitor-2.0.1-SNAPSHOT.jar pirate@fog-cell-1:~/bin

scp ./fogcell/target/fogcell-2.0.1-SNAPSHOT.jar pirate@192.168.1.111:~/bin
#scp ./hostmonitor/target/hostmonitor-2.0.1-SNAPSHOT.jar pirate@fog-cell-2:~/bin

scp ./fogcell/target/fogcell-2.0.1-SNAPSHOT.jar pirate@192.168.1.112:~/bin
#scp ./hostmonitor/target/hostmonitor-2.0.1-SNAPSHOT.jar pirate@fog-cell-3:~/bin