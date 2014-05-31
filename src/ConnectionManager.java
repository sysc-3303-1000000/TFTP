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
	public static final int DATA_SIZE = 516;
	public static final int TIMEOUT = 3000;

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

		if (req == Request.WRITE) { // done by Kais to keep alive until full write is serviced, needs testing
			// TODO write to file
			// form the write Acknowledge block
			System.out.println("writeAck");
			int blockNum = 0; // we are servicing a write, so first block number is 0
			int prevBlockNum = 0;
			int numberOfTimeouts = 0;
			boolean worked = false;
			byte writeAck[] = new byte[4];
			writeAck[0] = (byte)0;
			writeAck[1] = (byte)4;
			writeAck[2] = (byte)0;
			writeAck[3] = (byte)0;
			// send the first ack packet
			try {// create the acknowledge packet to send back to the client
				sendData = new DatagramPacket(writeAck, 4, InetAddress.getLocalHost(), port);
			} // end try
			catch (UnknownHostException uhe) {
				System.err.println("Unknown host exception error: " + uhe.getMessage());
			} // end catch
			try { // send response back
				send.send(sendData);
			} // end try
			catch (IOException ioe) {
				System.err.println("Unknown IO exception error: " + ioe.getMessage());
			} // end catch
			System.out.println("Server sent first ack for write back to ErrorSim");
			// while we receive data packets that are 516 in size (break inside while)
			while (true) {
				byte data[] = new byte[DATA_SIZE];
				receivedPacket = new DatagramPacket(data, data.length);
				numberOfTimeouts = 0;
				// wait to receive the data packet
				worked = false;
				while(!worked){
					try {
						System.out.println("Server receiving packet from intermediate...");
						send.setSoTimeout(TIMEOUT);
						send.receive(receivedPacket);
						worked = true;
					} // end try
					catch(SocketTimeoutException ste){
						numberOfTimeouts++;
					}
					catch (IOException ioe) {
						System.err.println("IO Exception error: " + ioe.getMessage());
					} // end catch
					if(numberOfTimeouts == 5){
						System.out.println("Server has timed out 5 times waiting for the next data packet from client");
						return;
					}
					if (worked && receivedPacket.getData()[1] == (byte)5) {
						printErrorMessage(receivedPacket.getData());
						return;
					} // end if
				}
				blockNum = 0;
				blockNum += (data[2]) * 256;
				blockNum += data[3];
				
				System.out.println("received blocknum: " + blockNum + " previous blocknum: " + prevBlockNum);

				if(blockNum != prevBlockNum){
					try {
						WriteToFile(blockNum, Arrays.copyOfRange(data, 4, receivedPacket.getLength())); // write the data
					} catch (FileNotFoundException e) {
						byte emsg[] = ("The file: " + fileName + " could not be written to the Server directory. Please ensure the server has write permission to the Server directory.").getBytes();
						try {
							send.send(new DatagramPacket(createErrorMessage((byte)2, emsg), 5 + emsg.length, InetAddress.getLocalHost(), receivedPacket.getPort()));
						} // end try
						catch (UnknownHostException e1) {
							System.err.println("Unknown Host: " + e1.toString());
						} // end catch
						catch (IOException e1) {
							System.err.println("IO Exception: " + e1.toString());
						} // end catch
						System.out.println("Server sent error packet 2");
						return;
					} catch (SyncFailedException sfe) { // respond with error packet 0503_0 at this point, then terminate client thread
						byte emsg[] = ("The file: " + fileName + " could not be written in the Server directory. Please ensure the server has write permission to the Server directory.").getBytes();
						try {
							send.send(new DatagramPacket(createErrorMessage((byte)3, emsg), 5 + emsg.length, InetAddress.getLocalHost(), receivedPacket.getPort()));
						} // end try
						catch (UnknownHostException e1) {
							System.err.println("Unknown Host: " + e1.toString());
						} // end catch
						catch (IOException e1) {
							System.err.println("IO Exception: " + e1.toString());
						} // end catch
						System.out.println("Server sent error packet 3");
						return;
					} // end catch
					catch(FileAlreadyExistsException f){
						byte emsg[] = ("The file: " + fileName + " already exists on the server, please specify a new file name.").getBytes();
						try {
							send.send(new DatagramPacket(createErrorMessage((byte)6, emsg), 5 + emsg.length, InetAddress.getLocalHost(), receivedPacket.getPort()));
						} // end try
						catch (UnknownHostException e1) {
							System.err.println("Unknown Host: " + e1.toString());
						} // end catch
						catch (IOException e1) {
							System.err.println("IO Exception: " + e1.toString());
						} // end catch
						System.out.println("Server sent error packet 6");
						return;
					}
					catch (IOException e) {
						System.out.println("IO Exception: " + e.toString());
						System.exit(0);
					}
				}
				// form the ack packet based on blockNumber
				writeAck[0] = (byte)0;
				writeAck[1] = (byte)4;
				writeAck[2] = (byte)((blockNum - (blockNum % 256))/256);
				writeAck[3] = (byte)(blockNum % 256);
				try {// create the acknowledge packet to send back to the client
					sendData = new DatagramPacket(writeAck, 4, InetAddress.getLocalHost(), receivedPacket.getPort()); // we are going to send this packet to the connectionManagerESim thread
				} // end try
				catch (UnknownHostException uhe) {
					System.err.println("Unknown host exception error: " + uhe.getMessage());
				} // end catch
				try { // send response back
					send.send(sendData);
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

		else if (req == Request.READ) {
			// form the read block
			System.out.println("readData");
			byte readData[] = new byte[516];
			int blockNum = 0;
			int finalBlockNum = 0;
			int prevBlockNum = 0;
			int numberOfTimeouts = 0;
			boolean finalBlock = false;
			boolean worked = false;
			boolean resend = true;
			readData[0] = (byte)0;
			readData[1] = (byte)3;
			readData[2] = (byte)0;
			readData[3] = (byte)1;

			byte[] fileData = new byte[0];

			try {
				fileData = ReadFromFile(1);
			} catch (FileNotFoundException e) {
				byte emsg[] = ("The file: " + fileName + " could not be located in the Server directory. Please ensure you are specifying the correct file name and try again.").getBytes();
				try {
					send.send(new DatagramPacket(createErrorMessage((byte)1, emsg), 5 + emsg.length, InetAddress.getLocalHost(), port));
				} // end try
				catch (UnknownHostException e1) {
					System.err.println("Unknown Host: " + e1.toString());
				} // end catch
				catch (IOException e1) {
					System.err.println("IO Exception: " + e1.toString());
				} // end catch
				System.out.println("Server sent error packet 1");
				return;
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
			

			//printInformation(sendData);

			while(true){
				byte data[] = new byte[DATA_SIZE];
				receivedPacket = new DatagramPacket(data, data.length);
				numberOfTimeouts = 0;
				worked = false;
				while(!worked){
					if(resend){
						try { // send response back
							send.send(sendData);
						} // end try
						catch (IOException ioe) {
							System.err.println("Unknown IO exception error: " + ioe.getMessage());
						} // end catch
					}
					try{
						System.out.println("Server receiving packet from intermediate...");
						send.setSoTimeout(TIMEOUT);
						send.receive(receivedPacket);
						worked = true;
					} catch(SocketTimeoutException ste){
						numberOfTimeouts++;
						System.out.println("timeout");
					} catch(IOException ioe) {
						System.err.println("IO Exception error: " + ioe.getMessage());
					} // end catch
					if (worked && receivedPacket.getData()[1] == (byte)5) {
						printErrorMessage(receivedPacket.getData());
						return;
					} // end if
					if(numberOfTimeouts == 5){
						System.out.println("Server has timed out 5 times waiting for the next data packet from client");
						return;
					}
				}
				
				receivedPacket.setData(Arrays.copyOfRange(data, 0, 4));

				System.out.println("Received packet from intermediate...");
				printInformation(receivedPacket);

				blockNum = 0;
				blockNum += (data[2]) * 256;
				blockNum += data[3];

				blockNum++;
				System.out.println("blocknum: " + blockNum + "prevblock: " + prevBlockNum);
				if(blockNum == prevBlockNum){		
					resend = false;
				}
				else{
					resend = true;
					if(finalBlock && blockNum > finalBlockNum){
						System.out.println("Exiting, blocknum: " + blockNum);
						break;
					}

					fileData = new byte[0];

					try{
						fileData = ReadFromFile(blockNum);
					} catch (FileNotFoundException e) {
						byte emsg[] = ("The file: " + fileName + " could not be located in the Server directory. Please ensure you are specifying the correct file name and try again.").getBytes();
						try {
							send.send(new DatagramPacket(createErrorMessage((byte)1, emsg), 5 + emsg.length, InetAddress.getLocalHost(), receivedPacket.getPort()));
						} // end try
						catch (UnknownHostException e1) {
							System.err.println("Unknown Host: " + e1.toString());
						} // end catch
						catch (IOException e1) {
							System.err.println("IO Exception: " + e1.toString());
						} // end catch
						System.out.println("Server sent error packet 1");
						return;
					} catch (IOException e) {
						System.out.println("IO Exception: " + e.toString());
						System.exit(0);
					}

					readData = new byte[4 + fileData.length];
					readData[0] = (byte)0;
					readData[1] = (byte)3;
					readData[2] = (byte)((blockNum - (blockNum % 256))/256);
					readData[3] = (byte)(blockNum % 256);

					System.arraycopy(fileData, 0, readData, 4, fileData.length);

					try { // create the data packet to send back to the client
						sendData = new DatagramPacket(readData, fileData.length + 4, InetAddress.getLocalHost(), receivedPacket.getPort());
					} // end try
					catch (UnknownHostException uhe) {
						System.err.println("Unknown host exception error: " + uhe.getMessage());
					} // end catch
					
					if(fileData.length < 512){
						System.out.println("Setting finalBlockNum: " + blockNum);
						finalBlockNum = blockNum;
						finalBlock = true;
					}
					prevBlockNum = blockNum;
					System.out.println("prev block num after set: " + prevBlockNum + " block num: " + blockNum);
				}
			}
		} // end if

		send.close();

	} // end method

	public void WriteToFile(int blockNum, byte[] writeData) throws FileNotFoundException, IOException, SyncFailedException, FileAlreadyExistsException
	{
		if(blockNum == 1){
			if(new File(System.getProperty("user.dir") + "\\Server\\output" + fileName).isFile()){
				throw new FileAlreadyExistsException();
			}
		}
		System.out.println("");
		FileOutputStream out = new FileOutputStream(System.getProperty("user.dir") + "\\Server\\output" + fileName, (blockNum > 1) ? true : false);
		out.write(writeData, 0, writeData.length);
		out.getFD().sync();
		out.close();
	}

	public byte[] ReadFromFile(int blockNum) throws FileNotFoundException, IOException
	{
		BufferedInputStream in = new BufferedInputStream(new FileInputStream(System.getProperty("user.dir") + "\\Server\\" + fileName));

		byte[] data = new byte[512];
		int i = 0;

		in.skip((blockNum-1)*512);

		if((i = in.read(data)) == -1){
			in.close();
			return new byte[0];
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
	
	private void printErrorMessage(byte[] errorMessage){
		System.out.println("Server has received error packet from client: " + new String(Arrays.copyOfRange(errorMessage, 4, errorMessage.length - 4)));
	}

	private void printInformation(DatagramPacket p) {

		// print out the information on the packet
		System.out.println("PACKET INFORMATION:");
		System.out.println("Host: " + p.getAddress());
		System.out.println("Host port: " + p.getPort());
		System.out.println("Containing the following \nString: " + new String(p.getData()));
		System.out.println("Length of packet: " + p.getLength());
		System.out.println("Bytes: ");
		for (int i = 0; i < p.getLength(); i++) {
			System.out.print(Integer.toHexString(p.getData()[i]));
		} // end forloop
		System.out.println("\n******************************************************");
		System.out.println("\n\n");
	} // end method

} // end class
