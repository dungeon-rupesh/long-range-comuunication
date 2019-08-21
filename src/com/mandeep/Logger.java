package com.mandeep;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {
	String logFileDir = null;
	static FileWriter logFileWriter = null;
	public Logger(String logFileDir) {
		this.logFileDir = logFileDir;
		try {
			logFileWriter = new FileWriter(logFileDir,true);
		}catch(IOException e) { }
	}
	public static synchronized void write(String logMsg) {
		String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
		String log = String.format("%s, %s,",timeStamp,logMsg);
		System.out.println("LOG : "+log);
		try {
			logFileWriter.write(log+"\n");
			logFileWriter.flush();
		}catch(IOException e) {
			e.printStackTrace();
		}
	}
}
