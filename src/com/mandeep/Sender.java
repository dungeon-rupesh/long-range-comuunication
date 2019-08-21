package com.mandeep;

import java.io.IOException;
import java.io.Serializable;
import java.io.File;
import java.io.ObjectOutputStream;
import java.io.DataInputStream;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutput;
import java.io.FileInputStream;

import java.net.InetAddress;
import java.net.Socket;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.SocketException;

import java.util.Arrays; 
 

class FileInfo implements Serializable{
	/*
	 * 
	 */
	private static final long serialVersionUID = 1L;
	String filename;
	long fileSize;
}

public class Sender implements Runnable{
	
    InetAddress IP_ADDRESS;
    int PORT;
    private FileScanner fileScanner;
    volatile boolean exit;
    
    public Sender(InetAddress IP_ADDRESS,int PORT){
        this.IP_ADDRESS = IP_ADDRESS;
        this.PORT = PORT; 
        fileScanner = new FileScanner("/home/lawliet/eclipse-workspace/JACK/Sync/");
        this.exit = false;
    }
    public void run(){

    	File folder = new File("/home/lawliet/eclipse-workspace/JACK/Sync/"); 
        String[] listOfFile = fileScanner.getList();        
        Socket sender=null;
        FileInfo fileInfo = null;
        label:
        for(String filename : listOfFile){
        	 File file = new File(folder,filename);
        	 fileInfo = new FileInfo();
        	 fileInfo.filename = filename;
        	 fileInfo.fileSize = file.length();
             long  presentBytes = 0;
        	 
             try{
                 //Check this:
                 sender = new Socket(IP_ADDRESS,PORT);
                 ObjectOutputStream objectOutputStream = new ObjectOutputStream(sender.getOutputStream());
                 Thread.sleep(100);
                 objectOutputStream.writeObject(fileInfo);
                 objectOutputStream.flush();
                 
             }catch(Exception e){
                 e.printStackTrace();
             }
             
            try{
            	
                DataInputStream br = new DataInputStream(sender.getInputStream());  
                presentBytes = br.readLong();
                br.close();
            }catch(IOException e){
                e.printStackTrace();
            }
            
            if(presentBytes<fileInfo.fileSize) { //check this
            	try {
                    DatagramSocket datagramSocket = new DatagramSocket();
                    byte[] ack = new byte[1];
                    DatagramPacket ackPacket = new DatagramPacket(ack,1);
                    BufferedInputStream br = new BufferedInputStream(new FileInputStream(file));
                    br.skip(presentBytes);
                    int read;
                    datagramSocket.setSoTimeout(700);
                    ByteArrayOutputStream bos;
                    ObjectOutput out;
                    Frame frame = new Frame();
                    frame.frame_id = 0; 
                    byte[] temp = new byte[1024];

                    while((read = br.read(temp,0,1024)) != -1){
                        frame.buffer = Arrays.copyOf(temp,read);

                        bos = new ByteArrayOutputStream();
                        out = new ObjectOutputStream(bos);
                        out.writeObject(frame);
                        out.flush();
                        byte[] buffer =  bos.toByteArray();
                        bos.close();
                        DatagramPacket datagramPacket = new DatagramPacket(buffer,buffer.length, IP_ADDRESS,PORT);
                        datagramPacket.setData(buffer,0,buffer.length);

                        while(!exit){
                        	
                            datagramSocket.send(datagramPacket);
                             
                             System.out.println("Sending Data ::  Frame id : "+frame.frame_id+" Data length : "+frame.buffer.length);
                            try{
                                datagramSocket.receive(ackPacket);
                            }catch(Exception e){
                                continue;
                            }
                            byte recv_ack = ackPacket.getData()[0];

                            if(frame.frame_id!=recv_ack){
                                frame.frame_id = recv_ack==1?1:0;
                                break;
                            }
                        }
                        if(exit) {
                        	try { Thread.sleep(1000);}catch(InterruptedException e){
                        		e.printStackTrace();
                        	}
                        	datagramSocket.close();
                        	break label;
                        }  
                    }
                    
                    datagramSocket.close();
                    br.close();
      
                }catch(SocketException e){
                    e.printStackTrace();
                }catch(Exception e){
                    e.printStackTrace();
                }

                System.out.println("[SENT] : "+filename);
            }
           
        }
       // At the End We have To send File name NULL  
        try{
        	fileInfo = new FileInfo();
        	fileInfo.filename="NULL";
        	fileInfo.fileSize = 0;
            //Check this:
        	
            sender = new Socket(IP_ADDRESS,PORT);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(sender.getOutputStream());
            Thread.sleep(100);
            objectOutputStream.writeObject(fileInfo);
            objectOutputStream.flush();
            
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    void stop() {
    	if(!exit) exit = true;
    }
}