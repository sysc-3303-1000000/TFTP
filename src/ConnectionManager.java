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
 * @version May 22 2014
 *
 */
public class ConnectionManager extends Thread {
	public static final int DATA_SIZE = 517;
	public static final int TIMEOUT = 3000;

	private DatagramSocket send;
	private DatagramPacket sendData;
	private int port;
	private InetAddress address;
	private Request req;
	private DatagramPacket receivedPacket;
	private String fileName; 
	private int TiD; // Transfer ID
	private String mode;
	
	/**
	 * The following is the constructor for ConnectionManager
	 * @param verbose whether verbose mode is enabled
	 * @param p DatagramPacket received by Listener
	 * @param req the type of request
	 * @param length the length of the data packet
	 * 
	 * @since May 13 2014
	 * 
	 * Latest Change: Added address
	 * @version June 14 2014
	 * @author Colin
	 * 
	 */
	public ConnectionManager(int port, Request r, String fileName, String mode,  InetAddress address) {
		this.port = port;
		this.TiD = port;
		req = r;
		this.fileName = fileName;
		this.mode = mode;
		this.address = address;

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
	 * Latest Change: Updated error codes for invalid TiD/IP
	 * @version June 15 2014
	 * @author Colin
	 * 
	 */
	public void run() {
		System.out.println("Spawned ConnectionManager thread");
		
		if(!mode.equals("octet") && !mode.equals("netascii")){ //check for file mode validity
			byte emsg[] = ("Server has received an invalid mode: '" + mode + "'").getBytes();
			try {
				send.send(new DatagramPacket(createErrorMessage((byte)4, emsg), 5 + emsg.length, address, TiD));	//report invalid mode
				System.out.println("Server sent error packet 4 with message: " + new String(emsg));
			} // end try
			catch (IOException e1) {
				System.err.println("IO Exception: " + e1.toString());
			} // end catch
			return; //exit file transfer
		}

		if (req == Request.WRITE) { // Write requests
			int blockNum = 0; 				// used for current block number
			int prevBlockNum = 0;			//keep track of previous block number
			int numberOfTimeouts = 0;		//holds current number of timeouts
			boolean worked = false;			//did transfer work?
			byte writeAck[] = new byte[4];	//write acknowledgment byte array
			writeAck[0] = (byte)0;
			writeAck[1] = (byte)4;
			writeAck[2] = (byte)0;
			writeAck[3] = (byte)0;
			// send the first ack packet
			sendData = new DatagramPacket(writeAck, 4, address, port);
			try { // send response back
				send.send(sendData);
				System.out.println("Server sent ACK packet 1 to the Client");
			} // end try
			catch (IOException ioe) {
				System.err.println("Unknown IO exception error: " + ioe.getMessage());
			} // end catch
			// while we receive data packets that are 516 in size (break inside while)
			while (true) {
				byte data[] = new byte[DATA_SIZE];
				receivedPacket = new DatagramPacket(data, data.length);
				numberOfTimeouts = 0;
				worked = false;
				while(!worked){		//while we havne't received a valid packed
					try {
						System.out.println("Receiving packet from client");
						send.setSoTimeout(TIMEOUT);		//set timeout
						send.receive(receivedPacket);	//receive packet
						worked = true;
						
					} // end try
					catch(SocketTimeoutException ste){	//if timeout, increment number of timeouts by one
						numberOfTimeouts++;
					}
					catch (IOException ioe) {
						System.err.println("IO Exception error: " + ioe.getMessage());
					} // end catch
					if(numberOfTimeouts == 5){			//if 5 timeouts, print error and exit
						System.out.println("Server has timed out 5 times waiting for the next data packet from client");
						return;
					}
					if(worked && (receivedPacket.getPort() != TiD || !receivedPacket.getAddress().equals(address))){	//check port and IP address of new packet
						byte emsg[] = ("Packet received from invalid source").getBytes();
						try {
							send.send(new DatagramPacket(createErrorMessage((byte)5, emsg), 5 + emsg.length, address, receivedPacket.getPort()));	//send error
							System.out.println("Server sent error packet 5 with message: " + new String(emsg));
							System.out.println("Server received packet from a different port or IP address than what it has been using for the transfer. File transfer will remain active.");
						} // end try
						catch (IOException e1) {
							System.err.println("IO Exception: " + e1.toString());
						} // end catch
						worked = false;
					}
					if (worked && receivedPacket.getData()[1] == (byte)5) {		//check  if packet is an error message
						printErrorMessage(data, receivedPacket.getLength());	//print error message
						return;
					} // end if
				}
				if(receivedPacket.getLength() > 516){	//check for valid packet size
					byte emsg[] = ("The data packet server received is greater than 516 bytes, which should not be, server thread terminating").getBytes();
					try {
						send.send(new DatagramPacket(createErrorMessage((byte)4, emsg), 5 + emsg.length, receivedPacket.getAddress(), receivedPacket.getPort()));	//send error
						System.out.println("Server sent error packet 4 with message: " + new String(emsg));
					} // end try
					catch (IOException e1) {
						System.err.println("IO Exception: " + e1.toString());
					} // end catch
					return;
				}
				if(data[0] == (byte)0 && data[1] == (byte)4){	//check if we received an ACK packet
					byte emsg[] = ("The last TFTP packet received was an ACK packet when it should have been a DATA packet, sever thread is exiting").getBytes();
					try {
						send.send(new DatagramPacket(createErrorMessage((byte)4, emsg), 5 + emsg.length, address, TiD));	//send error
						System.out.println("Server sent error packet 4 with message: " + new String(emsg));
					} // end try
					catch (IOException e1) {
						System.err.println("IO Exception: " + e1.toString());
					} // end catch
					return;
				}
				else if (data[0] != (byte)0 || data[1] != (byte)3) {	//check if it's NOT a DATA packet
					byte emsg[] = ("Server has received an unidentified packet type, client thread is exiting").getBytes();
					try {
						send.send(new DatagramPacket(createErrorMessage((byte)4, emsg), 5 + emsg.length, address, TiD));	//send error
						System.out.println("Server sent error packet 4 with message: " + new String(emsg));
					} // end try
					catch (IOException e1) {
						System.err.println("IO Exception: " + e1.toString());
					} // end catch
					return;
				}
				//calculate the received block number into an integer
				blockNum = 0;
				blockNum += (data[2] & 0xFF) * 256;
				blockNum += (data[3] & 0xFF);
				
				if(blockNum != prevBlockNum && blockNum != prevBlockNum+1){	//check if the block number makes sense for this point in the transfer
					byte emsg[] = ("The last TFTP packet received has a block number that doesn't make sense at this point in the transfer process, client thread is exiting").getBytes();
					try {
						send.send(new DatagramPacket(createErrorMessage((byte)4, emsg), 5 + emsg.length, address, TiD));	//send error
						System.out.println("Server sent error packet 4 with message: " + new String(emsg));
					} // end try
					catch (IOException e1) {
						System.err.println("IO Exception: " + e1.toString());
					} // end catch
					return;
				}
				
				System.out.println("Server has received DATA packet " + blockNum + " from the client");

				if(blockNum != prevBlockNum){	//if it's a new DATA packet
					try {
						WriteToFile(blockNum, Arrays.copyOfRange(data, 4, receivedPacket.getLength())); // write the data
					} catch (FileNotFoundException e) {	//catch file not found exception which is thrown when we don't have permission to write
						byte emsg[] = ("The file: " + fileName + " could not be written to the Server directory. Please ensure the server has write permission to the Server directory.").getBytes();
						try {
							send.send(new DatagramPacket(createErrorMessage((byte)2, emsg), 5 + emsg.length, address, receivedPacket.getPort()));	//send error
						} // end try
						catch (IOException e1) {
							System.err.println("IO Exception: " + e1.toString());
						} // end catch
						System.out.println("Server sent error packet 2 with message: " + new String(emsg));
						return;
					}
					catch(FileAlreadyExistsException f){	//catch file already existing, since we are not overwriting server files
						byte emsg[] = ("The file: " + fileName + " already exists on the server, please specify a new file name.").getBytes();
						try {
							send.send(new DatagramPacket(createErrorMessage((byte)6, emsg), 5 + emsg.length, address, receivedPacket.getPort()));	//send error
						} // end try
						catch (IOException e1) {
							System.err.println("IO Exception: " + e1.toString());
						} // end catch
						System.out.println("Server sent error packet 6 with message: " + new String(emsg));
						return;
					}
					catch (IOException e) {		//catch for disk being full
						byte emsg[] = ("The file: " + fileName + " could not be written to the Server, Server disk is full.").getBytes();
						try {
							send.send(new DatagramPacket(createErrorMessage((byte)3, emsg), 5 + emsg.length, address, receivedPacket.getPort()));	//send error
						} // end try
						catch (IOException e1) {
							System.err.println("IO Exception: " + e1.toString());
						} // end catch
						System.out.println("Server sent error packet 3 with message: " + new String(emsg));
						return;
					}
					catch(Exception e){
						System.out.println("in exception e");
					}
				}
				// form the ACK packet
				writeAck[0] = (byte)0;
				writeAck[1] = (byte)4;
				writeAck[2] = (byte)((blockNum - (blockNum % 256))/256);
				writeAck[3] = (byte)(blockNum % 256);
				
				sendData = new DatagramPacket(writeAck, 4, address, TiD); // prepare packet
				
				try { // send response back
					send.send(sendData);
					System.out.println("Sever sent ACK packet " + blockNum + " to the Client");
				} // end try
				catch (IOException ioe) {
					System.err.println("Unknown IO exception error: " + ioe.getMessage());
				} // end catch
				if (receivedPacket.getLength() < 516) { // repeat unless that was the last data packet
					System.out.println("Closing Connectionmanager thread");
					break;
				} // end if
				prevBlockNum = blockNum;
			}
			send.close();

		} // end if

		else if (req == Request.READ) {	//Read request
			byte readData[] = new byte[516];	//byte array for datagram data
			int blockNum = 1;					//store block number
			int finalBlockNum = 0;				//block number of final packet
			int prevBlockNum = 0;				//previous block number
			int numberOfTimeouts = 0;			//number of timeouts
			boolean finalBlock = false;			//did we send the final block?
			boolean worked = false;				//did transfer work?
			boolean sendPacket = true;				//are we sending a new packet?
			
			//set up first 4 bytes of readData
			readData[0] = (byte)0;
			readData[1] = (byte)3;
			readData[2] = (byte)0;
			readData[3] = (byte)1;

			byte[] fileData = new byte[0];		//data from file

			try {
				fileData = ReadFromFile(1);		//read first block
			} catch (FileNotFoundException e) {	//catch file not existing on server
				byte emsg[] = ("The file: " + fileName + " could not be located in the Server directory. Please ensure you are specifying the correct file name and try again.").getBytes();
				try {
					send.send(new DatagramPacket(createErrorMessage((byte)1, emsg), 5 + emsg.length, address, port));	//send error
				} // end try
				catch (IOException e1) {
					System.err.println("IO Exception: " + e1.toString());
				} // end catch
				System.out.println("Server sent error packet 1 with message: " + new String(emsg));
				return;
			} catch (IOException e) {
				System.out.println("IO Exception: " + e.toString());
				System.exit(0);
			}

			System.arraycopy(fileData, 0, readData, 4, fileData.length);	//copy fileData into our readData

			sendData = new DatagramPacket(readData, fileData.length + 4, address, port);
			
			if(sendData.getLength() < 516){	//check if last chunk of file
				finalBlock = true;
				finalBlockNum = 1;
			}

			prevBlockNum = 1;
			
			while(true){
				byte data[] = new byte[DATA_SIZE];	//byte array for received data
				receivedPacket = new DatagramPacket(data, data.length);
				numberOfTimeouts = 0;	//reset number of timeouts
				worked = false;			//set worked to false
				while(!worked){			
					if(sendPacket){			//if resending, send previous packet again
						try {
							send.send(sendData);
							System.out.println("Server sent DATA packet " + blockNum + " to the Client");
						} // end try
						catch (IOException ioe) {
							System.err.println("Unknown IO exception error: " + ioe.getMessage());
						} // end catch
						sendPacket = false;
					}
					try{
						System.out.println("Receiving packet from client");
						send.setSoTimeout(TIMEOUT);			//set timeout
						send.receive(receivedPacket);
						worked = true;
					} catch(SocketTimeoutException ste){	//if timeout
						numberOfTimeouts++;					//increment number of timeouts
						sendPacket = true;
					} catch(IOException ioe) {
						System.err.println("IO Exception error: " + ioe.getMessage());
					} // end catch
					if (worked && receivedPacket.getData()[1] == (byte)5) {	//check if error packet
						printErrorMessage(data, receivedPacket.getLength());
						return;
					} // end if
					if(numberOfTimeouts == 5){	//if timeout 5 times, exit
						System.out.println("Server has timed out 5 times waiting for the next data packet from client");
						return;
					}
					if(worked && (receivedPacket.getPort() != TiD || !receivedPacket.getAddress().equals(address))){	//check for invalid TiD or IP address
						System.out.println("Address: " + address + " received: " + receivedPacket.getAddress() + " port: " + port + " received: " + receivedPacket.getPort());
						byte emsg[] = ("Packet received from invalid source").getBytes();
						try {
							send.send(new DatagramPacket(createErrorMessage((byte)5, emsg), 5 + emsg.length, receivedPacket.getAddress(), receivedPacket.getPort()));	//send error
							System.out.println("Server sent error packet 5 with message: " + new String(emsg));
							System.out.println("Server received packet from a different port or IP address than what it has been using for the transfer. File transfer will remain active.");
						} // end try
						catch (IOException e1) {
							System.err.println("IO Exception: " + e1.toString());
						} // end catch
						worked = false;
						sendPacket = true;
					}
				}
				if (receivedPacket.getLength() > 4){	//check if invalid size for ACK packet
					byte emsg[] = ("The ACK packet server received is greater than 4 bytes, which should not be, server thread terminating").getBytes();
					try {
						send.send(new DatagramPacket(createErrorMessage((byte)4, emsg), 5 + emsg.length, address, receivedPacket.getPort()));	//send error
						System.out.println("Server sent error packet 4 with message: " + new String(emsg));
					} // end try
					catch (IOException e1) {
						System.err.println("IO Exception: " + e1.toString());
					} // end catch
					return;
				}
				if(data[0] == (byte)0 && data[1] == (byte)3){ //check if received DATA packet
					byte emsg[] = ("The last TFTP packet received was a DATA packet when it should have been an ACK packet, sever thread is exiting").getBytes();
					try {
						send.send(new DatagramPacket(createErrorMessage((byte)4, emsg), 5 + emsg.length, address, TiD));	//send error
						System.out.println("Server sent error packet 4 with message: " + new String(emsg));
					} // end try
					catch (IOException e1) {
						System.err.println("IO Exception: " + e1.toString());
					} // end catch
					return;
				}
				else if (data[0] != (byte)0 || data[1] != (byte)4) {	//check for any other packet type
					byte emsg[] = ("Server has received an unidentified packet type, Server thread is exiting").getBytes();
					try {
						send.send(new DatagramPacket(createErrorMessage((byte)4, emsg), 5 + emsg.length, address, TiD));	//send error
						System.out.println("Server sent error packet 4 with message: " + new String(emsg));
					} // end try
					catch (IOException e1) {
						System.err.println("IO Exception: " + e1.toString());
					} // end catch
					return;
				}
				receivedPacket.setData(Arrays.copyOfRange(data, 0, 4));

				//calculate block number
				blockNum = 0;
				blockNum += (data[2] & 0xFF) * 256;
				blockNum += (data[3] & 0xFF);
				blockNum++;
								
				if(blockNum != prevBlockNum && blockNum != prevBlockNum+1){	//check if the block number makes sense for this point in the transfer
					byte emsg[] = ("The last TFTP packet received has a block number that doesn't make sense at this point in the transfer process, client thread is exiting").getBytes();
					try {
						send.send(new DatagramPacket(createErrorMessage((byte)4, emsg), 5 + emsg.length, address, TiD));	//send error
						System.out.println("Server sent error packet 4 with message: " + new String(emsg));
					} // end try
					catch (IOException e1) {
						System.err.println("IO Exception: " + e1.toString());
					} // end catch
					return;
				}
				
				System.out.println("Received ACK packet " + (blockNum-1) + " from the Client");
				
				if(blockNum == prevBlockNum){	//if received duplicate ACK packet, ignore it	
					sendPacket = false;
				}
				else{
					sendPacket = true;
					if(finalBlock && blockNum > finalBlockNum){		//if we've sent final data and received a new ACK, exit
						System.out.println("Server has received final ACK, shutting down");
						break;
					}

					fileData = new byte[0];	//data from file

					try{
						fileData = ReadFromFile(blockNum);	//read file
					} catch (FileNotFoundException e) {	//if file doesn't exist
						byte emsg[] = ("The file: " + fileName + " could not be located in the Server directory. Please ensure you are specifying the correct file name and try again.").getBytes();
						try {
							send.send(new DatagramPacket(createErrorMessage((byte)1, emsg), 5 + emsg.length, address, receivedPacket.getPort()));	//send error
						} // end try
						catch (IOException e1) {
							System.err.println("IO Exception: " + e1.toString());
						} // end catch
						System.out.println("Server sent error packet 1 with message: " + new String(emsg));
						return;
					} catch (IOException e) {
						System.out.println("IO Exception: " + e.toString());
						System.exit(0);
					}
					
					//set the read data and the initial 4 bytes
					readData = new byte[4 + fileData.length];
					readData[0] = (byte)0;
					readData[1] = (byte)3;
					readData[2] = (byte)((blockNum - (blockNum % 256))/256);
					readData[3] = (byte)(blockNum % 256);

					System.arraycopy(fileData, 0, readData, 4, fileData.length);

					sendData = new DatagramPacket(readData, fileData.length + 4, address, receivedPacket.getPort());
					
					//if the data is less than 512, we've reached end of file
					if(fileData.length < 512){
						finalBlockNum = blockNum;
						finalBlock = true;
					}
					prevBlockNum = blockNum;	//save our previous block number
				}
			}
		} // end if

		send.close();

	} // end method
	
	/**
	 * The following is the method used to write data to a file, done if server receives write request
	 * @param blockNum the block which is to be written
	 * @param writeData the data which is to be written
	 * @throws FileNotFoundException if the file cannot be found to right to, meaning we have an access violation
	 * @throws IOException if there is an issue with IO
	 * @throws FileAlreadyExistsException if file exists on first write
	 * 
	 * @since May 17 2014
	 * 
	 * Latest Change: close the FileOutputStream if there's an exception
	 * @version June 15 2014
	 * @author Colin
	 * 
	 */
	public void WriteToFile(int blockNum, byte[] writeData) throws FileNotFoundException, IOException, SyncFailedException, FileAlreadyExistsException
	{
		if(blockNum == 1){	//if first time writing
			if(new File(System.getProperty("user.dir") + "\\Server\\" + fileName).isFile()){	//check if file exists
				throw new FileAlreadyExistsException();	//if file exists, throw error
			}
		}
		FileOutputStream out = null;
		try{
			out = new FileOutputStream(System.getProperty("user.dir") + "\\Server\\" + fileName, (blockNum > 1) ? true : false);
			out.write(writeData, 0, writeData.length);
		}
		catch(Exception e){
			out.close();
			throw e;
		}
		out.close();
	}
	
	/**
	 * The following is the method to read data from a file, if server receives read request
	 * @param blockNum the block which is to be read
	 * @return the block of data which was read
	 * @throws FileNotFoundException if the file cannot be found
	 * @throws IOException if there is an issue with IO
	 * 
	 * @since May 17 2014
	 * 
	 * Latest Change: BufferedInputStream closes if there's an exception
	 * @version June 15 2014
	 * @author Colin
	 * 
	 */

	public byte[] ReadFromFile(int blockNum) throws FileNotFoundException, IOException
	{
		BufferedInputStream in = null;
		
		byte[] data = new byte[512];
		int i = 0;

		try{
		in = new BufferedInputStream(new FileInputStream(System.getProperty("user.dir") + "\\Server\\" + fileName));


		in.skip((blockNum-1)*512);

		if((i = in.read(data)) == -1){
			in.close();
			return new byte[0];
		}
		}
		catch(Exception e){
			in.close();
			throw e;
		}

		in.close();

		if (i < 512)
		{
			byte[] tempData = new byte[i];
			System.arraycopy(data, 0, tempData, 0, i);
			return tempData;
		}

		return data;

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
	
	/**
	 * Following method will print the error message from an error packet
	 * @param errorMsg the byte array containing the entirety of the message from the error packet
	 * @param length the length of the error message
	 * 
	 * @since May 30 2014
	 * 
	 * Latest Change: Cleaned function
	 * @version June 12 2014
	 * @author Colin
	 */
	private void printErrorMessage(byte[] errorMessage, int length){
		System.out.println("Server has received error packet from client: " + new String(Arrays.copyOfRange(errorMessage, 4, length - 1)));
	}
	
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
		System.out.println("PACKET INFORMATION:");
		System.out.println("Host: " + p.getAddress());
		System.out.println("Host port: " + p.getPort());
		System.out.println("Containing the following \nString: " + new String(p.getData()));
		System.out.println("Length of packet: " + p.getLength());
		System.out.println("Bytes: ");
		for (int i = 0; i < 4; i++) {
			System.out.print(p.getData()[i] & 0xFF);
		} // end forloop
		System.out.println("\n******************************************************");
		System.out.println("\n\n");
	} // end method

} // end class
