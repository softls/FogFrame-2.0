lower=600;
upper=1100;
RANGE=$((lower-upper+1));
RESULT=$RANDOM;

let "RESULT %= $RANGE";
RESULT=$(($RESULT+lower));

echo $RESULT