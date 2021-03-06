import java.io.IOException;
import java.net.*;
import java.util.Scanner;

/**
 * The following is implementation for the ErrorSim
 * The error simulator is responsible for selecting the different modes including a normal mode or changing packets, corrupting, etc.
 * You need to have one ErrorSim running per unique client if you want to simulate different things as the operation 
 * will need to be reset each time. 
 * 
 * @author Mohammed Ahmed-Muhsin & Samson Truong
 * @version June 14 2014
 *
 */
public class ErrorSim {
	public static final int DATA_SIZE = 516;
	
	// declare socket
	private static DatagramSocket receiveSocket;
	
	// declare the packet
	private DatagramPacket receiveClientPacket;
	// the byte array for the data being stored
	private byte data[];
	// will determine the mode to work in
	private static int output;
	// value of how much we want to delay the packet by -- default to 0 if we are not running in delayed mode or duplicate
	private int delayAmount = 0;
	// stores which packet type we are altering -- default is 0 if we are running normally
	private int packetType = 0;
	// stores which packet number we are altering -- default is 0 if we are running normally
	private int packetNumber = 0;
	// stores the IP address of the server
	private InetAddress serverIP;
	private InetAddress clientIP;
	
	/**
	 * Will determine the user input based on an integer value
	 * 0 - Normal
	 * 1 - Lose a packet
	 * 2 - Delay a packet
	 * 3 - Duplicate
	 * 4 - Invalid packet type
	 * 5 - Invalid block number
	 * 6 - Invalid file mode
	 * 7 - Invalid packet size
	 * 8 - Invalid TID
	 * 9 - Shutdown
	 * 10 - Set output level
	 * 11 - Set IP Address
	 */
	private static int userChoice;
	

	@SuppressWarnings("static-access")
	public ErrorSim(int output, InetAddress ip) {	
		data = new byte[DATA_SIZE];
		if (ip == null) { // if no IP is specified, use local
			System.out.println("No IP specified, using local host by default");
			try {
				ip = InetAddress.getLocalHost();
			} catch (UnknownHostException e) {
				System.err.println("UnknownHostException: " + e.getMessage());
			}
		}
		else {
			System.out.println("Server IP being used: " + ip.toString());
			serverIP = ip;
		}
		this.output = output; // initialize the level of output to medium by default
		// initialize the DatagramSocket receiveSocket to bind to well-known port 68
		try {
			receiveSocket = new DatagramSocket(2068);
		} // end try 
		catch (SocketException se) {
			System.err.println("SocketException: " + se.getMessage());
		} // end catch
		
		receiveClientPacket = new DatagramPacket(data, data.length);
	} // end constructor 
	
	
	/**
	 * The following will be used to listen to the request being sent in by the client and spawn a new thread to deal 
	 * with that request using the ConnectionManagerESim class
	 * 
	 * @since May 21 2014
	 * 
	 * Latest Change: Added ability to receive a user input to see which error we are simulating
	 * @version May 21 2014
	 * @author Mohammed Ahmed-Muhsin & Samson Truong
	 */	
	public void sendReceive(){
		// NORMAL MODE
		if (userChoice == 0) {
			System.out.println("ErrorSim will be running in Normal mode");
		}
		
		// LOST MODE 
		else if (userChoice == 1) {
			@SuppressWarnings("resource")
			Scanner input = new Scanner(System.in);
			System.out.println("ErrorSim will be running in Lost packet mode");
			System.out.println("Please enter the type of packet you would like to lose:\n1 - RRQ\n2 - WRQ\n3 - DATA\n4 - ACK");
			packetType = input.nextInt();
			
			// check if it is a DATA or ACK that we are changing, grab which packet number
			if (packetType == 3 || packetType == 4){
				System.out.println("Which packet do you want to lose: ");
				packetNumber = input.nextInt();
			}
			
			// otherwise we are changing the RRQ and WRQ so the packet number that we are changing is 1
			else {
				packetNumber = 1;
			}
			
		}
		// DELAYED MODE
		else if (userChoice == 2) {
			@SuppressWarnings("resource")
			Scanner input = new Scanner(System.in);
			System.out.println("ErrorSim will be running in Delayed packet mode");
			System.out.println("Please enter the type of packet you would like to delay:\n1 - RRQ\n2 - WRQ\n3 - DATA\n4 - ACK");
			packetType = input.nextInt();
			// check if it is a DATA or ACK that we are changing, grab which packet number
			if (packetType == 3 || packetType == 4){
				System.out.println("Which packet do you want to lose: ");
				packetNumber = input.nextInt();
			}
			
			// otherwise we are changing the RRQ and WRQ so the packet number that we are changing is 1
			else {
				packetNumber = 1;
			}
			System.out.println("Please enter the delay of the packet (3000 will be 3000 milliseconds): ");
			delayAmount = input.nextInt();

		}
		// DUPLICATE MODE
		else if (userChoice == 3) {
			@SuppressWarnings("resource")
			Scanner input = new Scanner(System.in);
			System.out.println("ErrorSim will be running in Duplicate packet mode");
			System.out.println("Please enter the type of packet you would like to duplicate:\n1 - RRQ\n2 - WRQ\n3 - DATA\n4 - ACK");
			packetType = input.nextInt();
			
			// check if it is a DATA or ACK that we are changing, grab which packet number
			if (packetType == 3 || packetType == 4){
				System.out.println("Which packet do you want to duplicate: ");
				packetNumber = input.nextInt();
			}
			
			// otherwise we are changing the RRQ and WRQ so the packet number that we are changing is 1
			else {
				packetNumber = 1;
			}
			
			System.out.println("Please enter the delay between the duplicated packet (3000 will be 3000 milliseconds): ");
			delayAmount = input.nextInt();
		}
		//Corrupt mode Invalid packet type
		else if( userChoice == 4) {
			@SuppressWarnings("resource")
			Scanner input = new Scanner(System.in);
			System.out.println("ErrorSim will be running in Corrupt packet mode - Invalid packet type");
			System.out.println("Please enter the type of packet you would like to corrupt:\n1 or 2 - R/WRQ\n3 - DATA\n4 - ACK");
			packetType = input.nextInt();
			
			// check if it is a DATA or ACK that we are changing, grab which packet number
			if (packetType == 3 || packetType == 4){
				System.out.println("Which packet do you want to corrupt: ");
				packetNumber = input.nextInt();
			}
			// otherwise we are changing the RRQ and WRQ so the packet number that we are changing is 1
			else {
				packetNumber = 1;
			}		
		}
		//Corrupt mode Invalid block number 
		else if( userChoice == 5) {
			@SuppressWarnings("resource")
			Scanner input = new Scanner(System.in);
			System.out.println("ErrorSim will be running in Corrupt packet mode - Invalid block number");
			System.out.println("Please enter the type of packet you would like to corrupt:\n3 - DATA\n4 - ACK");
			packetType = input.nextInt();
			
			// check if it is a DATA or ACK that we are changing, grab which packet number
			if (packetType == 3 || packetType == 4){
				System.out.println("Which packet do you want to corrupt: ");
				packetNumber = input.nextInt();
			}
			// otherwise we are changing the RRQ and WRQ so the packet number that we are changing is 1
			else {
				System.out.println("Invalid choice");
				System.out.println ("ErrorSim: shutting down");
				System.exit(0);
			}		
		}
		//Corrupt mode Invalid file mode 
		else if( userChoice == 6) {
			@SuppressWarnings("resource")
			Scanner input = new Scanner(System.in);
			System.out.println("ErrorSim will be running in Corrupt packet mode - Invalid file mode");
			System.out.println("Please enter the type of packet you would like to corrupt:\n1 OR 2 R/WRQ");
			packetType = input.nextInt();
			
			// we are changing the RRQ and WRQ so the packet number that we are changing is 1
			if (packetType == 1 || packetType == 2){
				
				packetNumber = 1;
			}
			// otherwise 
			else {
				System.out.println("Invalid choice");
				System.out.println ("ErrorSim: shutting down");
				System.exit(0);
			}		
		}
		//Corrupt mode Invalid size 
		else if( userChoice == 7) {
			@SuppressWarnings("resource")
			Scanner input = new Scanner(System.in);
			System.out.println("ErrorSim will be running in Corrupt packet mode - Invalid size");
			System.out.println("Please enter the type of packet you would like to corrupt:\n3 - DATA\n4 - ACK");
			packetType = input.nextInt();

			// check if it is a DATA or ACK that we are changing, grab which packet number
			if (packetType == 3 || packetType == 4){
				System.out.println("Which packet do you want to corrupt: ");
				packetNumber = input.nextInt();
			}
			// otherwise we are changing the RRQ and WRQ so the packet number that we are changing is 1
			else {
				System.out.println("Invalid choice");
				System.out.println ("ErrorSim: shutting down");
				System.exit(0);
			}		
		}
		//Corrupt mode Invalid TID 
		else if( userChoice == 8) {
			@SuppressWarnings("resource")
			Scanner input = new Scanner(System.in);
			System.out.println("ErrorSim will be running in Corrupt packet mode - Invalid TID");
			System.out.println("Please enter the type of packet you would like to corrupt:\n3 - DATA\n4 - ACK");
			packetType = input.nextInt();
			
			// check if it is a DATA or ACK that we are changing, grab which packet number
			if (packetType == 3 || packetType == 4){
				System.out.println("Which packet do you want to corrupt: ");
				packetNumber = input.nextInt();
			}
			// otherwise we are changing the RRQ and WRQ so the packet number that we are changing is 1
			else {
				System.out.println("Invalid choice");
				System.out.println ("ErrorSim: shutting down");
				System.exit(0);
			}		
		}
		
		System.out.println("Error Simulator is waiting for new client request!");

		try { // wait to receive the packet from client
			receiveSocket.receive(receiveClientPacket);
		} // end try 
		catch (IOException ie) {
			System.err.println("IOException error: " + ie.getMessage());
		} // end catch
		clientIP = receiveClientPacket.getAddress();
		System.out.println("Client packet received from: " + clientIP.toString());
		Thread connectionmanager = new ConnectionManagerESim(serverIP, clientIP, output, userChoice, delayAmount, packetType, packetNumber, data, receiveClientPacket.getPort(), receiveClientPacket.getLength(), verifyReadWrite(receiveClientPacket));
		connectionmanager.start();

		while (connectionmanager.getState() != Thread.State.TERMINATED) {

		}

	}
	
	/**
	 * The following is used to verify the Packet.
	 * @param p DatagramPacket which will be verified
	 * 
	 * @since May 13 2014
	 * 
	 * Latest Change: Added length of packet
	 * @version May 17 2014
	 * @author Colin
	 * 
	 */
	private Request verifyReadWrite(DatagramPacket p) {
		Request r;
		if(p.getData()[0] != (byte)0)
			System.exit(1);
		if(p.getData()[1] == (byte)1)
		{
			r = Request.READ;
			return r;
		}
		else if(p.getData()[1] == (byte)2)
		{
			r = Request.WRITE;
			return r;
		}
		r = Request.ERROR;
		return r;
	} // end method
	
	/**
	 * The following is the run method for ErrorSim, used to execute code upon starting the thread.
	 * It will create a new thread for every packet received and this new thread will send the response
	 * to the server.
	 * 
	 * @since May 11 2014
	 * 
	 * Latest Change: Added functionality to receive an output
	 * @version June 12 2014
	 * @author Mohammed Ahmed-Muhsin & Samson Truong
	 */
	public static void main(String[] args) {
		// the scanner to receive a user input
		Scanner in = new Scanner(System.in);
		// will hold the IP address chosen
		InetAddress ip = null;
		// will hold the value if a valid choice has been entered
		boolean validChoice = false;
		boolean shutdown = false;
		
		while (!shutdown) { 
			while (!validChoice) {
				
				// print out information for the user depending on the mode of run they want to use
				System.out.println("0 - Normal\n1 - Lose a packet\n2 - Delay a packet\n3 - Duplicate\n4 - Invalid packet type\n5 - Invalid block number\n6 - Invalid file mode\n7"
						+ " - Invalid packet size\n8 - Invalid TID\n9 - Shutdown\n10 - Set output level\n11 - Set IP Address\n\n");
				System.out.println("Please enter a mode for the Error Simulator to start in:");
				
				userChoice = in.nextInt();
				// check if a valid choice has been entered
				if (userChoice == 0 || userChoice == 1 || userChoice == 2 || userChoice == 3 ||userChoice == 4 ||userChoice == 5 ||userChoice == 6 ||userChoice == 7 || userChoice == 8 || userChoice == 9) {
					validChoice = true;
				}// end if
				else if (userChoice == 10) { // user wants to change the output level
					@SuppressWarnings("resource")
					Scanner input = new Scanner(System.in);
					System.out.println("Please enter the level of output:\n1 - Debug (Full packet information)\n2 - Verbose (Some statements)\n3 - Silent (Minimal output)");
					output = input.nextInt();
					if (output == 1)
						System.out.println("System be running in full debug. This will include packet information as well as error messages and mode changes");
					else if (output == 2) 
						System.out.println("System be running in vebose. Only error messages and mode changes will be displayed");
					else if (output == 3)
						System.out.println("System be running in silent mode. Only errors will be displayed");
					else { 
						output = 2;
						System.out.println("Invalid output setting choice! Using default (Verbose)");
					}
					// go back to main menu
					validChoice = false; 
				}// end else if
				else if (userChoice == 11) { // user wants to specify the IP address
					int choice;
					@SuppressWarnings("resource")
					Scanner input = new Scanner(System.in);
					System.out.println("Would you like to specify an IP address or use the local IP? (1 - Specify IP, 2 - Use Local)"); 
					choice = input.nextInt();
					if (choice == 1) {
						String address;
						@SuppressWarnings("resource")
						Scanner stringInput = new Scanner(System.in);
						System.out.println("Enter the IP address of the Server. (i.e. 192.168.100.106)");
						address = stringInput.nextLine();
						try {
							ip = InetAddress.getByName(address);
						} catch (UnknownHostException e) {
							System.err.println("UnknownHostException: " + e.getMessage());
						}
					}// end if
					else { // any other entry will result in using local IP
						choice = 2;
						try {
							ip = InetAddress.getLocalHost();
						} catch (UnknownHostException e) {
							System.err.println("UnknownHostException: " + e.getMessage());
						}// end catch
					}// end else
				}// end else if
				else {
					System.out.println("Invalid choice entered. Please try again!");
					validChoice = false;
				}// end else
			} // end while
			
			// shutdown the listener 
			if (userChoice == 9) {
				shutdown = true;
			}
			if (ip == null) {
				try {
					ip = InetAddress.getLocalHost();
				} catch (UnknownHostException e) {
					System.err.println("UnknownHostException: " + e.getMessage());
					} // end catch
			}// end if
			ErrorSim esim = new ErrorSim(output, ip);
			
			esim.sendReceive();
			validChoice = false;
			receiveSocket.close();
		}// end while	
		
		// close the Scanner
		in.close();
		System.out.println ("ErrorSim: shutting down");
		System.exit(0);
	} // end method
} // end class
