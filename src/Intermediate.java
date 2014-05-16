import java.io.IOException;
import java.net.*;

/**
 * The following is implementation for the IntermediateHost
 * 
 * @since May 11 2014
 * 
 * @author 1000000
 * @version May 15 2014
 *
 */
public class Intermediate {
	private DatagramSocket receiveSocket, sendReceiveSocket, sendSocket; // socket deceleration for all three required sockets 
	private DatagramPacket receiveClientPacket, sendClientPacket, receiveServerPacket, sendServerPacket; // packet deceleration for all packets being sent and received for both client and server 
	
	/**
	 * The following is the constructor for Intermediate
	 * 
	 * @since May 11 2014
	 * 
	 * Latest Change: Added Code from assignment 1
	 * @version May 15 2014
	 * @author Moh
	 * 
	 */
	public Intermediate()
	{
		// initialize the DatagramSocket receiveSocket to bind to well-known port 68
		try {
			receiveSocket = new DatagramSocket(68);
		} // end try 
		catch (SocketException se) {
			System.err.println("SocketException: " + se.getMessage());
		} // end catch
		
		// initialize the DatagramSocket sendReceiveSocket
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
	 * send and receive procedure for the class
	 *
	 * @since May 11 2014
	 * 
	 * Latest Change: Added Code from assignment 1
	 * @version May 15 2014
	 * @author Moh
	 * 
	 */
	private void sendReceive() {
		System.out.println("Intermediate has started...\n\n");
		
		// repeat the following forever
		while (true) {
			
			// message received from client will be stored in here and the client packet is initialized 
			byte clientMsg[] = new byte[100];
			receiveClientPacket = new DatagramPacket(clientMsg, clientMsg.length);
			
			// block until you receive a packet from the client
			System.out.println("Intermediate is waiting to receive a packet from client...\n");
			try {
				receiveSocket.receive(receiveClientPacket);
			} // end try 
			catch (IOException ioe) {
				System.err.println("Unknown IO exception error: " + ioe.getMessage());
			} // end catch
			
			// print out information about the packet received from the client
			printInformation(receiveClientPacket);
			
			// prepare the new send packet to the server
			try {
				sendServerPacket = new DatagramPacket(clientMsg, receiveClientPacket.getLength(), InetAddress.getLocalHost(), 69);
			} // end try 
			catch (UnknownHostException uhe) {
				System.err.println("Unknown host exception error: " + uhe.getMessage());
			} // end catch
			
			System.out.println("Intermediate will attempt to send packet to server...\n");
			
			// print out information about the packet that we are sending to the server
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
			
			// the server message will be stored here and the server packet is initialized 
			byte serverMsg[] = new byte [4];
			receiveServerPacket = new DatagramPacket(serverMsg, serverMsg.length);
			
			System.out.println("Intermediate is waiting to receive a packet from server...\n");
			
			// block until you receive a packet from the server
			try {
				sendReceiveSocket.receive(receiveServerPacket);
			} // end try 
			catch (IOException ioe) {
				System.err.println("Unknown IO exception error: " + ioe.getMessage());
			} // end catch
			
		
			// print out information about the packet received from the server
			printInformation(receiveServerPacket);
			
			// prepare the new send packet to the client
			sendClientPacket = new DatagramPacket(serverMsg, receiveServerPacket.getLength(), receiveClientPacket.getAddress(), receiveClientPacket.getPort());
			
			System.out.println("Intermediate will attempt to send response back to client...\n");
			
			// print out information about the packet being sent to the client
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
						
		} // end whileloop
		
	} // end method
	
	/**
	 * the following method will be called when trying to print out information about a specific packet
	 * @param p the information displayed desired for this packet
	 * 
	 * @since May 11 2014
	 * 
	 * Latest Change: Added Code from assignment 1
	 * @version May 15 2014
	 * @author Moh
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
	 * Main method for the IntermediateHost
	 * @param args not used
	 * 
	 * @since May 11 2014
	 * 
	 * Latest Change: Added Code from assignment 1
	 * @version May 15 2014
	 * @author Moh
	 * 
	 */
	public static void main(String[] args) {
		Intermediate intermediate = new Intermediate();
		intermediate.sendReceive();
	} // end method
} // end class
