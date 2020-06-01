import java.io.PrintWriter;
import java.net.InetAddress;
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

    public Participant(int cport, int lport, int pport, int timeout) {

        this.coordinatorPort = cport;
        this.listenerPort = lport;
        this.participantport = pport;
        this.timeout = timeout;
        votingOptions = new ArrayList<>();

        try {
            // Creates a socket with host ip address and port
            Socket socket = new Socket(InetAddress.getLocalHost(), coordinatorPort);

/*            Printer writer which prints a message into the socket output stream and then flushes the stream
            and a sleeps for 1 second*/

            PrintWriter out = new PrintWriter(socket.getOutputStream());
            out.println("JOIN " + this.participantport);
            out.flush();
           /* for (int i = 0; i < 10; i++) {
                out.println("TCP message " + i);
                out.flush();
                System.out.println("TCP message " + i + " sent");
                Thread.sleep(1000);
            }*/
        } catch (Exception e) {
            System.out.println("error" + e);
        }
    }

    private String pickMyVote(){
        Random rand = new Random();
        int x = rand.nextInt(votingOptions.size());
        return votingOptions.get(x);
    }
}
