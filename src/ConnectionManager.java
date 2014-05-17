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
	 * Latest Change: Added response packets for read and write requests
	 * @version May 17 2014
	 * @author Moh
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
			// form the write Acknowledge block
			byte writeAck[] = new byte[4];
			writeAck[0] = (byte)0;
			writeAck[1] = (byte)4;
			writeAck[2] = (byte)0;
			writeAck[3] = (byte)0;
			
			// create the acknowledge packet to send back to the client
			sendData = new DatagramPacket(writeAck, 4, receivedPacket.getAddress(), receivedPacket.getPort());
		} // end if
		else if (req == Request.READ) {
			// form the read block
			byte readData[] = new byte[4];
			readData[0] = (byte)0;
			readData[1] = (byte)3;
			readData[2] = (byte)0;
			readData[3] = (byte)1;
			
			// create the data packet to send back to the client
			sendData = new DatagramPacket(readData, 4, receivedPacket.getAddress(), receivedPacket.getPort());
		} // end if
		
		try { // send response back
			send.send(sendData);
		} // end try
	    catch (IOException ioe) {
	    	System.err.println("Unknown IO exception error: " + ioe.getMessage());
	    } // end catch
		
		send.close();
	} // end method
	
	
} // end class
