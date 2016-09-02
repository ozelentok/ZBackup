#ZBackup
- Backup program for Android devices
- Server program is implemented in Python2 (Originally in Python3)
- Client program is implemented in Java (for Android devices)

##Backup Options
You can backup all the entries, or only the selected ones:
- When you backup everything, a directory named 'All' will be created and each entry will be saved there
- When you backup selected, a directory named 'Selected' will be created and only selected entries will be saved there

###Local Backup
- Backup a file or a directory into a Zip file

###Network Backup
- Backup a file or a directory to a remote server

##Installation

###Server Installation
- Install the latest Python2
	*(Remember to check the "Add Python to PATH environment variable" during instllation)
- Put the Server directory where ever you want
- Configure the 'server.ini' file
	- host (IP address to listen on, 0.0.0.0 means everything)
	- port (default is 1234)
	- timeout (time in seconds until disconnecting from an unresponsive connection)
	- root\_file\_dir (directory to save the backups to)
	- cert\_file (no need to change this, SSL Certificate)
	- key\_file (no need to change this, SSL Private Key)
	- users (no need to change this, it is for the Android app Backup All/Selected option)
	- log\_file (File path to the server log)
	- password (Password to connect to the server)

###Client Installation
- Simply install the ZBackup apk that was included
	*(You may need to enable non-google-play installations on the device)
- Open the app, open the menu, select 'Settings' and configure:
	-	Server(Hostname or IP)
	- Port
	- Password


##How to Use

###Running the server
Simply run the 'run.py' file

###Adding to files/directories to the ZBackup app
- Choose Local or Network backup
- Open the menu, select 'Add'
- To add a file:
	- Select the file you want to backup
- To add a directory:
	- (There is a problem with selecting a directory in android, so you need to change the path manually)
	- Select a file inside the directory or anywhere else
	- Select the new entry in the ZBackup list and edit the path

###Starting a backup
- Choose Local or Network backup
- If you want to backup everything, click 'BACKUP' at the top
- If you want to backup only selected items, click 'SELECTED' at the top
- A notification will appear, displaying the status of the backup
