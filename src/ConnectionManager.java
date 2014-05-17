import java.io.*;
import java.net.*;

/**
 * The following is implementation for the ConnectionManager which will be used on the
 * server to write to file if write request, and send an acknowledge to the client.
 * 
 * @since May 13 2014
 * 
 * @author 1000000
 * @version May 17 2014
 *
 */
public class ConnectionManager extends Thread {
	public static final int DATA_SIZE = 512;
	
	private DatagramSocket send;
	private DatagramPacket sendData;
	private boolean verbose;
	private int port;
	private Request req;
	private DatagramPacket receivedPacket;
	private byte data[];
	private int length;
	/**
	 * The following is the constructor for ConnectionManager
	 * @param verbose whether verbose mode is enabled
	 * @param p DatagramPacket received by Listener
	 * @param req the type of request
	 * @param length the length of the data packet
	 * 
	 * @since May 13 2014
	 * 
	 * Latest Change: Added in length
	 * @version May 15 2014
	 * @author Colin
	 * 
	 */
	public ConnectionManager(boolean verbose, byte[] data, int port, Request r, int length) {
		this.verbose = verbose;
		this.port = port;
		req = r;
		this.data = data;
		this.length = length;
		
		try { // initialize the socket
			send = new DatagramSocket();
		} // end try
		catch (SocketException se) {
			System.err.println("Socket exception error: " + se.getMessage());
		} // end catch
	} // end constructor
	
	/**
	 * The following is the run method for ConnectionManager, used to execute code upon starting the thread.
	 * It will service the request appropriately based on its validity
	 * 
	 * @since May 13 2014
	 * 
	 * Latest Change: Added response packets for read and write requests
	 * @version May 17 2014
	 * @author Moh
	 * 
	 */
	public void run() {
		System.out.println("Spawned ConnectionManager thread");
		boolean error = false;
		int innerzero = 0;
		boolean found = false;
		System.out.println("About to hit for loop statement in run");
		for (int i = 2; i < length - 1; i++) {
			//System.out.println("i: " + i + " found: " + found + " error: " + error + " data[i]: " + Integer.toHexString(data[i]));
			if (data[i] == (byte) 0) {
				if (!found) {
					innerzero = i;
					found = true;
				} // end if
				else
					error = true;
			}
		}
		
		if(error){
			System.out.println("Error");
			// TODO send error
		}
		
		System.out.println("Finished for loop to find innerzero");
		if (data[data.length-1] != (byte) 0)
			error = true;
		
		if(error){
			System.out.println("Error");
			// TODO send error
		}
		System.out.println("At right before checking is innerzero is set to 2 or 0");
		if (innerzero == 2 || innerzero == 0)
			error = true;

		if (req == Request.WRITE) {
			// TODO write to file
			// form the write Acknowledge block
			System.out.println("writeAck");
			byte writeAck[] = new byte[4];
			writeAck[0] = (byte)0;
			writeAck[1] = (byte)4;
			writeAck[2] = (byte)0;
			writeAck[3] = (byte)0;
			
			try {// create the acknowledge packet to send back to the client
				sendData = new DatagramPacket(writeAck, 4, InetAddress.getLocalHost(), port);
			} // end try
			catch (UnknownHostException uhe) {
				System.err.println("Unknown host exception error: " + uhe.getMessage());
			} // end catch
		} // end if
		else if (req == Request.READ) {
			// form the read block
			System.out.println("readData");
			byte readData[] = new byte[4];
			readData[0] = (byte)0;
			readData[1] = (byte)3;
			readData[2] = (byte)0;
			readData[3] = (byte)1;
			
			try { // create the data packet to send back to the client
				sendData = new DatagramPacket(readData, 4, InetAddress.getLocalHost(), port);
			} // end try
			catch (UnknownHostException uhe) {
				System.err.println("Unknown host exception error: " + uhe.getMessage());
			} // end catch
		} // end if
		
		try { // send response back
			send.send(sendData);
		} // end try
	    catch (IOException ioe) {
	    	System.err.println("Unknown IO exception error: " + ioe.getMessage());
	    } // end catch
		System.out.println("Server sent response back to ErrorSim");
		send.close();
	} // end method
	
	
} // end class
