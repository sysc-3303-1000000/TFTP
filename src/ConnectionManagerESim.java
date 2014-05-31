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
	private byte clientData[];
	private int clientPort;
	private int clientLength;
	private int serverLength;
	private int mode;	// will have the value of the current error simulation mode 
	private int delay; // will store the amount of delay if we are running in delayed mode
	private int packetType; // the type of packet we are changing
	private int packetNumber; // the packet number that we are changing
	private Request requestType;
	private boolean lastPacketWrite = false;
	private boolean lastPacketRead = false;
	private boolean firstPacket = true;
	private boolean end = false;
	byte clientReply[] = new byte[DATA_SIZE]; // this will store the reply from the client
	byte serverReply[] = new byte[DATA_SIZE]; // this will store the reply from the server
	byte serverData[] = new byte[DATA_SIZE]; // this will store the response from the server
	byte trueLastPacket[] = new byte[2]; // will store the block number of the truly last packet to verify if we have received it or not
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
		this.clientData = data;
		this.clientPort = port;
		this.clientLength = length;
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
		try {
			receiveClientPacket = new DatagramPacket(data, length, InetAddress.getLocalHost(), port);
		} catch (UnknownHostException e) {
			System.err.println("UnknownHostException: " + e.getMessage());
		}
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
		//System.out.println("Containing the following \nString: " + new String(p.getData()));
		System.out.println("Length of packet: " + p.getLength());
		System.out.println("Bytes: ");
		for (int i = 0; i < p.getLength(); i++) {
			System.out.print(Integer.toHexString(p.getData()[i]));
		} // end forloop
		System.out.println("\n******************************************************");
		System.out.println("\n");
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
		System.out.println("ConnectionManagerESim: closing its sockets and shutting down the thread");
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
		if (lastPacketRead == true && trueLastPacket[0] == sendServerPacket.getData()[2] && trueLastPacket[1] == sendServerPacket.getData()[3])	
			return true;	// Last packet is now sent. The thread will close
		if (requestType == Request.WRITE && !firstPacket) {
			if (verbose)
				System.out.println("Checking if this packet size is less than 512:");
			printInformation(sendServerPacket);
			if(sendServerPacket.getLength() < DATA_SIZE)
				lastPacketWrite = true;	
		} // end if

		//*********************************************************************************

		serverReceive();

		clientSend();
		firstPacket = false;		// any following packets the connection manager receives will be not the second packet
		if (lastPacketWrite == true)
			return true;	// Last packet is now sent. The thread will close
		if (requestType == Request.READ && !firstPacket) {
			if (verbose)
				System.out.println("Checking if this packet size is less than 512:");
			printInformation(sendClientPacket);
			if(sendClientPacket.getLength() < DATA_SIZE) { 
				lastPacketRead = true;
				trueLastPacket[0] = sendClientPacket.getData()[2];
				trueLastPacket[1] = sendClientPacket.getData()[3];
			}// end if
		} // end if

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
		// check if this is a read request or a write request
		if (requestType == Request.READ){ // this is a read request
			if (verbose)
				System.out.println("ConnectionManagerESim: lost op Request is a read");
			// ************* RRQ PACKET ***************			
			if (packetType == 1) { // lose a RRQ packet
				if (verbose){
					System.out.println("ConnectionManagerESim: simulating a lost client RRQ packet");
					printInformation(receiveClientPacket);
				}//end if
				return true; 
			}// end if

			// ************* ACK PACKET ***************
			else if (packetType == 4){ // check to lose a ACK packet
				if (verbose)
					System.out.println("ConnectionManagerESim: checking to lose ACK packet");
				if (!lastPacketRead) {

					if (!firstPacket) {
						// receive from client
						clientReceive();
					}// end if
					firstPacket = false; // this is no longer the first packet
					// check to see if this is the packet that we want to lose
					if (foundPacket(receiveClientPacket)) { // this is the packet we want to lose
						if (verbose) {
							System.out.println("ConnectionManagerESim: simulating a lost client ACK packet");
							printInformation(receiveClientPacket);
						}// end if
						//we go back to operating as normal
						mode = 0;
					}//end if

					else { // this is not the packet we want to lose, send to server
						serverSend();
					}//end else

					// we need to wait on a server packet
					serverReceive();

					//send to the client
					clientSend();
					//check to see if this is the last packet (DATA < 512b 
					if(sendClientPacket.getLength() < DATA_SIZE)
						lastPacketRead = true;

					return false;					
				} // end if
				else if (lastPacketRead) {
					// receive from client
					clientReceive();
					// check to see if this is the packet that we want to lose
					if (foundPacket(receiveClientPacket)) { // this is the packet we want to lose
						if (verbose) {
							System.out.println("ConnectionManagerESim: simulating a lost client ACK packet");
							printInformation(receiveClientPacket);
						}// end if

						return true;
					}//end if
					else { // this is not the packet we want to lose, send it to server
						serverSend();
						return true; //we're done, shut down thread
					}// end else
				}//end else if
			}// end else if

			// ************* DATA PACKET ***************
			else if (packetType == 3){ // check to lose a DATA packet
				if (verbose)
					System.out.println("ConnectionManagerESim: checking to lose a DATA packet");
				if (!firstPacket) {
					// receive from client
					clientReceive();
				}// end if
				firstPacket = false; // this is no longer the first packet

				serverSend();

				// we need to wait on a server packet
				serverReceive();
				// check to see if this is the packet that we want to lose
				if (foundPacket(receiveServerPacket)) { // this is the packet we want to lose
					if (verbose) {
						System.out.println("ConnectionManagerESim: simulating a lost server DATA packet");
						printInformation(receiveServerPacket);
					}// end if
					//we go back to operating as normal
					mode = 0;

					// wait on server to resend DATA
					serverReceive();
				}//end if

				//send to the client
				clientSend();
				//check to see if this is the last packet (DATA < 512b 
				if(sendClientPacket.getLength() < DATA_SIZE)
					lastPacketRead = true;

				return false;					
			} // end else if
		}//end if
		else if (requestType == Request.WRITE){ // this is a write request
			if (verbose)
				System.out.println("ConnectionManagerESim: lost op Request is a write");
			// ************* WRQ PACKET ***************			
			if (packetType == 2) { // lose a WRQ packet
				if (verbose){
					System.out.println("ConnectionManagerESim: simulating a lost client WRQ packet");
					printInformation(receiveClientPacket);
				}//end if
				return true; 
			}// end if

			// ************* ACK PACKET ***************
			else if (packetType == 4){ // check to lose a ACK packet
				if (verbose)
					System.out.println("ConnectionManagerESim: checking to lose ACK packet");
				if (firstPacket) { // if this is the first packet

					serverSend();

					serverReceive();
					if(foundPacket(receiveServerPacket)) { // this is the ACK 00 that we lost
						if (verbose) {
							System.out.println("ConnectionManagerESim: simulating a lost server ACK 00 packet");
							printInformation(receiveServerPacket);
						}// end if
						return true;
					}// end if
					else { // ACK 00 is not the packet we were looking for
						firstPacket = false; // this is no longer the first packet
						clientSend();
						return false;
					}//end else	
				}// end if		
				clientReceive();
				// check to see if packet is < 512 bytes
				if (receiveClientPacket.getLength() < DATA_SIZE) {
					lastPacketWrite = true;
				}

				serverSend();

				// we need to wait on a server packet
				serverReceive();
				// check to see if this is the packet that we want to lose
				if (foundPacket(receiveServerPacket)) { // this is the packet we want to lose
					if (verbose) {
						System.out.println("ConnectionManagerESim: simulating a lost server ACK packet");
						printInformation(receiveServerPacket);
					}// end if

					// check to see if this is the last packet
					if (lastPacketWrite) { // this is the last ACK packet that we are sending back to the client
						return true;
					}
					else {
						//we go back to operating as normal
						mode = 0;
						return false;
					}
				}//end if

				//send to the client
				clientSend();

				// we just sent the last ACK packet to the client, we are done.
				if (lastPacketWrite)
					return true;
				else
					return false;
			}// end else if

			// ************* DATA PACKET ***************
			else if (packetType == 3){ // check to lose a DATA packet
				if (verbose)
					System.out.println("ConnectionManagerESim: checking to lose a DATA packet");

				if (!firstPacket) {
					// receive from client
					clientReceive();
				}// end if
				firstPacket = false; // this is no longer the first packet

				//checking to see if this is the packet that we want to lose
				if (foundPacket(receiveClientPacket)) { // this is the packet we want to lose
					if (verbose) {
						System.out.println("ConnectionManagerESim: simulating a lost client DATA packet");
						printInformation(receiveClientPacket);
					}// end if
					//we go back to operating as normal
					mode = 0;

					return false;
				}//end if

				// send to the server
				serverSend();

				if (sendServerPacket.getLength() < DATA_SIZE)
					lastPacketWrite = true;

				// we need to wait on a server packet
				serverReceive(); 

				//send to the client
				clientSend();

				return false;					
			} // end else if
		}//end if
		return false;
	}

	/**
	 * The following is the run method for ConnectionManagerESim with delayed packet, used to execute code upon starting the thread. 
	 * 
	 * @since May 21 2014
	 * 
	 * Latest Change: Allow user to specify which packet to delay and by how much
	 * @version May 29 2014
	 * @author Samson Truong & Mohammed Ahmed-Muhsin 
	 */
	private boolean delayedOp()
	{
		// check to see if the which packet is the one being delayed
		if (packetType == 1 || packetType == 2) { // RRQ or WRQ packet is being delayed

			if(verbose)
				System.out.println("ConnectionManagerESim: RRQ/WRQ packet will be delayed. Thread will sleep now");
			try {
				Thread.sleep(delay);	//delays the packet for the specified amount
			}// end  try
			catch (InterruptedException e) { } // end catch

			System.out.println("ConnectionManagerESim: Packet was delayed by: " + delay + "ms!");

			serverSend();

			serverReceive();

			clientSend();

			// switch back to normal operation
			mode = 0;

			firstPacket = false;
			return false;
		}// end if

		// now we check if this is a read or write so we can delay on the appropriate packet
		else if (requestType == Request.READ) { // this is a read request
			if (packetType == 3) { // DATA packet being delayed from the server 

				if (!firstPacket)
					clientReceive();

				serverSend();
				if (lastPacketRead == true)	
					return true;	// Last packet is now sent. The thread will close
				serverReceive();
				// check  to see if this is the data packet we want to delay
				if (foundPacket(receiveServerPacket)) {
					if(verbose)
						System.out.println("ConnectionManagerESim: DATA packet will be delayed. Thread will sleep now");
					try {
						Thread.sleep(delay);	//delays the packet for the specified amount
					}// end try
					catch (InterruptedException e) { } // end catch

					System.out.println("ConnectionManagerESim: Packet was delayed by: " + delay + "ms!");
					// switch back to normal operation
					mode = 0;
				} // end if

				clientSend();
				if (!firstPacket) {
					if(sendClientPacket.getLength() < DATA_SIZE) 
						lastPacketRead = true;
				} // end if
				firstPacket = false;
				return false;
			}// end if

			else if (packetType == 4) { // ACK packet being delayed from the client 

				if (!firstPacket) {
					clientReceive();
					// check  to see if this is the ACK packet we want to delay
					if (foundPacket(receiveClientPacket)) {
						if(verbose)
							System.out.println("ConnectionManagerESim: ACK packet will be delayed. Thread will sleep now");
						try {
							Thread.sleep(delay);	//delays the packet for the specified amount
						}// end try
						catch (InterruptedException e) { } // end catch

						System.out.println("ConnectionManagerESim: Packet was delayed by: " + delay + "ms!");
						// switch back to normal operation
						mode = 0;
					} // end if
				}
				serverSend();
				if (lastPacketRead == true)	
					return true;	// Last packet is now sent. The thread will close

				serverReceive();
				clientSend();
				if (!firstPacket) {
					if(sendClientPacket.getLength() < DATA_SIZE) 
						lastPacketRead = true;
				} // end if	
				firstPacket = false;
				return false;
			}// end if
		}// end else if

		else if (requestType == Request.WRITE) { // this is a write request
			if (packetType == 4) { // ACK packet being delayed from the server 

				if (!firstPacket)
					clientReceive();

				serverSend();
				if (!firstPacket) {
					if(sendServerPacket.getLength() < DATA_SIZE)
						lastPacketWrite = true;	
				} // end if
				serverReceive();
				// check  to see if this is the ACK packet we want to delay
				if (foundPacket(receiveServerPacket)) {
					if(verbose)
						System.out.println("ConnectionManagerESim: ACK packet will be delayed. Thread will sleep now");
					try {
						Thread.sleep(delay);	//delays the packet for the specified amount
					}// end try
					catch (InterruptedException e) { } // end catch

					System.out.println("ConnectionManagerESim: Packet was delayed by: " + delay + "ms!");
					// switch back to normal operation
					mode = 0;
				} // end if

				clientSend();
				if (lastPacketWrite == true)
					return true;	// Last packet is now sent. The thread will close
				firstPacket = false;
				return false;
			}// end if

			else if (packetType == 3) { // DATA packet being delayed from the client 

				if (!firstPacket) {
					clientReceive();
					// check  to see if this is the DATA packet we want to delay
					if (foundPacket(receiveClientPacket)) {
						if(verbose)
							System.out.println("ConnectionManagerESim: DATA packet will be delayed. Thread will sleep now");
						try {
							Thread.sleep(delay);	//delays the packet for the specified amount
						}// end try
						catch (InterruptedException e) { } // end catch

						System.out.println("ConnectionManagerESim: Packet was delayed by: " + delay + "ms!");
						// switch back to normal operation
						mode = 0;
					} // end if
				}
				serverSend();
				if (!firstPacket) {
					if(sendServerPacket.getLength() < DATA_SIZE)
						lastPacketWrite = true;	
				} // end if
				serverReceive();
				clientSend();
				if (lastPacketWrite == true)
					return true;	// Last packet is now sent. The thread will close			
				firstPacket = false;
				return false;
			}// end if
		}// end else if

		return false;
	}

	/**
	 * The following is the run method for ConnectionManagerESim with a duplicated packet, used to execute code upon starting the thread. 
	 * We will duplicate the specified packet by the user
	 * @since May 21 2014
	 * 
	 * Latest Change: Accept a user input as to which packet to duplicate and how much space between
	 * @version May 29 2014
	 * @author Mohammed Ahmed-Muhsin & Samson Truong  
	 */
	private boolean duplicatedOp()
	{
		// check to see if the which packet is the one being duplicated
		if (packetType == 1 || packetType == 2) { // RRQ or WRQ packet is being duplicated
			if (verbose)
				System.out.println("ConnectionManagerESim: RRQ/WRQ packet will be duplicated. Sending first packet");
			serverSend();
			if(verbose)
				System.out.println("ConnectionManagerESim: Thread will sleep now");
			try {
				Thread.sleep(delay);	//delays the packet for the specified amount
			}// end  try
			catch (InterruptedException e) { } // end catch
			if (verbose)
				System.out.println("ConnectionManagerESim: Second duplicate packet will be sent and was delayed by: " + delay + "ms!");
			serverSend();

			serverReceive();

			clientSend();

			// switch back to normal operation
			mode = 0;

			firstPacket = false;
			return false;
		}// end if

		// now we check if this is a read or write so we can duplicate on the appropriate packet
		else if (requestType == Request.READ) { // this is a read request
			if (packetType == 3) { // DATA packet being duplicated from the server 

				if (!firstPacket)
					clientReceive();
				firstPacket = false;
				serverSend();
				if (lastPacketRead == true && trueLastPacket[0] == sendServerPacket.getData()[2] && trueLastPacket[1] == sendServerPacket.getData()[3])	
					return true;	// Last packet is now sent. The thread will close

				serverReceive();
				// check  to see if this is the data packet we want to duplicate
				if (foundPacket(receiveServerPacket)) {
					if (verbose)
						System.out.println("ConnectionManagerESim: DATA packet will be duplicated. Sending first packet");
					clientSend();
					if(verbose)
						System.out.println("ConnectionManagerESim: Thread will sleep now");
					try {
						Thread.sleep(delay);	//delays the packet for the specified amount
					}// end try
					catch (InterruptedException e) { } // end catch
					if (verbose)
						System.out.println("ConnectionManagerESim: Second duplicate packet will be sent and was delayed by: " + delay + "ms!");
					// switch back to normal operation
					mode = 0;
					clientSend();
					// this will ensure that we exit out of the program if we are duplicating the last DATA packet
					if (!firstPacket) {
						if (verbose)
							System.out.println("Checking if this packet size is less than 512:");
						printInformation(sendClientPacket);
						if(sendClientPacket.getLength() < DATA_SIZE) { 
							lastPacketRead = true;
							trueLastPacket[0] = sendClientPacket.getData()[2];
							trueLastPacket[1] = sendClientPacket.getData()[3];
						}// end if
					}//end if
					clientReceive(); // fetches next ack (ack 02)
					serverSend();
					if (lastPacketRead == true && trueLastPacket[0] == sendServerPacket.getData()[2] && trueLastPacket[1] == sendServerPacket.getData()[3]){
						return true;
					}// end if
					else {
						serverReceive();
						clientSend();
						if (!firstPacket) {
							if (verbose)
								System.out.println("Checking if this packet size is less than 512:");
							printInformation(sendClientPacket);
							if(sendClientPacket.getLength() < DATA_SIZE) { 
								lastPacketRead = true;
								trueLastPacket[0] = sendClientPacket.getData()[2];
								trueLastPacket[1] = sendClientPacket.getData()[3];
							}// end if
						}//end if
						clientReceive();
						serverSend();
						if (lastPacketRead == true && trueLastPacket[0] == sendServerPacket.getData()[2] && trueLastPacket[1] == sendServerPacket.getData()[3]){
							return true;
						}
						return false;
					}// end else
				} // end if

				clientSend();
				if (!firstPacket) {
					if(sendClientPacket.getLength() < DATA_SIZE) { 
						lastPacketRead = true;
						trueLastPacket[0] = sendClientPacket.getData()[2];
						trueLastPacket[1] = sendClientPacket.getData()[3];
					}
				} // end if
				firstPacket = false;
				return false;
			}// end if

			else if (packetType == 4) { // ACK packet being duplicated from the client 

				if (!firstPacket) {
					clientReceive();
					// check  to see if this is the ack packet we want to duplicate
					if (foundPacket(receiveClientPacket)) {
						if (verbose)
							System.out.println("ConnectionManagerESim: ACK packet will be duplicated. Sending first packet");
						serverSend();
						if(verbose)
							System.out.println("ConnectionManagerESim: Thread will sleep now");
						try {
							Thread.sleep(delay);	//delays the packet for the specified amount
						}// end try
						catch (InterruptedException e) { } // end catch
						if (verbose)
							System.out.println("ConnectionManagerESim: Second duplicate packet will be sent and was delayed by: " + delay + "ms!");
						// switch back to normal operation
						mode = 0;
						serverSend();
						// this will exit out if we are duplicating the last ACK packet
						if (lastPacketRead == true && trueLastPacket[0] == sendServerPacket.getData()[2] && trueLastPacket[1] == sendServerPacket.getData()[3])
							return true;
						serverReceive();
						clientSend();
						// this will ensure that we exit out of the program if we are duplicating the last ACK packet
						if (!firstPacket) {
							if (verbose)
								System.out.println("Checking if this packet size is less than 512:");
							printInformation(sendClientPacket);
							if(sendClientPacket.getLength() < DATA_SIZE) { 
								lastPacketRead = true;
								trueLastPacket[0] = sendClientPacket.getData()[2];
								trueLastPacket[1] = sendClientPacket.getData()[3];
							}// end if
						}//end if
						clientReceive();
						serverSend();
						if (lastPacketRead == true && trueLastPacket[0] == sendServerPacket.getData()[2] && trueLastPacket[1] == sendServerPacket.getData()[3])	
							return true;	// Last packet is now sent. The thread will close
						firstPacket = false;
					}
				} // end if

				serverSend();
				if (lastPacketRead == true && trueLastPacket[0] == sendServerPacket.getData()[2] && trueLastPacket[1] == sendServerPacket.getData()[3])	
					return true;	// Last packet is now sent. The thread will close
				serverReceive();
				clientSend();
				if (!firstPacket) {
					if(sendClientPacket.getLength() < DATA_SIZE) { 
						lastPacketRead = true;
						trueLastPacket[0] = sendClientPacket.getData()[2];
						trueLastPacket[1] = sendClientPacket.getData()[3];
					}
				} // end if
				firstPacket = false;
				return false;
			}// end else if
		}// end else if

		else if (requestType == Request.WRITE) { // this is a write request
			if (packetType == 3) { // DATA packet being duplicated from the client 
				if (!firstPacket)
					clientReceive();
				firstPacket = false;
				if (foundPacket(receiveClientPacket)){
					if (verbose)
						System.out.println("ConnectionManagerESim: DATA packet will be duplicated. Sending first packet");
					serverSend();
					if(verbose)
						System.out.println("ConnectionManagerESim: Thread will sleep now");
					try {
						Thread.sleep(delay);	//delays the packet for the specified amount
					}// end try
					catch (InterruptedException e) { } // end catch
					if (verbose)
						System.out.println("ConnectionManagerESim: Second duplicate packet will be sent and was delayed by: " + delay + "ms!");
					// switch back to normal operation
					mode = 0;
					serverSend();
					if (!firstPacket) {
						if (verbose)
							System.out.println("Checking if this packet size is less than 512:");
						printInformation(sendServerPacket);
						if(sendServerPacket.getLength() < DATA_SIZE) {
							lastPacketWrite = true;	
							trueLastPacket[0] = sendServerPacket.getData()[2];
							trueLastPacket[1] = sendServerPacket.getData()[3];
						}//end if
					} // end if
					serverReceive();
					clientSend();
					if (lastPacketWrite == true && trueLastPacket[0] == sendClientPacket.getData()[2] && trueLastPacket[1] == sendClientPacket.getData()[3]){	
						return true;	// Last packet is now sent. The thread will close
					}// end if
					else {
						clientReceive();
						serverSend();
						if (!firstPacket) {
							if (verbose)
								System.out.println("Checking if this packet size is less than 512:");
							printInformation(sendServerPacket);
							if(sendServerPacket.getLength() < DATA_SIZE) {
								lastPacketWrite = true;	
								trueLastPacket[0] = sendServerPacket.getData()[2];
								trueLastPacket[1] = sendServerPacket.getData()[3];
							}//end if
						} // end if
						serverReceive();
						clientSend();
						if (lastPacketWrite == true && trueLastPacket[0] == sendClientPacket.getData()[2] && trueLastPacket[1] == sendClientPacket.getData()[3]){	
							return true;	// Last packet is now sent. The thread will close
						}// end if
						serverReceive();
						clientSend();
						if (lastPacketWrite == true && trueLastPacket[0] == sendClientPacket.getData()[2] && trueLastPacket[1] == sendClientPacket.getData()[3]){	
							return true;	// Last packet is now sent. The thread will close
						}// end if
						return false;
					}// end else
				}// end if
				

				serverSend();
				if (!firstPacket) {
					if (verbose)
						System.out.println("Checking if this packet size is less than 512:");
					printInformation(sendServerPacket);
					if(sendServerPacket.getLength() < DATA_SIZE) {
						lastPacketWrite = true;	
						trueLastPacket[0] = sendServerPacket.getData()[2];
						trueLastPacket[1] = sendServerPacket.getData()[3];
					}//end if
				} // end if

				serverReceive();

				clientSend();
				if (lastPacketWrite == true && trueLastPacket[0] == sendClientPacket.getData()[2] && trueLastPacket[1] == sendClientPacket.getData()[3])	
					return true;	// Last packet is now sent. The thread will close
				firstPacket = false;
				return false;
			}// end if

			else if (packetType == 4) { // ACK packet being duplicated from the client 

				if (!firstPacket) {
					clientReceive();
				} // end if

				serverSend();
				if (!firstPacket) {
					if (verbose)
						System.out.println("Checking if this packet size is less than 512:");
					printInformation(sendServerPacket);
					if(sendServerPacket.getLength() < DATA_SIZE) {
						lastPacketWrite = true;
						trueLastPacket[0] = sendServerPacket.getData()[2];
						trueLastPacket[1] = sendServerPacket.getData()[3];
					}
				} // end if
				serverReceive();
				// check  to see if this is the ack packet we want to duplicate
				if (foundPacket(receiveServerPacket)) {
					if (verbose)
						System.out.println("ConnectionManagerESim: ACK packet will be duplicated. Sending first packet");
					clientSend();
					if(verbose)
						System.out.println("ConnectionManagerESim: Thread will sleep now");
					try {
						Thread.sleep(delay);	//delays the packet for the specified amount
					}// end try
					catch (InterruptedException e) { } // end catch
					if (verbose)
						System.out.println("ConnectionManagerESim: Second duplicate packet will be sent and was delayed by: " + delay + "ms!");
					// switch back to normal operation
					mode = 0;
				}
				clientSend();
				if (lastPacketWrite == true && trueLastPacket[0] == sendClientPacket.getData()[2] && trueLastPacket[1] == sendClientPacket.getData()[3])	
					return true;	// Last packet is now sent. The thread will close
				firstPacket = false;
				return false;
			}// end else if
		}// end else if
		return false;
	}// end method

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

		receiveClientPacket = new DatagramPacket(clientReply, clientReply.length);
		try { // wait to receive client packet
			clientSocket.receive(receiveClientPacket);
		}//end try 
		catch (IOException ie) {
			System.err.println("IOException error: " + ie.getMessage());
		}//end catch

		System.out.println("ConnectionManagerESim: Received packet from client");
		printInformation(receiveClientPacket);
		// updating the data and length in the packet being sent to the server
		clientData = receiveClientPacket.getData();
		clientLength = receiveClientPacket.getLength();
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
			System.out.println("ConnectionManagerESim: Preparing packet to send to Client");

		// prepare the new send packet to the client
		try {
			sendClientPacket = new DatagramPacket(serverData, serverLength, InetAddress.getLocalHost(), clientPort);
		} // end try
		catch (UnknownHostException uhe) {
			uhe.printStackTrace();
			System.exit(1);
		} // end catch

		// send the packet to the client via the send socket 
		try {
			clientSocket.send(sendClientPacket);

		} // end try 
		catch (IOException ioe) {
			System.err.println("Unknown IO exception error: " + ioe.getMessage());
		} // end catch

		// print confirmation message that the packet has been sent to the client
		if (verbose)
			System.out.println("ConnectionManagerESim: response packet sent to client");
		if(verbose) // print out information about the packet being sent to the client
			printInformation(sendClientPacket);
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
			System.out.println("ConnectionManagerESim: Waiting to receive a packet from server...\n");

		receiveServerPacket = new DatagramPacket(serverReply, serverReply.length);

		// block until you receive a packet from the server
		try {
			serverSocket.receive(receiveServerPacket);
		} // end try 
		catch (IOException ioe) {
			System.err.println("Unknown IO exception error: " + ioe.getMessage());
		} // end catch

		if(verbose) // print out information about the packet received from the server if verbose
			printInformation(receiveServerPacket);

		serverData = receiveServerPacket.getData();
		serverLength = receiveServerPacket.getLength();
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
			sendServerPacket = new DatagramPacket(clientData, clientLength, InetAddress.getLocalHost(), serverPort);
		} // end try 
		catch (UnknownHostException uhe) {
			System.err.println("Unknown host exception error: " + uhe.getMessage());
		} // end catch

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
		if(verbose)
			printInformation(sendServerPacket);
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
			System.out.println("Block number: " + Integer.toHexString(blockNum[0]) + "" + Integer.toHexString(blockNum[1]));
		return blockNum;
	} // end method


} // end class
