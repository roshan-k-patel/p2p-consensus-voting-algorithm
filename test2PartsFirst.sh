# !/ bin / sh

java Participant 4432 4000 12346 500 &
sleep 1 &
java Participant 4432 4000 12347 500 &
sleep 1 &
java Participant 4432 4000 12348 500 &
java Coordinator 4432 4000 3 500 A B C &
echo " Waiting for coordinator to start ... " &
sleep 500 
