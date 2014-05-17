import java.io.*;
import java.net.*;
import java.util.Arrays;

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
	public static final int DATA_SIZE = 516;
	
	private DatagramSocket send;
	private DatagramPacket sendData;
	private boolean verbose;
	private int port;
	private Request req;
	private DatagramPacket receivedPacket;
	private byte data[];
	private int length;
	private String fileName; 
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
	public ConnectionManager(boolean verbose, byte[] data, int port, Request r, int length, String fileName) {
		this.verbose = verbose;
		this.port = port;
		req = r;
		this.data = data;
		this.length = length;
		this.fileName = fileName;
		
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
		/*boolean error = false;
		int innerzero = 0;
		boolean found = false;
		for (int i = 2; i < length - 1; i++) {
			if (data[i] == (byte) 0) {
				if (!found) {
					innerzero = i;
					found = true;
				} // end if
				else
					error = true;
			}
		}
		
		if (data[data.length-1] != (byte) 0)
			error = true;
		
		if(error){
			System.out.println("Error");
			// TODO send error
		}
		if (innerzero == 2 || innerzero == 0)
			error = true;
		
		if(error){
			System.out.println("Error");
			// TODO send error
		}
		*/
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
		else if (req == Request.DATA) {
			System.out.println("DATA");
			
			
			int blockNum = 0;
			blockNum += (int)data[2] * 10;
			blockNum += data[3];

			System.out.println("calling write now");
			
			byte writeAck[] = new byte[4];
			writeAck[0] = (byte)0;
			writeAck[1] = (byte)4;
			writeAck[2] = (byte)(blockNum - (blockNum % 10));
			writeAck[3] = (byte)(blockNum % 10);
			System.out.println("calling write now");
			
			try {
				WriteToFile(blockNum, Arrays.copyOfRange(data, 4, data.length));
			} catch (FileNotFoundException e) {
				System.out.println("File Not Found: " + e.toString());
				System.exit(0);
			} catch (IOException e) {
				System.out.println("IO Exception: " + e.toString());
				System.exit(0);
			}
			
			try {// create the acknowledge packet to send back to the client
				sendData = new DatagramPacket(writeAck, 4, InetAddress.getLocalHost(), port);
			} // end try
			catch (UnknownHostException uhe) {
				System.err.println("Unknown host exception error: " + uhe.getMessage());
			} // end catch
			
		}
		else if (req == Request.READ) {
			// form the read block
			System.out.println("readData");
			byte readData[] = new byte[516];
			readData[0] = (byte)0;
			readData[1] = (byte)3;
			readData[2] = (byte)0;
			readData[3] = (byte)1;
			
			byte[] fileData = new byte[0];
			
			try {
				fileData = ReadFromFile(1);
			} catch (FileNotFoundException e) {
				System.out.println("File Not Found: " + e.toString());
				System.exit(0);
			} catch (IOException e) {
				System.out.println("IO Exception: " + e.toString());
				System.exit(0);
			}
			
			System.arraycopy(fileData, 0, readData, 4, fileData.length);
			
			try { // create the data packet to send back to the client
				sendData = new DatagramPacket(readData, fileData.length + 4, InetAddress.getLocalHost(), port);
			} // end try
			catch (UnknownHostException uhe) {
				System.err.println("Unknown host exception error: " + uhe.getMessage());
			} // end catch
		} // end if
		else if(req == Request.ACK)
		{
			int blockNum = 0;
			blockNum += (int)data[2] * 10;
			blockNum += data[3];
			
			blockNum++;
			
			System.out.println("readData");
			byte readData[] = new byte[516];
			readData[0] = (byte)0;
			readData[1] = (byte)3;
			readData[2] = (byte)(blockNum - (blockNum % 10));
			readData[3] = (byte)(blockNum % 10);
			
			byte[] fileData = new byte[0];
			
			try {
				fileData = ReadFromFile(blockNum);
			} catch (FileNotFoundException e) {
				System.out.println("File Not Found: " + e.toString());
				System.exit(0);
			} catch (IOException e) {
				System.out.println("IO Exception: " + e.toString());
				System.exit(0);
			}
			
			if (fileData.length == 0){
				send.close();
				return;
			}
			
			System.arraycopy(fileData, 0, readData, 4, fileData.length);
			
			try { // create the data packet to send back to the client
				sendData = new DatagramPacket(readData, fileData.length + 4, InetAddress.getLocalHost(), port);
			} // end try
			catch (UnknownHostException uhe) {
				System.err.println("Unknown host exception error: " + uhe.getMessage());
			} // end catch
			
		}
		
		try { // send response back
			send.send(sendData);
		} // end try
	    catch (IOException ioe) {
	    	System.err.println("Unknown IO exception error: " + ioe.getMessage());
	    } // end catch
		System.out.println("Server sent response back to ErrorSim");
		send.close();
		
	} // end method
	
	public void WriteToFile(int blockNum, byte[] writeData) throws FileNotFoundException, IOException
	{
	    System.out.println("writing to file");
	    
		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(System.getProperty("user.dir") + "\\output" + fileName));
		out.write(writeData, (blockNum-1)*512, writeData.length-1);
		out.close();
	}
	
	public byte[] ReadFromFile(int blockNum) throws FileNotFoundException, IOException
	{
		
		BufferedInputStream in = new BufferedInputStream(new FileInputStream(System.getProperty("user.dir") + "\\" + fileName));

		byte[] data = new byte[512];
		int i = 1;
		
		in.skip((blockNum-1)*512);
		
		if(in.read(data) == -1)
			return new byte[0];

		while (in.read(data) != -1) {
			i++;
		}
		
		in.close();
		
		BufferedInputStream in2 = new BufferedInputStream(new FileInputStream(System.getProperty("user.dir") + "\\" + fileName));

		in2.skip((blockNum-1)*512);
		while (in2.read() != -1) {
			i++;
		}
		
		in2.close();
		
		byte[] newData = new byte[i];
		System.arraycopy(data, 0, newData, 0, i);
		
		return newData;

	}

} // end class
