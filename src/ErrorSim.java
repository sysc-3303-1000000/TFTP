import java.io.IOException;
import java.net.*;

/**
 * The following is implementation for the ErrorSim
 * 
 * @since May 11 2014
 * 
 * @author 1000000
 * @version May 16 2014
 *
 */
public class ErrorSim {
	
	/**
	 * The following is the constructor for ErrorSim
	 * 
	 * @since May 11 2014
	 * 
	 * Latest Change: Removed implementation, could be used for later iterations. Does nothing for now.
	 * @version May 16 2014
	 * @author Kais
	 * 
	 */
	public ErrorSim() {
				
	} // end constructor 
	
	/**
	 * Main method for the ErrorSim
	 * @param args not used
	 * 
	 * @since May 11 2014
	 * 
	 * Latest Change: ErrorSim now launches a listener thread
	 * @version May 16 2014
	 * @author Kais
	 * 
	 */
	public static void main(String[] args) {
		Thread listenerESim = new ListenerESim(true); // enabled verbose for now
		listenerESim.start();
	} // end method
} // end class
