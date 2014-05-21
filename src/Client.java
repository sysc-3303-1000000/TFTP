import java.io.*;
import java.net.*;
import java.util.*;

/**
 * The following is implementation for the Client
 * 
 * @since May 11 2014
 * 
 * @author 1000000
 * @version May 17 2014
 *
 */
public class Client {
	public static final byte zero = (byte)0;
	public static final byte one = (byte)1;
	public static final byte two = (byte)2;	
	
	private DatagramSocket sendReceiveSocket; // Socket used to send and receive packets
	private DatagramPacket sendPacket, receivePacket, sendData; // Packets used to send an receive information
	private String filenameString, modeString;
	private byte filenameBytes[], modeBytes[];
	private byte[] ackNumber = new byte[2];
	private byte[] dataNumber = new byte[2];
	private int dataBlock, ackBlock;

	private static byte readMsg[];
	private static byte writeMsg[];
	
	/**
	 * The following is the constructor for Client
	 * 
	 * @since May 11 2014
	 * 
	 * Latest Change: Added Code from assignment 1
	 * @version May 15 2014
	 * @author Moh
	 * 
	 */
	public Client(){
		
		// initialize the DatagramSocket sendReceive to bind to any available port
		try {
			sendReceiveSocket = new DatagramSocket();
		} // end try
		catch (SocketException se){
			System.err.println("Socket exception error: " + se.getMessage());
		} // end catch
		
		// initialize the String variables that will be passed onto the program
		filenameString = "moh.txt";
		modeString = "netascii";
		
		// change string values to bytes
		filenameBytes = filenameString.getBytes();
		modeBytes = modeString.getBytes();
		
		// initialize the messages
		readMsg = createMsg(one, filenameBytes, modeBytes);
		writeMsg = createMsg(two, filenameBytes, modeBytes);

		
	} // end constructor

	/**
	 * Send and receive procedure for the packet
	 * this procedure will also be able to accept a request depending on its arguments
	 * @param rqstMsg a byte array which will have the message attached to the packet being sent
	 * 
	 * @since May 11 2014
	 * 
	 * Latest Change: Added implementation of reading and writing
	 * @version May 20 2014
	 * @author Kais
	 * 
	 */
	private void sendReceive(byte[] rqstMsg, Request req)
	{		
		System.out.println("Client has started...");
				
		// send the packet to well-known port 68
		try {
			sendPacket = new DatagramPacket(rqstMsg, rqstMsg.length, InetAddress.getLocalHost(), 68);
		} // end try
		catch (UnknownHostException uhe) {
			System.err.println("Unknown host exception error: " + uhe.getMessage());
		} // end catch
		
		System.out.println("Client sending packet to intermediate...");
	    // send the packet to the intermediate via the send socket 
	    try {
	       sendReceiveSocket.send(sendPacket);
	    } // end try
	    catch (IOException ioe) {
	    	System.err.println("Unknown IO exception error: " + ioe.getMessage());
	    } // end catch
	    
		// prints out the information on the sent packet
		System.out.println("Client packet sent to intermediate...");
		printInformation(sendPacket);

		
		// prepare to receive a packet
		byte rply[] = new byte[516];
		receivePacket = new DatagramPacket(rply, rply.length);
		
		// block until you receive a packet
		// TODO ADD A TIME OUT ON IT 
		try {
			System.out.println("Client receiving packet from intermediate...");
			sendReceiveSocket.receive(receivePacket);
		} // end try
		catch (IOException ioe) {
			System.err.println("IO Exception error: " + ioe.getMessage());
		} // end catch
		
		// prints out the information on the received packet
		printInformation(receivePacket);
		System.out.println("Client packet received..");	
		
		if(req == Request.READ) {
			byte dat[] = receivePacket.getData();
			byte ack[] = new byte[4];
			dataNumber[0] = (byte)0;
			dataNumber[1] = (byte)1;
			dataBlock = 1;
			ackBlock = 1;
			while(verifydata(dataNumber, receivePacket)) {
				ack[0] = (byte)0;
				ack[1] = (byte)4;
				ack[2] = (byte)0;
				ack[3] = (byte)ackBlock;
			try {
				WriteToFile(dataBlock, Arrays.copyOfRange(dat, 4, dat.length));
			} // end try 
			catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} // end catch 
			catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} // end catch
			
			try {// create the acknowledge packet to send back to the client
				sendData = new DatagramPacket(ack, 4, InetAddress.getLocalHost(), 68);
			} // end try
			catch (UnknownHostException uhe) {
				System.err.println("Unknown host exception error: " + uhe.getMessage());
			} // end catch
			try {
			    sendReceiveSocket.send(sendData);
			} // end try
			catch (IOException ioe) {
				System.err.println("Unknown IO exception error: " + ioe.getMessage());
			} // end catch
			if (dat.length < 516) {
				break;
			} // end if
			
			byte rly[] = new byte[516];
			receivePacket = new DatagramPacket(rly, rly.length);
				
			try {
				System.out.println("Client receiving packet from intermediate...");
				sendReceiveSocket.receive(receivePacket);
			} // end try
			catch (IOException ioe) {
				System.err.println("IO Exception error: " + ioe.getMessage());
			} // end catch
			dataNumber[1] = (byte)(dataNumber[1]+(byte)1);
			ackBlock++;
			} // end whileloop
		} // end if
		else if (req == Request.WRITE) {
			byte data[] = new byte[516];
			ackNumber[0] = (byte)0;
			ackNumber[1] = (byte)0;
			dataBlock = 1;
			ackBlock = 0;
			while(verifyack(ackNumber, receivePacket)) {
				data[0] = (byte)0;
				data[1] = (byte)3;
				data[2] = (byte)0;
				data[3] = (byte)dataBlock;
				
				byte[] fileData = new byte[0];
				   
				try {
					fileData = ReadFromFile(dataBlock);
				} // end try
				catch (FileNotFoundException e) {
				    System.out.println("File Not Found: " + e.toString());
				    System.exit(0);
				} // end catch 
				catch (IOException e) {
				    System.out.println("IO Exception: " + e.toString());
				    System.exit(0);
				} // end catch
				if (fileData.length == 0) {
					break;
				} // end if
				
				System.arraycopy(fileData, 0, data, 4, fileData.length);
				   
				try {
					sendData = new DatagramPacket(data, fileData.length + 4, InetAddress.getLocalHost(), 68);
				} // end try
				catch (UnknownHostException uhe) {
					System.err.println("Unknown host exception error: " + uhe.getMessage());
				} // end catch
				   
				try {
					sendReceiveSocket.send(sendData);
				} // end try
				catch (IOException ioe) {
					System.err.println("Unknown IO exception error: " + ioe.getMessage());
				} // end catch
				
				byte reply[] = new byte[4];
				receivePacket = new DatagramPacket(reply, reply.length);
					
				try {
					System.out.println("Client receiving packet from intermediate...");
					sendReceiveSocket.receive(receivePacket);
				} // end try
				catch (IOException ioe) {
					System.err.println("IO Exception error: " + ioe.getMessage());
				} // end catch
				ackNumber[1] = (byte)(ackNumber[1]+(byte)1);
				dataBlock++;
					
			} // end whileloop
		} // end if
		
	} // end method
	
	/**
	 * The following is the method to read data from a file, if client requests a write
	 * @param blockNum the block which is to be read
	 * @return the block of data which was read
	 * @throws FileNotFoundException if the file cannot be found
	 * @throws IOException if there is an issue with IO
	 * 
	 * @since May 17 2014
	 * 
	 * Latest Change: Added implementation of how stuff is read based on Colin's server code
	 * @version May 17 2014
	 * @author Colin
	 * 
	 */
	private byte[] ReadFromFile(int blockNum) throws FileNotFoundException, IOException
	{
		
		BufferedInputStream in = new BufferedInputStream(new FileInputStream(System.getProperty("user.dir") + "\\" + filenameString));

		byte[] data = new byte[512];
		int i = 1;
		
		in.skip((blockNum-1)*512);
		if (in.read(data) == -1) {
			byte[] data1 = new byte[0];
			return data1;
		} // end if
		while (in.read(data) != -1) {
		} // end whileloop
		
		BufferedInputStream in2 = new BufferedInputStream(new FileInputStream(System.getProperty("user.dir") + "\\" + filenameString));

		in2.skip((blockNum-1)*512);
		while (in2.read() != -1) {
			i++;
		} // end whileloop
		
		in2.close();
		
		byte[] newData = new byte[i];
		System.arraycopy(data, 0, newData, 0, i);
		
		return newData;
		
	} // end method
	
	/**
	 * The following is the method used to write data to a file, done if client requests a read
	 * @param blockNum the block which is to be written
	 * @param writeData the data which is to be written
	 * @throws FileNotFoundException if the file cannot be found
	 * @throws IOException if there is an issue with IO
	 * 
	 * @since May 17 2014
	 * 
	 * Latest Change: Added implementation of how stuff is written based on Colin's server code
	 * @version May 17 2014
	 * @author Colin
	 * 
	 */
	private void WriteToFile(int blockNum, byte[] writeData) throws FileNotFoundException, IOException
	{
		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(System.getProperty("user.dir") + "\\output" + filenameString));
	
		out.write(writeData, (blockNum-1)*512, writeData.length);
		out.close();

	} // end method
	
	/**
	 * The following verifies that the data packet contains the block number it should 
	 * @param ackNumber the current ack packet which p should be (block #)
	 * @param p the ack packet sent by the server
	 * @return if the ack block is the correct one
	 * 
	 * @since May 17 2014
	 * 
	 * Latest Change: Added implementation of how verification is done
	 * @version May 17 2014
	 * @author Kais
	 * 
	 */
	private boolean verifyack(byte[] ackNumber, DatagramPacket p) {
		byte ack[] = p.getData();
		if (ack[0] == (byte)0 && ack[1] == (byte)4) {
			if (ack[2] == ackNumber[0] && ack[3] == ackNumber[1]) {
				return true;
			} // end if
		} // end if
		return false;
	} // end method
	
	/**
	 * The following verifies that the data packet contains the block number it should 
	 * @param dataNumber the current data packet which p should be (block #)
	 * @param p the data packet sent by the server
	 * @return if the data block is the correct one
	 * 
	 * @since May 17 2014
	 * 
	 * Latest Change: Added implementation of how verification is done
	 * @version May 17 2014
	 * @author Kais
	 * 
	 */
	private boolean verifydata(byte[] dataNumber, DatagramPacket p) {
		byte data[] = p.getData();
		if (data[0] == (byte)0 && data[1] == (byte)3) {
			if (data[2] == dataNumber[0] && data[3] == dataNumber[1]) {
				return true;
			} // end if
		} // end if
		return false;
	} // end method

	/**
	 * The following method will be called when trying to print out information about a specific packet
	 * @param p the information displayed desired for this packet
	 * 
	 * @since May 11 2014
	 * 
	 * Latest Change: Added Code from assignment 1
	 * @version May 15 2014
	 * @author Moh
	 * 
	 */
	private void printInformation(DatagramPacket p) {
		
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
	 * Following method will create the message which is being put into the packet
	 * @param rqstByte will be 0 or 1 depending if it is a read or write request, otherwise invalid
	 * @param file the file name
	 * @param mode the mode 
	 * @return a byte array for the message that we are sending
	 * 
	 * @since May 11 2014
	 * 
	 * Latest Change: Added Code from assignment 1
	 * @version May 15 2014
	 * @author Moh
	 * 
	 */
	private byte[] createMsg(byte rqstByte, byte file[], byte mode[]) {
		// the message will be of size 4 (0, rqstByte, 0, 0) with the file length and mode length
		byte msg[] = new byte[4+file.length+mode.length];
		msg[0] = zero;
		msg[1] = rqstByte;
		
		// will keep track of the index of the file and mode we're in
		int byteCount = 0;
		
		// keeps track of the index of the message
		int msgIndex = 2;
		
		// add the file bytes starting at index 2
		for (int i = 0; i<file.length; i++) {
			msg[msgIndex] = file[byteCount];
			byteCount++;
			msgIndex++;
		} // end forloop
		
		// add a 0 at index position 
		msg[(msgIndex)] = zero;
		// increase the index
		msgIndex++;
		
		// reset the byteCount to 0 to begin adding the bytes for the mode 
		byteCount = 0;
		// add the mode bytes starting at the msgIndex
		for (int i = 0; i<mode.length; i++) {
			msg[msgIndex] = mode[byteCount];
			byteCount++;
			msgIndex++;		
		} // end forloop
		
		// add the final 0 in the message
		msg[msgIndex] = zero;
		
		return msg;
	} // end forloop
	
	/**
	 * Main method for the Client
	 * @param args not used
	 * 
	 * @since May 11 2014
	 * 
	 * Latest Change: Changed for Iteration 1
	 * @version May 20 2014
	 * @author Kais
	 * 
	 */
	public static void main(String[] args) {
		Client client = new Client();
		client.sendReceive(readMsg, Request.READ);
		client.sendReceive(writeMsg, Request.WRITE);

	} // end method
	
} // end class