package com.mandeep;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Comparator;
import java.util.Arrays;

public class FileScanner{

    private ConcurrentHashMap<String,Integer> priority;
    private File folder = null;

    FileScanner(String file){
    	
        folder = new File(file);
        
        //This hashMap will store Extension and it's priority.
        priority = new ConcurrentHashMap<>();
        
        priority.put("txt",9);
        priority.put("jpeg",8);
        priority.put("png",8);
        priority.put("jpg",8);
        priority.put("mp3",6);
        priority.put("mp4",5);
        priority.put("pdf",7);

    } 
    
    //This Function will return the file extension.
    String getExtension(String fileName){
        int lastIndex = fileName.lastIndexOf(".");
        if(lastIndex != -1 && lastIndex != 0){
        	return fileName.substring(fileName.lastIndexOf(".")+1);
        }
        else return "";
    }
    
    //This function will return file priority based on the extension of the file.
    int getPriority(String filename){
        String extension = getExtension(filename);
        int priority_value = priority.get(extension)==null?1:priority.get(extension);
        return priority_value;
    }
    
    //This function will return the sorted list containing names of the file present in the Sync folder. 
    public String[] getList(){
        String[] list = folder.list();
        if(list.length!=0){
        	
        	Comparator<String> compareImpl = (L, R)->{
                if(getPriority(L)<getPriority(R)){
                    return 1;
                }
                return -1;
            };
            
            Arrays.sort(list,compareImpl);    
        }
        
        return list;
    }
}