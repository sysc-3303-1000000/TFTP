import java.io.*;
import java.net.*;

/**
 * The following is implementation for the Listener which will be used on the
 * ErrorSim to receive and send packets between the client and the server.
 * 
 * @since May 16 2014
 * 
 * @author 1000000
 * @version May 16 2014
 *
 */
public class ListenerESim extends Thread {
	public static final int DATA_SIZE = 512;
	
	private DatagramSocket receiveSocket;
	private DatagramPacket receiveClientPacket;
	private byte data[];
	private boolean verbose;
	
	/**
	 * The following is the constructor for ListenerESim
	 * @param verbose whether verbose mode is enabled
	 * 
	 * @since May 16 2014
	 * 
	 * Latest Change: Added constructor code to initialize the socket and packet
	 * @version May 16 2014
	 * @author Kais
	 * 
	 */
	public ListenerESim(boolean verbose) {
		data = new byte[DATA_SIZE];
		this.verbose = verbose;
		// initialize the DatagramSocket receiveSocket to bind to well-known port 68
		try {
			receiveSocket = new DatagramSocket(68);
		} // end try 
		catch (SocketException se) {
			System.err.println("SocketException: " + se.getMessage());
		} // end catch
	} // end constructor
	
	/**
	 * The following is used to print information about the Packet.
	 * @param p DatagramPacket which will have its information printed
	 * 
	 * @since May 16 2014
	 * 
	 * Latest Change: Added function definition, no implementation done
	 * @version May 16 2014
	 * @author Kais
	 * 
	 */
	private void printPacketInfo(DatagramPacket p) {
		
		// print out the information on the packet
				System.out.println("Host: " + p.getAddress());
				System.out.println("Host port: " + p.getPort());
				System.out.println("Containing the following \nString: " + new String(p.getData()));
				System.out.println("Length of packet: " + p.getLength());
				System.out.println("Bytes: ");
				for (int i = 0; i < p.getLength(); i++) {
					System.out.print(Integer.toHexString(p.getData()[i]));
				} // end forloop
				System.out.println("\n\n");
	} // end method
	
	/**
	 * The following is the run method for ListenerESim, used to execute code upon starting the thread.
	 * It will create a new thread for every packet received and this new thread will send the response
	 * to the server.
	 * 
	 * @since May 16 2014
	 * 
	 * Latest Change: First revision, added basic implementation based on assumed functionality
	 * @version May 16 2014
	 * @author Kais
	 * 
	 */
	public void run() {
		int port;
		
		for(;;) {
			
			try { // wait to receive the packet from client
				receiveSocket.receive(receiveClientPacket);
			} // end try 
			catch (IOException ie) {
				// TODO implement what happens when exception occurs
			} // end catch
			
			data = receiveClientPacket.getData(); // extract message
			port = receiveClientPacket.getPort();
			
			if(verbose)
				printPacketInfo(receiveClientPacket);
			
			Thread connectionmanager = new ConnectionManagerESim(verbose, data, port);
			connectionmanager.start();
		} // end forloop
	} // end method
	
} // end class
