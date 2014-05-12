/**
 * SYSC 3303 - ASSIGNMENT 1
 * 
 * @author Mohammed Ahmed-Muhsin
 * @version 1.0 
 * The client will be creating and sending a DatagramPacket with either a read request or a write request
 * This version of the program will have a generic request with a string. 
 * The request will be sent to the intermediate and the client will wait until it receives a response from the intermediate
 */

import java.io.*;
import java.net.*;

public class Client {
	
	// declare packets
	DatagramPacket sendPacket, receivePacket;
	
	// declare socket
	DatagramSocket sendReceiveSocket;
	
	// declare string
	String s;
	
	public Client(){
		
		// create the send/receive socket
		try {
			sendReceiveSocket = new DatagramSocket();
		} catch (SocketException se){
			System.err.println("Socket exception error: " + se.getMessage());
		}
		
	}
	
	// send and receive procedure for the packet
	// this procedure will also be able to accept either a write or read depending on its arguments
	// to be implemented in a later version
	public void sendReceive()
	{
		s = "Testing!";
		
		// get a byte representation of the message
		byte msg[] = s.getBytes();
		
		// send the packet to well-known port 68
		try {
			sendPacket = new DatagramPacket(msg, msg.length, InetAddress.getLocalHost(), 68);
		} catch (UnknownHostException uhe) {
			System.err.println("Unknown host exception error: " + uhe.getMessage());
		}
		
		// prints out the information on the sent packet
		printInformation(sendPacket);
		System.out.println("Client packet sent...");
		
		// prepare to receive a packet
		byte rply[] = new byte[100];
		receivePacket = new DatagramPacket(rply, rply.length);
		
		// block until you receive a packet
		// TODO ADD A TIME OUT ON IT 
		try {
			sendReceiveSocket.receive(receivePacket);
		} catch (IOException ioe) {
			System.err.println("IO Exception error: " + ioe.getMessage());
		}
		
		// prints out the information on the received packet
		printInformation(receivePacket);
		System.out.println("Client packet received.. closing open sockets");
		
		// close the socket
		sendReceiveSocket.close();
		
	}

	/**
	 * the following method will be called when trying to print out information about a specific packet
	 * @param p the information displayed desired for this packet
	 */
	public void printInformation(DatagramPacket p) {
		
		// print out the information on the 
		System.out.println("Client is sending packet...");
		System.out.println("Host: " + p.getAddress());
		System.out.println("Host port: " + p.getPort());
		System.out.println("Containing the following \nString: " + new String(p.getData()));
		// TODO SEE IF PRINTS OUT BYTE ARRAY
		System.out.println("Bytes: " + p.getData());
	}
}
