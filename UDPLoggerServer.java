import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class UDPLoggerServer {

    DatagramSocket socket;

    public static void main(String [] args){
        UDPLoggerServer loggerServer = new UDPLoggerServer(Integer.parseInt(args[0]));
    }

    public UDPLoggerServer(int listenPort){
        try{
            socket = new DatagramSocket(listenPort);

            System.out.println("listening on port " + listenPort);


            while (true){
                byte[] buf = new byte[256];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
              //  System.out.println(("" + socket.getLocalPort() + " " + System.currentTimeMillis() + " " + (new String(packet.getData())).trim()));
                String text = (new String(packet.getData())).trim();
                saveToFile("logfile.txt",text,true);
                confirmation(packet);
                Thread.sleep(1);
            }

        } catch(Exception e){

        }

    }

    public void confirmation(DatagramPacket packet){
        String ack = "ACK";
        byte[] buf = ack.getBytes();
        DatagramPacket responsePacket = new DatagramPacket(buf,buf.length, packet.getAddress(), packet.getPort());
        try {
            socket.send(responsePacket);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static synchronized void saveToFile(String file, String text, Boolean reset){

        try {
            File f = new File(file);
            FileWriter fw = new FileWriter(f,reset);
            PrintWriter pw = new PrintWriter(fw);

            pw.println(text);
            pw.close();
        } catch (IOException e){

        }
    }
}
