import java.io.*;
import java.net.InetAddress;
/**
 * The following is implementation for the Client User Interface
 * 
 * @since May 21 2014
 * 
 * @author 1000000
 * @version June 5 2014
 *
 */
public class ClientUI {

	/**
	 * Main method for the Client User Interface
	 * @param args not used
	 * @throws IOException if file or folder not present
	 * 
	 * @since May 21 2014
	 * 
	 * Latest Change: Added so that you can specify an IP, and have the option to change upon every request or leave it the same
	 * @version June 14 2014
	 * @author Kais
	 * 
	 */
	public static void main(String[] args) throws IOException {
		BufferedReader br = new BufferedReader( new InputStreamReader(System.in));
		int exit = 0;
		String directory = null;
		String filename = null;
		int rw = 0;
		int socket = 0;
		int localOrNot = 0;
		InetAddress IP = null;
		int setAgain = 1;
		/* Handle which mode the user would like */
		do {
			try {
				System.out.println("Would you like to run in normal mode or error simulator mode? (1 - normal, 2 - error simulator)");
				socket = Integer.parseInt(br.readLine());
			} // end try
			catch (NumberFormatException nfe) {} // end catch
		} while (socket != 1 && socket != 2); // end dowhile
		do {
			if (rw != 0) {
				do { // Handle changing of the IP address
					try {
						System.out.println("Would you like to change the IP address for this next operation? (Current IP is: " + ((localOrNot == 1) ? IP : InetAddress.getLocalHost()) + ") (1 - yes, 2 - no)"); // use local for now
						setAgain = Integer.parseInt(br.readLine());
					} // end try
					catch (NumberFormatException nfe) {} // end catch
				} while (setAgain != 1 && setAgain != 2); // end dowhile
			} // end if
			if (setAgain == 1) {
				do { // Handle which IP address to use
					try {
						System.out.println("Would you like to specify an IP address or use the local IP? (1 - specify, 2 - use local)"); // use local for now
						localOrNot = Integer.parseInt(br.readLine());
					} // end try
					catch (NumberFormatException nfe) {} // end catch
				} while (localOrNot != 1 && localOrNot != 2); // end dowhile

				if (socket == 1 && localOrNot == 1) { // if normal mode and specify IP selected
					System.out.println("Enter the IP address of the Server. (i.e. 192.168.100.106)");
					IP = InetAddress.getByName(br.readLine());
				} // end if
				else if (socket == 2 && localOrNot == 1) { // if error simulator mode and specify IP selected
					System.out.println("Enter the IP address of the Error Simulator. (i.e. 192.168.100.106)");
					IP = InetAddress.getByName(br.readLine());
				} // end if
				setAgain = 2;
			} // end if
			do { // Handle the selection of operation
				try {
					System.out.println("Would you like to perform a read or a write? (1 - read, 2 - write)");
					rw = Integer.parseInt(br.readLine());
				} // end try
				catch (NumberFormatException nfe) {} // end catch
			} while (rw != 1 && rw != 2); // end dowhile

			if (rw == 1) { // if read request
				System.out.println("What is the name of the file you wish to read from the server? (i.e. 'Test.txt')");
				filename = br.readLine();
				System.out.println("Specify the full path of where you would like to save the file, including the filename (i.e. 'C:\\Users\\Kais\\git\\TFTP\\Test.txt')");
				directory = br.readLine();
				Thread client = new Client(filename, directory, Request.READ, (socket == 1) ? 69 : 2068, (localOrNot == 1) ? IP : InetAddress.getLocalHost()); // server or error sim
				client.start();
				while (client.getState() != Thread.State.TERMINATED) {} // end whileloop
			} // end if
			else if (rw == 2) { // if write request
				System.out.println("What would you like to name the file on the server side? (i.e. 'Test.txt')");
				filename = br.readLine();
				System.out.println("Specify the full path of where the file is located. (i.e. 'C:\\Users\\Kais\\git\\TFTP\\Test.txt')");
				directory = br.readLine();
				Thread client = new Client(filename, directory, Request.WRITE, (socket == 1) ? 69 : 2068, (localOrNot == 1) ? IP : InetAddress.getLocalHost()); // server or error sim
				client.start();
				while (client.getState() != Thread.State.TERMINATED) {} // end whileloop
			} // end if
			try { // handle more invocations
				System.out.println("Would you like to invoke another read or write? (0 - Yes, anything else - No)");
				exit = Integer.parseInt(br.readLine());
			} // end try
			catch (NumberFormatException nfe) { exit = 1;} // end catch
		} while (exit == 0); // end dowhile
		System.out.println("You have ended the TFTP session");
	} // end method

} // end class