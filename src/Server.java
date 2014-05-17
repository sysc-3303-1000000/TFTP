import java.io.*;
import java.net.*;

/**
 * The following is implementation for the Server
 * 
 * @since May 11 2014
 * 
 * @author 1000000
 * @version May 17 2014
 *
 */
public class Server {
	public static final int DATA_SIZE = 512;
	
	private DatagramSocket receive;
	private DatagramPacket receivedata;
	private byte data[];
	private boolean verbose;
	/**
	 * The following is the constructor for Server
	 * 
	 * @since May 11 2014
	 * 
	 * Latest Change: Removed everything, not needed
	 * @version May 17 2014
	 * @author Kais
	 * 
	 */
	public Server(boolean verbose) {
		data = new byte[DATA_SIZE];
		this.verbose = verbose;
		
		try { // initialize the socket to a well known port
			receive = new DatagramSocket(69);
		} // end try
		catch (SocketException se) {
			System.err.println("Socket exception error: " + se.getMessage());
		} // end catch
		
		receivedata = new DatagramPacket(data, data.length);
	} // end constructor
	
	/**
	 * The following is used to print information about the Packet.
	 * @param p DatagramPacket which will have its information printed
	 * 
	 * @since May 13 2014
	 * 
	 * Latest Change: Added implementation for printing packet info
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
	 * The following is used to verify the Packet.
	 * @param p DatagramPacket which will be verified
	 * 
	 * @since May 13 2014
	 * 
	 * Latest Change: Calls ConnectionManager thread and verifies read or write
	 * @version May 15 2014
	 * @author Colin
	 * 
	 */
	private void verify(DatagramPacket p) {
		Request r;
		if(p.getData()[0] != (byte)0)
			System.exit(1); // TODO properly handle error
		if(p.getData()[1] == (byte)1) {
			r = Request.READ;
			System.out.println("Is Read");
		}
		else if(p.getData()[1] == (byte)2) {
			r = Request.WRITE;
			System.out.println("Is Write");
		}
		else{
			System.exit(1); // TODO properly handle error
			return;
		}
		Thread newConnectionThread = new ConnectionManager(verbose, p, r);
		newConnectionThread.start();
	} // end method
	
	/**
	 * 
	 */
	public void sendReceive() {
		System.out.println("Starting the server infinite loop");
		for(;;) {
			
			try { // wait to receive the packet from client
				receive.receive(receivedata);
			} // end try 
			catch (IOException ie) {
				System.err.println("IOException error: " + ie.getMessage());
			} // end catch
			
			//data = receivedata.getData(); // extract message
			System.out.println("Packet received from ErrorSim");
			if(verbose)
				printPacketInfo(receivedata);

			verify(receivedata);
			
		} // end forloop
	}
	/**
	 * Main method for the Server
	 * @param args not used
	 * 
	 * @since May 11 2014
	 * 
	 * Latest Change: Changed to run all listener code in server
	 * @version May 17 2014
	 * @author Kais
	 * 
	 */
	public static void main(String[] args) {
		Server server = new Server(true);
		server.sendReceive();
	} // end method
	
} // end class
