import java.io.IOException;
import java.net.*;

/**
 * The following is implementation for the ConnectionManagerESim which will form
 * a packet with the message it receives from listener and send it to the server.
 * It will receive a packet back and send this to the client.
 * 
 * @since May 16 2014
 * 
 * @author Mohammed Ahmed-Muhsin & Samson Truong
 * @version June 6 2014
 *
 */
public class ConnectionManagerESim extends Thread {
	public static final int DATA_SIZE = 516;
	public static final int PERCENTPASS = 9;	//int value 0-10 (0 being 100 fail rate)
	public static final long TIMEOUT = 3000;	//int value in miliseconds 

	private int serverPort = 69; // the server port will be initiated to 69 and will change according to the thread needed 
	private DatagramSocket serverSocket, clientSocket, corruptSocket; // socket deceleration for all three required sockets 
	private DatagramPacket sendClientPacket, receiveClientPacket, receiveServerPacket, sendServerPacket; // packet deceleration for all packets being sent and received for both client and server
	private boolean debug;
	private boolean verbose;
	private boolean silent;
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
	private boolean errorReceived = false;
	private byte clientData[];
	private byte clientReply[] = new byte[DATA_SIZE]; // this will store the reply from the client
	private byte serverReply[] = new byte[DATA_SIZE]; // this will store the reply from the server
	private byte serverData[] = new byte[DATA_SIZE]; // this will store the response from the server
	private byte trueLastPacket[] = new byte[2]; // will store the block number of the truly last packet to verify if we have received it or not
	private byte errorCode[] = new byte[2]; // will store the error code number (if applicable)
	/**
	 * The following is the constructor for ListenerESim
	 * @param output determines the level of output we want to display
	 * @param data the message which will be sent to the server
	 * @param port the port which the message from the server will be sent to
	 * @param length the length of the data packet
	 * 
	 * @since May 16 2014
	 * 
	 * Latest Change: Improved output
	 * @version June 12 2014
	 * @author Mohammed Ahmed-Muhsin & Samson Truong
	 * 
	 */
	public ConnectionManagerESim(int output, int userChoice, int delay, int packetType, int packetNumber, byte[] data, int port, int length, Request requestType) {
		// initialize the variables
		this.clientData = data;
		this.clientPort = port;
		this.clientLength = length;
		this.mode = userChoice;
		this.requestType = requestType;
		this.delay = delay;
		this.packetType = packetType;
		this.packetNumber = packetNumber;
		// initialize the output level
		setOutput(output);
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
		if (debug || verbose)
			System.out.println("ConnectionManagerESim: Thread started to service request!\n");
		try {
			receiveClientPacket = new DatagramPacket(data, length, InetAddress.getLocalHost(), port);
		} catch (UnknownHostException e) {
			System.err.println("UnknownHostException: " + e.getMessage());
		}
	} // end constructor

	/**
	 * The following method will be setting the appropriate variables to determine the level output
	 * @param output sets the level of output that gets displayed in the console
	 * 
	 * @since June 12 2014
	 * 
	 * Latest Change: Added method
	 * @version June 12 2014
	 * @author Mohammed Ahmed-Muhsin & Samson Truong
	 * 
	 */
	private void setOutput(int output) {
		if (output == 1) {
			debug = true;
			verbose = false;
			silent = false;
		}// end if
		else if (output == 2) {
			debug = false;
			verbose = true;
			silent = false;
		}// end else if
		else if (output == 3) {
			debug = false;
			verbose = false;
			silent = true;
		}// end else if
	}
	/**
	 * The following method will be called when trying to print out information about a specific packet
	 * @param p the information displayed desired for this packet
	 * 
	 * @since May 16 2014
	 * 
	 * Latest Change: Improved output
	 * @version June 12 2014
	 * @author Mohammed Ahmed-Muhsin & Samson Truong
	 * 
	 */
	private void printInformation(DatagramPacket p) {

		if (debug) { // print all information
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
		}
		else if (verbose) { // print only relevant information
			// print out the information on the packet
			System.out.println("Length of packet: " + p.getLength());
			System.out.println("Packet Type: " + p.getData()[0] + p.getData()[1]);
			System.out.println("Block Number: " + p.getData()[2] + p.getData()[3]);
			System.out.println("\n");
		}
		else if (silent){
		}
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
			else if (mode == 4 || mode == 5 || mode == 7) {
				end = corruptOp();
			}
			else if (mode == 6) {
				end = corruptMode();
			}
			else if (mode == 8) {
				// check if we are changing the TID of the first DATA packet in a read
				if (requestType == Request.READ && packetType == 3 && packetNumber == 1) { // we CANNOT change this as there is no way to know the correct TID
					System.out.println("ConnectionManagerESim: You cannot change the TID for a READ and the first DATA packet as there is no way to validate");
					System.out.println("ConnectionManagerESim: Changing back to normal mode");
					mode = 0;
					end = false;
				} // end if
				// check if we are changing the TID of the first ACK in a write
				else if (requestType == Request.WRITE && packetType == 4 && packetNumber == 0) { // we CANNOT change this is as there is no way to know the correct TID 
					System.out.println("ConnectionManagerESim: You cannot change the TID for a WRITE and the first ACK (00) packet as there is no way to validate");
					System.out.println("ConnectionManagerESim: Changing back to normal mode");
					mode = 0;
					end = false;
				}// end else if
				// otherwise, proceed with the corruption
				else {
					end = invalidTID();
				}
			}
		}//end while

		// begin closing operations
		if (errorReceived) { // if an error was received, print out the error code and its message
			System.out.println("ConnectionManagerESim: Shutting down due to an error code: " + errorCode[0] + errorCode[1]);
		}// end if
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
	 * Latest Change: Fixes to ErrorSim not shutting down properly
	 * @version June 12 2014
	 * @author Samson Truong & Mohammed Ahmed-Muhsin 
	 */
	private boolean normalOp() 
	{

		if (debug)
			System.out.println("ConnectionManagerESim: Running Normal Operation\n");

		// this is not the first packet, we need to wait for the client to send back to us
		if (!firstPacket) {
			clientReceive();

		}//end if

		serverSend();
		if (errorReceived || (lastPacketRead && trueLastPacket[0] == sendServerPacket.getData()[2] && trueLastPacket[1] == sendServerPacket.getData()[3]))	
			return true;	// Last packet is now sent. The thread will close
		if (requestType == Request.WRITE && !firstPacket) {
			printInformation(sendServerPacket);
			if(sendServerPacket.getLength() < DATA_SIZE){
				lastPacketWrite = true;
				trueLastPacket[0] = sendServerPacket.getData()[2];
				trueLastPacket[1] = sendServerPacket.getData()[3];
			}// end if
		} // end if

		//*********************************************************************************

		serverReceive();

		clientSend();
		firstPacket = false;		// any following packets the connection manager receives will be not the second packet
		if (errorReceived || lastPacketWrite)
			return true;	// Last packet is now sent. The thread will close
		if (requestType == Request.READ && !firstPacket) {
			if (debug)
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
			if (debug)
				System.out.println("ConnectionManagerESim: Lost operation is a read request");
			// ************* RRQ PACKET ***************			
			if (packetType == 1) { // lose a RRQ packet
				if (debug || verbose){
					System.out.println("ConnectionManagerESim: Simulating a lost client RRQ packet");
					printInformation(receiveClientPacket);
				}//end if
				return true; 
			}// end if

			// ************* ACK PACKET ***************
			else if (packetType == 4){ // check to lose a ACK packet
				if (debug)
					System.out.println("ConnectionManagerESim: checking to lose ACK packet");
				if (!firstPacket) {
					// receive from client
					clientReceive();
				}// end if
				firstPacket = false; // this is no longer the first packet
				// check to see if this is the packet that we want to lose
				if (foundPacket(receiveClientPacket)) { // this is the packet we want to lose
					if (debug || verbose) {
						System.out.println("ConnectionManagerESim: This is the correct ACK packet:");
						printInformation(receiveClientPacket);
						System.out.println("ConnectionManagerESim: Lost ACK packet");
						System.out.println("ConnectionManagerESim: Waiting for Server to resend");
					}// end if
					//we go back to operating as normal
					mode = 0;
				}//end if

				else { // this is not the packet we want to lose, send to server
					serverSend();
					if (errorReceived || (lastPacketRead && trueLastPacket[0] == sendServerPacket.getData()[2] && trueLastPacket[1] == sendServerPacket.getData()[3]))	
						return true;	// Last packet is now sent. The thread will close
				}//end else

				// we need to wait on a server packet
				serverReceive();
				if (lastPacketRead && mode == 0) // will ensure that if this is the last ACK, we close out while server times out	
					return true;	// Last packet is now sent. The thread will close
				//send to the client
				clientSend();
				//check to see if this is the last packet (DATA < 512b 
				if(sendClientPacket.getLength() < DATA_SIZE) {
					lastPacketRead = true;
					trueLastPacket[0] = sendClientPacket.getData()[2];
					trueLastPacket[1] = sendClientPacket.getData()[3];
				}// end if
				return false;
			}

			// ************* DATA PACKET ***************
			else if (packetType == 3){ // check to lose a DATA packet
				if (debug)
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
					if (debug) {
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
				if(sendClientPacket.getLength() < DATA_SIZE) {
					lastPacketRead = true;
					trueLastPacket[0] = sendClientPacket.getData()[2];
					trueLastPacket[1] = sendClientPacket.getData()[3];
				}// end if
				return false;					
			} // end else if
		}//end if
		else if (requestType == Request.WRITE){ // this is a write request
			if (debug)
				System.out.println("ConnectionManagerESim: lost op Request is a write");
			// ************* WRQ PACKET ***************			
			if (packetType == 2) { // lose a WRQ packet
				if (debug){
					System.out.println("ConnectionManagerESim: simulating a lost client WRQ packet");
					printInformation(receiveClientPacket);
				}//end if
				return true; 
			}// end if

			// ************* ACK PACKET ***************
			else if (packetType == 4){ // check to lose a ACK packet
				if (debug)
					System.out.println("ConnectionManagerESim: checking to lose ACK packet");
				if (firstPacket) { // if this is the first packet

					serverSend();

					serverReceive();
					if(foundPacket(receiveServerPacket)) { // this is the ACK 00 that we lost
						if (debug) {
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
					trueLastPacket[0] = receiveClientPacket.getData()[2];
					trueLastPacket[1] = receiveClientPacket.getData()[3];
				}// end if

				serverSend();

				// we need to wait on a server packet
				serverReceive();
				// check to see if this is the packet that we want to lose
				if (foundPacket(receiveServerPacket)) { // this is the packet we want to lose
					if (debug) {
						System.out.println("ConnectionManagerESim: simulating a lost server ACK packet");
						printInformation(receiveServerPacket);
					}// end if

					// check to see if this is the last packet
					if (lastPacketWrite || errorReceived) { // this is the last ACK packet that we are sending back to the client
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
				if (lastPacketWrite || errorReceived)
					return true;
				else
					return false;
			}// end else if

			// ************* DATA PACKET ***************
			else if (packetType == 3){ // check to lose a DATA packet
				if (debug)
					System.out.println("ConnectionManagerESim: checking to lose a DATA packet");

				if (!firstPacket) {
					// receive from client
					clientReceive();
				}// end if
				firstPacket = false; // this is no longer the first packet

				//checking to see if this is the packet that we want to lose
				if (foundPacket(receiveClientPacket)) { // this is the packet we want to lose
					if (debug) {
						System.out.println("ConnectionManagerESim: simulating a lost client DATA packet");
						printInformation(receiveClientPacket);
					}// end if
					//we go back to operating as normal
					mode = 0;

					return false;
				}//end if

				// send to the server
				serverSend();

				if (sendServerPacket.getLength() < DATA_SIZE) {
					lastPacketWrite = true;
					trueLastPacket[0] = sendServerPacket.getData()[2];
					trueLastPacket[1] = sendServerPacket.getData()[3];
				}// end if
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

			if(debug)
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
				if (errorReceived || lastPacketRead)	
					return true;	// Last packet is now sent. The thread will close
				serverReceive();
				// check  to see if this is the data packet we want to delay
				if (foundPacket(receiveServerPacket)) {
					if(debug)
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
						if(debug)
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
				if (errorReceived || lastPacketRead)	
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
					if(debug)
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
				if (errorReceived || lastPacketWrite)
					return true;	// Last packet is now sent. The thread will close
				firstPacket = false;
				return false;
			}// end if

			else if (packetType == 3) { // DATA packet being delayed from the client 

				if (!firstPacket) {
					clientReceive();
					// check  to see if this is the DATA packet we want to delay
					if (foundPacket(receiveClientPacket)) {
						if(debug)
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
				if (errorReceived || lastPacketWrite)
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
			if (debug)
				System.out.println("ConnectionManagerESim: RRQ/WRQ packet will be duplicated. Sending first packet");
			serverSend();
			if(debug)
				System.out.println("ConnectionManagerESim: Thread will sleep now");
			try {
				Thread.sleep(delay);	//delays the packet for the specified amount
			}// end  try
			catch (InterruptedException e) { } // end catch
			if (debug)
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
				if (errorReceived || (lastPacketRead && trueLastPacket[0] == sendServerPacket.getData()[2] && trueLastPacket[1] == sendServerPacket.getData()[3]))	
					return true;	// Last packet is now sent. The thread will close

				serverReceive();
				// check  to see if this is the data packet we want to duplicate
				if (foundPacket(receiveServerPacket)) {
					if (debug)
						System.out.println("ConnectionManagerESim: DATA packet will be duplicated. Sending first packet");
					clientSend();
					if(debug)
						System.out.println("ConnectionManagerESim: Thread will sleep now");
					try {
						Thread.sleep(delay);	//delays the packet for the specified amount
					}// end try
					catch (InterruptedException e) { } // end catch
					if (debug)
						System.out.println("ConnectionManagerESim: Second duplicate packet will be sent and was delayed by: " + delay + "ms!");
					// switch back to normal operation
					mode = 0;
					clientSend();
					// this will ensure that we exit out of the program if we are duplicating the last DATA packet
					if (!firstPacket) {
						if (debug)
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
					if (errorReceived || (lastPacketRead && trueLastPacket[0] == sendServerPacket.getData()[2] && trueLastPacket[1] == sendServerPacket.getData()[3])){
						return true;
					}// end if
					else {
						serverReceive();
						clientSend();
						if (!firstPacket) {
							if (debug)
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
						if (errorReceived || (lastPacketRead && trueLastPacket[0] == sendServerPacket.getData()[2] && trueLastPacket[1] == sendServerPacket.getData()[3])){
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
						if (debug)
							System.out.println("ConnectionManagerESim: ACK packet will be duplicated. Sending first packet");
						serverSend();
						if(debug)
							System.out.println("ConnectionManagerESim: Thread will sleep now");
						try {
							Thread.sleep(delay);	//delays the packet for the specified amount
						}// end try
						catch (InterruptedException e) { } // end catch
						if (debug)
							System.out.println("ConnectionManagerESim: Second duplicate packet will be sent and was delayed by: " + delay + "ms!");
						// switch back to normal operation
						mode = 0;
						serverSend();
						// this will exit out if we are duplicating the last ACK packet
						if (errorReceived || (lastPacketRead && trueLastPacket[0] == sendServerPacket.getData()[2] && trueLastPacket[1] == sendServerPacket.getData()[3]))
							return true;
						serverReceive();
						clientSend();
						// this will ensure that we exit out of the program if we are duplicating the last ACK packet
						if (!firstPacket) {
							if (debug)
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
						if (errorReceived || (lastPacketRead && trueLastPacket[0] == sendServerPacket.getData()[2] && trueLastPacket[1] == sendServerPacket.getData()[3]))	
							return true;	// Last packet is now sent. The thread will close
						firstPacket = false;
					}
				} // end if

				serverSend();
				if (errorReceived || (lastPacketRead && trueLastPacket[0] == sendServerPacket.getData()[2] && trueLastPacket[1] == sendServerPacket.getData()[3]))	
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
					if (debug)
						System.out.println("ConnectionManagerESim: DATA packet will be duplicated. Sending first packet");
					serverSend();
					if(debug)
						System.out.println("ConnectionManagerESim: Thread will sleep now");
					try {
						Thread.sleep(delay);	//delays the packet for the specified amount
					}// end try
					catch (InterruptedException e) { } // end catch
					if (debug)
						System.out.println("ConnectionManagerESim: Second duplicate packet will be sent and was delayed by: " + delay + "ms!");
					// switch back to normal operation
					mode = 0;
					serverSend();
					if (!firstPacket) {
						if (debug)
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
					if (errorReceived || (lastPacketWrite && trueLastPacket[0] == sendClientPacket.getData()[2] && trueLastPacket[1] == sendClientPacket.getData()[3])){	
						return true;	// Last packet is now sent. The thread will close
					}// end if
					else {
						clientReceive();
						serverSend();
						if (!firstPacket) {
							if (debug)
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
						if (errorReceived || (lastPacketWrite && trueLastPacket[0] == sendClientPacket.getData()[2] && trueLastPacket[1] == sendClientPacket.getData()[3])){	
							return true;	// Last packet is now sent. The thread will close
						}// end if
						serverReceive();
						clientSend();
						if (errorReceived || (lastPacketWrite && trueLastPacket[0] == sendClientPacket.getData()[2] && trueLastPacket[1] == sendClientPacket.getData()[3])){	
							return true;	// Last packet is now sent. The thread will close
						}// end if
						return false;
					}// end else
				}// end if


				serverSend();
				if (!firstPacket) {
					if (debug)
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
				if (errorReceived || (lastPacketWrite && trueLastPacket[0] == sendClientPacket.getData()[2] && trueLastPacket[1] == sendClientPacket.getData()[3]))	
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
					if (debug)
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
					if (debug)
						System.out.println("ConnectionManagerESim: ACK packet will be duplicated. Sending first packet");
					clientSend();
					if(debug)
						System.out.println("ConnectionManagerESim: Thread will sleep now");
					try {
						Thread.sleep(delay);	//delays the packet for the specified amount
					}// end try
					catch (InterruptedException e) { } // end catch
					if (debug)
						System.out.println("ConnectionManagerESim: Second duplicate packet will be sent and was delayed by: " + delay + "ms!");
					// switch back to normal operation
					mode = 0;
				}
				clientSend();
				if (errorReceived || (lastPacketWrite && trueLastPacket[0] == sendClientPacket.getData()[2] && trueLastPacket[1] == sendClientPacket.getData()[3]))	
					return true;	// Last packet is now sent. The thread will close
				firstPacket = false;
				return false;
			}// end else if
		}// end else if
		return false;
	}// end method

	/**
	 * The following is the run method for ConnectionManagerESim with a corrupt packet, used to execute code upon starting the thread. 
	 * User will be able to specify which packet to corrupt and how it is corrupted
	 * @since June 4 2014
	 * 
	 * @version June 4 2014
	 * @author Mohammed Ahmed-Muhsin & Samson Truong  
	 * @param x is the position of the corruption
	 */
	private boolean corruptOp()
	{
		// check to see if the which packet is the one being delayed
		if (packetType == 1 || packetType == 2) { // RRQ or WRQ packet is being corrupt 
			if(debug)
				System.out.println("ConnectionManagerESim: RRQ/WRQ packet will be corrupted.");
			int x = corruptPos() ;
			clientData[x] = (byte)7;	//All read and write request corruption will be turned into an invalid
			serverSend();

			serverReceive();

			clientSend();

			return true;
		}// end if

		// now we check if this is a read or write so we can delay on the appropriate packet
		else if (requestType == Request.READ) { // this is a read request
			if (packetType == 3) { // DATA packet being corrupt from the server 

				if (!firstPacket)
					clientReceive();

				serverSend();
				if (errorReceived || lastPacketRead)	
					return true;	// Last packet is now sent. The thread will close
				serverReceive();
				// check  to see if this is the data packet we want to corrupt
				if (foundPacket(receiveServerPacket)) {
					if(debug) {
						System.out.println("ConnectionManagerESim: DATA packet will be corrupt.");
					}
					int x = corruptPos() ;
					if(packetNumber == 7)
						serverData[x] = (byte)8;	//Packet will now have an invalid request
					else 
						serverData[x] = (byte)7;	//Packet will now have an invalid request
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
						if(debug)
							System.out.println("ConnectionManagerESim: ACK packet will be corrupted.");
						int x = corruptPos() ;
						if(packetNumber == 7)
							clientData[x] = (byte)8;	//Packet will now have an invalid request
						else 
							clientData[x] = (byte)7;	//Packet will now have an invalid request
					} // end if
				}
				serverSend();
				if (errorReceived || lastPacketRead)	
					return true;	// Last packet is now sent. The thread will close

				serverReceive();
				clientSend();
				if (errorReceived)	
					return true;	// Last packet is now sent. The thread will close
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
				if (errorReceived)
					return true;	// Last packet is now sent. The thread will close
				if (!firstPacket) {
					if(sendServerPacket.getLength() < DATA_SIZE)
						lastPacketWrite = true;	
				} // end if
				serverReceive();
				// check  to see if this is the ACK packet we want to corrupt
				if (foundPacket(receiveServerPacket)) {
					if(debug)
						System.out.println("ConnectionManagerESim: ACK packet will be corrupted.");
					int x = corruptPos() ;
					if(packetNumber == 7)
						serverData[x] = (byte)8;	//Packet will now have an invalid request
					else 
						serverData[x] = (byte)7;	//Packet will now have an invalid request
				} // end if

				clientSend();
				if (errorReceived || lastPacketWrite)
					return true;	// Last packet is now sent. The thread will close
				firstPacket = false;
				return false;
			}// end if

			else if (packetType == 3) { // DATA packet being corrupted from the client 

				if (!firstPacket) {
					clientReceive();
					// check  to see if this is the DATA packet we want to corrupt
					if (foundPacket(receiveClientPacket)) {
						if(debug)
							System.out.println("ConnectionManagerESim: DATA packet will be corrupted.");
						int x = corruptPos() ;
						if(packetNumber == 7)
							clientData[x] = (byte)8;	//Packet will now have an invalid request
						else 
							clientData[x] = (byte)7;	//Packet will now have an invalid request
					} // end if
				}
				serverSend();
				if (!firstPacket) {
					if(sendServerPacket.getLength() < DATA_SIZE)
						lastPacketWrite = true;	
				} // end if
				serverReceive();
				clientSend();
				if (errorReceived || lastPacketWrite)
					return true;	// Last packet is now sent. The thread will close			
				firstPacket = false;
				return false;
			}// end if
		}// end else if
		return false;
	} //end method

	/** 
	 * The following method will simulate an invalid file mode on the request packets
	 * The server must ensure that the packet has a valid file mode: netascii or octet
	 * If not, they must send error code 04 to the other party and shuts down
	 * @since June 11 2014
	 * Latest Change: Added method to handle file mode
	 * @version June 11 2014
	 * @author Mohammed Ahmed-Muhsin & Samson Truong 
	 */
	private boolean corruptMode() {
		if (packetType == 1 || packetType == 2){ // corrupting the appropriate request for a file mode
			if (debug)
				System.out.println("ConnectionManagerESim: corrupting File Mode in request");			

			int count = 0;
			int index = 0;
			// parses the request, to change the file mode
			for (int i = 0; i < clientData.length; i++) {
				if (clientData[i] == (byte)0)
					count++;
				if (count == 2) {
					index = i; 
					break;
				}
			}// end for
			clientData[index+1] = (byte)48;

			if (debug)
				System.out.println("ConnectionManagerESim: simulating a corrupted request packet");
			// send server invalid file mode	
			serverSend();
			// receive error packet from server
			serverReceive();
			// send error to client
			clientSend();
			return true; 
		}
		mode = 0;
		return false;
	}

	/** 
	 * The following method will simulate an invalid TID on the packets
	 * The client and server must ensure that the packet is coming from the expected source
	 * If not, they must send error code 05 to the other party and continue working 
	 * @since June 5 2014
	 * Latest Change: Fixed to ensure proper end in our method
	 * @version June 11 2014
	 * @author Mohammed Ahmed-Muhsin & Samson Truong 
	 */
	private boolean invalidTID(){
		// check if this is a read request or a write request
		if (requestType == Request.READ){ // this is a read request
			// ************* ACK PACKET ***************
			if (packetType == 4){ // check to corrupt TID for ACK packet
				if (debug)
					System.out.println("ConnectionManagerESim: checking to corrupt TID of ACK packet");
				if (!lastPacketRead) {

					if (!firstPacket) {
						// receive from client
						clientReceive();
					}// end if
					firstPacket = false; // this is no longer the first packet
					// check to see if this is the packet that we want to corrupt
					if (foundPacket(receiveClientPacket)) { // this is the packet we want to lose
						if (debug) {
							System.out.println("ConnectionManagerESim: simulating a corrupt TID client ACK packet");
							printInformation(receiveClientPacket);
						}// end if
						// corrupt the packet being sent to the server
						corruptPortServer();
						//we go back to operating as normal
						mode = 0;
					}//end if

					else { // this is not the packet we want to corrupt, send to server
						serverSend();
						if (lastPacketRead)	
							return true;	// Last packet is now sent. The thread will close
					}//end else

					// we need to wait on a server packet
					serverReceive();

					//send to the client
					clientSend();
					//check to see if this is the last packet (DATA < 512b 
					if(sendClientPacket.getLength() < DATA_SIZE) {
						lastPacketRead = true;
						trueLastPacket[0] = sendClientPacket.getData()[2];
						trueLastPacket[1] = sendClientPacket.getData()[3];
					}// end if
					return false;					
				} // end if
				else if (lastPacketRead) {
					// receive from client
					clientReceive();
					// check to see if this is the packet that we want to corrupt
					if (foundPacket(receiveClientPacket)) { // this is the packet we want to corrupt
						if (debug) {
							System.out.println("ConnectionManagerESim: simulating a corrupt TID client ACK packet");
							printInformation(receiveClientPacket);
						}// end if
						// corrupt the packet being sent to the server
						corruptPortServer();
						//we go back to operating as normal
						mode = 0;
						return true;
					}//end if
					else { // this is not the packet we want to corrupt, send it to server
						serverSend();
						return true; //we're done, shut down thread
					}// end else
				}//end else if
			}// end else if

			// ************* DATA PACKET ***************
			else if (packetType == 3){ // check to lose a DATA packet
				if (debug)
					System.out.println("ConnectionManagerESim: checking to corrupt TID for a DATA packet");
				if (!firstPacket) {
					// receive from client
					clientReceive();
				}// end if
				firstPacket = false; // this is no longer the first packet

				serverSend();
				if (lastPacketRead)	
					return true;	// Last packet is now sent. The thread will close
				// we need to wait on a server packet
				serverReceive();
				// check to see if this is the packet that we want to corrupt
				if (foundPacket(receiveServerPacket)) { // this is the packet we want to corrupt
					if (debug) {
						System.out.println("ConnectionManagerESim: simulating a corrupt TID server DATA packet");
						printInformation(receiveServerPacket);
					}// end if
					// corrupt the packet being sent to the server
					corruptPortClient();
					//we go back to operating as normal
					mode = 0;
					serverReceive();
				}//end if

				//send to the client
				clientSend();
				//check to see if this is the last packet (DATA < 512b 
				if(sendClientPacket.getLength() < DATA_SIZE) {
					lastPacketRead = true;
					trueLastPacket[0] = sendClientPacket.getData()[2];
					trueLastPacket[1] = sendClientPacket.getData()[3];
				}// end if
				return false;	
			} // end else if
		}//end if
		else if (requestType == Request.WRITE){ // this is a write request
			// ************* ACK PACKET ***************
			if (packetType == 4){ // check to lose a ACK packet
				if (debug)
					System.out.println("ConnectionManagerESim: checking to lose ACK packet");
				if (firstPacket) { // if this is the first packet
					serverSend();
					serverReceive();
					firstPacket = false; // this is no longer the first packet
					clientSend();
					return false;
				}// end if		
				clientReceive();
				// check to see if packet is < 512 bytes
				if (receiveClientPacket.getLength() < DATA_SIZE) {
					lastPacketWrite = true;
				}

				serverSend();

				// we need to wait on a server packet
				serverReceive();
				// check to see if this is the packet that we want to corrupt
				if (foundPacket(receiveServerPacket)) { // this is the packet we want to corrupt
					if (debug) {
						System.out.println("ConnectionManagerESim: simulating a corrupt TID server ACK packet");
						printInformation(receiveServerPacket);
					}// end if
					// corrupt the packet being sent to the server
					corruptPortClient();
					//we go back to operating as normal
					mode = 0;

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
			else if (packetType == 3){ // check to corrupt a DATA packet
				if (debug)
					System.out.println("ConnectionManagerESim: checking to lose a DATA packet");

				if (!firstPacket) {
					// receive from client
					clientReceive();
				}// end if

				// check to see if this is the packet that we want to corrupt
				if (foundPacket(receiveClientPacket)) { // this is the packet we want to corrupt
					if (debug) {
						System.out.println("ConnectionManagerESim: simulating a corrupt TID client DATA packet");
						printInformation(receiveClientPacket);
					}// end if
					// corrupt the packet being sent to the server
					corruptPortServer();
					clientReceive();
					serverSend();
					if (sendServerPacket.getLength() < DATA_SIZE && !firstPacket) {
						lastPacketWrite = true;
						trueLastPacket[0] = sendServerPacket.getData()[2];
						trueLastPacket[1] = sendServerPacket.getData()[3];
					} // end if
					//we go back to operating as normal
					mode = 0;
				}//end if
				else {
					// send to the server
					serverSend();
				} // end else
				if (sendServerPacket.getLength() < DATA_SIZE && !firstPacket) {
					lastPacketWrite = true;
					trueLastPacket[0] = sendServerPacket.getData()[2];
					trueLastPacket[1] = sendServerPacket.getData()[3];
				} // end if
				// we need to wait on a server packet
				serverReceive(); 

				//send to the client
				clientSend();
				if (lastPacketWrite)
					return true;	// Last packet is now sent. The thread will close
				firstPacket = false; // this is no longer the first packet
				return false;					
			} // end else if
		}//end if
		return false;
	}// end method

	/**
	 * The following will be the method to RECEIVE CLIENT PACKETS
	 * 
	 * @since May 27 2014
	 * 
	 * Latest Change: Improved output
	 * @version June 12 2014
	 * @author Mohammed Ahmed-Muhsin
	 */	
	private void clientReceive() {
		if (debug)
			System.out.println("ConnectionManagerESim: Waiting to receive packet from client");

		receiveClientPacket = new DatagramPacket(clientReply, clientReply.length);
		try { // wait to receive client packet
			clientSocket.receive(receiveClientPacket);
		}//end try 
		catch (IOException ie) {
			System.err.println("IOException error: " + ie.getMessage());
		}//end catch
		if (receiveClientPacket.getData()[1] == (byte)5){
			errorReceived = true;
			errorCode[0] = receiveServerPacket.getData()[2];
			errorCode[1] = receiveServerPacket.getData()[3];
		} // end if
		if (debug) {
			System.out.println("ConnectionManagerESim: Received packet from client");
			printInformation(receiveClientPacket);
		}
		// updating the data and length in the packet being sent to the server
		clientData = receiveClientPacket.getData();
		clientLength = receiveClientPacket.getLength();
	}

	/**
	 * The following will be the method to SEND CLIENT PACKETS
	 * 
	 * @since May 27 2014
	 * 
	 * Latest Change: Improved output
	 * @version June 12 2014
	 * @author Samson Truong & Mohammed Ahmed-Muhsin 
	 */	
	private void clientSend(){

		if (debug)
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
		if (debug){
			System.out.println("ConnectionManagerESim: Packet sent to client");
			printInformation(sendClientPacket);
		}// end if
	}

	/**
	 * The following will be the method to RECEIVE SERVER PACKETS
	 * 
	 * @since May 27 2014
	 * 
	 * Latest Change: Improved output
	 * @version June 12 2014
	 * @author Mohammed Ahmed-Muhsin
	 */	
	private void serverReceive(){

		if (debug)
			System.out.println("ConnectionManagerESim: Waiting to receive a packet from server...\n");

		receiveServerPacket = new DatagramPacket(serverReply, serverReply.length);

		// block until you receive a packet from the server
		try {
			serverSocket.receive(receiveServerPacket);
		} // end try 
		catch (IOException ioe) {
			System.err.println("Unknown IO exception error: " + ioe.getMessage());
		} // end catch

		if (receiveServerPacket.getData()[1] == (byte)5) {
			errorReceived = true;
			errorCode[0] = receiveServerPacket.getData()[2];
			errorCode[1] = receiveServerPacket.getData()[3];
		}
		serverData = receiveServerPacket.getData();
		serverLength = receiveServerPacket.getLength();
		// set the serverPort to the port we have just received it from (meaning to the Server Thread that will deal with this request
		serverPort = receiveServerPacket.getPort();

		if (debug) {
			System.out.println("ConnectionManagerESim: Received packet from server");
			printInformation(receiveServerPacket);
		}// end if
	}

	/**
	 * The following will be the method to SEND SERVER PACKETS
	 * 
	 * @since May 27 2014
	 * 
	 * Latest Change: Improved output
	 * @version June 12 2014
	 * @author Samson Truong & Mohammed Ahmed-Muhsin 
	 */	
	private void serverSend() {
		if (debug)
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
		if (debug) {
			System.out.println("ConnectionManagerESim: Packet sent to server");
			printInformation(sendServerPacket);
		}// end if
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
		if (debug)
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
			if (debug)
				System.out.println("ConnectionManagerESim: this is the correct packet type, checking if it is the correct number..");
			if (blk[0] == blkCheck[0] && blk[1] == blkCheck[1]){
				if (debug)
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
		if (debug)
			System.out.println("Block number: " + Integer.toHexString(blockNum[0]) + "" + Integer.toHexString(blockNum[1]));
		return blockNum;
	} // end method

	/**
	 * This method will give us the block number in which to corrupt
	 * 
	 * @return the position of the corruption point
	 * @since June 4 2014
	 * 
	 * @version June 4 2014
	 * @author Samson Truong 
	 */	
	private int corruptPos()
	{
		if (mode == 4)	//Invalid Packet Type
			return 1;	//position of the packet type is 1
		else if (mode == 5)	//Invalid block number
			return 3;	//position of the block number is 2 and 3
		else if (mode == 7)	//Invalid packet size
			if (requestType == Request.WRITE) { // this is a write request
				if (packetType == 4){  // ACK packet being delayed from the server 
					byte[] tempData = new byte[5];
					System.arraycopy(serverData, 0, tempData, 0, 4);
					serverData = tempData;
					serverLength = 5;
					return 4;	//This will add an extra byte to an ACK
				}
				else if (packetType == 3){	// DATA
					byte[] tempData = new byte[517];
					System.arraycopy(clientData, 0, tempData, 0, 516);
					clientData = tempData;
					clientLength = 517;
					return 516;	//this will add an extra byte to the end of the data, (if data is not 512, then this will have no affect)
				}
			}
			else if (requestType == Request.READ) {
				if (packetType == 4){  // ACK packet being delayed from the client
					byte[] tempData = new byte[5];
					System.arraycopy(clientData, 0, tempData, 0, 4);
					clientData = tempData;
					clientLength = 5;
					return 4;	//This will add an extra byte to an ACK
				}
				else if (packetType == 3){
					byte[] tempData = new byte[517];
					System.arraycopy(serverData, 0, tempData, 0, 516);
					serverData = tempData;
					serverLength = 517;
					return 516;	//this will add an extra byte to the end of the data, (if data is not 512, then this will have no affect)
				}
			}
		System.out.println("Corrupt position 0");
		return 0;
	}

	/** 
	 * The following method will create a corrupt TID to send to the server
	 * @since June 5 2014
	 * 
	 * Latest Change: Print the error message received from the client
	 * @version June 11 2014
	 * @author Mohammed Ahmed-Muhsin & Samson Truong 
	 */
	private void corruptPortServer() {
		// initialize the DatagramSocket corruptSocket
		try {
			corruptSocket = new DatagramSocket();
		} // end try 
		catch (SocketException se) {
			System.err.println("SocketException: " + se.getMessage());
		} // end catch

		if (debug)
			System.out.println("ConnectionManageESim: Preparing corrupt packet to send to Server");
		// prepare the new send packet to the server
		try {
			sendServerPacket = new DatagramPacket(clientData, clientLength, InetAddress.getLocalHost(), serverPort);
		} // end try 
		catch (UnknownHostException uhe) {
			System.err.println("Unknown host exception error: " + uhe.getMessage());
		} // end catch

		// send the packet to the server via the corruptSocket to server port
		try {
			corruptSocket.send(sendServerPacket);
		} // end try 
		catch (IOException ioe) {
			System.err.println("Unknown IO exception error: " + ioe.getMessage());
		} // end catch

		// print confirmation message that the packet has been sent to the server
		if (debug)
			System.out.println("ConnectionManagerESim: Corrupt packet sent to server");
		if(debug)
			printInformation(sendServerPacket);

		// wait to receive the error packet 05 from the server
		if (debug)
			System.out.println("ConnectionManagerESim: Waiting to receive a packet from server...\n");

		receiveServerPacket = new DatagramPacket(serverReply, serverReply.length);

		// block until you receive a packet from the server
		try {
			corruptSocket.receive(receiveServerPacket);
		} // end try 
		catch (IOException ioe) {
			System.err.println("Unknown IO exception error: " + ioe.getMessage());
		} // end catch

		if(debug) // print out information about the packet received from the server if verbose
			printInformation(receiveServerPacket);
		if (receiveServerPacket.getData()[1] == (byte)5 && receiveServerPacket.getData()[2] == (byte)0 && receiveServerPacket.getData()[3] == (byte)5) {
			if(debug) 
				System.out.println("ConnectionManagerESim: server has sent us an error packet with error code 5");
			System.out.println("ConnectionManagerESim: server has the following error message: " + new String(receiveServerPacket.getData()));
		}
		else {
			if (debug)
				System.out.println("ConnectionManagerESim: Server did not process the invalid TID properly!");
		}
		corruptSocket.close();
	}

	/** 
	 * The following method will create a corrupt TID to send to the client
	 * @since June 5 2014
	 * Latest Change: Added the method
	 * @version June 4 2014
	 * @author Mohammed Ahmed-Muhsin & Samson Truong 
	 */
	private void corruptPortClient() {
		// initialize the DatagramSocket corruptSocket
		try {
			corruptSocket = new DatagramSocket();
		} // end try 
		catch (SocketException se) {
			System.err.println("SocketException: " + se.getMessage());
		} // end catch

		if (debug)
			System.out.println("ConnectionManageESim: Preparing corrupt packet to send to Client");
		// prepare the new send packet to the client
		try {
			sendClientPacket = new DatagramPacket(serverData, serverLength, InetAddress.getLocalHost(), clientPort);
		} // end try 
		catch (UnknownHostException uhe) {
			System.err.println("Unknown host exception error: " + uhe.getMessage());
		} // end catch

		// send the packet to the client via the corruptSocket to client port
		try {
			corruptSocket.send(sendClientPacket);
		} // end try 
		catch (IOException ioe) {
			System.err.println("Unknown IO exception error: " + ioe.getMessage());
		} // end catch

		// print confirmation message that the packet has been sent to the client
		if (debug)
			System.out.println("ConnectionManagerESim: Corrupt packet sent to client");
		if(debug)
			printInformation(sendClientPacket);

		// wait to receive the error packet 05 from the client
		if (debug)
			System.out.println("ConnectionManagerESim: Waiting to receive a packet from client...\n");

		receiveClientPacket = new DatagramPacket(clientReply, clientReply.length);

		// block until you receive a packet from the client
		try {
			corruptSocket.receive(receiveClientPacket);
		} // end try 
		catch (IOException ioe) {
			System.err.println("Unknown IO exception error: " + ioe.getMessage());
		} // end catch

		if(debug) // print out information about the packet received from the client if verbose
			printInformation(receiveClientPacket);
		if (receiveClientPacket.getData()[1] == (byte)5 && receiveClientPacket.getData()[2] == (byte)0 && receiveClientPacket.getData()[3] == (byte)5) {
			if(debug) 
				System.out.println("ConnectionManagerESim: client has sent us an error packet with error code 5");
			System.out.println("ConnectionManagerESim: client has the following error message: " + new String(receiveClientPacket.getData()));

		}
		else {
			if (debug)
				System.out.println("ConnectionManagerESim: Client did not process the invalid TID properly!");
		}
		corruptSocket.close();
	}
} // end class
