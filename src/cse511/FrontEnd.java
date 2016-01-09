package cse511;


import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.ObjectInputStream;


public class FrontEnd {
	public static void main(String args[]) throws IOException {
        int port=9013;
        @SuppressWarnings("resource")
		ServerSocket serverSocket= new ServerSocket(port);
        while(true){
        	Socket socket = serverSocket.accept();
        	socket.setSoTimeout(100000);
        	new Thread(new FrontEndPrimaryService(socket)).start();
        }
	}
}


class FrontEndPrimaryService implements Runnable{
	Socket socket;
	public FrontEndPrimaryService(Socket socket){
		this.socket = socket;
	}
	public void run(){
		try {
			ObjectInputStream oi = null;
			ObjectOutputStream oi2 = null;     //write back
			ObjectInputStream oi2back = null;
			oi = new ObjectInputStream(socket.getInputStream());
			oi2 = new ObjectOutputStream(socket.getOutputStream());
			
			String[] arrStrings = null;
			arrStrings = (String[])oi.readObject();
			for(String str : arrStrings){
				System.out.println(str);
			}
			System.out.println("received from client");
			
			Socket socket2 = new Socket("localhost",9015);
			socket2.setSoTimeout(100000);
	    	ObjectOutputStream out = new ObjectOutputStream(socket2.getOutputStream());
	    	out.writeObject(arrStrings);
	    	out.flush();
	    	oi2back = new ObjectInputStream(socket2.getInputStream());
	    	
	    	String res = null;
			res = (String)oi2back.readObject();
	
	    	oi2.writeObject(res); 
	    	oi2.flush();
	    	
	    	out.close();
	    	oi.close();
	    	oi2.close();
	    	oi2back.close();
			socket2.close();
			socket.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
	}
}


//public class FrontEnd {
//	public static void main(String args[]) throws IOException {
//		Socket socket = null;
//		ObjectInputStream oi = null;
//		ObjectOutputStream oi2 = null;     //write back
//		ObjectInputStream oi2back = null;
//		try {
//			ServerSocket serversocket = new ServerSocket(9013); 
//			while (true) {
//				System.out.println("front-end is running");
//				socket = serversocket.accept();
//				oi = new ObjectInputStream(socket.getInputStream());
//				oi2 = new ObjectOutputStream(socket.getOutputStream());
//				
//				String[] arrStrings = null;
//				arrStrings = (String[])oi.readObject();
//				for(String str : arrStrings){
//					System.out.println(str);
//				}
//				System.out.println("received from client");
//				
//				Socket socket2 = new Socket("localhost",9015);
//		    	ObjectOutputStream out = new ObjectOutputStream(socket2.getOutputStream());
//		    	out.writeObject(arrStrings);
//		    	
//		    	oi2back = new ObjectInputStream(socket2.getInputStream());
//		    	
//		    	String res = null;
//				res = (String)oi2back.readObject();
//
//		    	oi2.writeObject(res); 
//		    	
//		    	out.flush();
//		    	out.close();
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			oi.close();
//			socket.close();
//		}
//	}
//}