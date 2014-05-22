import java.io.IOException;
import java.net.*;
import java.io.*;
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
 * @version May 21 2014
 *
 */
public class ErrorSim {
	public static final int DATA_SIZE = 516;
	
	// declare socket
	private DatagramSocket receiveSocket;
	
	// declare the packet
	private DatagramPacket receiveClientPacket;
	
	private byte data[];
	// will determine if we go into verbose mode or a silent mode
	// to have full implementation in a later version
	private boolean verbose;
	
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
	public ErrorSim(boolean verbose, int choice) {
		data = new byte[DATA_SIZE];
		this.verbose = verbose;
		// initialize the DatagramSocket receiveSocket to bind to well-known port 68
		try {
			receiveSocket = new DatagramSocket(68);
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
		System.out.println("*******************************************");
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
		if (userChoice == 0) {
				System.out.println("ErrorSim will be running in Normal mode");
			}
			
			else if (userChoice == 1) {
				System.out.println("ErrorSim will be running in Lost packet mode");
			}
			
			else if (userChoice == 2) {
				System.out.println("ErrorSim will be running in Delayed packet mode");

			}
			
			else if (userChoice == 3) {
				System.out.println("ErrorSim will be running in Duplicate packet mode");
			}
			System.out.println("Error Simulator is waiting for new client request!");
			
		for(;;) 
		{
			try { // wait to receive the packet from client
				receiveSocket.receive(receiveClientPacket);
			} // end try 
			catch (IOException ie) {
				System.err.println("IOException error: " + ie.getMessage());
			} // end catch
								
			if(verbose)
				printPacketInfo(receiveClientPacket);
				
			Thread connectionmanager = new ConnectionManagerESim(verbose, userChoice, data, receiveClientPacket.getPort(), receiveClientPacket.getLength());
			connectionmanager.start();
		} // end forloop
	}
	
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
		}
		
		// shutdown the listener 
		if (userChoice == 9) {
			System.out.println ("Error Simulator will shut down");
			System.exit(0);
		}
		ErrorSim esim = new ErrorSim(true, userChoice);
		
		// close the Scanner
		in.close();
		esim.sendReceive();
	} // end method
} // end class
