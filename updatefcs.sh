#!/usr/bin/env bash
# fogcells
# --------------------------------------------------------------------------------------------------------------------
scp ./fogcell/target/fogcell-2.0.1-SNAPSHOT.jar pirate@fog-cell-1:~/bin
#scp ./hostmonitor/target/hostmonitor-2.0.1-SNAPSHOT.jar pirate@fog-cell-1:~/bin

scp ./fogcell/target/fogcell-2.0.1-SNAPSHOT.jar pirate@fog-cell-2:~/bin
#scp ./hostmonitor/target/hostmonitor-2.0.1-SNAPSHOT.jar pirate@fog-cell-2:~/bin

scp ./fogcell/target/fogcell-2.0.1-SNAPSHOT.jar pirate@fog-cell-3:~/bin
#scp ./hostmonitor/target/hostmonitor-2.0.1-SNAPSHOT.jar pirate@fog-cell-3:~/bin