import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Array;
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


    public Participant(int cport, int lport, int pport, int timeout) {

        this.coordinatorPort = cport;
        this.listenerPort = lport;
        this.participantport = pport;
        this.timeout = timeout;
        votingOptions = new ArrayList<>();
        hasDetails = false;
        hasVoteOptions = false;
        tempVotes = new HashMap<>();
        mainVotes = new HashMap<>();
        newVotes = new HashMap<>();
        currentRound = 1;
        roundVotesSent = false;

        try {
            // Creates a socket with host ip address and coordinator port
            Socket socket = new Socket(InetAddress.getLocalHost(), coordinatorPort);


            new Thread(new ParticipantVoteInitThread(socket, this)).start();


            otherParticipants = new ArrayList<>();


        } catch (Exception e) {
            System.out.println("error" + e);
        }


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
                BufferedReader in = new BufferedReader(new InputStreamReader(socketPartCord.getInputStream()));
                PrintWriter out = new PrintWriter(socketPartCord.getOutputStream(), true);

                // SENDING JOIN REQUEST AND PORT ID TO COORDINATOR
                // Printer writer which prints a message into the socket output stream
                out.println("JOIN " + participant.participantport);

                Thread.sleep(2000);

                // ACCEPTING OTHER PARTICIPANTS (DETAILS) AND VOTE OPTIONS
                String line;
                while ((line = in.readLine()) != null) {

                    //DETAILS
                    if (line.contains("DETAILS")) {
                        String[] parts = line.split(" ");

                        for (int i = 1; i < parts.length; i++) {
                            participant.addOtherParticipant(parts[i]);
                        }
                        participant.hasDetails = true;
                        System.out.println("received details");
                    }

                    //VOTE_OPTIONS
                    if (line.contains("VOTE_OPTIONS")) {
                        String[] parts = line.split(" ");

                        Thread.sleep(200);

                        for (int i = 1; i < parts.length; i++) {
                            participant.addVotingOption(parts[i]);
                        }

                        participant.hasVoteOptions = true;
                        System.out.println("Received voting options");



                    }
                    if (participant.hasVoteOptions && participant.hasDetails) {
                        break;
                    }


                }
                /*SETS NUMBER OF ROUNDS TO N + 1 ROUNDS AND RANDOMLY GENERATES THIS PARTICIPANT'S VOTE AND
                THEN SETS IT TO THE OBJECT VARIABLE MYVOTE*/
                participant.setTotalRounds(getOtherParticipantSize()+2);
                participant.setVote(pickMyVote());
                Thread.sleep(500);
                System.out.println("Vote is ready to commence");

             /*   CREATES A NEW THREAD THAT CREATES A SERVER SOCKET ON THIS PARTICIPANT'S PORT AND LISTENS
                FOR INCOMING CONNECTION REQUESTS FROM OTHER PARTICIPANT SOCKETS
                WHEN IT GETS A SOCKET CONNECTION IT MAKES A NEW THREAD AND LISTENS FOR INCOMING VOTES ON IT*/
                new Thread (new ListenVoterConnectionThread(participant)).start();

                Thread.sleep(1000);

                int delay = 0; // delay for 5 sec.
                int period = 3000; // repeat every sec.
                Timer timer = new Timer();
                timer.scheduleAtFixedRate(new TimerTask()
                {
                    public void run()
                    {
                        // Your code
                        participant.incrementRound();

                        System.out.println("ROUND IS: " + participant.getCurrentRound());


                    }
                }, delay, period);

                /*CREATES A SOCKET FOR EVERY OTHER PARTICIPANT ON THE NETWORK AND CONNECTS TO THEIR PORT
                 * THEN STARTS A NEW THREAD TO SEND IT'S VOTE TO THE SOCKET CONNECTED*/
                // TODO CHANGE TO FOR EACH TO MAKE IT FAILURE TOLERANT
                for (int i = 0; i < participant.getOtherParticipantSize(); i++){
                    Socket socket = new Socket(InetAddress.getLocalHost(), Integer.parseInt(participant.getOtherParticipantAtIndex(i)));
                    new Thread(new SendVoteThread(socket,participant)).start();
                }

                // 10 sockets connect to server wait 500 for connections then send votes

                // participant.close();
            } catch (Exception e) {
                System.out.println("error" + e);
            }
        }
    }

    class SendVoteThread implements Runnable {
        Socket socket;
        Participant participant;

        SendVoteThread(Socket c, Participant p) {
            socket = c;
            participant = p;
        }

        public void run() {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

                //    while (participant.currentRound <= participant.totalRounds){}


                if (participant.getCurrentRound() == 1) {
                    out.println("VOTE " + participant.getParticipantport() + " " + participant.getMyvote());
                    System.out.println("Sent Vote: " + participant.getParticipantport() + " " + participant.getMyvote());
                    //  participant.currentRound = participant.currentRound + 1;
                    Thread.sleep(1000);



/*                if (participant.currentRound > 1) {
                    participant.newVotes.putAll(tempVotes);
                    Thread.sleep(1000);
                    participant.tempVotes.clear();

                    for (String x : participant.newVotes.keySet()) {
                        out.println("VOTE " + x + " " + participant.newVotes.get(x));
                        System.out.println("NEWER VOTE: " + x + " " + participant.newVotes.get(x));
                    }
                    // System.out.println("Sent NEW Votes");
                    participant.currentRound = participant.currentRound + 1;
                    Thread.sleep(1000);

                }*/

                }

                System.out.println("Main votes for " + participant.getParticipantport() + " are " + participant.getMainVotes());
                System.out.println("Temp votes for " + participant.getParticipantport() + " are " + participant.getTempVotes());

                Thread.sleep(2000);

            } catch (Exception e) {
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
                        String[] parts = line.split(" ");

                        port = parts[1];
                        vote = parts[2];

                        putVoteInTemp(participant,port,vote);
                        putNewVoteInMain(participant.mainVotes, participant.tempVotes);

                        //  participant.deleteRecordedVotes(participant);


                    }

                }
            } catch (Exception e) {
            }
        }
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

    public synchronized void deleteRecordedVotes(Participant participant){
        for (String x : participant.tempVotes.keySet()){
            if (participant.mainVotes.keySet().contains(x)){
                participant.tempVotes.remove(x);
            }
        }
    }

    public synchronized void putVoteInTemp(Participant participant, String port, String vote){
        participant.tempVotes.put(port,vote);
    }

    public synchronized void putNewVoteInMain(HashMap<String,String> mainVotes, HashMap<String,String> tempVotes){
        mainVotes.putAll(tempVotes);
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
}
