import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Participant {

    int coordinatorPort;
    int listenerPort;
    int participantport;
    int timeout;
    ArrayList<String> votingOptions;
    String myvote;
    ArrayList<String> otherParticipants;
    boolean hasDetails;
    boolean hasVoteOptions;
    int currentRound;
    int totalRounds;
    HashMap<String,String> tempVotes;
    HashMap<String,String> mainVotes;
    HashMap<String,String> newVotes;
    boolean roundVotesSent;
    ArrayList<Socket> socketsOtherParticipants;
    String winningVote;
    HashMap<String,ArrayList<Vote>> votesReceived;


    public Participant(String[] args) {
        this.coordinatorPort = Integer.parseInt(args[0]);
        this.listenerPort = Integer.parseInt(args[1]);
        this.participantport = Integer.parseInt(args[2]);
        this.timeout = Integer.parseInt(args[3]);
        votingOptions = new ArrayList<>();
        hasDetails = false;
        hasVoteOptions = false;
        tempVotes = new HashMap<>();
        mainVotes = new HashMap<>();
        newVotes = new HashMap<>();
        currentRound = 1;
        roundVotesSent = false;
        socketsOtherParticipants = new ArrayList<>();
        winningVote = null;
        votesReceived = new HashMap<>();

        while (true) {
            try {
                // Creates a socket with host ip address and coordinator port
                Socket socket = new Socket(InetAddress.getLocalHost(), coordinatorPort);
                new Thread(new ParticipantVoteInitThread(socket, this)).start();
                otherParticipants = new ArrayList<>();
                break;

            } catch (Exception e) {
                System.out.println("Waiting for Coordinator on port: " + coordinatorPort);
            }
        }
    }

    public static void main(String[] args) {
        try {
            ParticipantLogger.initLogger(Integer.parseInt(args[1]), Integer.parseInt(args[2]),60000);
            Participant participant = new Participant(args);
        } catch (IOException e) {
            e.printStackTrace();
        }

            //Waits for all participants to connect

    }

    class ParticipantVoteInitThread implements Runnable {
        Socket socketPartCord;
        Participant participant;

        ParticipantVoteInitThread(Socket s, Participant p) {
            // SOCKET FROM THE PARTICIPANTS REFERENCE, IN = PARTICIPANT SIDE, OUT = COORD SIDE
            socketPartCord = s;
            participant = p;
        }

        public void run() {
            try {
            //    Thread.sleep(100);
                ParticipantLogger.getLogger().startedListening();
                BufferedReader in = new BufferedReader(new InputStreamReader(socketPartCord.getInputStream()));
                PrintWriter out = new PrintWriter(socketPartCord.getOutputStream(), true);
                ParticipantLogger.getLogger().connectionEstablished(socketPartCord.getPort());
                // SENDING JOIN REQUEST AND PORT ID TO COORDINATOR
                // Printer writer which prints a message into the socket output stream
                out.println("JOIN " + participant.participantport);
                ParticipantLogger.getLogger().joinSent(socketPartCord.getPort());

                Thread.sleep(1000);


                // ACCEPTING OTHER PARTICIPANTS (DETAILS) AND VOTE OPTIONS
                String line;
                while ((line = in.readLine()) != null) {


                    //DETAILS
                    if (line.contains("DETAILS")) {
                        ParticipantLogger.getLogger().messageReceived(socketPartCord.getPort(),line);
                        String[] parts = line.split(" ");
                        ArrayList<Integer> otherParticipantInts = new ArrayList<>();
                        for (int i = 1; i < parts.length; i++) {
                            participant.addOtherParticipant(parts[i]);
                            participant.addOtherParticipantInts(otherParticipantInts,parts[i]);
                        }
                        participant.hasDetails = true;
                        ParticipantLogger.getLogger().detailsReceived(otherParticipantInts);
                        System.out.println("received details");

                    }

                    //VOTE_OPTIONS
                    if (line.contains("VOTE_OPTIONS")) {
                        String[] parts = line.split(" ");
                        ParticipantLogger.getLogger().messageReceived(socketPartCord.getPort(),line);

                        for (int i = 1; i < parts.length; i++) {
                            participant.addVotingOption(parts[i]);
                        }

                        participant.hasVoteOptions = true;

                        ParticipantLogger.getLogger().voteOptionsReceived(participant.getVotingOptions());
                        System.out.println("Received voting options");



                    }
                    if (participant.hasVoteOptions && participant.hasDetails) {

                        break;
                    }

                }

                /*SETS NUMBER OF ROUNDS TO N + 1 ROUNDS AND RANDOMLY GENERATES THIS PARTICIPANT'S VOTE AND
                THEN SETS IT TO THE OBJECT VARIABLE MYVOTE. ADDS THIS VOTE TO MAIN LIST*/
                Thread.sleep(1000);
                participant.setTotalRounds(getOtherParticipantSize()+2);
                participant.setVote(pickMyVote());
                participant.recordMyVote(participant.getMainVotes(),participant.getParticipantport(),participant.myvote);
                System.out.println("Vote is ready to commence, please wait, waiting time is proportional to no of participants");

             /*   CREATES A NEW THREAD THAT CREATES A SERVER SOCKET ON THIS PARTICIPANT'S PORT AND LISTENS
                FOR INCOMING CONNECTION REQUESTS FROM OTHER PARTICIPANT SOCKETS
                WHEN IT GETS A SOCKET CONNECTION IT MAKES A NEW THREAD AND LISTENS FOR INCOMING VOTES ON IT*/
                new Thread (new ListenVoterConnectionThread(participant)).start();

                Thread.sleep(500*participant.getOtherParticipantSize());

                //ROUNDS OF VOTING

                for (participant.getCurrentRound(); participant.getCurrentRound() <= participant.getTotalRounds(); participant.incrementRound()){
                    Thread.sleep(100);

                    ParticipantLogger.getLogger().beginRound(participant.getCurrentRound());

                    if (participant.getCurrentRound() == 1){
                        sendMyVote(participant);
                        Thread.sleep(100);
                        participant.putNewVotesInNewVoteMap(participant);
                        Thread.sleep(100);
                        participant.clearMap(participant.getTempVotes());
                        Thread.sleep(100);
                        participant.putNewInMain(participant);
                        Thread.sleep(100);
                    }

                    if (participant.getCurrentRound() > 1){
                        //TESTING FAILURE
                        /*Random random = new Random();
                        int fail = random.nextInt(10);
                        if (fail == 1){
                            System.exit(0);
                        }*/
                        Thread.sleep(100);
                        participant.sendPreviousRoundNewVotes(participant);
                        participant.clearMap(participant.newVotes);
                        Thread.sleep(100);
                        participant.putNewVotesInNewVoteMap(participant);
                        Thread.sleep(100);
                        participant.clearMap(participant.getTempVotes());
                        Thread.sleep(100);
                        participant.putNewInMain(participant);
                    }

                    //calls the votes received method for all the votes received
                    for (String x : participant.getVotesReceived().keySet()){
                        int portReceievedFrom = Integer.parseInt(x);
                        ParticipantLogger.getLogger().votesReceived(portReceievedFrom,participant.getVotesReceived().get(x));
                    }
                    participant.getVotesReceived().clear();

                    Thread.sleep(50);

                    ParticipantLogger.getLogger().endRound(participant.getCurrentRound());

                    System.out.println( "Round " + participant.getCurrentRound() + " Main votes for " + participant.getParticipantport() + " are " + participant.getMainVotes());
                    System.out.println("Round " + participant.getCurrentRound() + " New votes for " + participant.getParticipantport() + " are " + participant.getNewVotes());

                }

                participant.calculateWinningVote(participant);
                System.out.println("Winning vote is: " + participant.getOutcomeVote(participant));
                ParticipantLogger.getLogger().outcomeDecided(participant.getOutcomeVote(participant),participant.getAllParticipantInts());
                Thread.sleep(100);
                String outcomeMessage = "OUTCOME " + participant.getOutcomeVote(participant) + " " + participant.getOutcomePortsString(participant);
                out.println(outcomeMessage);
                ParticipantLogger.getLogger().messageSent(socketPartCord.getPort(),outcomeMessage);
                ParticipantLogger.getLogger().outcomeNotified(participant.getOutcomeVote(participant),participant.getAllParticipantInts());
                Thread.sleep(500*participant.getOtherParticipantSize());
                System.exit(1);

                // participant.close();
            } catch (Exception e) {
              //  System.out.println("error" + e);
            }
        }
    }


    class ListenVoterConnectionThread implements Runnable {
        Participant participant;

        ListenVoterConnectionThread(Participant p) {
            participant = p;
        }

        public void run() {
            try {
                /*BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);*/


                ServerSocket socketVoteReceiver = new ServerSocket(participant.getParticipantport());

                while(true){
                    try {
                        Socket socketParticipantConnection = socketVoteReceiver.accept();
                        ParticipantLogger.getLogger().connectionAccepted(socketParticipantConnection.getPort());
                        new Thread(new ListenForVotes(socketParticipantConnection,participant)).start();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            } catch (Exception e) {
            }
        }
    }

    class ListenForVotes implements Runnable {
        Socket socket;
        Participant participant;

        ListenForVotes(Socket s, Participant p) {
            socket = s;
            participant = p;
        }

        public void run() {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

                //  System.out.println("LISTENING FOR VOTES");
                String line;
                String port = "";
                String vote = "";
                while ((line = in.readLine()) != null) {
                    //   Thread.sleep(500);
                    if (line.contains("VOTE")){
                        ParticipantLogger.getLogger().messageReceived(socket.getPort(),line);
                        String[] parts = line.split(" ");

                        port = parts[1];
                        vote = parts[2];

                        putVoteInTemp(participant,port,vote);
                        Vote voteObject = new Vote(Integer.parseInt(port),vote);
                        String socketPort = String.valueOf(socket.getPort());

                        // checks if the receivedVotes hashmap doesnt have the port makes it and adds the vote
                        // else it just adds the vote
                        if (!participant.getVotesReceived().keySet().contains(socketPort)) {
                            ArrayList<Vote> receivedVotes = new ArrayList<>();
                            participant.getVotesReceived().put(socketPort, receivedVotes);
                        }
                        participant.getVotesReceived().get(socketPort).add(voteObject);


                    }

                }
            } catch (Exception e) {
            }
        }
    }


    public synchronized void sendMyVote(Participant participant){
        for (String port : participant.getOtherParticipants()) {
            ArrayList<Vote> sentVotes = new ArrayList<>();
            Socket socket = null;
            PrintWriter out = null;
            try {
                socket = new Socket(InetAddress.getLocalHost(), Integer.parseInt(port));
                ParticipantLogger.getLogger().connectionEstablished(socket.getPort());
                out = new PrintWriter(socket.getOutputStream(), true);
                addSocketToSocketsList(participant, socket);
                String message = "VOTE " + participant.getParticipantport() + " " + participant.getMyvote();
                Vote vote = new Vote(participant.getParticipantport(),participant.getMyvote());
                sentVotes.add(vote);
                out.println(message);
                ParticipantLogger.getLogger().messageSent(socket.getPort(),message);
            } catch (IOException e) {
                e.printStackTrace();
            }
            ParticipantLogger.getLogger().votesSent(socket.getPort(),sentVotes);
            System.out.println("Sent Vote: " + participant.getParticipantport() + " " + participant.getMyvote());
        }
    }

    public synchronized void sendPreviousRoundNewVotes(Participant participant){
        Socket socket = null;
        for (Socket s : participant.getSocketsOtherParticipants()) {
        try {
                PrintWriter out = null;
                socket = s;
                ArrayList<Vote> sentVotes = new ArrayList<>();
                out = new PrintWriter(s.getOutputStream(), true);

                for (String x : participant.getNewVotes().keySet()) {
                    String message = "VOTE " + x + " " + participant.getNewVotes().get(x);
                    out.println(message);
                    ParticipantLogger.getLogger().messageSent(s.getPort(), message);
                    System.out.println("New Votes last round " + x + " " + participant.getNewVotes().get(x));
                    Vote vote = new Vote(Integer.parseInt(x),participant.getNewVotes().get(x));
                    sentVotes.add(vote);
                }

                ParticipantLogger.getLogger().votesSent(socket.getPort(),sentVotes);

            } catch(Exception e){
                ParticipantLogger.getLogger().participantCrashed(socket.getPort());
            }
        }
    }

    public synchronized void recordMyVote(HashMap<String,String> hashMap, int port, String vote){
        String portString = String.valueOf(port);
        hashMap.put(portString,vote);
    }

    private String pickMyVote() {
        Random rand = new Random();
        int x = rand.nextInt(votingOptions.size());
        return votingOptions.get(x);
    }

    private void setVote(String vote){
        myvote = vote;
    }

    public void setTotalRounds(int totalRounds) {
        this.totalRounds = totalRounds;
    }

    public synchronized void putNewVotesInNewVoteMap(Participant participant){
        //IF ITS NOT IN THE MAIN VOTES PUT IT IN NEW
        for (Map.Entry<String, String> x : participant.getTempVotes().entrySet()){
            if (!(participant.getMainVotes().entrySet().contains(x))){
                putVoteInNew(participant,x);
            }
        }
    }

    public synchronized void putVoteInNew(Participant participant, Map.Entry<String, String> x){
        participant.getNewVotes().put(x.getKey(),x.getValue());
    }

    public synchronized void putVoteInTemp(Participant participant, String port, String vote){
        participant.tempVotes.put(port,vote);
    }

    public synchronized void putTempInMain(HashMap<String,String> mainVotes, HashMap<String,String> tempVotes){
        mainVotes.putAll(tempVotes);
    }

    public synchronized void putNewInMain(Participant participant){
        participant.getMainVotes().putAll(participant.getNewVotes());
    }

    public synchronized int getParticipantport() {
        return participantport;
    }

    public synchronized String getMyvote() {
        return myvote;
    }

    public synchronized int getCurrentRound() {
        return currentRound;
    }

    public synchronized int getTotalRounds() {
        return totalRounds;
    }

    public synchronized void incrementRound(){
        currentRound = currentRound + 1;
    }

    public synchronized ArrayList<String> getOtherParticipants(){
        return otherParticipants;
    }

    public synchronized int getOtherParticipantSize(){
        return otherParticipants.size();
    }

    public synchronized String getOtherParticipantAtIndex(int index){
        return otherParticipants.get(index);
    }

    public synchronized void addOtherParticipant(String participant) {
        this.otherParticipants.add(participant);
    }

    public synchronized void addOtherParticipantInts(ArrayList<Integer> integerArrayList,String participant) {
        integerArrayList.add(Integer.valueOf(participant));
    }

    public synchronized void addVotingOption(String option) {
        this.votingOptions.add(option);
    }

    public synchronized HashMap<String, String> getTempVotes() {
        return tempVotes;
    }

    public synchronized HashMap<String, String> getMainVotes() {
        return mainVotes;
    }

    public synchronized HashMap<String, String> getNewVotes() {
        return newVotes;
    }

    public synchronized void putTempInNew(Participant participant){
        participant.getNewVotes().putAll(tempVotes);
    }

    public synchronized void clearMap(HashMap<String,String> hashMap){
        hashMap.clear();
    }

    public ArrayList<Socket> getSocketsOtherParticipants() {
        return socketsOtherParticipants;
    }

    public synchronized void addSocketToSocketsList(Participant participant, Socket socket){
        participant.getSocketsOtherParticipants().add(socket);
    }

    public synchronized void setWinningVote(Participant participant, String s){
        participant.winningVote = s;
    }

    public synchronized String getOutcomeVote(Participant participant){
        return participant.winningVote;
    }

    public synchronized void calculateWinningVote(Participant participant){
        ArrayList<String> votes = new ArrayList<>();

        for(String x : participant.getMainVotes().values()){
            votes.add(x);
        }

        Map<String, Integer> voteCountMap = new HashMap<>();

        for (String s : votes) {
            if (voteCountMap.get(s) != null) {
                Integer count = voteCountMap.get(s) + 1;
                voteCountMap.put(s, count);
            } else {
                voteCountMap.put(s, 1);
            }
        }

        String winningVote = Collections.max(voteCountMap.entrySet(), Map.Entry.comparingByValue()).getKey();
        setWinningVote(participant,winningVote);
    }

    public synchronized String getOutcomePortsString(Participant participant){
        String ports = String.valueOf(participant.getParticipantport());

        for (String s : participant.getOtherParticipants()){
            ports = ports + " " + s;
        }

        return ports;
    }

    public synchronized ArrayList<String> getVotingOptions(){
        return this.votingOptions;
    }

    public synchronized ArrayList<Integer> getAllParticipantInts(){
        ArrayList<Integer> allIds = new ArrayList<>();
        allIds.add(this.getParticipantport());

        for (String x : this.getOtherParticipants()){
            int y = Integer.parseInt(x);
            allIds.add(y);
        }

        return allIds;
    }

    public synchronized HashMap<String, ArrayList<Vote>> getVotesReceived(){
        return this.votesReceived;
    }


}