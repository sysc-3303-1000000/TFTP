import java.io.*;
import java.net.*;

/**
 * The following is implementation for the Listener which will be used on the
 * server to receive packets and to verify them
 * 
 * @since May 13 2014
 * 
 * @author 1000000
 * @version May 13 2014
 *
 */
public class Listener extends Thread {
	public static final int DATA_SIZE = 512;
	
	private DatagramSocket receive;
	private DatagramPacket receivedata;
	private byte data[];
	private boolean verbose;
	
	/**
	 * The following is the constructor for Listener
	 * @param v whether verbose mode is enabled
	 * 
	 * @since May 13 2014
	 * 
	 * Latest Change: Added constructor code to initialize the socket and packet
	 * @version May 13 2014
	 * @author Kais
	 * 
	 */
	public Listener(boolean v) {
		data = new byte[DATA_SIZE];
		verbose = v;
		
		try { // initialize the socket to a well known port
			receive = new DatagramSocket(69);
		} // end try
		catch (SocketException se) {
			// TODO implement what happens when exception occurs
		} // end catch
		
		receivedata = new DatagramPacket(data, data.length);
	} // end constructor
	
	/**
	 * The following is used to print information about the Packet.
	 * @param p DatagramPacket which will have its information printed
	 * 
	 * @since May 13 2014
	 * 
	 * Latest Change: Added function definition, no implementation done
	 * @version May 13 2014
	 * @author Kais
	 * 
	 */
	private void printPacketInfo(DatagramPacket p) {
		// TODO implement how it will print
	} // end method
	
	/**
	 * The following is used to verify the Packet.
	 * @param p DatagramPacket which will be verified
	 * 
	 * @since May 13 2014
	 * 
	 * Latest Change: Added function definition, no implementation done
	 * @version May 13 2014
	 * @author Kais
	 * 
	 */
	private void verify(DatagramPacket p) {
		// TODO implement how verification is done
	} // end method
	
	/**
	 * The following is the run method for Listener, used to execute code upon starting the thread.
	 * It will create a new thread for every packet received and this new thread will send the response.
	 * 
	 * @since May 13 2014
	 * 
	 * Latest Change: First revision, added basic implementation based on assumed functionality
	 * @version May 13 2014
	 * @author Kais
	 * 
	 */
	public void run() {
		int port;
		
		for(;;) {
			
			try { // wait to receive the packet from client
				receive.receive(receivedata);
			} // end try 
			catch (IOException ie) {
				// TODO implement what happens when exception occurs
			} // end catch
			
			data = receivedata.getData(); // extract message
			port = receivedata.getPort();
			
			if(verbose)
				printPacketInfo(receivedata);

			verify(receivedata);
			
			// TODO spawn new thread ConnectionManager which will deal with the rest of the things the server has to do
			
		} // end forloop
	} // end method
	
} // end class
