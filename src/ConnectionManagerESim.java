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
	public static final long TIMEOUT = 3000;		//int value in miliseconds 
	
	private int serverPort = 69; // the server port will be initiated to 69 and will change according to the thread needed 
	private DatagramSocket serverSocket, clientSocket; // socket deceleration for all three required sockets 
	private DatagramPacket sendClientPacket, receiveClientPacket, receiveServerPacket, sendServerPacket; // packet deceleration for all packets being sent and received for both client and server
	private boolean verbose;
	private byte data[];
	private int clientPort;
	private int length;
	private int mode;	// will have the value of the current error simulation mode 
	//private int numberOfTimeouts; // keeps tracks of the number of timeouts 
	private Request requestType;
	private boolean lastPacketWrite = false;
	private boolean lastPacketRead = false;
	private boolean firstPacket = true;
	private boolean end = false;
	
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
	public ConnectionManagerESim(boolean verbose, int userChoice, byte[] data, int port, int length, Request requestType) {
		this.verbose = verbose;
		this.data = data;
		this.clientPort = port;
		this.length = length;
		this.mode = userChoice;
		this.requestType = requestType;
		//this.numberOfTimeouts = 0;
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
		
		System.out.println("ConnectionManagerESim Thread started to service request!");
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
		System.out.println("ConnectionManagerESim: ErrorSim is now closiong its sockets");
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
		System.out.println("ConnectionManagerESim: Running Normal Operation\n");

		System.out.println("ConnectionManagerESim: Waiting to receive packet from client");

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
		double rando = randoNum();	//Random number from 1-10
		if(rando <= PERCENTPASS)	//Check if the Random number is less than the percent pass
		{
			System.out.println("Lost packet will not be simulated, normal operation will continue");
			return normalOp();
		}
		else
		{
			System.out.println("Lost packet is simulated");
			try {
				Thread.sleep(TIMEOUT);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if(firstPacket)
				return true;
			return false;
		}
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
                Thread.sleep(TIMEOUT + (int)(3*rando*100));	//delays the packet for the timeout period plus a random amount. 
            } catch (InterruptedException e)
            {}
			System.out.println("*************Packet was delayed by: " + (TIMEOUT + (3*rando*100)) + "!************");
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
						Thread.sleep((int)(rando*100));
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
						Thread.sleep((int)(rando*100));
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
} // end class
