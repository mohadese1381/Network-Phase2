package org.example.model;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class FTPClient {
    private static ServerSocket dataConnectionServer;
    private static Socket dataConnectionClient;

    public static void main(String[] args) throws IOException {
        final String serverAddress = "127.0.0.1"; // Replace with your server's IP address
        final int port = 1025;
        String targetFile = null;
        String targetFile2 = null;
        try {
            Socket socket = new Socket(serverAddress, port);
            System.out.println("Connected to FTP server");

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
            String userInputStr;

            while ((userInputStr = userInput.readLine()) != null) {

                int index = userInputStr.indexOf(' ');
                String command = ((index == -1) ? userInputStr.toUpperCase() : (userInputStr.substring(0, index)).toUpperCase());
                String path = ((index == -1) ? null : userInputStr.substring(index + 1));
                if(command.equals("RETR"))
                {
                    int index2 = path.lastIndexOf('\\');
                    targetFile = path.substring(index2 + 1, path.length());
                }
                if(command.equals("STOR"))
                {
                    int index4 = path.indexOf(' ');
                    targetFile2 = path.substring(0, index4);
                }
                //Downloading file
                if (command.equals("RETR")) {
                    openDataConnectionActiveServer(80);
                }
                //Uploading file
                if (command.equals("STOR")) {
                    openDataConnectionActiveClient("127.0.0.1", 85);
                }

                out.println(userInputStr);

                //Uploading file
                if (command.equals("STOR")) {
                    BufferedOutputStream fout = null;
                    BufferedInputStream fin = null;
                    File file = new File("C:\\Users\\Mohadese\\IdeaProjects\\Network-phase2\\Network_phase2\\src\\ClientFiles" + targetFile2);
                    try {
                        // create streams
                        fout = new BufferedOutputStream(dataConnectionClient.getOutputStream());
                        fin = new BufferedInputStream(new FileInputStream(file));
                    } catch (Exception e) {
                        System.out.println("Could not create file streams");
                    }
                    byte[] buf = new byte[1024];
                    int l = 0;
                    try {
                        while ((l = fin.read(buf, 0, 1024)) != -1) {
                            fout.write(buf, 0, l);
                        }
                    } catch (IOException e) {
                        System.out.println("Could not read from or write to file streams");
                        e.printStackTrace();
                    }
                    // close streams
                    try {
                        fout.flush();
                        fin.close();
                        fout.close();
                        dataConnectionClient.close();
                    } catch (IOException e) {
                        System.out.println("Could not close file streams");
                        e.printStackTrace();
                    }
                }

                //Downloading file
                if (command.equals("RETR")) {
                    Socket client = dataConnectionServer.accept();
                    BufferedOutputStream fout = null;
                    BufferedInputStream fin = null;
                    try {
                        File file = new File("C:\\Users\\Mohadese\\IdeaProjects\\Network-phase2\\Network_phase2\\src\\ClientFiles\\" + targetFile);
                        fout = new BufferedOutputStream(new FileOutputStream(file));
                        fin = new BufferedInputStream(client.getInputStream());

                        // write file with buffer
                        byte[] buf = new byte[1024];
                        int l = 0;
                        try {
                            while ((l = fin.read(buf, 0, 1024)) != -1) {
                                fout.write(buf, 0, l);
                            }
                        } catch (IOException e) {
                            System.out.println("Could not read from or write to file streams");
                            e.printStackTrace();
                        } finally {
                            fout.flush();
                            fout.close();
                            fin.close();
                            dataConnectionServer.close();
                        }
                    } catch (Exception ex) {

                    }
                }

                String serverResponse = in.readLine();

                if (serverResponse != null) {

                    if (serverResponse.equals("Stop Connection")) {
                        System.out.println("Server says: " + serverResponse);
                        System.exit(0);
                    } else if (command.equals("LIST") || command.equals("REPO") ) {
                        String answer[] = serverResponse.split("}");
                        for (String s : answer) {
                            System.out.println(s);
                        }
                    }else {
                        System.out.println("Server says: " + serverResponse);
                    }
                }
//                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static void openDataConnectionActiveClient(String ipAddress, int port) {
        try {
            dataConnectionClient = new Socket(ipAddress, port);
            System.out.println("Data connection - Active Mode - established");
        } catch (IOException e) {
            System.out.println("Could not connect to client data socket");
            e.printStackTrace();
        }
    }

    private static void openDataConnectionActiveServer(int port) {
        try {
            dataConnectionServer = new ServerSocket(port);
            System.out.println("Data connection - Active Mode - established");
        } catch (IOException e) {
            System.out.println("Could not connect to client data socket");
            e.printStackTrace();
        }
    }

}