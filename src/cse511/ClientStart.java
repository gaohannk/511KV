package cse511;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.io.ObjectOutputStream;



public class ClientStart {
	public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {		
		for(int i=0;i<500;i++){
			if((i&1)==0){
				new Thread(new ClientPutThread("localhost", 9013, "user"+Integer.toString(i), Integer.toString(i))).start();
			}else{
				new Thread(new ClientPutThread("localhost", 9014, "user"+Integer.toString(i), Integer.toString(i))).start();
			}
//			Thread.currentThread().sleep(100);
		}
		Thread.currentThread();
		Thread.sleep(100);
		new Thread(new ClientGetThread("localhost", 9014, "user1")).start();
	}
}

class ClientPutThread implements Runnable{
	String servername;
	int port;
	String key;
	String value;
	public ClientPutThread(String servername, int port, String key, String value){
		this.servername = servername;
		this.port = port;
		this.key = key;
		this.value = value;
	}
	public void run(){
		try {
			Socket s = new Socket(servername,port);
			s.setSoTimeout(100000);
	    	ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
			ObjectInputStream in = new ObjectInputStream(s.getInputStream());
	    	out.writeObject(new String[]{"put",key,value});
	    	out.flush();
			String res = (String)in.readObject();
			System.out.println("key: "+key+" value:"+value+" "+res);		    	
			out.close();
			s.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

class ClientGetThread implements Runnable{
	String servername;
	int port;
	String key;

	public ClientGetThread(String servername, int port, String key){
		this.servername = servername;
		this.port = port;
		this.key = key;

	}
	public void run(){
		try {
			Socket s = new Socket(servername,port);
			s.setSoTimeout(100000);
	    	ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
			ObjectInputStream in = new ObjectInputStream(s.getInputStream());
	    	out.writeObject(new String[]{"get",key,""});
	    	out.flush();
			String res = (String)in.readObject();
			System.out.println("the value of "+key+" is:"+res);		    	
	    	out.close();
	    	s.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}




