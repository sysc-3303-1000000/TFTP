  ______   ______   ____   __________  ___ _____ 
 / ___\ \ / / ___| / ___| |___ /___ / / _ \___ / 
 \___ \\ V /\___ \| |       |_ \ |_ \| | | ||_ \ 
  ___) || |  ___) | |___   ___) |__) | |_| |__) |
 |____/ |_| |____/ \____| |____/____/ \___/____/ 
                                                 
= ITERATION 1 = 
Colin Kealty: 100855810
Kais Hassanali: 100861319
Mohammed Ahmed-Muhsin: 100855437
Samson Truong: 100848346
Last updated: 7:30 - 2014-05-17

= DESCRIPTION = 
The program and its files attached here address the requirements assigned in Iteration 0 and 1 of the project. 

= COMPILATION AND UTILIZATION = 
	1) File>Import
	2) select General > Existing Projects into Workspace 
		"Select root directory" - browse and navigate to the folder of iteration
		If not already checked, check the box in the "Projects:" box next to the project which you imported
There are 6 files that need to be compiled to ensure that all intended features work properly. The three steps outlined below must be followed in sequence to ensure full utility:
	1) Run Server.java
	2) Run Intermediate.java
	3) Run Client.java


= KNOWN ISSUES =
No way to handle errors. Program prints out a message and quits. This is to be handled in later iterations

= TESTED AND WORKING FOR = 
* Windows 7 at Carleton University lab AA 508
	JAVA 1.7.0_25
* Windows 8.1 8 GB RAM i7 960 @ 3.20GHz 
	JAVA 1.7.0_25

= IDE AND JAVA DEVELOPED ON = 
* Eclipse IDE Relase 4.3.2
* JAVA 1.7.0_25

= CHANGELOG = 
CLIENT:
3.0:
* Added file I/O support to actually receive and send a file

2.0:
* Improved documentation and in-line comments
* Improved system messages on what is being done by the program
* Implemented a single read, write, and invalid request to be sent
* Improved information printing regarding the bytes and the string
* Create separate methods for the read, write, and invalid request  

1.0:
* All DatagramSockets and Packets implemented
* Basic message sent to intermediate to ensure all connections working
* Basic information about the packet displayed at each instance

ERRORSIM - Includes ErrorSim.java and ConnectionManagerESim.java:
3.0: 
* Added multi-threading capabilities to program
* Will remain to be sending and receiving files as needed with no errors simulated

2.0:
* Improved documentation and in-line comments
* Improved system messages on what is being done by the system
* Improved information printing regarding the bytes and the string
* Reworked the sendSocket to remain open throughout the session

1.0:
* Implemented all DatagramSockets required (receive, sendReceive, and send)
* Implemented all DatagramPackets required (client and server)
* Exception handling with try/catch blocks
* Basic information about the packet displayed at each instance

SERVER - Includes ConnectionManager.java and Server.java:
3.0:
* Separated into a listening module which spawns out the threads to deal with request
* Multi-threaded requests to handle the reading and writing of the file

2.0:
* Improved documentation and in-line commenting
* Improved the printing of information about the packet
* Added a parser which will recognize a valid request (read or write) and an invalid one
* Keep the server alive forever until killed

1.0:
* Implemented a procedure that can receive a general request and send a response
* Print all necessary information about the packet
* Create a sending DatagramSocket and close it after a successful send

= DEBUGGING = 
Possible expansion is to add a debug mode with more outputs and more details on each step 

= SUPPORT = 
For technical support or to report a bug, please contact: 
* KaisHassanali@cmail.carleton.ca
* SamsonTruong@cmail.carleton.ca
* MohammedAhmedMuhsin@cmail.carleton.ca
* ColinKealty@cmail.carleton.ca