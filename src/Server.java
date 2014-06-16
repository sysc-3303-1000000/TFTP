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
	private static String fileName;
	private volatile boolean interrupted = false;
	private static String mode;
	
	/**
	 * The following is the constructor for Server
	 * 
	 * @since May 11 2014
	 * 
	 * Latest Change: Removed everything not needed
	 * @version May 17 2014
	 * @author Kais
	 * 
	 */
	public Server() {
		data = new byte[DATA_SIZE];
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
	 * Latest Change: Send address
	 * @version June 15
	 * @author Colin
	 * 
	 */
	private void verify(DatagramPacket p) {  
		Request r = null;
		boolean invalid = false;
		//check data from datagrampacket for validity and request type
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
		//parse the request
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
		
		if(invalid)	//if invalid read or write
		{
			byte emsg[] = ("Server has received an invalid Read or Write request").getBytes();
			try {
				receive.send(new DatagramPacket(createErrorMessage((byte)4, emsg), 5 + emsg.length, p.getAddress(), p.getPort()));	//send error
				System.out.println("Server sent error packet 4 with message: " + new String(emsg));
			} // end try
			catch (IOException e1) {
				System.err.println("IO Exception: " + e1.toString());
			} // end catch
			return;
		}
		
		byte[] fileNameByteArray = new byte[innerzero-2];
		System.arraycopy(data, 2, fileNameByteArray, 0, innerzero-2);	//copy name of file
		fileName = new String(fileNameByteArray);
		byte[] modeArray = new byte[p.getLength() - innerzero - 2];		//copy mode of file
		System.arraycopy(data, innerzero + 1, modeArray, 0, p.getLength() - innerzero - 2);
		mode = new String(modeArray);

		Thread newConnectionThread = new ConnectionManager(p.getPort(), r, fileName, mode, p.getAddress());	//create new connection manager thread
		newConnectionThread.start();
	} // end method

	/**
	 *  This method receives new requests from client
	 *  
	 *  @since May 13 2014
	 *  
	 *  Latest Change: add interrupted catch
	 *  @version June 12 2014
	 *  @author Colin
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
		verify(receivedata);

	}
	
	/**
	 * Following method will create an error message which will be put into a packet and sent to the server
	 * @param type the type of error we have encountered on the client side
	 * @param errorMsg the corresponding error message for the type of error
	 * @return the message which will be put into a packet and sent to the server
	 * 
	 * @since May 30 2014
	 * 
	 * Latest Change: Cleaned function
	 * @version May 31 2014
	 * @author Colin
	 */
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
	 * Latest Change: Added intteruptability
	 * @version May 17 2014
	 * @author Colin
	 * 
	 */
	public void run() {
		receivedata =  new DatagramPacket(data, data.length);
		while(!interrupted)
			this.sendReceive();

	} // end method

} // end class
