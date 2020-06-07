import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDPLoggerClient {
	
	private final int loggerServerPort;
	private final int processId;
	private final int timeout;

	/**
	 * @param loggerServerPort the UDP port where the Logger process is listening o
	 * @param processId the ID of the Participant/Coordinator, i.e. the TCP port where the Participant/Coordinator is listening on
	 * @param timeout the timeout in milliseconds for this process 
	 */
	public UDPLoggerClient(int loggerServerPort, int processId, int timeout) {
		this.loggerServerPort = loggerServerPort;
		this.processId = processId;
		this.timeout = timeout;
	}
	
	public int getLoggerServerPort() {
		return loggerServerPort;
	}

	public int getProcessId() {
		return processId;
	}
	
	public int getTimeout() {
		return timeout;
	}

	/**
	 * Sends a log message to the Logger process
	 * 
	 * @param message the log message
	 * @throws IOException
	 */
	public synchronized void logToServer(String message) throws IOException {
		DatagramSocket socket;
		int attempts = 0;
		message = processId + " " + System.currentTimeMillis() + " " + message;

		while (attempts < 3) {

			try {
				//This address is your ipv4 adress
				InetAddress address = InetAddress.getLocalHost();
				socket = new DatagramSocket();

				byte[] buf = message.getBytes();
				DatagramPacket packet = new DatagramPacket(buf, buf.length, address, loggerServerPort);
				socket.send(packet);

				byte[] buf1 = new byte[256];
				DatagramPacket packet1 = new DatagramPacket(buf1, buf1.length);
				Thread.sleep(2);
				try {
					socket.receive(packet1);
					break;

				} catch (IOException e) {
					attempts = attempts + 1;
				}

			} catch (IOException e) {
				attempts = attempts + 1;
				try {
					Thread.sleep(1);
				} catch (InterruptedException ex) {
					ex.printStackTrace();
				}


			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}
	}


}
