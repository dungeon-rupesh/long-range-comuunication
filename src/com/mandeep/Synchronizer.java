package com.mandeep;
import java.util.ArrayList;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

//This class will try to connect to all those machines whose IPAddress is stored by Broadcast_Receiver module of Discoverer class.

public class Synchronizer implements Runnable {
    
	private ConcurrentHashMap<String, InetAddress> ConnectedPeerInfo = null;
    volatile boolean exit;
    Socket remoteHost = null;
    Discoverer discoverer = null; 
    
    // This ArrayList stores all the Sender Object called during one iteration.  
    ArrayList <Sender> storeSenderObject = null; 

    public Synchronizer(Discoverer discoverer){
        this.exit = false;
        this.discoverer = discoverer;
        this.storeSenderObject = new ArrayList<>();
    }

    public void run(){
    	
        while(!exit){
            DataOutputStream dos = null;
            DataInputStream dis = null;
            ConnectedPeerInfo = discoverer.getConnectedPeerInfo();
            
            //Loop to iterate through all the IP present in the map.
            for( Map.Entry<String, InetAddress> X : ConnectedPeerInfo.entrySet()){
                
                	try{
                        remoteHost = new Socket(X.getValue(),7777);
                        int request = 200;
                        dos = new DataOutputStream(remoteHost.getOutputStream());
                        dos.writeInt(request);
                        dos.flush();
                        dis = new DataInputStream(remoteHost.getInputStream());
                        int port = dis.readInt();
                        Sender sender = new Sender(X.getValue(),port);
                        Thread senderThread = new Thread(sender);
                        senderThread.start();
                        storeSenderObject.add(sender);
                    }catch(IOException e){
                        e.printStackTrace();
                    }       
            }
            try{
            	if(dos!=null) dos.close();
            	
            	if(dis!=null) dis.close();
            }catch(IOException e) {}
            
            try{
            	Thread.sleep(60000); // Synchronize with connected peers after every minute.
            }catch(InterruptedException e){ }
            
            threadStopper(); //Will stop all the  sender threads called during above process. 
            
            storeSenderObject.clear();
        }
    }
    
    //This function will stop all the sender thread called during  above process.
    
    void threadStopper() {
    	for(Sender s : storeSenderObject) {
        	s.stop();
        }
    }
    
    void stop(){
        if(!exit) {
        	exit = true;
        	threadStopper();
        } 
    }
}