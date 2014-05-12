/**
 * SYSC 3303 - ASSIGNMENT 1
 * Intermediate.java
 * 
 * @author Mohammed Ahmed-Muhsin
 * @version 2.0 
 * This class is the intermediate object between the client and the server
 * Intermediate will create a DatagramSocket to use to receive (port 68)
 * Intermediate will create a DatagramSocket to use to send and receive
 * All packets coming into the Intermediate and out of it will have its information printed
 * The intermediate runs forever.
 */

import java.io.IOException;
import java.net.*;

public class Intermediate {
	
	// socket deceleration for all three required sockets 
	DatagramSocket receiveSocket;
	DatagramSocket sendReceiveSocket;
	DatagramSocket sendSocket;
	
	// packet deceleration for all packets being sent and received
	// for both client and server 
	DatagramPacket receiveClientPacket, sendClientPacket;
	DatagramPacket receiveServerPacket, sendServerPacket;
	
	// default constructor for Intermediate class
	public Intermediate()
	{
		// initialize the DatagramSocket receiveSocket to bind to well-known port 68
		try {
			receiveSocket = new DatagramSocket(68);
		} catch (SocketException se) {
			System.err.println("SocketException: " + se.getMessage());
		}
		
		// initialize the DatagramSocket sendReceiveSocket
		try {
			sendReceiveSocket = new DatagramSocket();
		} catch (SocketException se) {
			System.err.println("SocketException: " + se.getMessage());
		}
		
		// initialize the DatagramSocket sendSocket
		try {
			sendSocket = new DatagramSocket();
		} catch (SocketException se) {
			System.err.println("SocketException: " + se.getMessage());
		}
				
	}
	
	// sending and receiving method for the class
	// this will be running almost always other than the initializing that happens before
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
			} catch (IOException ioe) {
				System.err.println("Unknown IO exception error: " + ioe.getMessage());
			}
			
			// print out information about the packet received from the client
			printInformation(receiveClientPacket);
			
			// prepare the new send packet to the server
			try {
				sendServerPacket = new DatagramPacket(clientMsg, receiveClientPacket.getLength(), InetAddress.getLocalHost(), 69);
			} catch (UnknownHostException uhe) {
				System.err.println("Unknown host exception error: " + uhe.getMessage());
			}
			
			System.out.println("Intermediate will attempt to send packet to server...\n");
			
			// print out information about the packet that we are sending to the server
			printInformation(sendServerPacket);
			
			// send the packet to the server via the send/receive socket to port 69
		    try {
		       sendReceiveSocket.send(sendServerPacket);
		    } catch (IOException ioe) {
		    	System.err.println("Unknown IO exception error: " + ioe.getMessage());
		    }
		    
		    // print confirmation message that the packet has been sent to the server
			System.out.println("Packet sent to server\n");
			
			// the server message will be stored here and the server packet is initialized 
			byte serverMsg[] = new byte [4];
			receiveServerPacket = new DatagramPacket(serverMsg, serverMsg.length);
			
			System.out.println("Intermediate is waiting to receive a packet from server...\n");
			
			// block until you receive a packet from the server
			try {
				sendReceiveSocket.receive(receiveServerPacket);
			} catch (IOException ioe) {
				System.err.println("Unknown IO exception error: " + ioe.getMessage());
			}
			
		
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
		    } catch (IOException ioe) {
		    	System.err.println("Unknown IO exception error: " + ioe.getMessage());
		    }
		    
		    // print confirmation message that the packet has been sent to the client
			System.out.println("Response packet sent to client\n");
						
		}
		
	}
	
	/**
	 * the following method will be called when trying to print out information about a specific packet
	 * @param p the information displayed desired for this packet
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
		}
		System.out.println("\n\n");
	}
	
	// the main program
	public static void main(String[] args) {
		Intermediate intermediate = new Intermediate();
		intermediate.sendReceive();
	}
}
