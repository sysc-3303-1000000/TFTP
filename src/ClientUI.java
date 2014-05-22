import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
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
		int rw;
		Request req;
		Scanner in = new Scanner(System.in);
		String directory;
		
		System.out.println("Please Enter the file you wish to read from or write to");
		//filename = in.nextLine();
		do {
			System.out.println("Would you like to perform a read or a write? (1 - read, 2 - write)");
			rw = in.nextInt();
		} while (rw != 1 && rw != 2); // end do while
		
		if (rw == 1) {
			req = Request.READ;
			System.out.println("Please select the directory you wish to write to");
			final JFrame frame = new JFrame("Folder Selector");
			final JFileChooser chooser = new JFileChooser();
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			JButton btn1 = new JButton("Show Open Dialog");
			btn1.addActionListener(new ActionListener() {
				 
	            public void actionPerformed(ActionEvent e) {
	                int retVal = chooser.showOpenDialog(frame);
	                if (retVal == JFileChooser.APPROVE_OPTION) {
	                    File directory = chooser.getCurrentDirectory();
	                    System.out.println(directory.getAbsolutePath());
	                }
	 
	            }
	        });
			Container pane = frame.getContentPane();
	        pane.setLayout(new GridLayout(1, 1, 10, 10));
	        pane.add(btn1);
	 
	        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	        frame.setSize(300, 200);
	        frame.setVisible(true);
		}
			
		else {
			req = Request.WRITE;
			System.out.println("Please select the directory you wish to write to");
			final JFrame frame = new JFrame("Folder Selector");
			final JFileChooser chooser = new JFileChooser();
			JButton btn1 = new JButton("Show Open Dialog");
			btn1.addActionListener(new ActionListener() {
			 
				public void actionPerformed(ActionEvent e) {
					int retVal = chooser.showOpenDialog(frame);
					if (retVal == JFileChooser.APPROVE_OPTION) {
						File directory = chooser.getCurrentDirectory();
						File file = chooser.getSelectedFile();
						String filename = file.getName();
						System.out.println(directory.getAbsolutePath());
						System.out.println(filename);
						frame.dispose();
					}
 
				}
			});
			Container pane = frame.getContentPane();
			pane.setLayout(new GridLayout(1, 1, 10, 10));
			pane.add(btn1);
 
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setSize(300, 200);
			frame.setVisible(true);
	}
		
		
		
		

	} // end method

} // end class
