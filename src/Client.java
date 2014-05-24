import java.io.*;
import java.net.*;
import java.util.*;

/**
 * The following is implementation for the Client
 * 
 * @since May 11 2014
 * 
 * @author 1000000
 * @version May 21 2014
 *
 */
public class Client extends Thread {
	public static final byte zero = (byte)0;
	public static final byte one = (byte)1;
	public static final byte two = (byte)2;
	public static final int TIMEOUT = 3000;

	private DatagramSocket sendReceiveSocket; // Socket used to send and receive packets
	private DatagramPacket sendPacket, receivePacket, sendData; // Packets used to send an receive information
	private String filenameString, modeString; // filename will be user input, while mode will remain hardcoded for now
	private String directory;
	private byte filenameBytes[], modeBytes[];
	private byte[] ackNumber = new byte[2];
	private byte[] dataNumber = new byte[2];
	private int blockNum, ackNum; // the current datablock number and ack block number

	private byte message[];
	private Request req;

	/**
	 * The following is the constructor for the Client
	 * @param filename the file we are going to read from the server or write to on our side
	 * @param path the path of the file we are going to write to on our side or the path to place the file we are reading from the server
	 * @param req whether it is a read or write request
	 * 
	 * @since May 22 2014
	 * 
	 * Latest Change: Added implementation
	 * @version May 22 2014
	 * @author Kais
	 * 
	 */
	public Client(String filename, String path, Request req) {

		directory = path;
		filenameString = filename;
		this.req = req;

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
	 * @param rqstMsg a byte array which will have the message attached to the packet being sent
	 * 
	 * @since May 11 2014
	 * 
	 * Latest Change: Added implementation of reading and writing
	 * @version May 20 2014
	 * @author Kais
	 * 
	 */
	private void sendReceive(Request req)
	{		
		System.out.println("Client has started...");

		// send the packet to well-known port 68
		try {
			sendPacket = new DatagramPacket(message, message.length, InetAddress.getLocalHost(), 2068);
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
		boolean worked = false;
		int numberOfTimeouts = 0;
		while (!worked) {
			try {
				worked = true;
				System.out.println("Client receiving packet from intermediate...");
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
			if (numberOfTimeouts == 3) {
				System.out.println("Client has timed out 3 times waiting for the first packet back from server");
				return;
			} // end if
		} // end whileloop

		// prints out the information on the received packet
		printInformation(receivePacket);
		System.out.println("Client packet received..");
		System.out.println(req);

		if (req == Request.READ) { // if request was a read
			System.out.println("In Read");
			byte dat[] = receivePacket.getData();
			byte ack[] = new byte[4];
			dataNumber[0] = (byte)0;
			dataNumber[1] = (byte)1;
			blockNum = 1;
			ackNum = 1;
			while(true) { 
				try {
					if (verifydata(dataNumber, receivePacket))
						WriteToFile(blockNum, Arrays.copyOfRange(dat, 4, receivePacket.getLength()));// make sure if we receive a duplicate data packet, we only write the first one
					else {
						dataNumber[1]--;
						if(dataNumber[1] == 255) {
							dataNumber[0]--;
						} // end if
						ackNum--;
						blockNum--;
					} // end else
				} // end try 
				catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} // end catch 
				catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} // end catch
				
				ack[0] = (byte)0;
				ack[1] = (byte)4;
				ack[2] = (byte)((ackNum - (ackNum % 256))/256);
				ack[3] = (byte)(ackNum % 256);
				try {// create the acknowledge packet to send back to the client
					sendData = new DatagramPacket(ack, 4, InetAddress.getLocalHost(), receivePacket.getPort());
				} // end try
				catch (UnknownHostException uhe) {
					System.err.println("Unknown host exception error: " + uhe.getMessage());
				} // end catch
				worked = false;
				numberOfTimeouts = 0;
					try {
						sendReceiveSocket.send(sendData);
					} // end try
					catch (IOException ioe) {
						System.err.println("Unknown IO exception error: " + ioe.getMessage());
					} // end catch
					System.out.println("Sent ack packet");
					printInformation(sendData);
					if (receivePacket.getLength() < 516) {
						break;
					} // end if

					byte rly[] = new byte[516];
					receivePacket = new DatagramPacket(rly, rly.length);
				while(!worked) {
					try {
						worked = true;
						System.out.println("Client receiving packet from intermediate...");
						sendReceiveSocket.setSoTimeout(TIMEOUT);
						sendReceiveSocket.receive(receivePacket);
						printInformation(receivePacket);
					} // end try
					catch (SocketTimeoutException ste) {
						numberOfTimeouts++;
						worked = false;
					} // end catch
					catch (IOException ioe) {
						System.err.println("IO Exception error: " + ioe.getMessage());
						worked = false;
					} // end catch
					if (numberOfTimeouts == 5) {
						System.out.println("Client has timed out 3 times waiting for the next data packet from server");
						return;
					}
					if (worked)
						dat = rly; // new dat will be the data from the packet just received
				} // end whileloop
				dataNumber[1]++;
				if(dataNumber[1] == 0) {
					dataNumber[0]++;
				}
				ackNum++; // next ack
				blockNum++; // next block
			} // end whileloop
		} // end if
		else if (req == Request.WRITE) { // if request was a write
			ackNumber[0] = (byte)0;
			ackNumber[1] = (byte)0;
			blockNum = 1;
			ackNum = 0;
			while(/*verifyack(ackNumber, receivePacket)*/ true) {
				byte[] fileData = new byte[0];
				if (verifyack(ackNumber, receivePacket)) {
				/*	ackNumber[1]--;
					if(ackNumber[1] == 255) {
						ackNumber[0]--;
					}
					blockNum--;
				}*/
				try {
					fileData = ReadFromFile(blockNum);
				} // end try
				catch (FileNotFoundException e) {
					System.out.println("File Not Found: " + e.toString());
					System.exit(0);
				} // end catch 
				catch (IOException e) {
					System.out.println("IO Exception: " + e.toString());
					System.exit(0);
				} // end catch

				byte data[] = new byte[4 + fileData.length];
				data[0] = (byte)0;
				data[1] = (byte)3;
				data[2] = (byte)((blockNum - (blockNum % 256))/256);
				data[3] = (byte)(blockNum % 256);

				if (fileData.length == 0) {
					break;
				} // end if

				System.arraycopy(fileData, 0, data, 4, fileData.length);

				try {
					sendData = new DatagramPacket(data, fileData.length + 4, InetAddress.getLocalHost(), receivePacket.getPort());
				} // end try
				catch (UnknownHostException uhe) {
					System.err.println("Unknown host exception error: " + uhe.getMessage());
				} // end catch
				System.out.println("created the following data packet to send off");
				printInformation(sendData);
				}
				else {
					ackNumber[1]--;
					if(ackNumber[1] == 255) {
						ackNumber[0]--;
					}
					blockNum--;
				}
				worked = false;
				numberOfTimeouts = 0;
				
				while(!worked) {
				try {
					sendReceiveSocket.send(sendData);
				} // end try
				catch (IOException ioe) {
					System.err.println("Unknown IO exception error: " + ioe.getMessage());
				} // end catch

				byte reply[] = new byte[4];
				receivePacket = new DatagramPacket(reply, reply.length);

				try {
					worked = true;
					System.out.println("Client receiving packet from intermediate...");
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
				if (numberOfTimeouts == 5) {
					System.out.println("Client has timed out 3 times waiting for the next ack packet from server");
					return;
				}
				} // end whileloop
				ackNumber[1]++;
				if(ackNumber[1] == 0) {
					ackNumber[0]++;
				}
				blockNum++;
				System.out.println("Client just received ack packet from server");
				printInformation(receivePacket);
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

		BufferedInputStream in = new BufferedInputStream(new FileInputStream(directory + "\\" + filenameString));

		byte[] data = new byte[512];
		int i = 0;

		in.skip((blockNum-1)*512);

		if ((i = in.read(data)) == -1){
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
		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(directory + "\\output" + filenameString, (blockNum > 1) ? true : false));
		System.out.println("write datas length " + writeData.length);
		out.write(writeData, 0, writeData.length);
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

	public void run() {
		sendReceive(req);
	}
} // end class