import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;


public class Coordinator {

    int currentParticipants = 0;
    int expectedParticipants;
    ArrayList<String> votingOptions;
    int listenPort;
    int timeout;
    int coordinatorPort;
    int participantStartPort = 12346;

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
            for (int i = 0; i < expectedParticipants ; i++ ) {
                // TODO put participant parameters here
                Participant participant = new Participant(coordinatorPort,listenPort,participantStartPort,timeout); // cport lport pport timeout
                participantStartPort++;
                try {
                    Socket partSocket = ss.accept();
                    currentParticipants++;
                    new Thread(new Coordinator.ServiceThread(partSocket)).start();
                } catch (Exception e) {
                    System.out.println("error " + e);
                }
            }
        } catch (Exception e) {
            System.out.println("error " + e);
        }

        try {
            Thread.sleep(500);
            System.out.println("All participants have joined. There are " + currentParticipants + " participants");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    static class ServiceThread implements Runnable {
        Socket participant;


        ServiceThread(Socket p) {
            participant = p;
        }

        public void run() {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(participant.getInputStream()));
                String line;
                while ((line = in.readLine()) != null) System.out.println(line + " received");
                // participant.close();
            } catch (Exception e) {
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
}
