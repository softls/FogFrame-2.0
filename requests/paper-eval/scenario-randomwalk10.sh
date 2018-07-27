#!/usr/bin/env bash
# reasoner/test/{countT1}/{countT2}/{countT3}/{countT4}/{minutes}/{minutes}
# 21 [6, 8, 3, 12, 14, 9, 7, 4, 3, 12]
# -2 [4, 6, 1, 10, 12, 7, 5, 2, 1, 10]
date
http post http://192.168.1.105:8080/reasoner/test/2/0/4/0/1/3 --timeout=1

sleep 59
date

http post http://192.168.1.105:8080/reasoner/test/2/0/6/0/1/3 --timeout=1

sleep 59
date

http post http://192.168.1.105:8080/reasoner/test/2/0/1/0/1/3 --timeout=1

sleep 59
date

http post http://192.168.1.105:8080/reasoner/test/2/0/10/0/1/3 --timeout=1

sleep 59
date

http post http://192.168.1.105:8080/reasoner/test/2/0/12/0/1/3 --timeout=1

sleep 59
date

http post http://192.168.1.105:8080/reasoner/test/2/0/7/0/1/3 --timeout=1

sleep 59
date

http post http://192.168.1.105:8080/reasoner/test/2/0/5/0/1/3 --timeout=1

sleep 59
date

http post http://192.168.1.105:8080/reasoner/test/2/0/2/0/1/3 --timeout=1

sleep 59
date

http post http://192.168.1.105:8080/reasoner/test/2/0/1/0/1/3 --timeout=1

sleep 59
date

http post http://192.168.1.105:8080/reasoner/test/2/0/10/0/1/3 --timeout=1