# !/ bin / sh


java Coordinator 4432 4000 7 500 A B C &
echo " Waiting for coordinator to start ... " &
sleep 500 &
java Participant 4432 4000 12346 500 &
sleep 10 &
java Participant 4432 4000 12347 500 &
sleep 10 &
java Participant 4432 4000 12348 500 &
sleep 10 &
java Participant 4432 4000 12349 500 &
sleep 10 &
java Participant 4432 4000 12350 500 &
sleep 10 &
java Participant 4432 4000 12351 500 &
sleep 10 &
java Participant 4432 4000 12352 500 &
sleep 10 &
sleep 5000 