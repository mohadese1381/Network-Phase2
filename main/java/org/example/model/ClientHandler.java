package org.example.model;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class ClientHandler extends Thread {

    private boolean debugMode = true;

    /**
     * Indicating the last set transfer type
     */
    private enum transferType {
        ASCII, BINARY
    }

    /**
     * Indicates the authentication status of a user
     */
    private enum userStatus {
        NOTLOGGEDIN, ENTEREDUSERNAME, LOGGEDIN
    }

    Map<String, Boolean> admin = new HashMap<>();
    private String myUername;

    // Path information
    private String root;
    private final String adminsPath = "C:\\Users\\hp\\Desktop\\network-project-phase02-momir\\admins";
    private String currDirectory;
    final private String fileSeparator = "\\";

    // control connection
    private Socket controlSocket;
    private PrintWriter controlOutWriter;
    private BufferedReader controlIn;

    // data Connection
    private ServerSocket dataSocket;
    private Socket dataConnection;
    private ServerSocket dataConnectionServer;
    private PrintWriter dataOutWriter;

    private int dataPort;
    private transferType transferMode = transferType.BINARY;

    // user properly logged in?

    private String validUsers[] = {"mamad", "ali", "hasan", "hossein"};
    private String validPasswords[] = {"11", "22", "33", "44"};

    private int passwordIndex;
    private userStatus currentUserStatus = userStatus.NOTLOGGEDIN;
    private boolean quitCommandLoop = false;
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public ClientHandler(Socket client, int dataPort) {
        super();
        this.controlSocket = client;
        openDataConnectionActiveServer(85);
        this.dataPort = dataPort;
        this.currDirectory = System.getProperty("user.dir") + "\\src";
        this.root = System.getProperty("user.dir");
        admin.put("ali", true);
        admin.put("mamad", false);
        admin.put("hasan", true);
        admin.put("hossein", false);
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Run method required by Java thread model
     */
    public void run() {
        debugOutput("Current working directory " + this.currDirectory);
        try {
            // Input from client
            controlIn = new BufferedReader(new InputStreamReader(controlSocket.getInputStream()));

            // Output to client, automatically flushed after each print
            controlOutWriter = new PrintWriter(controlSocket.getOutputStream(), true);

            // Get new command from client
            while (!quitCommandLoop) {
                executeCommand(controlIn.readLine());
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Clean up
            try {
                controlIn.close();
                controlOutWriter.close();
                controlSocket.close();
                debugOutput("Sockets closed and Client stopped");
            } catch (IOException e) {
                e.printStackTrace();
                debugOutput("Could not close sockets");
            }
            sendMsgToClient("Socket closed bye bye");
        }

    }
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void Login(String c) {
        // split command and arguments
        int index = c.indexOf(' ');
        String command = ((index == -1) ? c.toUpperCase() : (c.substring(0, index)).toUpperCase());
        String args = ((index == -1) ? null : c.substring(index + 1));
        switch (command) {
            case "USER":
                handleUser(args);
                break;
            case "PASS":
                handlePass(args);
                break;
            default:
                sendMsgToClient("501 Unknown command");
                break;
        }
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void executeCommand(String c) throws IOException {
        // split command and arguments
        Server.log += "User:\t" + myUername + "\tcommand:\t" + c + "}";
        int index = c.indexOf(' ');
        String command = ((index == -1) ? c.toUpperCase() : (c.substring(0, index)).toUpperCase());
        String args = ((index == -1) ? null : c.substring(index + 1));
        if (args != null && args.contains(adminsPath) && !admin.get(myUername)){
            sendMsgToClient("403 access denied");
            return;
        }


        // dispatcher mechanism for different commands
        if (command.equals("USER") || command.equals("PASS")) {
            Login(c);
        } else if (currentUserStatus == userStatus.LOGGEDIN) {
            switch (command) {
                case "CWD":
                    handleCwd(args);
                    break;
                case "LIST":
                    handleList(args);
                    break;
                case "PWD":
                    handlePwd();
                    break;
                case "QUIT":
                    handleQuit();
                    break;
                case "DELE":
                    handleDelete(args);
                    break;
                case "RETR":
                    handleRetr(args);
                    break;
                case "MKD":
                    handleMkd(args);
                    break;
                case "RMD":
                    handleRmd(args);
                    break;
                case "CDUP":
                    handleCdup();
                    break;
                case "REPO":
                    if (admin.get(myUername)) {
                        sendMsgToClient("200 you can see the log now" + Server.log);
                    } else
                        sendMsgToClient("403 access denied");
                    break;
                case "STOR":
                    int index2 = args.indexOf(' ');
                    String targetFile = args.substring(index2 + 1, args.length());
                    targetFile += args.substring(1, index2);
                    handleStor(targetFile);
                    break;
                default:
                    sendMsgToClient("501 Unknown command");
                    break;
            }
        } else if (currentUserStatus == userStatus.NOTLOGGEDIN) {
            sendMsgToClient("403 User need to login first");
        }
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private void handleCdup() {
        System.out.println(currDirectory);
        String splitedDir[] = currDirectory.split(Pattern.quote(File.separator));
        String newDir = "";
        for (int i = 0; i < splitedDir.length - 1; i++) {
            newDir += splitedDir[i] + "\\";
        }
        System.out.println(newDir);
        currDirectory = newDir;
        sendMsgToClient("current directory is: " + currDirectory);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void handleUser(String username) {
        if (currentUserStatus == userStatus.LOGGEDIN) {
            sendMsgToClient("530 User already logged in");
            return;
        }
        for (int i = 0; i < 3; i++)
            if (username.toLowerCase().equals(validUsers[i])) {
                sendMsgToClient("331 User name okay, need password");
                currentUserStatus = userStatus.ENTEREDUSERNAME;
                myUername = validUsers[i];
                passwordIndex = i;
                Server.log += "User:\t" + username + "\tlogged in without password}";
                return;
            }
        sendMsgToClient("400 Invalid username");
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void handlePass(String password) {
        // User has entered a valid username and password is correct
        if (currentUserStatus == userStatus.ENTEREDUSERNAME && password.equals(validPasswords[passwordIndex])) {
            currentUserStatus = userStatus.LOGGEDIN;
            sendMsgToClient("230 User logged in successfully");
            Server.log += "User:\t" + myUername + "\tlogged in successfully}";
            return;
        }
        sendMsgToClient("403 access denied");
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //sends file from server to client side
    private void handleRetr(String file) {
        File f = new File(currDirectory + fileSeparator + file);
        if (!f.exists()) {
            sendMsgToClient("550 File does not exist");
        } else {
            openDataConnectionActive("127.0.0.1", 80);
            // Binary mode
            if (transferMode == transferType.BINARY) {
                BufferedOutputStream fout = null;
                BufferedInputStream fin = null;

                //sendMsgToClient("150 Opening binary mode data connection for requested file " + f.getName());

                try {
                    // create streams
                    fout = new BufferedOutputStream(dataConnection.getOutputStream());
                    fin = new BufferedInputStream(new FileInputStream(f));
                } catch (Exception e) {
                    debugOutput("Could not create file streams");
                }

                debugOutput("Starting file transmission of " + f.getName());

                // write file with buffer
                byte[] buf = new byte[1024];
                int l = 0;
                try {
                    while ((l = fin.read(buf, 0, 1024)) != -1) {
                        fout.write(buf, 0, l);
                    }
                } catch (IOException e) {
                    debugOutput("Could not read from or write to file streams");
                    e.printStackTrace();
                }
                // close streams
                try {
                    fin.close();
                    fout.close();
                } catch (IOException e) {
                    debugOutput("Could not close file streams");
                    e.printStackTrace();
                }

                debugOutput("Completed file transmission of " + f.getName());

                sendMsgToClient("File transfer successfully. Closing data connection.");
            }
            closeDataConnection();
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //sends file from client to server
    private void handleStor(String file) {
        if (file == null) {
            sendMsgToClient("501 No filename given");
        } else {
            File f = new File(currDirectory + fileSeparator + file);

            if (f.exists()) {
                sendMsgToClient("550 File already exists");
            } else {
                // Binary mode
                if (transferMode == transferType.BINARY) {
                    BufferedOutputStream fout = null;
                    BufferedInputStream fin = null;

                    // sendMsgToClient("150 Opening binary mode data connection for requested file " + f.getName());

                    try {
                        Socket socket = dataConnectionServer.accept();

                        // create streams
                        fout = new BufferedOutputStream(new FileOutputStream(f));
                        fin = new BufferedInputStream(socket.getInputStream());
                    } catch (Exception e) {
                        debugOutput("Could not create file streams");
                    }

                    debugOutput("Start receiving file " + f.getName());

                    // write file with buffer
                    byte[] buf = new byte[1024];
                    int l = 0;
                    try {
                        while ((l = fin.read(buf, 0, 1024)) != -1) {
                            fout.write(buf, 0, l);
                        }
                    } catch (IOException e) {
                        debugOutput("Could not read from or write to file streams");
                        e.printStackTrace();
                    }

                    // close streams
                    try {
                        fout.flush();
                        fin.close();
                        fout.close();
                        dataConnectionServer.close();
                    } catch (IOException e) {
                        debugOutput("Could not close file streams");
                        e.printStackTrace();
                    }

                    debugOutput("Completed receiving file " + f.getName());

                    sendMsgToClient("226 File transfer successful. Closing data connection.");

                }
            }
            closeDataConnection();
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void handleCwd(String args) {
        String filename = currDirectory;

        // go one level up (cd ..)
        if (args.equals("..")) {
            int ind = filename.lastIndexOf(fileSeparator);
            if (ind > 0) {
                filename = filename.substring(0, ind);
            }
        }

        // if argument is anything else (cd . does nothing)
        else if ((args != null) && (!args.equals(".")) && args.charAt(0) != '\\') {
            filename = filename + fileSeparator + args;
        } else if (args.charAt(0) == '\\') {
            filename = args;
        }
        // check if file exists, is directory
        File f = new File(filename);

        if (f.exists() && f.isDirectory()  /*(filename.length() >= root.length())*/) {
            currDirectory = filename;
            sendMsgToClient("250 The current directory has been changed to " + currDirectory);
        } else {
            sendMsgToClient("550 Requested action not taken. File unavailable.");
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void handlePwd() {
        sendMsgToClient("Current directory is : " + currDirectory + "\"");

    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void handleQuit() {
        quitCommandLoop = true;
        sendMsgToClient("Stop Connection");
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void openDataConnectionActive(String ipAddress, int port) {
        try {
            dataConnection = new Socket(ipAddress, port);
            dataOutWriter = new PrintWriter(dataConnection.getOutputStream(), true);
            debugOutput("Data connection - Active Mode - established");
        } catch (IOException e) {
            debugOutput("Could not connect to client data socket");
            e.printStackTrace();
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void openDataConnectionActiveServer(int port) {
        try {
            dataConnectionServer = new ServerSocket(port);
            System.out.println("Data connection - Active Mode - established");
        } catch (IOException e) {
            System.out.println("Could not connect to client data socket");
            e.printStackTrace();
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //Close previously established data connection sockets and streams
    private void closeDataConnection() {
        try {
            dataOutWriter.close();
            dataConnection.close();
            if (dataSocket != null) {
                dataSocket.close();
            }

            debugOutput("Data connection was closed");
        } catch (IOException e) {
            debugOutput("Could not close data connection");
            e.printStackTrace();
        }
        dataOutWriter = null;
        dataConnection = null;
        dataSocket = null;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void sendMsgToClient(String msg) {
        controlOutWriter.println(msg);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void handleMkd(String args) {

        File dir;
        String path[];
        if (args.charAt(0) == '.') {
            path = args.split("/");
            dir = new File(currDirectory + fileSeparator + path[1]);
            System.out.println(path[1]);
        } else {
            dir = new File(args);
        }
        System.out.println(dir);
        if (!dir.mkdir()) {
            sendMsgToClient("550 Failed to create new directory");
        } else {
            sendMsgToClient("250 Directory successfully created");
        }

        //test : mkd C:\Users\hp\Desktop\ali
        //test : mkd ../mamad

    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void debugOutput(String msg) {
        if (debugMode) {
            System.out.println("Thread " + this.getId() + ": " + msg);
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void handleRmd(String args) {
        File dir;
        String path[];
        if (args.charAt(0) == '.') {
            path = args.split("/");
            dir = new File(currDirectory + fileSeparator + path[1]);
            System.out.println(path[1]);
        } else {
            dir = new File(args);
        }
        // check if file exists, is directory

        if (dir.exists() && dir.isDirectory()) {
            dir.delete();
            sendMsgToClient("250 Directory was successfully removed");
        } else {
            sendMsgToClient("550 Requested action not taken. File unavailable.");
        }
        //test: rmd C://Users/hp/Desktop/mamad
        //test: rmd ../ali
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void handleDelete(String args) {
        try {
            File target = new File(args);
            sendMsgToClient("Do you really wish to delete? Y/N");
            String line = controlIn.readLine();
            System.out.println(line);
            switch (line) {
                case "Y":
                    target.delete();
                    sendMsgToClient("250 File was successfully removed");
                    break;
                case "N":
                    sendMsgToClient("200 process cancled");
                    break;
                default:
                    sendMsgToClient("501 Unknown command");
                    break;
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            sendMsgToClient("Error. Action not taken");
        }
        //test: dele C:/Users/hp/Desktop/network-project-phase02-momir/src/text.txt
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void handleList(String args) throws IOException {


        String sendToClient = "";
        Set<String> answer = listFilesUsingDirectoryStream(args);

        if (answer.size() == 0) {
            sendMsgToClient("550 No file exist.");
        } else {

            for (String s : answer) {
                sendToClient += s + "}";
                System.out.println(s);
            }

            sendMsgToClient(sendToClient);
        }

        // test : LIST
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public Set<String> listFilesUsingDirectoryStream(String dir) throws IOException {

        Set<String> fileSet = new HashSet<>();
        BasicFileAttributes attr;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(dir))) {
            for (Path path : stream) {
                attr = Files.readAttributes(path, BasicFileAttributes.class);
                fileSet.add("Name: " + path.getFileName() + "\t" +
                        "Creation time:" + attr.creationTime() + "\t" +
                        "isDirectory: " + attr.isDirectory() + "\t" +
                        "size: " + attr.size()
                );
            }
        }
        return fileSet;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void sendDataMsgToClient(String msg) {
        if (dataConnection == null || dataConnection.isClosed()) {
            sendMsgToClient("425 No data connection was established");
            debugOutput("Cannot send message, because no data connection is established");
        } else {
            dataOutWriter.print(msg + '\r' + '\n');
        }

    }
}