package org.example.model;
import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {
   /* private Socket socket;

    public Socket getSocket() {
        return socket;
    }

    private String ip;
    private int port;
    public  Client (String ip,int port){
        this.ip = ip;
        this.port = port;
        if(!createSocket(ip, port))
        {
            System.out.println("Can not connect to server by IP : " + ip + " and Port : " + port);
        }
        else{
            System.out.println("Client is connected to server on : " + ip + ":" + port);
        }
    }


    public boolean createSocket(String ip,int port)
    {
        try{
            socket = new Socket(ip,port);
            return true;
        }
        catch (Exception ex){
            ex.printStackTrace();
            return false;
        }
    }

    public void sendsCommand(String cmd) throws IOException {
        try{
            DataOutputStream out = new DataOutputStream(this.socket.getOutputStream());
            out.writeUTF(cmd);
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
    }

    public static class Main {
        public static void main(String[] args) throws IOException, InterruptedException {

            Client client = new Client("127.0.0.1", 1025);

             new Thread(() -> {
                Scanner sc = new Scanner(System.in);
                String cmd;
                String line;
                try{
                    BufferedReader in = new BufferedReader(new InputStreamReader(client.getSocket().getInputStream()));
                    while (client.getSocket().isConnected()) {

                        cmd = sc.nextLine();
                        client.sendsCommand(cmd);

                        while ((line = in.readLine()) != null) {
                            System.out.println(line);
                        }
                    }
                }catch (Exception ex)
                {
                    ex.printStackTrace();
                }
            }).start();
        }
    }*/
}
