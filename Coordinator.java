import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;


public class Coordinator {

    static int currentParticipants = 0;
    static int expectedParticipants;
    ArrayList<String> votingOptions;
    int listenPort;
    int timeout;
    int coordinatorPort;
    static ArrayList<Integer> participantPorts = new ArrayList<>();
    static boolean allConnected = false;
    static int joinReceived;

    private Coordinator(String[] args) throws InsufficientArgsException {

        // java Coordinator <port> <lport> <parts> <timeout> [option]
        //java Participant <cport> <lporti> <pport> <timeout>


        if (args.length < 5) {
            System.out.println("Minimum 5 arguments <port> <lport> <parts> <timeout> [option] ");
            throw new InsufficientArgsException(args);
        }
        coordinatorPort = Integer.parseInt(args[0]);
        listenPort = Integer.parseInt(args[1]);
        expectedParticipants = Integer.parseInt(args[2]);
        timeout = Integer.parseInt(args[3]);

        joinReceived = 0;



        // adds the voting options to an arraylist of Strings
        votingOptions = new ArrayList<>();
        for (int i = 4; i < args.length; i++) {
            votingOptions.add(args[i]);
        }


        try {
            //creates a serversocket for the coordinator
            ServerSocket ss = new ServerSocket(coordinatorPort,0, InetAddress.getLocalHost());
            System.out.println("A Coordinator has been established on port: " + coordinatorPort);

            //creates the correct number of participants with the necessary information. using the participant start port and incrementing it each creation
            while (currentParticipants < expectedParticipants) {
                // create participant object with parameters here
              //  Participant participant = new Participant(coordinatorPort, listenPort, participantStartPort, timeout); // cport lport pport timeout

                try {
                    //creates a new socket from the accepted server connection, and then starts a new thread with this socket (participant connects directly with this socket)
                    Socket partSocket = ss.accept();

                    //adds the connected participant port to the arraylist of ports
                    currentParticipants++;
                    new Thread(new CoordinatorServerThread(partSocket, this)).start();
                } catch (Exception e) {
                    System.out.println("error " + e);
                }
            }
            //sets all connected to true once current participants = expected participants
            allConnected = true;
        } catch (Exception e) {
            System.out.println("error " + e);
        }

        // sleeps for 500 ms to let all threads read their streams
        try {
            Thread.sleep(500);
            System.out.println();
            System.out.println("All participants have joined. There are " + currentParticipants + " participants");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


    }

    // runs coordinator server connections on their own thread, keep open for any communication rec from corresponding participant
    class CoordinatorServerThread implements Runnable {
        // SOCKET FROM THE COORDS REFERENCE, IN = COORD SIDE, OUT = PARTICIPANT SIDE
        Socket socketCoordPart;
        Coordinator coordinator;

        CoordinatorServerThread(Socket s, Coordinator c) {
            socketCoordPart = s;
            coordinator = c;
        }
        String line;
        String sPort = "";
        int port = 0;
        boolean detailsOptionsSent = false;
        public void run() {
            while(true) {
                try {
                    BufferedReader in = new BufferedReader(new InputStreamReader(socketCoordPart.getInputStream()));
                    PrintWriter out = new PrintWriter(socketCoordPart.getOutputStream(), true);
                    CoordinatorLogger.getLogger().startedListening(socketCoordPart.getLocalPort());
                    while ((line = in.readLine()) != null) {
                        // if the message is a join message then splits the string and adds the port to the static arraylist
                        if (line.contains("JOIN")) {
                            String[] parts = line.split(" ");
                            sPort = parts[1];
                            port = Integer.parseInt(sPort);
                            CoordinatorLogger.getLogger().connectionAccepted(port);
                            Thread.sleep(50);
                            CoordinatorLogger.getLogger().messageReceived(port, line);
                            CoordinatorLogger.getLogger().joinReceived(port);
                            coordinator.addParticipant(port);

                            //waits for other threads to get their port
                            Thread.sleep(250);
                            coordinator.incrementJoinReceived();
                        }

                        if (coordinator.isAllConnected() && !detailsOptionsSent) {
                            Thread.sleep(5000);
                            String details = coordinator.getParticipantListString();
                            String detailsSpecific = "";

                            if (details.contains(sPort)) {
                                detailsSpecific = details.replace(sPort, "");
                                detailsSpecific = detailsSpecific.trim();
                                detailsSpecific = detailsSpecific.replace("  ", " ");
                                ArrayList<Integer> otherParts = new ArrayList<Integer>();
                                String[] otherPartsParts = detailsSpecific.split(" ");
                                for (int i = 0; i < otherPartsParts.length; i++) {
                                    otherParts.add(Integer.valueOf(otherPartsParts[i]));
                                }

                                detailsSpecific = "DETAILS " + detailsSpecific;
                                Thread.sleep(100);
                                // SENDS THE PARTICIPANT DETAILS
                                out.println(detailsSpecific);
                                CoordinatorLogger.getLogger().messageSent(port, detailsSpecific);
                                CoordinatorLogger.getLogger().detailsSent(port, otherParts);
                            }

                            Thread.sleep(100);

                            // SENDS THE VOTING OPTIONS
                            String votingOptions = "";
                            for (String x : coordinator.votingOptions) {
                                votingOptions = votingOptions + x + " ";
                            }

                            votingOptions = "VOTE_OPTIONS " + votingOptions;
                            out.println(votingOptions);
                            CoordinatorLogger.getLogger().messageSent(port, votingOptions);
                            CoordinatorLogger.getLogger().voteOptionsSent(port, coordinator.getVotingOptions());
                            detailsOptionsSent = true;

                        }

                        // removes own port from the list of ports and sends details of other participants


                        if (line.contains("OUTCOME")) {
                            System.out.println(line);
                            String[] parts = line.split(" ");
                            String vote = parts[1];
                            CoordinatorLogger.getLogger().messageReceived(port, line);
                            CoordinatorLogger.getLogger().outcomeReceived(port, vote);
                        }


                    }

                    // participant.close();
                } catch (Exception e) {
                    CoordinatorLogger.getLogger().participantCrashed(port);
                    System.out.println("coord detected crash/ end of vote, port: " + port);
                    break;
                }

            }



        }
    }

    public static void main(String[] args) {
        try {
            CoordinatorLogger.initLogger(7777, Integer.parseInt(args[0]),60000);
            Coordinator coordinator = new Coordinator(args);
            //Waits for all participants to connect
        } catch (InsufficientArgsException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class InsufficientArgsException extends Exception {
        String[] args;

        InsufficientArgsException(String[] args) {
            this.args = args;
        }
    }

    public int getParticipantListSize() {
        return participantPorts.size();
    }

    public synchronized String getParticipantListString() {
        String partList = "";
        for (Integer x : participantPorts) {
            partList = partList + x + " ";
        }

        return partList;
    }

    public synchronized void addParticipant(Integer port) {
        participantPorts.add(port);
    }

    public synchronized ArrayList<String> getVotingOptions(){
        return this.votingOptions;
    }

    public synchronized boolean isAllConnected(){
        return allConnected;
    }

    public synchronized void incrementJoinReceived(){
        joinReceived = joinReceived + 1;
    }

    public synchronized int getJoinReceived() {
        return joinReceived;
    }

}
