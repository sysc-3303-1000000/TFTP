import java.io.*;
import java.net.*;
import java.util.*;

/**
 * The following is implementation for the Server UI
 * 
 * @since June 10 2014
 * 
 * @author Colin
 */
public class ServerUI {

	
	/**
	 * Main method for the Server User Interface
	 * @param args not used
	 * 
	 * @throws IOException if invalid input
	 * @throws NumberFormatException if not a number is given
	 * 
	 * @since June 10 2014
	 * 
	 * Latest Change: Added ability to interrupt server
	 * @version June 14 2014
	 * @author Colin
	 * 
	 */
	public static void main(String[] args) throws NumberFormatException, IOException {
		BufferedReader br = new BufferedReader( new InputStreamReader(System.in));
		int exit = 0;
		int rw;
		int input = 0;

		do{
			do{
				System.out.println("Would you like to start the server? (1 - start server, 2 - exit)");
				rw = Integer.parseInt(br.readLine());
			} while(rw != 1 && rw != 2);
			if (rw == 1){
				System.out.println("Starting server...");
				Server server = new Server();
				server.start();
				do{
					System.out.println("To stop server, type 1");
					input = Integer.parseInt(br.readLine());
				} while (input == 0);
				System.out.println("Server interrupted");
				server.interruptThread();
			}
			else
				exit = 1;
		} while(exit == 0);

	}
}
