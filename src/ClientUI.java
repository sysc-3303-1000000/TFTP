import java.io.*;
/**
 * The following is implementation for the Client User Interface
 * 
 * @since May 21 2014
 * 
 * @author 1000000
 * @version May 31 2014
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
	 * Latest Change: Now catch numberformatexceptions and act accordingly
	 * @version May 31 2014
	 * @author Kais
	 * 
	 */
	public static void main(String[] args) throws IOException {
		BufferedReader br = new BufferedReader( new InputStreamReader(System.in));
		int exit = 0;
		String directory = null;
		String filename = null;
		int rw = 0;
		
		do {
			do {
				try {
					System.out.println("Would you like to perform a read or a write? (1 - read, 2 - write)");
					rw = Integer.parseInt(br.readLine());
				} // end try
				catch (NumberFormatException nfe) {} // end catch
			} while (rw != 1 && rw != 2); // end dowhile
		
			if (rw == 1) {
				System.out.println("What is the name of the file you wish to read from the server? (i.e. 'Test.txt')");
				filename = br.readLine();
				System.out.println("Which directory would you like to save this file into? (i.e. 'C:\\Users\\Kais\\git\\TFTP')");
				directory = br.readLine();
				Thread client = new Client(filename, directory, Request.READ);
				client.start();
				while (client.getState() != Thread.State.TERMINATED) {} // end whileloop
			} // end if
			else if (rw == 2) {
				System.out.println("What is the name of the file you wish to write to the server? (i.e. 'Test.txt')");
				filename = br.readLine();
				System.out.println("Which directory is this file located in? (i.e. 'C:\\Users\\Kais\\git\\TFTP')");
				directory = br.readLine();
				Thread client = new Client(filename, directory, Request.WRITE);
				client.start();
				while (client.getState() != Thread.State.TERMINATED) {} // end whileloop
			} // end if
			try {
				System.out.println("Would you like to invoke another read or write? (0 - Yes, anything else - No)");
				exit = Integer.parseInt(br.readLine());
			} // end try
			catch (NumberFormatException nfe) { exit = 1;} // end catch
		} while (exit == 0); // end dowhile
		System.out.println("You have ended the TFTP session");
	} // end method

} // end class