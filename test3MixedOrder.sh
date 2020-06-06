# !/ bin / sh

java Participant 4432 4000 12346 500 &
sleep 1 &
java Participant 4432 4000 12347 500 &
sleep 1 &
java Coordinator 4432 4000 4 500 A B C &
echo " Waiting for coordinator to start ... " &
java Participant 4432 4000 12348 500 &
sleep 1 &
sleep 500 
