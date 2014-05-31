import java.io.*;
import java.net.*;
import java.lang.Object.*;

/**
 * The following is implementation for the Server
 * 
 * @since May 11 2014
 * 
 * @author 1000000
 * @version May 17 2014
 *
 */
public class Server extends Thread {
	public static final int DATA_SIZE = 516;

	private DatagramSocket receive;
	private DatagramPacket receivedata;
	private byte data[];
	private boolean verbose;
	private static String fileName;
	private volatile boolean interrupted = false;
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
	public Server(boolean verbose) {
		data = new byte[DATA_SIZE];
		this.verbose = verbose;
		this.fileName = "";

		try { // initialize the socket to a well known port
			receive = new DatagramSocket(69);
		} // end try
		catch (SocketException se) {
			System.err.println("Socket exception error: " + se.getMessage());
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
	 * Latest Change: No more ack or data stuff
	 * @version May 22 2014
	 * @author Kais
	 * 
	 */
	private void verify(DatagramPacket p) {  
		Request r = null;
		if(p.getData()[0] != (byte)0)
			System.exit(1); // TODO properly handle error

		else if(p.getData()[1] == (byte)5) {
			System.exit(1);
			return;
		}
		else if(p.getData()[1] == (byte)1)
			r = Request.READ;
		else if(p.getData()[1] == (byte)2)
			r = Request.WRITE;
		else
			return;
		if(r == null)
			return;

		int innerzero = 0;
		boolean found = false;
		System.out.println(p.getLength());
		for (int i = 2; i < p.getLength() - 1; i++) {
			if (data[i] == (byte) 0) {
				if (!found) {
					innerzero = i;
					found = true;
				} // end if
			}
		}
		byte[] fileNameByteArray = new byte[innerzero-2];
		System.arraycopy(data, 2, fileNameByteArray, 0, innerzero-2);
		fileName = new String(fileNameByteArray);


		Thread newConnectionThread = new ConnectionManager(verbose, p.getData(), p.getPort(), r, p.getLength(), fileName);
		newConnectionThread.start();
	} // end method

	/**
	 * 
	 */
	public void sendReceive() {

		try { // wait to receive the packet from client
			receive.receive(receivedata);
		} // end try 
		catch (IOException ie) {
			if(!interrupted)
				System.err.println("IOException error: " + ie.getMessage());
			else{
				System.out.println("Server shutting down");
				return;
			}
		} // end catch

		//data = receivedata.getData(); // extract message
		System.out.println("Packet received from ErrorSim");
		System.out.println(receivedata.getLength());
		if(verbose)
			printPacketInfo(receivedata);

		verify(receivedata);

	}

	public void interruptThread(){
		interrupted = true;
		receive.close();
	}
	/**
	 * Main method for the Server
	 * @param args not used
	 * 
	 * @since May 11 2014
	 * 
	 * Latest Change: Changed to run all listener code in server
	 * @version May 17 2014
	 * @author Kais
	 * 
	 */
	public void run() {
		System.out.println("Starting the server infinite loop");
		receivedata =  new DatagramPacket(data, data.length);
		while(!interrupted)
			this.sendReceive();

	} // end method

} // end class
