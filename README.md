<h1>How this Application Works: </h1>

<h2>NOTE: This application was deigned for ECCC and is not functional for non-ECCC employees as I have removed all the sensetive data. </h2>

This application allows users to perform config table updates and drop-in tests via a simple GUI interface. It requires
a VPN internet connection to the ECCC network otherwise it will not work. 

<h4>BASH SCRIPTS</h4>
This application uses many different bash scripts remotely. All these scripts are found
in the "HIDDEN" directory. The "HIDDEN" contains the username and
email that is needed to automate the git push to stability feature.

<h4>PROPERTIES FILE</H4>

This properties file contains all the data the user inputs, as well as some backend information like the ssh config.
This properties file also serves as a cache so users dont have to re-enter values on every start up. 

<hr>
<h3>Config Table Updater</h3>

This tab allows users to update their config table changes to the docker and stability. When the user fire's the update
button, the selected files are sftp's to the docker and a remote bash script is ran that updates all the docker config tables.

If the "request push to stability" option is checked, a merge request is created on gitlab of the changes.
What makes this possible is a cloned version of the config warnings repo exists on the docker, and a bash script 
that automates the entire git push procedure is executed on that repo. Note that this does not merge the changes,
only makes a mr for them. 

<h4>AUTOMATED MANTIS NOTE</h4>

After the merge request is created, a note is added to a specific mantis page that describes what changes were made, who
made them, and at what time were they performed. How we know which mantis page we should send the note to is decided by value passed in
the "mantis issue number" text box. A POST request using the SOAP API protocol for Mantis is used to send this note.

<hr>
<h3>Drop-in Test</h3>

This tab allows users to perform drop-in testing and allows you to see the live logs of the ninjo-decoder component
in a separate window after performing the drop test. 

The drop test is performed via stfp'ing the selected file to the dockers "one-at-a-time" directory 
and running the "single-run.sh" script to execute the drop test. 

After the drop-test button is fired, a new window pops up showing the live logs of the ninjo-decoder. This new window
has its own controller and fxml file. This window displays the logs of the ninjo-Decoder component from the docker
after the test is executed by capturing the output of the "tail -f -n0" command running on the components log file.
When the window is closed, a kill signal is send to that tail -f command to stop capturing the logs. 

<hr>
<h3>SSH Settings</h3>

This tab is used if the ssh configuration of the docker container changes. This info is saved in the properties file. 











