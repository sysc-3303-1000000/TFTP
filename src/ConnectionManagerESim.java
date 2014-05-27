import java.io.IOException;
import java.net.*;

/**
 * The following is implementation for the ConnectionManagerESim which will form
 * a packet with the message it receives from listener and send it to the server.
 * It will receive a packet back and send this to the client.
 * 
 * @since May 16 2014
 * 
 * @author 1000000
 * @version May 23 2014
 *
 */
public class ConnectionManagerESim extends Thread {
	public static final int DATA_SIZE = 516;
	public static final int PERCENTPASS = 9;	//int value 0-10 (0 being 100 fail rate)
	public static final long TIMEOUT = 3000;	//int value in miliseconds 
	
	private int serverPort = 69; // the server port will be initiated to 69 and will change according to the thread needed 
	private DatagramSocket serverSocket, clientSocket; // socket deceleration for all three required sockets 
	private DatagramPacket sendClientPacket, receiveClientPacket, receiveServerPacket, sendServerPacket; // packet deceleration for all packets being sent and received for both client and server
	private boolean verbose;
	private byte data[];
	private int clientPort;
	private int length;
	private int mode;	// will have the value of the current error simulation mode 
	private int delay; // will store the amount of delay if we are running in delayed mode
	private int packetType; // the type of packet we are changing
	private int packetNumber; // the packet number that we are changing
	private Request requestType;
	private boolean lastPacketWrite = false;
	private boolean lastPacketRead = false;
	private boolean firstPacket = true;
	private boolean end = false;
	private boolean isLost = false; // this value will change to true when we find the first packet that we want to lose
	byte rly[] = new byte[DATA_SIZE]; // this will store the reply from the client
	byte response[] = new byte[DATA_SIZE]; // this will store the response from the server
	/**
	 * The following is the constructor for ListenerESim
	 * @param verbose whether verbose mode is enabled
	 * @param data the message which will be sent to the server
	 * @param port the port which the message from the server will be sent to
	 * @param length the length of the data packet
	 * 
	 * @since May 16 2014
	 * 
	 * Latest Change: Added different modes
	 * @version May 21 2014
	 * @author Mohammed Ahmed-Muhsin & Samson Truong
	 * 
	 */
	public ConnectionManagerESim(boolean verbose, int userChoice, int delay, int packetType, int packetNumber, byte[] data, int port, int length, Request requestType) {
		// initialize the variables
		this.verbose = verbose;
		this.data = data;
		this.clientPort = port;
		this.length = length;
		this.mode = userChoice;
		this.requestType = requestType;
		this.delay = delay;
		this.packetType = packetType;
		this.packetNumber = packetNumber;
		
		try {
			serverSocket = new DatagramSocket();
		} // end try 
		catch (SocketException se) {
			System.err.println("SocketException: " + se.getMessage());
		} // end catch
		
		// initialize the DatagramSocket sendSocket
		try {
			clientSocket = new DatagramSocket();
		} // end try 
		catch (SocketException se) {
			System.err.println("SocketException: " + se.getMessage());
		} // end catch
		
		System.out.println("ConnectionManagerESim: Thread started to service request!");
	} // end constructor
	
	/**
	 * The following method will be called when trying to print out information about a specific packet
	 * @param p the information displayed desired for this packet
	 * 
	 * @since May 16 2014
	 * 
	 * Latest Change: Added better formatting to the information being printed
	 * @version May 21 2014
	 * @author Mohammed Ahmed-Muhsin & Samson Truong
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
		for (int i = 0; i < p.getLength(); i++) {
			System.out.print(Integer.toHexString(p.getData()[i]));
		} // end forloop
		System.out.println("\n******************************************************");
		System.out.println("\n\n");
	} // end method
	
	/**
	 * The following is the run method for ConnectionManagerESim, used to execute code upon starting the thread.
	 * It will pass an receive packets between the errorsim and server and the server and the client.
	 * 
	 * @since May 16 2014
	 * 
	 * Latest Change: added the loop to keep the thread alive until exit
	 * @version May 24 2014
	 * @author Samson Truong & Mohammed Ahmed-Muhsin 
	 * 
	 */
	public void run() {
		
		while (!end) {
			if (mode == 0)
			{
				end = normalOp();	//Runs the Error Sim normally (ie. No errors)
			}
			else if (mode == 1)
			{
				end = lostOp();
			}
			else if (mode == 2)
			{
				end = delayedOp();
			}
			else if (mode == 3) {
				end = duplicatedOp();
			}
		}//end while
		
		// begin closing operations
		System.out.println("ConnectionManagerESim: ErrorSim is now closing its sockets");
		serverSocket.close();
		clientSocket.close();
	} // end method
	
	/**
	 * The following is the run method for ConnectionManagerESim, used to execute code upon starting the thread.
	 * It will pass an receive packets between the errorsim and server and the server and the client.
	 * 
	 * @since May 21 2014
	 * 
	 * Latest Change: Cleaned up the normal operation send and receive as well as changed the server port to work as expected
	 * @version May 21 2014
	 * @author Samson Truong & Mohammed Ahmed-Muhsin 
	 */
	private boolean normalOp() 
	{
		
		if (verbose)
			System.out.println("ConnectionManagerESim: Running Normal Operation\n");

		// this is not the first packet, we need to wait for the client to send back to us
		if (!firstPacket) {
			clientReceive();

		}//end if

		serverSend();
		if (lastPacketRead == true)	
		{
			return true;	// Last packet is now sent. The thread will close
		}
		if (requestType == Request.WRITE && !firstPacket)
		{
			if(sendServerPacket.getLength() < DATA_SIZE)
			{
				lastPacketWrite = true;	
			}
		}

		//*********************************************************************************

		serverReceive();

		clientSend();
		firstPacket = false;		// any following packets the connection manager receives will be not the second packet
		if (lastPacketWrite == true)
		{
			return true;	// Last packet is now sent. The thread will close
		}
		if (requestType == Request.READ && !firstPacket)	
		{
			if(sendClientPacket.getLength() < DATA_SIZE)
			{
				lastPacketRead = true;
			}
		}
			
			return false;
	}
	
	/**
	 * The following is the run method for ConnectionManagerESim with lost packets, used to execute code upon starting the thread.
	 * It will pass an receive packets between the errorsim and server and the server and the client.
	 * 
	 * @since May 21 2014
	 * 
	 * Latest Change: Run with lost packets method added, full implementation added
	 * @version May 24 2014
	 * @author Samson Truong & Mohammed Ahmed-Muhsin 
	 * 
	 */
	private boolean lostOp()
	{
		// this is not the first packet, we need to wait for the client to send back to us
		if (!firstPacket) {
			if (verbose)
				System.out.println("ConnectionManagerESim: Waiting to receive packet from client");

			byte rly[] = new byte[DATA_SIZE];
			receiveClientPacket = new DatagramPacket(rly, rly.length);
			try { // wait to receive client packet
				clientSocket.receive(receiveClientPacket);
			}//end try 
			catch (IOException ie) {
				System.err.println("IOException error: " + ie.getMessage());
			}//end catch

			System.out.println("ConnectionManagerESim: Received packet from client");
			printInformation(receiveClientPacket);
			// updating the data and length in the packet being sent to the server
			data = receiveClientPacket.getData();
			length = receiveClientPacket.getLength();
			// check to see if this is the packet that we want to lose
			if (!isLost && foundPacket(receiveClientPacket)) {
				if (verbose) {
					System.out.println("ConnectionManagerESim: simulating a lost packet");
					printInformation(receiveClientPacket);
				}
					
				isLost = true;
				return false;
			}
		}//end if

		System.out.println("ConnectionManageESim: Received packet from client. Preparing packet to send to Server");
		

		// prepare the new send packet to the server
		try {
			sendServerPacket = new DatagramPacket(data, length, InetAddress.getLocalHost(), serverPort);
		} // end try 
		catch (UnknownHostException uhe) {
			System.err.println("Unknown host exception error: " + uhe.getMessage());
		} // end catch

		if(verbose)
			printInformation(sendServerPacket);

		// send the packet to the server via the send/receive socket to server port
		try {
			serverSocket.send(sendServerPacket);
		} // end try 
		catch (IOException ioe) {
			System.err.println("Unknown IO exception error: " + ioe.getMessage());
		} // end catch

		// print confirmation message that the packet has been sent to the server
		System.out.println("Packet sent to server");
		if (lastPacketRead == true)	
		{
			return true;	// Last packet is now sent. The thread will close
		}
		if (requestType == Request.WRITE && !firstPacket)
		{
			if(sendServerPacket.getLength() < DATA_SIZE)
			{
				lastPacketWrite = true;	
			}
		
		}

		//*********************************************************************************

		byte response[] = new byte[DATA_SIZE];

		receiveServerPacket = new DatagramPacket(response, response.length);

		System.out.println("******************************************************");
		System.out.println("ConnectrionManagerESim: waiting to receive a packet from server...\n");

		// block until you receive a packet from the server
		try {
			serverSocket.receive(receiveServerPacket);
		} // end try 
		catch (IOException ioe) {
			System.err.println("Unknown IO exception error: " + ioe.getMessage());
		} // end catch

		// check to see if this is the packet that we want to lose
		if (!isLost && foundPacket(receiveServerPacket)) {
			if (verbose) {
				System.out.println("ConnectionManagerESim: simulating a lost packet");
				printInformation(receiveServerPacket);
			}
			isLost = true;
			return false;
		}
		
		response = receiveServerPacket.getData();
		if(verbose) // print out information about the packet received from the server if verbose
			printInformation(receiveServerPacket);

		// set the serverPort to the port we have just received it from (meaning to the Server Thread that will deal with this request
		serverPort = receiveServerPacket.getPort();

		// prepare the new send packet to the client
		try {
			sendClientPacket = new DatagramPacket(response, receiveServerPacket.getLength(), InetAddress.getLocalHost(), clientPort);
		} // end try
		catch (UnknownHostException uhe) {
			uhe.printStackTrace();
			System.exit(1);
		} // end catch
		System.out.println("ErrorSim will attempt to send response back to client...\n");

		if(verbose) // print out information about the packet being sent to the client
			printInformation(sendClientPacket);

		// send the packet to the client via the send socket 
		try {
			clientSocket.send(sendClientPacket);

		} // end try 
		catch (IOException ioe) {
			System.err.println("Unknown IO exception error: " + ioe.getMessage());
		} // end catch

		// print confirmation message that the packet has been sent to the client
		System.out.println("Response packet sent to client");
		firstPacket = false;		// any following packets the connection manager receives will be not the second packet
		if (lastPacketWrite == true)
		{
			return true;	// Last packet is now sent. The thread will close
		}
		if (requestType == Request.READ && !firstPacket)	
		{
			if(sendClientPacket.getLength() < DATA_SIZE)
			{
				lastPacketRead = true;
			}
		}
		return false;
	}
	
	/**
	 * The following is the run method for ConnectionManagerESim with delayed packet, used to execute code upon starting the thread. 
	 * 
	 * @since May 21 2014
	 * 
	 * Latest Change: Increased possible delay and made it to return the boolean value
	 * @version May 24 2014
	 * @author Samson Truong & Mohammed Ahmed-Muhsin 
	 */
	private boolean delayedOp()
	{
		double rando = randoNum();	//Random number from 1-10
		if(rando <= PERCENTPASS)	//Check if the Random number is less than the percent pass
		{
			System.out.println("Delayed packet will not be simulated, normal operation will continue");
			return normalOp();	//Normal forward operation
		}
		else
		{
			System.out.println("Packet will be delayed. Thread will sleep now");
			try {
                Thread.sleep(delay);	//delays the packet for the specified amount
            } catch (InterruptedException e)
            {}
			System.out.println("*************Packet was delayed by: " + delay + "ms!************");
			System.out.println("Normal operation will continue now");
			return normalOp();
		}
	}
	
	/**
	 * The following is the run method for ConnectionManagerESim with a duplicated packet, used to execute code upon starting the thread. 
	 * We will duplicate if rando is less than the PERCENTPASS, but the difference is that we will alternate from duplicating the packets being sent to the client 
	 * or we will duplicate sending to the server. This is done by generating another random number to determine if we duplicate server or client
	 * 
	 * If random number for duplicate <= 5: duplicate server packets
	 * If random number > 5: duplicate client packets
	 * @since May 21 2014
	 * 
	 * Latest Change: Run with Duplicated packets method added, full implementation added
	 * @version May 21 2014
	 * @author Mohammed Ahmed-Muhsin & Samson Truong  
	 */
	private boolean duplicatedOp()
	{
		double rando = randoNum();	//Random number from 1-10
		if(rando <= PERCENTPASS) {	//Check if the Random number is less than the percent pass
			// normal operation
			System.out.println("Duplicate packet will not be simulated, normal operation will continue");
			return normalOp();
		}
		else {
			System.out.println("Duplicate packet will be simulated");
			// generate a random number to see if we duplicate client or server
			double duplicate = randoNum(); // Random number from 1-10;
			// this is not the first packet, we need to wait for the client to send back to us
			if (!firstPacket) {
				byte rly[] = new byte[DATA_SIZE];
				receiveClientPacket = new DatagramPacket(rly, rly.length);
				try { // wait to receive client packet
					clientSocket.receive(receiveClientPacket);
				}//end try 
				catch (IOException ie) {
					System.err.println("IOException error: " + ie.getMessage());
				}//end catch

				System.out.println("ConnectionManagerESim: Received packet from client");
				printInformation(receiveClientPacket);
				// updating the data and length in the packet being sent to the server
				data = receiveClientPacket.getData();
				length = receiveClientPacket.getLength();

			}//end if

			System.out.println("ConnectionManageESim: Received packet from client. Preparing packet to send to Server");
			// prepare the new send packet to the server
			try {
				sendServerPacket = new DatagramPacket(data, length, InetAddress.getLocalHost(), serverPort);
			} // end try 
			catch (UnknownHostException uhe) {
				System.err.println("Unknown host exception error: " + uhe.getMessage());
			} // end catch

			if(verbose)
				printInformation(sendServerPacket);

			// send the packet to the server TWICE via the send/receive socket to server port
			try {
				serverSocket.send(sendServerPacket);
				
				// if the number generated for duplicate is <= 5, send the server packet twice 
				if (duplicate <= 10) {
					System.out.println("The packet being sent to the server will be duplicated");
					try {
						Thread.sleep(delay);
					} catch (InterruptedException e) {}
					serverSocket.send(sendServerPacket);				
				} // end try 
			} catch (IOException ioe) {
				System.err.println("Unknown IO exception error: " + ioe.getMessage());
			} // end catch

			// print confirmation message that the packet has been sent to the server
			System.out.println("Packet sent to server");
			if (lastPacketRead)	
			{
				return true;	// Last packet is now sent. The thread will close
			}
			if (requestType == Request.WRITE && !firstPacket)
			{
				if(sendServerPacket.getLength() < DATA_SIZE)
				{
					lastPacketWrite = true;	
				}
			}

			//*********************************************************************************

			byte response[] = new byte[DATA_SIZE];

			receiveServerPacket = new DatagramPacket(response, response.length);

			System.out.println("******************************************************");
			System.out.println("ConnectrionManagerESim: waiting to receive a packet from server...\n");

			// block until you receive a packet from the server
			try {
				serverSocket.receive(receiveServerPacket);
			} // end try 
			catch (IOException ioe) {
				System.err.println("Unknown IO exception error: " + ioe.getMessage());
			} // end catch

			response = receiveServerPacket.getData();
			if(verbose) // print out information about the packet received from the server if verbose
				printInformation(receiveServerPacket);

			// set the serverPort to the port we have just received it from (meaning to the Server Thread that will deal with this request
			serverPort = receiveServerPacket.getPort();

			// prepare the new send packet to the client
			try {
				sendClientPacket = new DatagramPacket(response, receiveServerPacket.getLength(), InetAddress.getLocalHost(), clientPort);
			} // end try
			catch (UnknownHostException uhe) {
				uhe.printStackTrace();
				System.exit(1);
			} // end catch
			System.out.println("ErrorSim will attempt to send response back to client...\n");

			if(verbose) // print out information about the packet being sent to the client
				printInformation(sendClientPacket);

			// send the packet to the client via the send socket 
			try {
				clientSocket.send(sendClientPacket);

				// if the number generated for duplicate is > 5, send the client packet twice 
				/*if (duplicate > 5) {
					System.out.println("The packet being sent to the client will be duplicated");
					try {
						Thread.sleep(delay);
					} catch (InterruptedException e) {}
					serverSocket.send(sendClientPacket);				
				} // end try */
			} catch (IOException ioe) {
				System.err.println("Unknown IO exception error: " + ioe.getMessage());
			}

			// print confirmation message that the packet has been sent to the client
			System.out.println("Response packet sent to client");
			firstPacket = false;		// any following packets the connection manager receives will be not the second packet
			if (lastPacketWrite == true)
			{
				return true;	// Last packet is now sent. The thread will close
			}
			if (requestType == Request.READ && !firstPacket)	
			{
				if(sendClientPacket.getLength() < DATA_SIZE)
				{
					lastPacketRead = true;
				}
			}

			return false;
		}
	}

	/**
	 * The following will be the method to RECEIVE CLIENT PACKETS
	 * 
	 * @since May 27 2014
	 * 
	 * Latest Change: 
	 * @version May 27 2014
	 * @author Samson Truong & Mohammed Ahmed-Muhsin 
	 */	
	private void clientReceive() {
		if (verbose)
			System.out.println("ConnectionManagerESim: Waiting to receive packet from client");

		receiveClientPacket = new DatagramPacket(rly, rly.length);
		try { // wait to receive client packet
			clientSocket.receive(receiveClientPacket);
		}//end try 
		catch (IOException ie) {
			System.err.println("IOException error: " + ie.getMessage());
		}//end catch

		System.out.println("ConnectionManagerESim: Received packet from client");
		printInformation(receiveClientPacket);
		// updating the data and length in the packet being sent to the server
		data = receiveClientPacket.getData();
		length = receiveClientPacket.getLength();
	}
	
	/**
	 * The following will be the method to SEND CLIENT PACKETS
	 * 
	 * @since May 27 2014
	 * 
	 * Latest Change: 
	 * @version May 27 2014
	 * @author Samson Truong & Mohammed Ahmed-Muhsin 
	 */	
	private void clientSend(){
		
		if (verbose)
			System.out.println("ConnetionManagerESim: Preparing packet to send to Client");
		
		// prepare the new send packet to the client
		try {
			sendClientPacket = new DatagramPacket(response, receiveServerPacket.getLength(), InetAddress.getLocalHost(), clientPort);
		} // end try
		catch (UnknownHostException uhe) {
			uhe.printStackTrace();
			System.exit(1);
		} // end catch

		if(verbose) // print out information about the packet being sent to the client
			printInformation(sendClientPacket);

		// send the packet to the client via the send socket 
		try {
			clientSocket.send(sendClientPacket);

		} // end try 
		catch (IOException ioe) {
			System.err.println("Unknown IO exception error: " + ioe.getMessage());
		} // end catch

		// print confirmation message that the packet has been sent to the client
		if (verbose)
			System.out.println("Response packet sent to client");
	}
	
	/**
	 * The following will be the method to RECEIVE SERVER PACKETS
	 * 
	 * @since May 27 2014
	 * 
	 * Latest Change: 
	 * @version May 27 2014
	 * @author Samson Truong & Mohammed Ahmed-Muhsin 
	 */	
	private void serverReceive(){
		
		if (verbose)
			System.out.println("ConnectrionManagerESim: Waiting to receive a packet from server...\n");
		
		receiveServerPacket = new DatagramPacket(response, response.length);

		// block until you receive a packet from the server
		try {
			serverSocket.receive(receiveServerPacket);
		} // end try 
		catch (IOException ioe) {
			System.err.println("Unknown IO exception error: " + ioe.getMessage());
		} // end catch

		response = receiveServerPacket.getData();
		
		if(verbose) // print out information about the packet received from the server if verbose
			printInformation(receiveServerPacket);

		// set the serverPort to the port we have just received it from (meaning to the Server Thread that will deal with this request
		serverPort = receiveServerPacket.getPort();

	}
		
	/**
	 * The following will be the method to SEND SERVER PACKETS
	 * 
	 * @since May 27 2014
	 * 
	 * Latest Change: 
	 * @version May 27 2014
	 * @author Samson Truong & Mohammed Ahmed-Muhsin 
	 */	
	private void serverSend() {
		if (verbose)
			System.out.println("ConnectionManageESim: Preparing packet to send to Server");
		// prepare the new send packet to the server
		try {
			sendServerPacket = new DatagramPacket(data, length, InetAddress.getLocalHost(), serverPort);
		} // end try 
		catch (UnknownHostException uhe) {
			System.err.println("Unknown host exception error: " + uhe.getMessage());
		} // end catch

		if(verbose)
			printInformation(sendServerPacket);

		// send the packet to the server via the send/receive socket to server port
		try {
			serverSocket.send(sendServerPacket);
		} // end try 
		catch (IOException ioe) {
			System.err.println("Unknown IO exception error: " + ioe.getMessage());
		} // end catch

		// print confirmation message that the packet has been sent to the server
		if (verbose)
			System.out.println("Packet sent to server");
	}
	
		
	/**
	 * The following is a method for a random number generator from 1-10 used to randomly generate errors
	 * 
	 * @since May 21 2014
	 * 
	 * Latest Change: randoNum method added, full implementation added
	 * @version May 21 2014
	 * @author Samson Truong & Mohammed Ahmed-Muhsin 
	 */
	private double randoNum()
	{
		return Math.random()*10;
	}
	
	/**
	 * This method will check whether or not this is a DATA or ACK packet. 
	 * The return will be used to keep track of which of the packets we actually want to change
	 * @param p the DatagramPacket that we are looking to implement the changes to
	 * @return an integer value of the packet type: 1 RRQ, 2 WRQ, 3 DATA, 4 ACK
	 * @since May 24 2014
	 * 
	 * Latest Change: added implementation to check on whether or not this is a DATA or ACK packet
	 * @version May 24 2014
	 * @author Mohammed Ahmed-Muhsin & Samson Truong 
	 */
	private int packetType(DatagramPacket p){
		int type;
		// set type, to the type of packet it is based on its second byte
		type = (int)(p.getData()[1]);
		if (verbose)
			System.out.println("ConnectionManagerESim: packet type is " + type);
		return type;
	}// end method
	
	/**
	 * This method will check if this is the packet that we are looking for
	 * @param p the DatagramPacket that we are looking to implement the changes to
	 * @param packetCount the packet number we are checking to see if that is what the user entered 
	 * @since May 24 2014
	 * 
	 * Latest Change: added implementation to check on whether or not this is a DATA or ACK packet
	 * @version May 24 2014
	 * @author Mohammed Ahmed-Muhsin & Samson Truong 
	 */
	private boolean foundPacket(DatagramPacket p) {
		int pType = packetType(p);
		// the block number we are looking for in bytes
		byte blk[] = new byte[2];
		// the high byte
		blk[0] = (byte)((packetNumber - (packetNumber % 256)) /256);
		// the low byte
		blk[1] = (byte)(packetNumber % 256);
		
		// the packet's block number that we are checking
		byte blkCheck[] = blockNumber(p);
		// check if it is the right packet type
		if (pType ==  packetType) {
			if (verbose)
				System.out.println("ConnectionManagerESim: this is the correct packet type, checking if it is the correct number..");
			if (blk[0] == blkCheck[0] && blk[1] == blkCheck[1]){
				if (verbose)
					System.out.println("ConnectionManagerESim: this is the right packet to change!");
				return true;
			} // end if
		} // end if
		return false;
	}// end method
	
	/**
	 * This method will give us the block number
	 * 
	 * @param p the DatagramPacket that need to get the block number from'
	 * @return the block number of the packet
	 * @since May 24 2014
	 * 
	 * Latest Change: returning the block number
	 * @version May 24 2014
	 * @author Mohammed Ahmed-Muhsin & Samson Truong 
	 */	
	private byte[] blockNumber(DatagramPacket p) {
		byte[] blockNum = {p.getData()[2], p.getData()[3]};
		if (verbose)
			System.out.println("Block number: " + Integer.toHexString(blockNum[0]) + Integer.toHexString(blockNum[1]));
		return blockNum;
	} // end method
	

} // end class
