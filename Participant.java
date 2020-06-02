import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Random;

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

    public Participant(int cport, int lport, int pport, int timeout) {

        this.coordinatorPort = cport;
        this.listenerPort = lport;
        this.participantport = pport;
        this.timeout = timeout;
        votingOptions = new ArrayList<>();
        hasDetails = false;
        hasVoteOptions = false;

        try {
            // Creates a socket with host ip address and coordinator port
            Socket socket = new Socket(InetAddress.getLocalHost(), coordinatorPort);


            new Thread(new Participant.ParticipantListenerThread(socket, this)).start();


            otherParticipants = new ArrayList<>();


        } catch (Exception e) {
            System.out.println("error" + e);
        }


    }

    public synchronized void addOtherParticipant(String participant) {
        this.otherParticipants.add(participant);
    }

    public synchronized void addVotingOption(String option) {
        this.votingOptions.add(option);
    }


    static class ParticipantListenerThread implements Runnable {
        Socket partServer;
        Participant participant;

        ParticipantListenerThread(Socket s, Participant p) {
            partServer = s;
            participant = p;
        }

        public void run() {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(partServer.getInputStream()));
                PrintWriter out = new PrintWriter(partServer.getOutputStream(), true);

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

                        for (int i = 1; i < parts.length; i++) {
                            participant.addVotingOption(parts[i]);
                        }

                        participant.hasVoteOptions = true;
                        System.out.println("Received voting options");

                        if (participant.hasVoteOptions && participant.hasDetails) {
                            break;
                        }

                    }
                }
                Thread.sleep(500);
                System.out.println("Vote is ready to commence");
                // participant.close();
            } catch (Exception e) {
            }
        }
    }

    private String pickMyVote() {
        Random rand = new Random();
        int x = rand.nextInt(votingOptions.size());
        return votingOptions.get(x);
    }
}
