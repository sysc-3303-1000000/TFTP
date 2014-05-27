import java.io.IOException;
import java.net.*;
import java.util.Scanner;

/**
 * The following is implementation for the ErrorSim
 * For this implementation, the ErrorSim will simulate three types of errors: 
 * 1) Lost packet
 * 2) Delayed packet
 * 3) Duplicate packet
 * 
 * There will also be a user interface to get options to run in Normal mode, any of the errors above, or to shut down the simulation
 * @since May 11 2014
 * 
 * @author 1000000
 * @version May 24 2014
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
	// will determine if we go into verbose mode or a silent mode
	// to have full implementation in a later version
	private boolean verbose;
	// value of how much we want to delay the packet by -- default to 0 if we are not running in delayed mode or duplicate
	private int delayAmount = 0;
	// stores which packet type we are altering -- default is 0 if we are running normally
	private int packetType = 0;
	// stores which packet number we are altering -- default is 0 if we are running normally
	private int packetNumber = 0;
	/**
	 * 	Will determine the user input based on an integer value
	 * 0 Normal packets
	 * 1 Lost packet
	 * 2 Delayed packet
	 * 3 Duplicate packet
	 * 9 Shutdown Listener
	 */
	private static int userChoice;
	
	/**
	 * The following is the run method for ErrorSim, used to execute code upon starting the thread.
	 * It will create a new thread for every packet received and this new thread will send the response
	 * to the server.
	 * 
	 * @since May 16 2014
	 * 
	 * Latest Change: First revision, added basic implementation based on assumed functionality
	 * @version May 16 2014
	 * @author Kais
	 */
	public ErrorSim(boolean verbose) {	
		data = new byte[DATA_SIZE];
		this.verbose = verbose;
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
	 * The following is used to print information about the Packet.
	 * @param p DatagramPacket which will have its information printed
	 * 
	 * @since May 16 2014
	 * 
	 * Latest Change: Added length being sent
	 * @version May 17 2014
	 * @author Colin
	 * 
	 */
	private void printPacketInfo(DatagramPacket p) {
		
		// print out the information on the packet
		System.out.println("PACKET INFORMATION");
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
			System.out.println("Please enter the delay of the packet (3000 will be 3000 milliseconds): ");
			delayAmount = input.nextInt();

		}
		// DUPLICATE MODE
		else if (userChoice == 3) {
			@SuppressWarnings("resource")
			Scanner input = new Scanner(System.in);
			System.out.println("ErrorSim will be running in Duplicate packet mode");
			System.out.println("Please enter the delay between the duplicated packet (3000 will be 3000 milliseconds): ");
			delayAmount = input.nextInt();
		}
		
		System.out.println("Error Simulator is waiting for new client request!");

		try { // wait to receive the packet from client
			receiveSocket.receive(receiveClientPacket);
		} // end try 
		catch (IOException ie) {
			System.err.println("IOException error: " + ie.getMessage());
		} // end catch

		if(verbose)
			printPacketInfo(receiveClientPacket);

		Thread connectionmanager = new ConnectionManagerESim(verbose, userChoice, delayAmount, packetType, packetNumber, data, receiveClientPacket.getPort(), receiveClientPacket.getLength(), verifyReadWrite(receiveClientPacket));
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
			System.exit(1); // TODO properly handle error
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
	 * The main class for the Error Simulator
	 * 
	 * @since May 11 2014
	 * 
	 * Latest Change: Added ability to receive a user input to see which error we are simulating
	 * @version May 21 2014
	 * @author Mohammed Ahmed-Muhsin & Samson Truong
	 */	
	// main class for the Error Simulator
	public static void main(String[] args) {
		// the scanner to receive a user input
		Scanner in = new Scanner(System.in);
		
		// will hold the value if a valid choice has been entered
		boolean validChoice = false;
		boolean shutdown = false;
		
		while (!shutdown) { 
			while (!validChoice) {
				
				// print out information for the user depending on the mode of run they want to use
				System.out.println("0 - Normal\n1 - Lose a packet\n2 - Delay a packet\n3 - Duplicate\n9 - Shutdown\n\n");
				System.out.println("Please enter a mode for the Error Simulator to start in:");
				
				userChoice = in.nextInt();
				// check if a valid choice has been entered
				if (userChoice == 0 || userChoice == 1 || userChoice == 2 || userChoice == 3 || userChoice == 9) {
					validChoice = true;
				}
				else {
					System.out.println("Invalid choice entered. Please try again!");
					validChoice = false;
				}
			} // end while
			
			// shutdown the listener 
			if (userChoice == 9) {
				shutdown = true;
			}
			ErrorSim esim = new ErrorSim(true);

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
