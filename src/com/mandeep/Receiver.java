package com.mandeep;

import java.io.IOException;
import java.io.Serializable;
import java.io.DataOutputStream;
import java.io.ObjectInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.BufferedOutputStream;
import java.io.ObjectInput;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

class Frame implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	byte[] buffer;
        int frame_id;
        Frame(){
            buffer = new byte[1024];
        }
}

public class Receiver implements Runnable {
    private int PORT;

    public Receiver(int PORT){    
        this.PORT = PORT;
    }

    public void run(){ 
    	ServerSocket receiver = null;
        FileInfo fileInfo=null;
        ObjectInputStream R_objectOutputStream=null;
        Socket connected = null;
        try { 
        	receiver = new ServerSocket(PORT); 
        }catch(IOException e) {
        	
        }
            while(true){
            	try{  
                    connected = receiver.accept();
                    R_objectOutputStream = new ObjectInputStream(connected.getInputStream());
                    fileInfo = (FileInfo)R_objectOutputStream.readObject();
                }catch(Exception e){
                    e.printStackTrace();
                }
            	if(fileInfo.filename.compareTo("NULL")==0) {
            		break;
            	}
                File file = new File("/home/lawliet/eclipse-workspace/JACK/Sync",fileInfo.filename);
                FileOutputStream fileOutputStream = null;
                
                try{
                    if(file.exists()){
                        fileOutputStream = new FileOutputStream(file,true);
                    }else{
                        fileOutputStream  = new FileOutputStream(file);
                    }
                }catch(FileNotFoundException e){
                    e.printStackTrace();
                }
                boolean toSend = false;
                DataOutputStream R_dataOutputStream = null;
                try {
                	R_dataOutputStream = new DataOutputStream(connected.getOutputStream());;
             
                    if(file.length()<fileInfo.fileSize) {
                    	
                    	R_dataOutputStream.writeLong(file.length());
                    	toSend = true;
                    }else {
                    	R_dataOutputStream.writeLong(fileInfo.fileSize);
                    	toSend = false;
                    }
                    R_dataOutputStream.flush();
                }catch(IOException e) {
                	e.printStackTrace();
                }finally {
                	try{R_dataOutputStream.close();}catch(IOException e) {e.printStackTrace();}
                }
                       
                if(toSend) {
                	try{

                        DatagramSocket datagramSocket = new DatagramSocket(PORT);

                        byte[] buff = new byte[10000];
                        DatagramPacket datagramPacket = new DatagramPacket(buff,10000);

                        BufferedOutputStream br = new BufferedOutputStream(fileOutputStream);

                        int old_frame_id = 1;
                        byte[] ack = new byte[1];
                        ack[0] = 1;  // first Acknowledgement :
                        DatagramPacket ackPacket = new DatagramPacket(ack,1); //Acknowledgement Packet
                        Frame frame;
                        datagramSocket.setSoTimeout(3000); // just Changed This
                        while(true){
                        	try {
                                datagramSocket.receive(datagramPacket); // Receiving Data
                        	}catch(Exception e) {
                        		continue;
                        	}

                            ByteArrayInputStream bis = new ByteArrayInputStream(datagramPacket.getData());
                            ObjectInput in = null;

                            try{
                                in = new ObjectInputStream(bis);
                                frame = (Frame) in.readObject();
                            }finally{
                                    try{
                                        if(in != null){
                                            in.close();
                                        }
                                        bis.close();
                                    }catch(IOException e){
                                    	
                                    }
                            }

                                System.out.println("Receiving Data ::  Frame id : "+frame.frame_id+" Data length : "+frame.buffer.length);

                                if(old_frame_id != frame.frame_id){
                                    br.write(frame.buffer,0,frame.buffer.length);

                                    old_frame_id = frame.frame_id;

                                    br.flush();
                                }
                                ack[0] = (frame.frame_id==1)? (byte)0:(byte)1;
                                frame.frame_id = 1;
                                ackPacket.setData(ack,0,1);
                                ackPacket.setAddress(datagramPacket.getAddress());
                                ackPacket.setPort(datagramPacket.getPort());
                                datagramSocket.send(ackPacket);
                                if(frame.buffer.length < 1024){
                                    break;
                                }
                                br.flush();
                            }

                        datagramSocket.close();
                        br.close();
                    }catch(IOException e){
                        e.printStackTrace();
                    }catch(ClassNotFoundException e){
                        e.printStackTrace();
                    }catch(Exception e){
                    	e.printStackTrace();
                    }finally {
                    }
                    System.out.println("[RECEIVED] : "+fileInfo.filename);

                }
                
            }
    }

}