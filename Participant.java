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

    public Participant(int cport, int lport, int pport, int timeout) {

        this.coordinatorPort = cport;
        this.listenerPort = lport;
        this.participantport = pport;
        this.timeout = timeout;
        votingOptions = new ArrayList<>();
        boolean hasDetails = false;

        try {
            // Creates a socket with host ip address and coordinator port
            Socket socket = new Socket(InetAddress.getLocalHost(), coordinatorPort);

            // SENDING JOIN REQUEST AND PORT ID TO COORDINATOR
            // Printer writer which prints a message into the socket output stream
            PrintWriter out = new PrintWriter(socket.getOutputStream());
            out.println("JOIN " + this.participantport);
            out.flush();

            // RECEIVING PARTICIPANT DETAILS FROM COORDINATOR
            // Buffered reader which reads a message from the input stream
            otherParticipants = new ArrayList<>();

            // TRY TO HAVE A LISTENING THREAD LIKE WITH COORDINATOR


        } catch (Exception e) {
            System.out.println("error" + e);
        }


    }

    private synchronized void readDetails(){

    }

    private String pickMyVote() {
        Random rand = new Random();
        int x = rand.nextInt(votingOptions.size());
        return votingOptions.get(x);
    }

    static class ParticipantServer implements Runnable {
        Socket partServer;

        ParticipantServer(Socket p) {
            partServer = p;
        }

        public void run() {
            try {
                //TODO check the string to see if it's details or options. If it's details do something, if it's options do something else
                BufferedReader in = new BufferedReader(new InputStreamReader(partServer.getInputStream()));
                String line;
                while ((line = in.readLine()) != null) System.out.println(line + " received");
                // participant.close();
            } catch (Exception e) {
            }
        }
    }
}
