import java.io.*;
import java.net.*;

/**
 * The following is implementation for the Listener which will be used on the
 * server to receive packets and to verify them
 * 
 * @since May 13 2014
 * 
 * @author 1000000
 * @version May 16 2014
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
	 * @param verbose whether verbose mode is enabled
	 * 
	 * @since May 13 2014
	 * 
	 * Latest Change: Added constructor to class
	 * @version May 16 2014
	 * @author Kais
	 * 
	 */
	public Listener(boolean verbose) {
		data = new byte[DATA_SIZE];
		this.verbose = verbose;
		
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
		if(p.getData()[1] == (byte)1)
			r = Request.READ;
		else if(p.getData()[1] == (byte)2)
			r = Request.WRITE;
		else{
			System.exit(1); // TODO properly handle error
			return;
		}
		Thread newConnectionThread = new ConnectionManager(verbose, p, r);
		newConnectionThread.start();
	} // end method
	
	/**
	 * The following is the run method for Listener, used to execute code upon starting the thread.
	 * It will create a new thread for every packet received and this new thread will send the response.
	 * 
	 * @since May 13 2014
	 * 
	 * Latest Change: Removed the getData() line, we don't actually have a reason to extract the message
	 * @version May 16 2014
	 * @author Kais
	 * 
	 */
	public void run() {
		
		for(;;) {
			
			try { // wait to receive the packet from client
				receive.receive(receivedata);
			} // end try 
			catch (IOException ie) {
				// TODO implement what happens when exception occurs
			} // end catch
			
			//data = receivedata.getData(); // extract message
			
			if(verbose)
				printPacketInfo(receivedata);

			verify(receivedata);
			
		} // end forloop
	} // end method
	
} // end class
