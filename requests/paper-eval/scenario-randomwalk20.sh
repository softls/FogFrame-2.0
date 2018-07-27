#!/usr/bin/env bash
# reasoner/test/{countT1}/{countT2}/{countT3}/{countT4}/{minutes}/{minutes}
# 8 [3, 3, 7, 11, 8, 13, 8, 4, 8, 12, 7, 3, 3, 6, 3, 6, 11, 14, 9, 6]
# -2[1, 1, 5,  9, 6, 11, 6, 2, 6, 10, 5, 1, 1, 4, 2, 4, 9, 11, 7, 4]

date
http post http://192.168.1.105:8080/reasoner/test/2/0/1/0/1/3 --timeout=1

sleep 59
date

http post http://192.168.1.105:8080/reasoner/test/2/0/1/0/1/3 --timeout=1

sleep 59
date

http post http://192.168.1.105:8080/reasoner/test/2/0/5/0/1/3 --timeout=1

sleep 59
date

http post http://192.168.1.105:8080/reasoner/test/2/0/9/0/1/3 --timeout=1

sleep 59
date

http post http://192.168.1.105:8080/reasoner/test/2/0/6/0/1/3 --timeout=1

sleep 59
date

http post http://192.168.1.105:8080/reasoner/test/2/0/11/0/1/3 --timeout=1

sleep 59
date

http post http://192.168.1.105:8080/reasoner/test/2/0/6/0/1/3 --timeout=1

sleep 59
date

http post http://192.168.1.105:8080/reasoner/test/2/0/2/0/1/3 --timeout=1

sleep 59
date

http post http://192.168.1.105:8080/reasoner/test/2/0/6/0/1/3 --timeout=1

sleep 59
date

http post http://192.168.1.105:8080/reasoner/test/2/0/10/0/1/3 --timeout=1

sleep 59
date

http post http://192.168.1.105:8080/reasoner/test/2/0/5/0/1/3 --timeout=1

sleep 59
date

http post http://192.168.1.105:8080/reasoner/test/2/0/1/0/1/3 --timeout=1

sleep 59
date

http post http://192.168.1.105:8080/reasoner/test/2/0/1/0/1/3 --timeout=1

sleep 59
date

http post http://192.168.1.105:8080/reasoner/test/2/0/4/0/1/3 --timeout=1

sleep 59
date

http post http://192.168.1.105:8080/reasoner/test/2/0/2/0/1/3 --timeout=1

sleep 59
date

http post http://192.168.1.105:8080/reasoner/test/2/0/4/0/1/3 --timeout=1

sleep 59
date

http post http://192.168.1.105:8080/reasoner/test/2/0/9/0/1/3 --timeout=1

sleep 59
date

http post http://192.168.1.105:8080/reasoner/test/2/0/11/0/1/3 --timeout=1

sleep 59
date

http post http://192.168.1.105:8080/reasoner/test/2/0/7/0/1/3 --timeout=1

sleep 59
date

http post http://192.168.1.105:8080/reasoner/test/2/0/4/0/1/3 --timeout=1