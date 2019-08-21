package com.mandeep;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import java.net.InetAddress;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.net.ServerSocket;
import java.net.Socket;

import java.util.concurrent.ConcurrentHashMap;

public class Discoverer{

    final private String BROADCAST_ADDRESS;
    final private int PORT;
    private String PEER_ID = null;
    
    final private Thread[] thread = new Thread[3];
    private Broadcast_Sender broadcast_Sender = null;
    private Broadcast_Receiver broadcast_Receiver = null;
    private SecondaryListner secondaryListner = null;
    private PortGenerator portGenerator = null;
    private ConcurrentHashMap<String,InetAddress> ConnectedPeerInfo = null;

    public Discoverer(String PEER_ID,String BROADCAST_ADDRESS,int PORT){
        this.PEER_ID = PEER_ID;
        this.BROADCAST_ADDRESS = BROADCAST_ADDRESS;
        this.PORT = PORT;

        ConnectedPeerInfo = new ConcurrentHashMap<>();
        portGenerator = new PortGenerator();
        broadcast_Sender = new Broadcast_Sender();
        broadcast_Receiver = new Broadcast_Receiver();
        secondaryListner = new SecondaryListner();
    }
    public ConcurrentHashMap<String,InetAddress> getConnectedPeerInfo(){
    	return ConnectedPeerInfo;
    }
    
    //Function to start Broadcast_Sender thread.
    void startBroadcast_Sender(){
        if(!broadcast_Sender.isRunning){
            thread[0] = new Thread(broadcast_Sender);
            thread[0].start();
        }
    }

    //Function to start Broadcast_Receiver thread.
    void startBroadcast_Receiver(){
        if(!broadcast_Receiver.isRunning){
            thread[1] = new Thread(broadcast_Receiver);
            thread[1].start();
        }
    }
    
    //Function to start SecondaryListner thread.
    void startSecondaryListner(){
        if(!secondaryListner.isRunning){
            thread[2] = new Thread(secondaryListner);
            thread[2].start();
        }
    }

    //Function to stop Broadcast_Sender thread.
    void stopBroadcast_Sender(){
        broadcast_Sender.stop();
    }
    
    //Function to stop Broadcast_Receiver thread.
    void stopBroadcast_Receiver(){
        broadcast_Receiver.stop();
    }
    
    //Function to stop SecondaryListner thread.
    void stopSecondaryListner(){
        secondaryListner.stop();
    }
    
    //Function to start Discoverer thread.
    void startDiscoverer(){
        startBroadcast_Sender();
        startBroadcast_Receiver();
        startSecondaryListner();
    }

    //Function to start Discoverer thread.
    void stopDiscoverer(){
        stopBroadcast_Sender();
        stopBroadcast_Receiver();
        stopSecondaryListner();
    }

    
    class Broadcast_Sender implements Runnable {
    	
        volatile boolean exit;
        volatile boolean isRunning;
        //Bs stands for Broadcast Sender
        private DatagramSocket Bs_datagramSocket = null;
        private DatagramPacket Bs_datagramPacket = null;
        Broadcast_Sender(){
            isRunning = false;
            exit = false;
        }
        public void run(){
            try {
                Bs_datagramSocket = new DatagramSocket();
                Bs_datagramSocket.setBroadcast(true);
                
                byte[] buffer =  PEER_ID.getBytes();
                Bs_datagramPacket = new DatagramPacket(buffer,buffer.length, InetAddress.getByName(BROADCAST_ADDRESS), PORT);
                isRunning =  true;
                while (!exit) {
                    System.out.println("[SENDING BROADCAST PACKET]");
                    Bs_datagramSocket.send(Bs_datagramPacket);
                    try{Thread.sleep(5000); } catch(InterruptedException e) { e.printStackTrace(); }
                }
  
                isRunning = false;
            }catch(SocketException e){
                e.printStackTrace();
            }catch(UnknownHostException e){
                e.printStackTrace();
            }catch(IOException e){
                e.printStackTrace();
            }finally {
                Bs_datagramSocket.close();
            }
        }
        
        void stop(){
            if(isRunning) this.exit = true;
        }
    }

    class Broadcast_Receiver implements Runnable{
        volatile boolean exit;
        volatile boolean isRunning;
        //Br stands for Broadcast Receiver
        private DatagramPacket Br_datagramPacket = null;
        private DatagramSocket Br_datagramSocket = null;
        Broadcast_Receiver(){
            isRunning = false;
            exit = false;
        }
        
        public void run(){
            try{
                Br_datagramSocket = new DatagramSocket(PORT,InetAddress.getByName("0.0.0.0"));
                Br_datagramSocket.setSoTimeout(200);
                Br_datagramSocket.setBroadcast(true);
                byte[] buffer = new byte[1000];
                Br_datagramPacket = new DatagramPacket(buffer,buffer.length);
                isRunning =true;
                while(!exit){
                    Br_datagramPacket = new DatagramPacket(buffer,buffer.length);
                    
                    try{
                      
                    	Br_datagramSocket.receive(Br_datagramPacket);
                        String PEER_NAME = new String(Br_datagramPacket.getData(),0,Br_datagramPacket.getLength());

                        if((PEER_NAME.compareTo(PEER_ID)!=0) && (!ConnectedPeerInfo.contains(Br_datagramPacket.getAddress()))) {
                        	ConnectedPeerInfo.put(PEER_NAME,Br_datagramPacket.getAddress());
                        	Logger.write("Connected to "+Br_datagramPacket.getAddress());
                        }
                    }catch(IOException e){

                    }
                }
            }catch(SocketException e){
                e.printStackTrace();
            }catch(UnknownHostException e){
                e.printStackTrace();
            }finally {
                if(Br_datagramSocket!=null) Br_datagramSocket.close();
            }
        }
        void stop(){
            if(isRunning) exit = true;
        }

    }
    //This module will listen to all the 
    class SecondaryListner implements Runnable {

        volatile ServerSocket secondaryListnerSocket = null;
        volatile boolean isRunning;
        volatile boolean exit;

        SecondaryListner() {
            isRunning = false;
            exit = false;
        }

        public void run() {
            try{
                secondaryListnerSocket = new ServerSocket(7777); // it must be fixed:
                secondaryListnerSocket.setSoTimeout(1000); //critical Area
            
            isRunning = true;
            while (!exit) {
                DataInputStream Sl_dataInputStream = null;
                DataOutputStream Sl_dataOutputStream = null;
                Socket connected = null;
                try {
                
                	connected = secondaryListnerSocket.accept();
                    System.out.println(connected.getInetAddress().toString());
                    Sl_dataInputStream = new DataInputStream(connected.getInputStream());
                    Sl_dataInputStream.readInt();
                    int receiverPort = portGenerator.getPort();
                    Thread thread = new Thread(new Receiver(receiverPort));
                    thread.start();
                    Sl_dataOutputStream = new DataOutputStream(connected.getOutputStream());
                    Sl_dataOutputStream.writeInt(receiverPort);

                } catch (IOException e) {
                   // e.printStackTrace();
                } finally {
                    try{
                        if(Sl_dataInputStream!=null) Sl_dataInputStream.close();
                    
                        if(Sl_dataOutputStream!=null) Sl_dataOutputStream.close();
                        
                        if(connected!=null) connected.close();
                        
                         
                    }catch(IOException e){
                        e.printStackTrace();
                    }
                }
            }
            }catch(IOException e){
                e.printStackTrace();
            }finally {
                if(secondaryListnerSocket!=null) try{secondaryListnerSocket.close();}catch(IOException x) {}
            }
          
            isRunning = false;
        }

        void stop(){
            if(isRunning)exit = true;
        }
    }
}