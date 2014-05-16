import java.io.*;
import java.net.*;

/**
 * The following is implementation for the Server
 * 
 * @since May 11 2014
 * 
 * @author 1000000
 * @version May 15 2014
 *
 */
public class Server {
	private DatagramSocket receiveSocket; // Socket used to receive
	private DatagramSocket sendSocket; // Socket used to send
	private DatagramPacket receivePacket; // Packet used to receive
	private DatagramPacket sendPacket; // Packet used to send
	private byte readAck[];
	private byte writeAck[];
	private byte invAck[];
	
	/**
	 * The following is the constructor for Server
	 * 
	 * @since May 11 2014
	 * 
	 * Latest Change: Added Code from assignment 1
	 * @version May 15 2014
	 * @author Moh
	 * 
	 */
	public Server() {
		// initialize the socket to receive on port 69
		try {
			receiveSocket = new DatagramSocket(69);
		} // end try
		catch (SocketException se){
			System.err.println("Socket exception error: " + se.getMessage());
		} // end catch
	} // end constructor
	
	/**
	 * send and receive procedure for the packet
	 * this procedure will also be able to read and write on its arguments
	 *
	 * @since May 11 2014
	 * 
	 * Latest Change: Added Code from assignment 1
	 * @version May 15 2014
	 * @author Moh
	 * 
	 */
	private void sendReceive() {
		System.out.println("Server has started...");
		byte data[] = new byte[100];
		
		// initialize the receive packet
		receivePacket = new DatagramPacket(data, data.length);
		while (true) {
			// block until you receive a packet to well-known Socket port 69
			try {
				receiveSocket.receive(receivePacket);
			} // end try 
			catch (IOException ioe) {
				System.err.println("Unknown IO exception error: " + ioe.getMessage());
			} // end catch
			
			System.out.println("Server is receiving packet from intermediate...");
			printInformation(receivePacket);
					
			// depending on the outcome, generate the response and set the sendPacket to equal that
			if (parseRqst(receivePacket) == 0) { // read response
				// create the read acknowledge [0301]
				readAck = new byte[4];
				readAck[0] = (byte)0;
				readAck[1] = (byte)3;
				readAck[2] = (byte)0;
				readAck[3] = (byte)1;
				
				sendPacket = new DatagramPacket(readAck, 4, receivePacket.getAddress(), receivePacket.getPort());
			} // end if

			else if (parseRqst(receivePacket) == 1) { // write response
				// create the write acknowledge [0301]
				writeAck = new byte[4];
				writeAck[0] = (byte)0;
				writeAck[1] = (byte)4;
				writeAck[2] = (byte)0;
				writeAck[3] = (byte)0;
				
				sendPacket = new DatagramPacket(writeAck, 4, receivePacket.getAddress(), receivePacket.getPort());
			} // end if
			else { // invalid response
				// create the invalid acknowledge [05]
				invAck = new byte[2];
				invAck[0] = (byte)0;
				invAck[1] = (byte)5;
							
				sendPacket = new DatagramPacket(invAck, 2, receivePacket.getAddress(), receivePacket.getPort());
			} // end else
			
			// print out information about the response packet
			System.out.println("Server is sending packet to intermediate...");
			
			// initialize the send socket
			try {
				sendSocket = new DatagramSocket();
			} // end try 
			catch (SocketException se){
				System.err.println("Socket exception error: " + se.getMessage());
			} // end catch
			
		    // send the packet to the client via the send socket 
		    try {
		       sendSocket.send(sendPacket);
		    } // end try 
		    catch (IOException ioe) {
		    	System.err.println("Unknown IO exception error: " + ioe.getMessage());
		    } // end catch
		    printInformation(sendPacket);
			System.out.println("Server packet sent to intermediate.. closing the sending socket");
			
			// close the socket
			sendSocket.close();
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
	 * The following method is used to parse the data received, and validate it
	 * @param p packet that we are trying to parse
	 * @return an integer value of 0 if it is a read request, 1 if it is a write request, or 404 for invalid
	 * 
	 * @since May 11 2014
	 * 
	 * Latest Change: Added Code from assignment 1
	 * @version May 15 2014
	 * @author Moh
	 * 
	 */
	private int parseRqst(DatagramPacket p){
		// used for checking the form of the packet
		boolean isText = true;
		int i = 2;
		
		// check first index of the byte array
		// if 0, then check if its a read or a write
		if (p.getData()[0] == (byte)0) {
			// read request bit is read
			if (p.getData()[1] == (byte)1) {
				// check if last byte is a 0
				if (p.getData()[p.getLength()-1] != 0) {
					// if not, return invalid request
					return 404;
				} // end if
				// now we check for a 0 in the middle
				else
				// if it is a null character, that means it has our 0 in the middle
				while (isText) {
					if (p.getData()[i] == 0){
						// if there is a 0 right after our request bit, it is in valid
						if (i == 2) {
							return 404;
						} // end if
						// otherwise, we have a file name and return a valid read
						else {
							return 0;
						} // end else
					} // end if
					// otherwise, this is part of the file name
					else {
						i++;
					} // end else
				} // end whileloop
			} // end if
			// write request bit is read
			else if (p.getData()[1] == (byte)2) {
				// check if last byte is a 0
				if (p.getData()[p.getLength()-1] != 0) {
					// if not, return invalid request
					return 404;
				} // end if
				// now we check for a 0 in the middle
				else
				// if it is a null character, that means it has our 0 in the middle
				while (isText) {
					if (p.getData()[i] == 0){
						// if there is a 0 right after our request bit, it is in valid
						if (i == 2) {
							return 404;
						} // end if
						// otherwise, we have a file name and return a valid write
						else {
							return 1;
						} // end else
					} // end if
					// otherwise, this is part of the file name
					else {
						i++;
					} // end else
				} // end whileloop
			} // end if
		} // end if
		return 404;
	} // end method
	
	/**
	 * Main method for the Server
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
		Server server = new Server();
		server.sendReceive();
	} // end method
	
} // end class
