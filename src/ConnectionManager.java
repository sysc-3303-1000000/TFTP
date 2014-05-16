import java.io.*;
import java.net.*;

/**
 * The following is implementation for the ConnectionManager which will be used on the
 * server to write to file if write request, and send an acknowledge to the client.
 * 
 * @since May 13 2014
 * 
 * @author 1000000
 * @version May 13 2014
 *
 */
public class ConnectionManager extends Thread {
	private DatagramSocket send;
	private DatagramPacket sendData;
	private boolean verbose;
	private int port;
	private Request req;
	private DatagramPacket receivedPacket;
	
	/**
	 * The following is the constructor for ConnectionManager
	 * @param verbose whether verbose mode is enabled
	 * @param p DatagramPacket received by Listener
	 * @param req the type of request
	 * 
	 * @since May 13 2014
	 * 
	 * Latest Change: Taking in DatagramPacket instead of port
	 * @version May 15 2014
	 * @author Colin
	 * 
	 */
	public ConnectionManager(boolean verbose, DatagramPacket p, Request r) {
		this.verbose = verbose;
		this.port = p.getPort();
		this.req = r;
		this.receivedPacket = p;
		
		try { // initialize the socket
			send = new DatagramSocket();
		} // end try
		catch (SocketException se) {
			// TODO implement what happens when exception occurs
		} // end catch
	} // end constructor
	
	/**
	 * The following is the run method for ConnectionManager, used to execute code upon starting the thread.
	 * It will service the request appropriately based on its validity
	 * 
	 * @since May 13 2014
	 * 
	 * Latest Change: Add format error checking
	 * @version May 13 2014
	 * @author Colin
	 * 
	 */
	public void run() {
		
		boolean error = false;
		int innerzero = 0;

		boolean found = false;
		for (int i = 2; i < receivedPacket.getLength() - 1; i++) {
			if (receivedPacket.getData()[i] == (byte) 0) {
				if (!found)
					innerzero = i;
				else
					error = true;
			}
		}
		if (receivedPacket.getData()[receivedPacket.getLength()] != (byte) 0)
			error = true;
		
		if(error){
			// TODO send error
		}

		if (innerzero == 2 || innerzero == 0)
			error = true;

		if (req == Request.WRITE) {
			// TODO write to file
			// TODO Form acknowledge
		} // end if
		else if (req == Request.READ) {
			// TODO Form acknowledge
		} // end if
		
		// TODO Form Datagram
		// TODO Send Datagram
		
		send.close();
	} // end method
	
	
} // end class
