package com.mandeep;

import java.io.IOException;

public class Main{
	
	public static void main(String[] args){
		Discoverer discoverer = new Discoverer("PEER1","192.168.100.255",8080); 
		System.out.println("Server Started");
		new Logger("/home/lawliet/eclipse-workspace/JACK/Log/log.csv");
		discoverer.startDiscoverer();
		Synchronizer sync = new Synchronizer(discoverer);
		Thread syncThread =new Thread(sync);
		syncThread.start();
		System.out.println("Press Enter to Stop Server : ");
		try{System.in.read();} catch(IOException e) { e.printStackTrace(); }
		discoverer.stopDiscoverer();
		System.out.println("STOP");
		sync.stop();
	}
}