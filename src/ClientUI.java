import java.io.*;
import java.net.*;
import java.util.*;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;

import java.awt.*;
import java.awt.event.*;

/**
 * The following is implementation for the Client User Interface
 * 
 * @since May 21 2014
 * 
 * @author 1000000
 * @version May 21 2014
 *
 */
public class ClientUI {
	private static Request req;
	private static int exit;
	private static Scanner in;
	
	/**
	 * Main method for the Client User Interface
	 * @param args not used
	 * 
	 * @since May 21 2014
	 * 
	 * Latest Change: Added User Interface
	 * @version May 21 2014
	 * @author Kais
	 * 
	 */
	public static void main(String[] args) {
		in = new Scanner(System.in);
		exit = 0;
		//String directory;
		//String filename;
		/*
		do {
			System.out.println("Would you like to perform a read or a write? (1 - read, 2 - write)");
			rw = in.nextInt();
		} while (rw != 1 && rw != 2); // end do while
		*/
		do {
		final JFrame buttonFrame = new JFrame("Read or Write");
		JButton readButton = new JButton("Read");
		JButton writeButton = new JButton("Write");
		exit = 0;
		
		readButton.addActionListener(new ActionListener() {
			 
         	public void actionPerformed(ActionEvent e) {
         		req = Request.READ;
    			System.out.println("Please select the file you wish to read you wish to write to");
    			final JFrame frame = new JFrame("Folder Selector");
    			File serverLocation = new File(System.getProperty("user.dir") + "\\Server");
    			FileSystemView fsv = new SingleRootFileSystemView(serverLocation);
    			final JFileChooser chooser = new JFileChooser(fsv);
    			System.out.println(serverLocation.getAbsolutePath());
    			//chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    			JButton btn1 = new JButton("Show Open Dialog");
    			btn1.addActionListener(new ActionListener() {
    				 
    	            public void actionPerformed(ActionEvent e) {
    	                int retVal = chooser.showOpenDialog(frame);
    	                if (retVal == JFileChooser.APPROVE_OPTION) {
    	                	File file = chooser.getSelectedFile();
    	                	frame.dispose();
    	                	Thread client = new Client(file.getName(), req);
    	                	System.out.println("Would you like to perform another read or write? (1 - yes, anything else - no)");
    	                    exit = in.nextInt();
    	                }
    	 
    	            }
    	        });
    			Container pane = frame.getContentPane();
    	        pane.setLayout(new GridLayout(1, 1, 10, 10));
    	        pane.add(btn1);
    	 
    	        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    	        frame.setSize(300, 200);
    	        frame.setVisible(true);
         		buttonFrame.dispose();
            }
        });
		writeButton.addActionListener(new ActionListener() {
			 
         	public void actionPerformed(ActionEvent e) {
         		req = Request.WRITE;
    			System.out.println("Please select the file you which to write to the server");
    			final JFrame frame = new JFrame("File Selector");
    			final JFileChooser chooser = new JFileChooser();
    			JButton btn1 = new JButton("Show Open Dialog");
    			btn1.addActionListener(new ActionListener() {
    			 
    				public void actionPerformed(ActionEvent e) {
    					int retVal = chooser.showOpenDialog(frame);
    					if (retVal == JFileChooser.APPROVE_OPTION) {
    						File directory = chooser.getCurrentDirectory();
    						File file = chooser.getSelectedFile();
    						String filename = file.getName();
    						//System.out.println(directory.getAbsolutePath());
    						//System.out.println(filename);
    						frame.dispose();
    						Thread client = new Client(filename, directory.getAbsolutePath(), req);
    						System.out.println("Would you like to perform another read or write? (1 - yes, anything else - no)");
    				        exit = in.nextInt();
    					}
     
    				}
    			});
    			Container pane = frame.getContentPane();
    			pane.setLayout(new GridLayout(1, 1, 10, 10));
    			pane.add(btn1);
     
    			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    			frame.setSize(300, 200);
    			frame.setVisible(true);
         		buttonFrame.dispose();
            }
        });
		Container rwpane = buttonFrame.getContentPane();
		rwpane.setLayout(new GridLayout(2, 1, 10, 10));
		rwpane.add(readButton);
        rwpane.add(writeButton);
 
        buttonFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        buttonFrame.setSize(300, 200);
        buttonFrame.setVisible(true);
        
        while(exit == 0){ 
        	try {
        		Thread.currentThread().sleep(10);
        	} catch (InterruptedException e1) {
        		// TODO Auto-generated catch block
        		e1.printStackTrace();
        	} 
        }
		} while (exit == 1);
        
	} // end method

} // end class
