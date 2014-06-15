import java.io.*;
import java.net.*;
import java.util.*;

/**
 * The following is implementation for the Client
 * 
 * @since May 11 2014
 * 
 * @author 1000000
 * @version June 14 2014
 *
 */
public class Client extends Thread {
	/* Some constants defined in order to make better looking code */
	public static final int DATA_SIZE = 517; // one plus the maximum we can handle to see if we have received extra data
	public static final byte zero = (byte)0;
	public static final byte one = (byte)1;
	public static final byte two = (byte)2;
	public static final byte three = (byte)3;
	public static final byte four = (byte)4;
	public static final byte five = (byte)5;
	public static final int TIMEOUT = 3000;

	private DatagramSocket sendReceiveSocket; // Socket used to send and receive packets
	private DatagramPacket sendPacket, receivePacket, sendData; // Packets used to send an receive information
	private String filenameString, modeString, directory; // filename will be user input, while mode will remain hardcoded for now, directory contains where the user entered for the file
	private byte filenameBytes[], modeBytes[], message[];
	private byte[] ackNumber = new byte[2];
	private byte[] dataNumber = new byte[2];
	private int blockNum, socket, threadPort; // the current datablock number and ack block number
	private boolean endFile = false; // indicates if we have reached the last DATA block
	private Request req; // store the type of request
	private InetAddress address, threadAddress; // address of server, address of thread we are dealing with it

	/**
	 * The following is the constructor for the Client
	 * @param filename the file we are going to read from the server or write to on our side
	 * @param path the path of the file we are going to write to on our side or the path to place the file we are reading from the server
	 * @param req whether it is a read or write request
	 * @param socket set to either the server or error sim socket depending on user input
	 * @param address the InetAddress of the server or error sim
	 * 
	 * @since May 22 2014
	 * 
	 * Latest Change: Added parameter of InetAddress to handle remote server
	 * @version June 14 2014
	 * @author Kais
	 * 
	 */
	public Client(String filename, String path, Request req, int socket, InetAddress address) {

		directory = path;
		filenameString = filename;
		this.req = req;
		this.socket = socket; // server or error sim port
		this.address = address;

		// initialize the DatagramSocket sendReceive to bind to any available port
		try {
			sendReceiveSocket = new DatagramSocket();
		} // end try
		catch (SocketException se){
			System.err.println("Socket exception error: " + se.getMessage());
		} // end catch

		filenameBytes = filename.getBytes();

		// initialize the String variables that will be passed onto the program
		modeString = "netascii";

		// change string values to bytes
		modeBytes = modeString.getBytes();

		if (req == Request.READ)
			message = createMsg(one, filenameBytes, modeBytes);
		else if (req == Request.WRITE)
			message = createMsg(two, filenameBytes, modeBytes);
	} // end constructor

	/**
	 * Send and receive procedure for the packet
	 * this procedure will also be able to accept a request depending on its arguments
	 * @param req the type of request we are going to service
	 * 
	 * @since May 11 2014
	 * 
	 * Latest Change: Changed so we no longer to localhost for address, changed way error packet 5 was being handled
	 * @version June 14 2014
	 * @author Kais
	 * 
	 */
	private void sendReceive(Request req)
	{
		/* Following if statement block will check to see if the file we wish to write upon write request exists where the user specified, if not we stop */
		if (req == Request.WRITE) {
			File file = new File(directory);
			if (!file.exists()) {
				System.out.println("The file you specified to write to the server doesn't not exist, please check the name of the file and directory you specified and try again");
				return;
			} // end if
		} // end if
		
		sendPacket = new DatagramPacket(message, message.length, address, socket);

		System.out.println("Client sending " + req.toString() + " request to server");
		
		// send the packet to the error simulator or server via the send socket 
		try {
			sendReceiveSocket.send(sendPacket);
		} // end try
		catch (IOException ioe) {
			System.err.println("Unknown IO exception error: " + ioe.getMessage());
		} // end catch

		// prints out the information on the sent packet
		System.out.println("Client sent " + req.toString() + " request to server");


		// prepare to receive a packet
		byte rply[] = new byte[DATA_SIZE];
		receivePacket = new DatagramPacket(rply, rply.length);

		// block until you receive a packet
		boolean worked = false; // set to false since receiving a packet hasn't worked yet
		int numberOfTimeouts = 0; // no timeouts have occurred yet
		while (!worked) {
			
			try {
				worked = true; // set worked to true, will be set back to false in cases where we didn't receive a packet
				System.out.println("Client receiving packet from server");
				sendReceiveSocket.setSoTimeout(TIMEOUT); // set how long we wait to receive a packet before giving up
				sendReceiveSocket.receive(receivePacket);
			} // end try
			catch (SocketTimeoutException ste) {
				numberOfTimeouts++;
				worked = false; // did not work
			} // end catch
			catch (IOException ioe) {
				System.err.println("IO Exception error: " + ioe.getMessage());
				worked = false; // did not work
			} // end catch
			
			if (numberOfTimeouts == 5) { // we give up after timing out 5 times
				System.out.println("Client has timed out 5 times waiting for the first packet back from server, assuming the server is dead and exiting.");
				return;
			} // end if
			
		} // end whileloop
		
		System.out.println("Client has received a packet from server");
		
		/* Handle receiving an error packet */
		if (receivePacket.getData()[0] == zero && receivePacket.getData()[1] == five) {
			printErrorMsg(receivePacket.getData(), receivePacket.getLength());
			return;
		} // end if
		
		threadPort = receivePacket.getPort();
		threadAddress = receivePacket.getAddress();
		if (req == Request.READ) { // if request was a read
			byte dat[] = receivePacket.getData();
			byte ack[] = new byte[4];
			dataNumber[0] = (byte)0;
			dataNumber[1] = (byte)1;
			blockNum = 1;
			
			while(true) {
				
				/* Handle receiving a DATA packet instead of an ACK packet */
				if (receivePacket.getData()[0] == zero && receivePacket.getData()[1] == four) {
					byte emsg[] = ("The last TFTP packet received was an ACK packet when it should have been a DATA packet during a read request, client thread is exiting.").getBytes();
					try {
						sendReceiveSocket.send(new DatagramPacket(createErrorMsg(four, emsg), 5 + emsg.length, address, receivePacket.getPort()));
						System.out.println("Client sent error packet 4 with message: " + new String(emsg));
					} // end try
					catch (UnknownHostException e1) {
						System.err.println("Unknown Host: " + e1.toString());
					} // end catch
					catch (IOException e1) {
						System.err.println("IO Exception: " + e1.toString());
					} // end catch
					return;
				} // end if
				
				/* Handle receiving a packet which is not a DATA, ACK, or ERROR */
				else if (receivePacket.getData()[0] != zero || receivePacket.getData()[1] != three) {
					byte emsg[] = ("Client has received an unidentified packet type during a read request, client thread is exiting.").getBytes();
					try {
						sendReceiveSocket.send(new DatagramPacket(createErrorMsg(four, emsg), 5 + emsg.length, address, receivePacket.getPort()));
						System.out.println("Client sent error packet 4 with message: " + new String(emsg));
					} // end try
					catch (UnknownHostException e1) {
						System.err.println("Unknown Host: " + e1.toString());
					} // end catch
					catch (IOException e1) {
						System.err.println("IO Exception: " + e1.toString());
					} // end catch
					return;
				} // end if
				
				/* Handle receiving a block number that doesn't make sense at this point in the operation */
				if ((receivePacket.getData()[2] != dataNumber[0] && receivePacket.getData()[2] != dataNumber[0] - one) || (receivePacket.getData()[3] != dataNumber[1] && receivePacket.getData()[3] != dataNumber[1] - one)) {
					byte emsg[] = ("The last TFTP packet received has a block number that doesn't make sense at this point in the transfer process for a read request, client thread is exiting").getBytes();
					try {
						sendReceiveSocket.send(new DatagramPacket(createErrorMsg(four, emsg), 5 + emsg.length, address, receivePacket.getPort()));
						System.out.println("Client sent error packet 4 with message: " + new String(emsg));
					} // end try
					catch (UnknownHostException e1) {
						System.err.println("Unknown Host: " + e1.toString());
					} // end catch
					catch (IOException e1) {
						System.err.println("IO Exception: " + e1.toString());
					} // end catch
					return;
				} // end if
				
				/* Handle receiving a packet with an invalid size  */
				if (receivePacket.getLength() > 516 || receivePacket.getLength() < 4) {
					byte emsg[] = ("The data packet client received has an invalid size, client thread terminating").getBytes();
					try {
						sendReceiveSocket.send(new DatagramPacket(createErrorMsg(four, emsg), 5 + emsg.length, address, receivePacket.getPort()));
						System.out.println("Client sent error packet 4 with message: " + new String(emsg));
					} // end try
					catch (UnknownHostException e1) {
						System.err.println("Unknown Host: " + e1.toString());
					} // end catch
					catch (IOException e1) {
						System.err.println("IO Exception: " + e1.toString());
					} // end catch
					return;
				} // end if
				
				/* Check to see if the block number matches up */
				if (verifydata(dataNumber, receivePacket)) {
					System.out.println("Client has received DATA packet " + blockNum + " from the server.");
					try {
						WriteToFile(blockNum, Arrays.copyOfRange(dat, 4, receivePacket.getLength()));// make sure if we receive a duplicate data packet, we only write the first one
					} // end try
					catch (FileNotFoundException e) { // respond with error packet 0502_0 at this point, then terminate client thread
						byte emsg[] = ("The file: " + filenameString + " could not be written in the following path: " + directory + ". Please ensure that you have write permission to the directory you specified, and check to see if the directory you specified is the correct one, client thread exiting.").getBytes();
						try {
							sendReceiveSocket.send(new DatagramPacket(createErrorMsg(two, emsg), 5 + emsg.length, address, receivePacket.getPort()));
							System.out.println("Client sent error packet 2 with message: " + new String(emsg));
						} // end try
						catch (UnknownHostException e1) {
							System.err.println("Unknown Host: " + e1.toString());
						} // end catch
						catch (IOException e1) {
							System.err.println("IO Exception: " + e1.toString());
						} // end catch
						return;
					} // end catch
					catch (IOException e) { // respond with error packet 0503_0 at this point, then terminate client thread
						byte emsg[] = ("The file: " + filenameString + "could not be written in the following path: " + directory + " because the disk where this directory is located is full. Please remove files from the disk to have sufficient room and try again, client thread exiting.").getBytes();
						try {
							sendReceiveSocket.send(new DatagramPacket(createErrorMsg(three, emsg), 5 + emsg.length, address, receivePacket.getPort()));
							System.out.println("Client sent error packet 3 with message: "+ new String(emsg));
						} // end try
						catch (UnknownHostException e1) {
							System.err.println("Unknown Host: " + e1.toString());
						} // end catch
						catch (IOException e1) {
							System.err.println("IO Exception: " + e1.toString());
						} // end catch
						return;
					} // end catch
				} // end if
				
				else {
					dataNumber[1]--;
					if(dataNumber[1] == 255) {
						dataNumber[0]--;
					} // end if
					blockNum--;
				} // end else

				/* Set the acknowledge header */
				ack[0] = (byte)0;
				ack[1] = (byte)4;
				ack[2] = (byte)((blockNum - (blockNum % 256))/256);
				ack[3] = (byte)(blockNum % 256);
				
				sendData = new DatagramPacket(ack, 4, address, receivePacket.getPort());
				
				System.out.println("Client sending ACK packet "+ blockNum + " to the server.");
				
				worked = false;
				numberOfTimeouts = 0;
				try {
					sendReceiveSocket.send(sendData);
				} // end try
				catch (IOException ioe) {
					System.err.println("Unknown IO exception error: " + ioe.getMessage());
				} // end catch
				System.out.println("Client sent ACK packet "+ blockNum + " to the server.");
				
				if (receivePacket.getLength() < 516) {
					System.out.println("Client has received last packet from the server during a read request");
					break;
				} // end if
				byte rly[] = new byte[DATA_SIZE];
				receivePacket = new DatagramPacket(rly, rly.length);
				while(!worked) {
					try {
						worked = true;
						System.out.println("Client receiving packet from server.");
						sendReceiveSocket.setSoTimeout(TIMEOUT);
						sendReceiveSocket.receive(receivePacket);
					} // end try
					catch (SocketTimeoutException ste) {
						numberOfTimeouts++;
						worked = false;
					} // end catch
					catch (IOException ioe) {
						System.err.println("IO Exception error: " + ioe.getMessage());
						worked = false;
					} // end catch
					
					if (worked && (receivePacket.getData()[0] == zero && receivePacket.getData()[1] == five)) { // if we get an ERROR packet
						printErrorMsg(receivePacket.getData(), receivePacket.getLength());
						return;
					} // end if
					
					if (numberOfTimeouts == 5) { // if we timeout 5 times
						System.out.println("Client has timed out 5 times waiting for the next data packet from server during a read request, server is assumed dead, client thread exiting.");
						return;
					} // end if
					
					if ((receivePacket.getPort() != threadPort || !receivePacket.getAddress().equals(threadAddress)) && worked) { // if we receive a packet from a different TID
						byte emsg[] = ("Packet received from invalid source").getBytes();
						try {
							sendReceiveSocket.send(new DatagramPacket(createErrorMsg(five, emsg), 5 + emsg.length, receivePacket.getAddress(), receivePacket.getPort()));
							System.out.println("Client sent error packet 5 with message: " + new String(emsg));
							System.out.println("Client received packet from a different port or IP address than what it has been using for the transfer. File transfer will remain active.");
						} // end try
						catch (UnknownHostException e1) {
							System.err.println("Unknown Host: " + e1.toString());
						} // end catch
						catch (IOException e1) {
							System.err.println("IO Exception: " + e1.toString());
						} // end catch
						worked = false;
					} // end if
					
					if (worked) {
						dat = rly; // new dat will be the data from the packet just received
					} // end if
					
				} // end whileloop
				
				/* increment values required */
				dataNumber[1]++;
				if(dataNumber[1] == 0) {
					dataNumber[0]++;
				} // end if
				blockNum++; // next block
				
			} // end whileloop
			
		} // end if
		
		else if (req == Request.WRITE) { // if request was a write
			
			ackNumber[0] = (byte)0;
			ackNumber[1] = (byte)0;
			blockNum = 1;
			
			while(true) {
				
				/* Handle what happens when we get a DATA packet instead of an ACK packet */
				if (receivePacket.getData()[0] == zero && receivePacket.getData()[1] == three) {
					byte emsg[] = ("The last TFTP packet received was a DATA packet when it should have been an ACK packet during a write request, client thread is exiting").getBytes();
					try {
						sendReceiveSocket.send(new DatagramPacket(createErrorMsg(four, emsg), 5 + emsg.length, address, receivePacket.getPort()));
						System.out.println("Client sent error packet 4 with message: " + new String(emsg));
					} // end try
					catch (UnknownHostException e1) {
						System.err.println("Unknown Host: " + e1.toString());
					} // end catch
					catch (IOException e1) {
						System.err.println("IO Exception: " + e1.toString());
					} // end catch
					return;
				} // end if
				
				/* Handle what happens when packet isn't a DATA, ACK, or ERROR packet */
				else if (receivePacket.getData()[0] != zero || receivePacket.getData()[1] != four) {
					byte emsg[] = ("Client has received an unidentified packet type during a write request, client thread is exiting").getBytes();
					try {
						sendReceiveSocket.send(new DatagramPacket(createErrorMsg(four, emsg), 5 + emsg.length, address, receivePacket.getPort()));
						System.out.println("Client sent error packet 4 with message: " + new String(emsg));
					} // end try
					catch (UnknownHostException e1) {
						System.err.println("Unknown Host: " + e1.toString());
					} // end catch
					catch (IOException e1) {
						System.err.println("IO Exception: " + e1.toString());
					} // end catch
					return;
				}
				
				/* Handle receiving a block number that doesn't make sense at this point in the operation */
				if ((receivePacket.getData()[2] != ackNumber[0] && receivePacket.getData()[2] != ackNumber[0] - one) || (receivePacket.getData()[3] != ackNumber[1] && receivePacket.getData()[3] != ackNumber[1] - one)) {
					byte emsg[] = ("The last TFTP packet received has a block number that doesn't make sense at this point in the transfer process during a write request, client thread is exiting").getBytes();
					try {
						sendReceiveSocket.send(new DatagramPacket(createErrorMsg(four, emsg), 5 + emsg.length, address, receivePacket.getPort()));
						System.out.println("Client sent error packet 4 with message: " + new String(emsg));
					} // end try
					catch (UnknownHostException e1) {
						System.err.println("Unknown Host: " + e1.toString());
					} // end catch
					catch (IOException e1) {
						System.err.println("IO Exception: " + e1.toString());
					} // end catch
					return;
				} // end if
				
				/* Handle what happens when we receive an ACK packet with an incorrect size */
				if (receivePacket.getLength() > 4 || receivePacket.getLength() < 4) {
					byte emsg[] = ("The ack packet client received has an invalid size, which should not be, client thread terminating").getBytes();
					try {
						sendReceiveSocket.send(new DatagramPacket(createErrorMsg(four, emsg), 5 + emsg.length, address, receivePacket.getPort()));
						System.out.println("Client sent error packet 4 with message: " + new String(emsg));
					} // end try
					catch (UnknownHostException e1) {
						System.err.println("Unknown Host: " + e1.toString());
					} // end catch
					catch (IOException e1) {
						System.err.println("IO Exception: " + e1.toString());
					} // end catch
					return;
				} // end if
				
				byte[] fileData = new byte[0];
				
				/* If we received the correct packet */
				if (verifyack(ackNumber, receivePacket)) {
					System.out.println("Client has received ACK packet " + blockNum + " from the server.");
					try {
						fileData = ReadFromFile(blockNum); // read a block of data from a file
					} // end try
					catch (FileNotFoundException e) { // respond with error packet 0501_0 at this point, then terminate client thread
						byte emsg[] = ("The full path you specified: " +  directory + " does not contain the file you specified. Please ensure that you are specifying the correct filename and the correct directory name and try again. Also ensure you have read permissions for the file and directory").getBytes();
						try {
							sendReceiveSocket.send(new DatagramPacket(createErrorMsg(one, emsg), 5 + emsg.length, address, receivePacket.getPort()));
							System.out.println("Client sending error packet 1 with message: " + new String(emsg));
						} // end try 
						catch (UnknownHostException e1) {
							System.err.println("Unknown Host: " + e1.toString());
						} // end catch 
						catch (IOException e1) {
							System.err.println("IO Exception: " + e1.toString());
						} // end catch
						return;
					} // end catch 
					catch (IOException e) { 
						System.out.println("IO Exception: " + e.toString());
						System.exit(0);
					} // end catch

					byte data[] = new byte[4 + fileData.length];
					/* Set the DATA packet header */
					data[0] = (byte)0;
					data[1] = (byte)3;
					data[2] = (byte)((blockNum - (blockNum % 256))/256);
					data[3] = (byte)(blockNum % 256);

					if (fileData.length < 512) { // if we read the last block
						endFile = true;
					} // end if

					System.arraycopy(fileData, 0, data, 4, fileData.length);
					sendData = new DatagramPacket(data, fileData.length + 4, address, receivePacket.getPort());
				} // end if
				
				/* We received the last packet again */
				else {
					ackNumber[1]--;
					if(ackNumber[1] == 255) {
						ackNumber[0]--;
					}
					blockNum--;
				} // end else
				
				worked = false;
				numberOfTimeouts = 0;

				while(!worked) {
					System.out.println("Client sending DATA packet " + blockNum + " to the server.");
					try {
						sendReceiveSocket.send(sendData);
					} // end try
					catch (IOException ioe) {
						System.err.println("Unknown IO exception error: " + ioe.getMessage());
					} // end catch
					System.out.println("Client sent DATA packet " + blockNum + " to the server.");
					
					byte reply[] = new byte[DATA_SIZE];
					receivePacket = new DatagramPacket(reply, reply.length);

					try {
						worked = true;
						System.out.println("Client receiving packet from server.");
						sendReceiveSocket.setSoTimeout(TIMEOUT); // give up after our timeout
						sendReceiveSocket.receive(receivePacket);
					} // end try
					catch (SocketTimeoutException ste) {
						numberOfTimeouts++;
						worked = false;
					} // end catch
					catch (IOException ioe) {
						System.err.println("IO Exception error: " + ioe.getMessage());
						worked = false;
					} // end catch
					
					if (worked && (receivePacket.getData()[0] == zero && receivePacket.getData()[1] == five)) { // handle ERROR packet being received
						printErrorMsg(receivePacket.getData(), receivePacket.getLength());
						return;
					} // end if
					
					if (worked && receivePacket.getLength() != 4 && endFile) {
						System.out.println("We got an incorrect packet as our last packet for write, but connection has been dropped due to sending last data packet, so not sending error packet 4.");
					} // end if
					
					if ((receivePacket.getPort() != threadPort || !receivePacket.getAddress().equals(threadAddress)) && worked) { // handle receiving from the wrong TID
						byte emsg[] = ("Packet received from invalid source").getBytes();
						try {
							sendReceiveSocket.send(new DatagramPacket(createErrorMsg(five, emsg), 5 + emsg.length, receivePacket.getAddress(), receivePacket.getPort()));
							System.out.println("Client sent error packet 5 with message: " + new String(emsg));
							System.out.println("Client received packet from a different port or IP address than what it has been using for the transfer. File transfer will remain active.");
						} // end try
						catch (UnknownHostException e1) {
							System.err.println("Unknown Host: " + e1.toString());
						} // end catch
						catch (IOException e1) {
							System.err.println("IO Exception: " + e1.toString());
						} // end catch
						worked = false;
					} // end if
					
					else if(worked) {
						receivePacket.setData(Arrays.copyOfRange(reply, 0, receivePacket.getLength()));
					} // end if
					
					if (numberOfTimeouts == 5) {
						System.out.println("Client has timed out 5 times waiting for the next ack packet from server, server assumed dead, client thread terminating");
						return;
					} // end if
					
					if (endFile && worked) { // breaks if endfile and the it received a response
						System.out.println("Client has received last packet from the server during a write request");
						return;
					} // end if
					
				} // end whileloop
				
				ackNumber[1]++;
				if(ackNumber[1] == 0) {
					ackNumber[0]++;
				}
				blockNum++;

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

		BufferedInputStream in = new BufferedInputStream(new FileInputStream(directory));

		byte[] data = new byte[512];
		int i = 0;

		in.skip((blockNum-1)*512);

		if ((i = in.read(data)) == -1){
			in.close();
			return new byte[0];
		} // end if

		in.close();

		if (i < 512) // act differently if we've hit the end of the file
		{
			byte[] tempData = new byte[i];
			System.arraycopy(data, 0, tempData, 0, i);
			return tempData;
		} // end if

		return data;

	} // end method

	/**
	 * The following is the method used to write data to a file, done if client requests a read
	 * @param blockNum the block which is to be written
	 * @param writeData the data which is to be written
	 * @throws FileNotFoundException if the file cannot be found to right to, meaning we have an access violation
	 * @throws IOException if there is an issue with IO
	 * 
	 * @since May 17 2014
	 * 
	 * Latest Change: Changed so user enters the full path of the file to save as
	 * @version June 14 2014
	 * @author Kais
	 * 
	 */
	private void WriteToFile(int blockNum, byte[] writeData) throws FileNotFoundException, IOException
	{
		FileOutputStream out = new FileOutputStream(directory, (blockNum > 1) ? true : false);
		out.write(writeData, 0, writeData.length);
		out.getFD().sync();
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
	 * Latest Change: Added macro for byte 4
	 * @version May 31 2014
	 * @author Kais
	 * 
	 */
	private boolean verifyack(byte[] ackNumber, DatagramPacket p) {
		byte ack[] = p.getData();
		if (ack[0] == zero && ack[1] == four) {
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
	 * Latest Change: Added macro for byte 3
	 * @version May 31 2014
	 * @author Kais
	 * 
	 */
	private boolean verifydata(byte[] dataNumber, DatagramPacket p) {
		byte data[] = p.getData();
		if (data[0] == zero && data[1] == three) {
			if (data[2] == dataNumber[0] && data[3] == dataNumber[1]) {
				return true;
			} // end if
		} // end if
		return false;
	} // end method

	/**
	 * The following method will be called when trying to print out information about a specific packet, used in testing and debugging during the project only
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
	 * Following method will print the error message from an error packet
	 * @param errorMsg the byte array containing the entirety of the message from the error packet
	 * @param length the length of the error message
	 * 
	 * @since May 30 2014
	 * 
	 * Latest Change: Added implementation for this method
	 * @version June 9 2014
	 * @author Kais
	 */
	private void printErrorMsg(byte[] errorMsg, int length) {
		System.out.println("Client has received error packet from server: " + new String(Arrays.copyOfRange(errorMsg, 4, length - 1)));
	} // end method

	/**
	 * Following method will create an error message which will be put into a packet and sent to the server
	 * @param type the type of error we have encountered on the client side
	 * @param errorMsg the corresponding error message for the type of error
	 * @return the message which will be put into a packet and sent to the server
	 * 
	 * @since May 30 2014
	 * 
	 * Latest Change: Added macro for byte 5
	 * @version May 31 2014
	 * @author Kais
	 */
	private byte[] createErrorMsg(byte type, byte errorMsg[]) {
		byte msg[] = new byte[5 + errorMsg.length];

		/* Form the Error packet header */
		msg[0] = zero;
		msg[1] = five;
		msg[2] = zero;
		msg[3] = type;

		/* Insert the error message */
		for(int i = 0; i < errorMsg.length; i++)
			msg[i+4] = errorMsg[i];

		/* Add the footer */
		msg[msg.length -1] = zero;

		return msg;
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
	} // end method

	public void run() {
		sendReceive(req);
		sendReceiveSocket.close();
	} // end method
} // end class