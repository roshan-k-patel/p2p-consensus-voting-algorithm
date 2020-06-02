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
    static boolean sentDetails = false;
    ArrayList<String> votingOptions;
    int listenPort;
    int timeout;
    int coordinatorPort;
    int participantStartPort = 12346;
    static ArrayList<Integer> participantPorts  = new ArrayList<>();

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

        // adds the voting options to an arraylist of Strings
        votingOptions = new ArrayList<>();
        for(int i = 4; i< args.length; i++) {
            votingOptions.add(args[i]);
        }


        try {
            //creates a serversocket for the coordinator
            ServerSocket ss = new ServerSocket(coordinatorPort);
            System.out.println("A Coordinator has been established on port: " + coordinatorPort);

            //creates the correct number of participants with the necessary information. using the participant start port and incrementing it each creation
            while (currentParticipants < expectedParticipants){
                // create participant object with parameters here
                Participant participant = new Participant(coordinatorPort,listenPort,participantStartPort,timeout); // cport lport pport timeout

                try {
                    //creates a new socket from the accepted server connection, and then starts a new thread with this socket (participant connects directly with this socket)
                    Socket partSocket = ss.accept();
                    //adds the connected participant port to the arraylist of ports
                    participantStartPort++;
                    currentParticipants++;
                    new Thread(new CoordinatorServer(partSocket, this)).start();
                } catch (Exception e) {
                    System.out.println("error " + e);
                }
            }
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

    public int getParticipantListSize(){
        return participantPorts.size();
    }

    public synchronized String getParticipantListString(){
        String partList = "";
        for (Integer x : participantPorts){
            partList = partList + x + " ";
        }

        return partList;
    }

    public synchronized void addParticipant(Integer port){
        participantPorts.add(port);
    }

    // runs coordinator server connections on their own thread, keep open for any communication rec from corresponding participant
    class CoordinatorServer implements Runnable {
        Socket participant;
        Coordinator coordinator;

        CoordinatorServer(Socket p, Coordinator c) {
            participant = p;
            coordinator = c;
        }

        public void run() {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(participant.getInputStream()));
                PrintWriter out = new PrintWriter(participant.getOutputStream());
                String line;
                String sPort = "";
                int port;
                while ((line = in.readLine()) != null) {
                    // if the message is a join message then splits the string and adds the port to the static arraylist
                    if (line.contains("JOIN")){
                        String[] parts = line.split(" ");
                        sPort = parts[1];
                        port = Integer.parseInt(sPort);
                        coordinator.addParticipant(port);

                    }
                    System.out.println(line + " received");

                    //waits for other threads to get their port
                    Thread.sleep(500);

                    String details = coordinator.getParticipantListString();
                    String detailsSpecific = "";

                    // removes own port from the list of ports and
                    if (details.contains(sPort)){
                        detailsSpecific = details.replace(sPort,"");
                        detailsSpecific = detailsSpecific.trim();
                        detailsSpecific = detailsSpecific.replace("  ", " ");
                        System.out.println(detailsSpecific);
                    }

                    Thread.sleep(500);
                    // TODO SENDS THE PARTICIPANT DETAILS
                    out.println(detailsSpecific);
                    System.out.println("Sent: " + detailsSpecific);
                }

                // participant.close();
            } catch (Exception e) {
                System.out.println("error" + e);
            }
        }
    }

    public static void main(String[] args) {
        try {
            Coordinator coordinator = new Coordinator(args);
            //Waits for all participants to connect
        } catch (InsufficientArgsException e) {
            e.printStackTrace();
        }
    }

    static class InsufficientArgsException extends Exception {
        String[] args;

        InsufficientArgsException(String[] args) {
            this.args = args;
        }
    }

    public void testMethod(){

    }
}
