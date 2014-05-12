/**
 * SYSC 3303 - ASSIGNMENT 1
 * Intermediate.java
 * 
 * @author Mohammed Ahmed-Muhsin
 * @version 1.0 
 * This class is the intermediate object between the client and the server
 * Host will create a DatagramSocket to use to receive (port 68)
 * The host creates a DatagramSocket to use to send and receive
 */

import java.net.*;

public class Intermediate {
	
	// socket deceleration 
	DatagramSocket receiveSocket;
	DatagramSocket sendReceiveSocket;
	
	public Intermediate()
	{
		// create the DatagramSocket receiveSocket to bind to well-known port 68
		try {
			receiveSocket = new DatagramSocket(68);
		} catch (SocketException se) {
			System.err.println("SocketException: " + se.getMessage());
		}
	}
}
