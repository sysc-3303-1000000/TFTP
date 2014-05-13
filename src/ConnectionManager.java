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
	private DatagramPacket senddata;
	private boolean verbose;
	private int port;
	private Request req;
	
	/**
	 * The following is the constructor for ConnectionManager
	 * @param verbose whether verbose mode is enabled
	 * @param port the port which the acknowledge will be sent to
	 * @param req the type of request
	 * 
	 * @since May 13 2014
	 * 
	 * Latest Change: Added constructor code to initialize the socket
	 * @version May 13 2014
	 * @author Kais
	 * 
	 */
	public ConnectionManager(boolean verbose, int port, Request req) {
		this.verbose = verbose;
		this.port = port;
		this.req = req;
		
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
	 * Latest Change: First revision, added basic implementation. Basic skeleton.
	 * @version May 13 2014
	 * @author Kais
	 * 
	 */
	public void run() {
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
