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
	public Server() {

	} // end constructor
	
	/**
	 * Main method for the Server
	 * @param args not used
	 * 
	 * @since May 11 2014
	 * 
	 * Latest Change: Changed the process to spawn a listener thread to wait for a packet
	 * @version May 17 2014
	 * @author Kais
	 * 
	 */
	public static void main(String[] args) {
		Thread listener = new Listener(true); // enable verbose for now
		listener.start();
	} // end method
	
} // end class
