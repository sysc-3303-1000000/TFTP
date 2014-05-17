import java.io.*;
import java.net.*;

/**
 * The following is implementation for the Client
 * 
 * @since May 11 2014
 * 
 * @author 1000000
 * @version May 15 2014
 *
 */
public class Client {
	public static final byte zero = (byte)0;
	public static final byte one = (byte)1;
	public static final byte two = (byte)2;	
	
	private DatagramSocket sendReceiveSocket; // Socket used to send and receive packets
	private DatagramPacket sendPacket, receivePacket, sendData; // Packets used to send an receive information
	private String filenameString, modeString;
	private byte filenameBytes[], modeBytes[];
	private byte[] ackNumber, dataNumber;
	private int dataBlock, ackBlock;

	private static byte readMsg[];
	private static byte writeMsg[];
	
	/**
	 * The following is the constructor for Client
	 * 
	 * @since May 11 2014
	 * 
	 * Latest Change: Added Code from assignment 1
	 * @version May 15 2014
	 * @author Moh
	 * 
	 */
	public Client(){
		
		// initialize the DatagramSocket sendReceive to bind to any available port
		try {
			sendReceiveSocket = new DatagramSocket();
		} // end try
		catch (SocketException se){
			System.err.println("Socket exception error: " + se.getMessage());
		} // end catch
		
		// initialize the String variables that will be passed onto the program
		filenameString = "moh.txt";
		modeString = "netascii";
		
		// change string values to bytes
		filenameBytes = filenameString.getBytes();
		modeBytes = modeString.getBytes();
		
		// initialize the messages
		readMsg = createMsg(one, filenameBytes, modeBytes);
		writeMsg = createMsg(two, filenameBytes, modeBytes);

		
	} // end constructor

	/**
	 * send and receive procedure for the packet
	 * this procedure will also be able to accept a request depending on its arguments
	 * @param rqstMsg a byte array which will have the message attached to the packet being sent
	 * 
	 * @since May 11 2014
	 * 
	 * Latest Change: Added Code from assignment 1
	 * @version May 15 2014
	 * @author Moh
	 * 
	 */
	private void sendReceive(byte[] rqstMsg, Request req)
	{		
		System.out.println("Client has started...");
				
		// send the packet to well-known port 68
		try {
			sendPacket = new DatagramPacket(rqstMsg, rqstMsg.length, InetAddress.getLocalHost(), 68);
		} // end try
		catch (UnknownHostException uhe) {
			System.err.println("Unknown host exception error: " + uhe.getMessage());
		} // end catch
		
		System.out.println("Client sending packet to intermediate...");
	    // send the packet to the intermediate via the send socket 
	    try {
	       sendReceiveSocket.send(sendPacket);
	    } // end try
	    catch (IOException ioe) {
	    	System.err.println("Unknown IO exception error: " + ioe.getMessage());
	    } // end catch
	    
		// prints out the information on the sent packet
		System.out.println("Client packet sent to intermediate...");
		printInformation(sendPacket);

		
		// prepare to receive a packet
		byte rply[] = new byte[516];
		receivePacket = new DatagramPacket(rply, rply.length);
		
		// block until you receive a packet
		// TODO ADD A TIME OUT ON IT 
		try {
			System.out.println("Client receiving packet from intermediate...");
			sendReceiveSocket.receive(receivePacket);
		} // end try
		catch (IOException ioe) {
			System.err.println("IO Exception error: " + ioe.getMessage());
		} // end catch
		
		// prints out the information on the received packet
		printInformation(receivePacket);
		System.out.println("Client packet received..");	
		
		if(req == Request.READ) {
			
		}
		else if (req == Request.WRITE) {
			byte data[] = new byte[516];
			ackNumber[0] = (byte)0;
			ackNumber[1] = (byte)0;
			dataBlock = 1;
			ackBlock = 0;
			while(verifyack(ackNumber, receivePacket)) {
				data[0] = (byte)0;
				data[1] = (byte)3;
				data[2] = (byte)0;
				data[3] = (byte)dataBlock;
				
				byte[] fileData = new byte[0];
				   
				   try {
				    fileData = ReadFromFile(dataBlock);
				   } catch (FileNotFoundException e) {
				    System.out.println("File Not Found: " + e.toString());
				    System.exit(0);
				   } catch (IOException e) {
				    System.out.println("IO Exception: " + e.toString());
				    System.exit(0);
				   }
				   
				   System.arraycopy(fileData, 0, data, 4, fileData.length);
				   
				   try {
						sendData = new DatagramPacket(data, data.length, InetAddress.getLocalHost(), 68);
					} // end try
					catch (UnknownHostException uhe) {
						System.err.println("Unknown host exception error: " + uhe.getMessage());
					} // end catch
				   
				   try {
				       sendReceiveSocket.send(sendData);
				    } // end try
				    catch (IOException ioe) {
				    	System.err.println("Unknown IO exception error: " + ioe.getMessage());
				    } // end catch
				    byte reply[] = new byte[4];
					receivePacket = new DatagramPacket(reply, reply.length);
					
					try {
						System.out.println("Client receiving packet from intermediate...");
						sendReceiveSocket.receive(receivePacket);
					} // end try
					catch (IOException ioe) {
						System.err.println("IO Exception error: " + ioe.getMessage());
					} // end catch
					ackNumber[1] = reply[3];
					dataBlock++;
					
			}
		}
		
	} // end method
	/**
	 * 
	 * @param blockNum
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public byte[] ReadFromFile(int blockNum) throws FileNotFoundException, IOException
	{
		
		BufferedInputStream in = new BufferedInputStream(new FileInputStream(filenameString));

		byte[] data = new byte[512];
		int n;
		
		in.skip((blockNum)*512);

		while ((n = in.read(data)) != -1) {
		}
		
		in.close();
		
		return data;

	}
	/**
	 * 
	 */
	private boolean verifyack(byte[] ackNumber, DatagramPacket p) {
		byte ack[] = p.getData();
		if (ack[0] == (byte)0 && ack[1] == (byte)4) {
			if (ack[2] == ackNumber[0] && ack[3] == ackNumber[1]) {
				return true;
			}
		}
		return false;
	}/*
				data[0] = (byte)0;
				data[1] = (byte)3;
				data[2] = dataNumber[0];
				data[3] = (byte)dataBlock;
				
				 byte[] fileData = new byte[0];
				   
				   try {
				    fileData = ReadFromFile(dataBlock);
				   } catch (FileNotFoundException e) {
				    System.out.println("File Not Found: " + e.toString());
				    System.exit(0);
				   } catch (IOException e) {
				    System.out.println("IO Exception: " + e.toString());
				    System.exit(0);
				   }
				   
				   System.arraycopy(fileData, 0, data, 4, fileData.length);
			}
		}
	}*/
	/**
	 * the following method will be called when trying to print out information about a specific packet
	 * @param p the information displayed desired for this packet
	 * 
	 * @since May 11 2014
	 * 
	 * Latest Change: Added Code from assignment 1
	 * @version May 15 2014
	 * @author Moh
	 * 
	 */
	private void printInformation(DatagramPacket p) {
		
		// print out the information on the packet
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
	 * Following method will create the message which is being put into the packet
	 * @param rqstByte will be 0 or 1 depending if it is a read or write request, otherwise invalid
	 * @param file the file name
	 * @param mode the mode 
	 * @return a byte array for the message that we are sending
	 * 
	 * @since May 11 2014
	 * 
	 * Latest Change: Added Code from assignment 1
	 * @version May 15 2014
	 * @author Moh
	 * 
	 */
	private byte[] createMsg(byte rqstByte, byte file[], byte mode[]) {
		// the message will be of size 4 (0, rqstByte, 0, 0) with the file length and mode length
		byte msg[] = new byte[4+file.length+mode.length];
		msg[0] = zero;
		msg[1] = rqstByte;
		
		// will keep track of the index of the file and mode we're in
		int byteCount = 0;
		
		// keeps track of the index of the message
		int msgIndex = 2;
		
		// add the file bytes starting at index 2
		for (int i = 0; i<file.length; i++) {
			msg[msgIndex] = file[byteCount];
			byteCount++;
			msgIndex++;
		} // end forloop
		
		// add a 0 at index position 
		msg[(msgIndex)] = zero;
		// increase the index
		msgIndex++;
		
		// reset the byteCount to 0 to begin adding the bytes for the mode 
		byteCount = 0;
		// add the mode bytes starting at the msgIndex
		for (int i = 0; i<mode.length; i++) {
			msg[msgIndex] = mode[byteCount];
			byteCount++;
			msgIndex++;		
		} // end forloop
		
		// add the final 0 in the message
		msg[msgIndex] = zero;
		
		return msg;
	} // end forloop
	
	/**
	 * Main method for the Client
	 * @param args not used
	 * 
	 * @since May 11 2014
	 * 
	 * Latest Change: Added Code from assignment 1
	 * @version May 15 2014
	 * @author Moh
	 * 
	 */
	public static void main(String[] args) {
		Client client = new Client();
		
		// start with an invalid request 
		//client.sendReceive(invMsg);
		
		// repeat the read and write requests 4 times, alternating
		for (int i = 0; i < 4; i++) {
		client.sendReceive(readMsg, Request.READ);
		client.sendReceive(writeMsg, Request.WRITE);
		} // end forloop
		
		// have an invalid request sent
		//client.sendReceive(invMsg);
		
	} // end method
	
} // end class
