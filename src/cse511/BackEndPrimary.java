package cse511;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.io.ObjectInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;



public class BackEndPrimary {
	@SuppressWarnings("resource")
	public static void main(String args[]) throws IOException, ParserConfigurationException, TransformerException {
		int port = 9015;
		ServerSocket serverSocket;
		serverSocket = new ServerSocket(port);

		BackEndPrimary backEndPrimary = new BackEndPrimary();
		backEndPrimary.BuildXML();

		while (true) {
			Socket socket = serverSocket.accept();
			socket.setSoTimeout(100000);
			new Thread(new BackEndPrimaryService(socket)).start();
		}
	}

	public void BuildXML() throws ParserConfigurationException, IOException, TransformerException {
		Document document;
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		document = builder.newDocument();
		Element root = document.createElement("KVPairs");
		document.appendChild(root);

		Element entryElement = document.createElement("entry");
		entryElement.setAttribute("key", "key10");
		Element valueElement = document.createElement("value");
		valueElement.setTextContent("value10");
		entryElement.appendChild(valueElement);
		root.appendChild(entryElement);

		TransformerFactory transFactory = TransformerFactory.newInstance();

		Transformer transformer = transFactory.newTransformer();

		DOMSource domSource = new DOMSource(document);
		File file = new File("localstore.xml");
		if (!file.exists()) {
			file.createNewFile();
		}
		FileOutputStream outputStream = new FileOutputStream(file);
		StreamResult xmlResult = new StreamResult(outputStream);
		transformer.transform(domSource, xmlResult);
	}

	static Map<String, String> store = new HashMap<String, String>();
}

class BackEndPrimaryService implements Runnable {
	Socket socket;
	private final static int num=100;


	public BackEndPrimaryService(Socket socket) {
		this.socket = socket;
	}

	public void run() {
		synchronized (BackEndPrimary.store) {
			try {
				final double u = 1000000;
				int i = 0;
				double sum = 0;
				long time1 = System.nanoTime();

				ObjectInputStream oi = null;
				ObjectOutputStream oi2 = null;
				oi = new ObjectInputStream(socket.getInputStream());
				oi2 = new ObjectOutputStream(socket.getOutputStream());
				String[] arrStrings = null;
				arrStrings = (String[]) oi.readObject();
				String result = Handler(arrStrings);
				oi2.writeObject(result);
				oi2.flush();
				ReSend(arrStrings);
				oi2.close();
				oi.close();
				socket.close();
				long time2 = System.nanoTime();
				System.out.println("each operate time is " + (time2 - time1) / u + "ms");
				if (i == num) {
					i = 0;
					System.out.println("total operate time is " + sum / u + "ms");
					sum = 0;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		/*	Iterator<String> iterator = BackEndPrimary.store.keySet().iterator();
			System.out.println("______________________________");
			while (iterator.hasNext()) {
				String keyString = iterator.next();
				String valuelString = BackEndPrimary.store.get(keyString);
				System.out.println(keyString + " " + valuelString);
			}*/
		}

	}

	synchronized String Handler(String[] strarray) throws XPathExpressionException, ParserConfigurationException,
			IOException, TransformerException, SAXException {
		String res = null;
		if (strarray[0].equals("put")) {
			if (BackEndPrimary.store.containsKey(strarray[1])) {
				BackEndPrimary.store.remove(strarray[1]);
				BackEndPrimary.store.put(strarray[1], strarray[2]);
				// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
				AddAndUpdate(strarray);
				res = "success";
			} else {
				BackEndPrimary.store.put(strarray[1], strarray[2]);
				// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
				AddAndUpdate(strarray);
				res = "success";
			}
		} else if (strarray[0].equals("get")) {
			res = BackEndPrimary.store.get(strarray[1]);
		}
		return res;
	}

	public synchronized String QueryXML(String key) throws ParserConfigurationException, SAXException, IOException,
			XPathExpressionException {
		String reString = null;
		DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = builderFactory.newDocumentBuilder();
		Document document = builder.parse(new File("localstore.xml"));
		Element rootElement = document.getDocumentElement();
		XPathFactory factory = XPathFactory.newInstance();
		XPath xpath = factory.newXPath();
		String express = "/KVPairs/entry[@key='" + key + "']";
		Node node = (Node) xpath.evaluate(express, rootElement, XPathConstants.NODE);
		NodeList valueNode = node.getChildNodes();
		for (int i = 0; i < valueNode.getLength(); i++) {
			Node childNode = valueNode.item(i);
			reString = childNode.getTextContent();
		}
		return reString;
	}

	public synchronized void AddAndUpdate(String[] strarray) throws ParserConfigurationException, IOException,
			TransformerException, XPathExpressionException, SAXException {

		Document document;
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		document = builder.parse(new File("localstore.xml"));
		Element root = document.getDocumentElement();

		XPathFactory xpathfactory = XPathFactory.newInstance();
		XPath xpath = xpathfactory.newXPath();
		String express = "/KVPairs/entry[@key='" + strarray[1] + "']";
		Node node = (Node) xpath.evaluate(express, root, XPathConstants.NODE);
		if (node == null) {
			Element entryElement = document.createElement("entry");
			entryElement.setAttribute("key", strarray[1]);
			Element valueElement = document.createElement("value");
			valueElement.setTextContent(strarray[2]);
			entryElement.appendChild(valueElement);
			root.appendChild(entryElement);
			// System.out.println("ok");
		} else {
			NodeList valueNode = node.getChildNodes();
			for (int i = 0; i < valueNode.getLength(); i++) {
				Node childNode = valueNode.item(i);
				childNode.setTextContent(strarray[2]);
			}
		}

		TransformerFactory transFactory = TransformerFactory.newInstance();

		Transformer transformer = transFactory.newTransformer();

		DOMSource domSource = new DOMSource(document);
		File file = new File("localstore.xml");
		if (!file.exists()) {
			file.createNewFile();
		}
		FileOutputStream outputStream = new FileOutputStream(file);
		StreamResult xmlResult = new StreamResult(outputStream);
		transformer.transform(domSource, xmlResult);
	}

	public void ReSend(String[] arrStrings) throws UnknownHostException, IOException {

		Socket socket3 = new Socket("localhost", 9020);
		socket3.setSoTimeout(100000);
		ObjectOutputStream out = new ObjectOutputStream(socket3.getOutputStream());
		out.writeObject(arrStrings);
		out.flush();

		out.close();
		socket3.close();
	}

}

// public class BackEndPrimary {
// public static void main(String args[]) throws IOException,
// ParserConfigurationException, TransformerException {
//
// RequestHandler reqhandler = new RequestHandler();
//
// Socket socket = null;
// ObjectInputStream oi = null;
// ObjectOutputStream oi2 = null;
// try {
// ServerSocket serversocket = new ServerSocket(9015);
// while (true) {
// System.out.println("back-end is running");
// socket = serversocket.accept();
//
// oi = new ObjectInputStream(socket.getInputStream());
// oi2 = new ObjectOutputStream(socket.getOutputStream());
//
// String[] arrStrings = null;
// arrStrings = (String[])oi.readObject();
// String result = reqhandler.Handler(arrStrings);
// oi2.writeObject(result);
//
// // !!!!!!!!!!!!!!!!!!!!
// // reqhandler.ReSend(arrStrings);
// Iterator<String> iterator = reqhandler.store.keySet().iterator();
// while(iterator.hasNext()){
// String keyString = iterator.next();
// String valuelString = reqhandler.store.get(keyString);
// System.out.println(keyString+" "+valuelString);
// }
// }
// } catch (Exception e) {
// e.printStackTrace();
// } finally {
// oi.close();
// socket.close();
// }
// }
// }