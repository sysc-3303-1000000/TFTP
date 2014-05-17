import java.io.IOException;
import java.net.*;

/**
 * The following is implementation for the ConnectionManagerESim which will form
 * a packet with the message it receives from listener and send it to the server.
 * It will receive a packet back and send this to the client.
 * 
 * @since May 16 2014
 * 
 * @author 1000000
 * @version May 16 2014
 *
 */
public class ConnectionManagerESim extends Thread {
	public static final int DATA_SIZE = 516;
	public static final int SERVER_PORT = 69;
	
	private DatagramSocket sendReceiveSocket, sendSocket; // socket deceleration for all three required sockets 
	private DatagramPacket sendClientPacket, receiveServerPacket, sendServerPacket; // packet deceleration for all packets being sent and received for both client and server
	private boolean verbose;
	private byte data[];
	private int port;
	private int length;
	
	/**
	 * The following is the constructor for ListenerESim
	 * @param verbose whether verbose mode is enabled
	 * @param data the message which will be sent to the server
	 * @param port the port which the message from the server will be sent to
	 * @param length the length of the data packet
	 * 
	 * @since May 16 2014
	 * 
	 * Latest Change: Added length
	 * @version May 17 2014
	 * @author Colin
	 * 
	 */
	public ConnectionManagerESim(boolean verbose, byte[] data, int port, int length) {
		this.verbose = verbose;
		this.data = data;
		this.port = port;
		this.length = length;
		
		try {
			sendReceiveSocket = new DatagramSocket();
		} // end try 
		catch (SocketException se) {
			System.err.println("SocketException: " + se.getMessage());
		} // end catch
		
		// initialize the DatagramSocket sendSocket
		try {
			sendSocket = new DatagramSocket();
		} // end try 
		catch (SocketException se) {
			System.err.println("SocketException: " + se.getMessage());
		} // end catch
		
	} // end constructor
	
	/**
	 * The following method will be called when trying to print out information about a specific packet
	 * @param p the information displayed desired for this packet
	 * 
	 * @since May 16 2014
	 * 
	 * Latest Change: Ported from old intermediate, implements printing packet info
	 * @version May 16 2014
	 * @author Kais
	 * 
	 */
	private void printInformation(DatagramPacket p) {
		
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
	 * The following is the run method for ConnectionManagerESim, used to execute code upon starting the thread.
	 * It will pass an receive packets between the errorsim and server and the server and the client.
	 * 
	 * @since May 16 2014
	 * 
	 * Latest Change: Run method added, full implementation added
	 * @version May 16 2014
	 * @author Kais
	 * 
	 */
	public void run() {
		// prepare the new send packet to the server
		try {
			sendServerPacket = new DatagramPacket(data, length, InetAddress.getLocalHost(), SERVER_PORT);
		} // end try 
		catch (UnknownHostException uhe) {
			System.err.println("Unknown host exception error: " + uhe.getMessage());
		} // end catch
		
		if(verbose)
			printInformation(sendServerPacket);
		
		// send the packet to the server via the send/receive socket to port 69
	    try {
	       sendReceiveSocket.send(sendServerPacket);
	    } // end try 
	    catch (IOException ioe) {
	    	System.err.println("Unknown IO exception error: " + ioe.getMessage());
	    } // end catch
	    
	    // print confirmation message that the packet has been sent to the server
		System.out.println("Packet sent to server\n");
		
		byte response[] = new byte[DATA_SIZE];
		
		receiveServerPacket = new DatagramPacket(response, response.length);
		
		System.out.println("ErrorSim is waiting to receive a packet from server...\n");
		
		// block until you receive a packet from the server
		try {
			sendReceiveSocket.receive(receiveServerPacket);
		} // end try 
		catch (IOException ioe) {
			System.err.println("Unknown IO exception error: " + ioe.getMessage());
		} // end catch
		
		response = receiveServerPacket.getData();
		if(verbose) // print out information about the packet received from the server if verbose
			printInformation(receiveServerPacket);
		
		// prepare the new send packet to the client
		try {
			sendClientPacket = new DatagramPacket(response, receiveServerPacket.getLength(), InetAddress.getLocalHost(), port);
		} // end try
		catch (UnknownHostException uhe) {
			uhe.printStackTrace();
	        System.exit(1);
		} // end catch
		System.out.println("ErrorSim will attempt to send response back to client...\n");
		
		if(verbose) // print out information about the packet being sent to the client
			printInformation(sendClientPacket);
		
	    // send the packet to the client via the send socket 
	    try {
	       sendSocket.send(sendClientPacket);
	    } // end try 
	    catch (IOException ioe) {
	    	System.err.println("Unknown IO exception error: " + ioe.getMessage());
	    } // end catch
	    
	    // print confirmation message that the packet has been sent to the client
		System.out.println("Response packet sent to client\n");
		
		sendReceiveSocket.close();
		sendSocket.close();
	} // end method
} // end class
