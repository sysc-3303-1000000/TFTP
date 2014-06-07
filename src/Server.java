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
	private static String mode;
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
		boolean invalid = false;
		if(p.getData()[0] != (byte)0)
			invalid = true;
		else if(p.getData()[1] == (byte)5) {
			invalid = true;
		}
		else if(p.getData()[1] == (byte)1)
			r = Request.READ;
		else if(p.getData()[1] == (byte)2)
			r = Request.WRITE;
		else
			invalid = true;

		
		
		int innerzero = 0;
		boolean found = false;
		System.out.println(p.getLength());
		for (int i = 2; i < p.getLength() - 1; i++) {
			if (data[i] == (byte) 0) {
				if (!found) {
					innerzero = i;
					found = true;
				} // end if
				else{
					invalid = true;
				}
			}
		}
		
		if(invalid)
		{
			byte emsg[] = ("Server has received an invalid Read or Write request").getBytes();
			try {
				receive.send(new DatagramPacket(createErrorMessage((byte)4, emsg), 5 + emsg.length, InetAddress.getLocalHost(), p.getPort()));
				System.out.println("Server sent error packet 4");
			} // end try
			catch (UnknownHostException e1) {
				System.err.println("Unknown Host: " + e1.toString());
			} // end catch
			catch (IOException e1) {
				System.err.println("IO Exception: " + e1.toString());
			} // end catch
			return;
		}
		
		byte[] fileNameByteArray = new byte[innerzero-2];
		System.arraycopy(data, 2, fileNameByteArray, 0, innerzero-2);
		fileName = new String(fileNameByteArray);
		byte[] modeArray = new byte[p.getLength() - innerzero - 2];
		System.arraycopy(data, innerzero + 1, modeArray, 0, p.getLength() - innerzero - 2);
		mode = new String(modeArray);

		Thread newConnectionThread = new ConnectionManager(verbose, p.getData(), p.getPort(), r, p.getLength(), fileName, mode);
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
		if(verbose)
			printPacketInfo(receivedata);

		verify(receivedata);

	}
	
	private byte[] createErrorMessage(byte type, byte errorMessage[]){
		byte msg[] = new byte[errorMessage.length + 5];
		
		msg[0] = (byte)0;
		msg[1] = (byte)5;
		msg[2] = (byte)0;
		msg[3] = type;
		
		System.arraycopy(errorMessage, 0, msg, 4, errorMessage.length);
		
		msg[msg.length - 1] = (byte)0;
				
		return msg;
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
