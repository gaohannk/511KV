package cse511;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
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

public class BackEndSecondary {
	@SuppressWarnings("resource")
	public static void main(String args[]) throws IOException, ParserConfigurationException, TransformerException {
		int port = 9020;
		ServerSocket serverSocket;
		serverSocket = new ServerSocket(port);
		BackEndSecondary backEndSecondary = new BackEndSecondary();
		backEndSecondary.BuildXML();

		while (true) {
			Socket socket = serverSocket.accept();
			socket.setSoTimeout(100000);
			new Thread(new BackEndSecondaryService(socket)).start();
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
		File file = new File("localstore2.xml");
		if (!file.exists()) {
			file.createNewFile();
		}
		FileOutputStream outputStream = new FileOutputStream(file);
		StreamResult xmlResult = new StreamResult(outputStream);
		transformer.transform(domSource, xmlResult);
	}

	static Map<String, String> store = new HashMap<String, String>();
}

class BackEndSecondaryService implements Runnable {
	Socket socket;

	public BackEndSecondaryService(Socket socket) {
		this.socket = socket;
	}

	public void run() {
		synchronized (BackEndSecondary.store) {
			try {
				ObjectInputStream oi = null;
				oi = new ObjectInputStream(socket.getInputStream());
				oi.close();
				socket.close();
				// ReSend(arrStrings);
			} catch (Exception e) {
				e.printStackTrace();
			}
			Iterator<String> iterator = BackEndSecondary.store.keySet().iterator();
			System.out.println("______________________________");
			while (iterator.hasNext()) {
				String keyString = iterator.next();
				String valuelString = BackEndSecondary.store.get(keyString);
				System.out.println(keyString + " " + valuelString);
			}

		}

	}

	synchronized String Handler(String[] strarray) throws XPathExpressionException, ParserConfigurationException,
			IOException, TransformerException, SAXException {
		String res = null;
		if (strarray[0].equals("put")) {
			if (BackEndSecondary.store.containsKey(strarray[1])) {
				BackEndSecondary.store.remove(strarray[1]);
				BackEndSecondary.store.put(strarray[1], strarray[2]);
				// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
				AddAndUpdate(strarray);
				res = "success";
			} else {
				BackEndSecondary.store.put(strarray[1], strarray[2]);
				// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
				AddAndUpdate(strarray);
				res = "success";
			}
		} else if (strarray[0].equals("get")) {
			res = BackEndSecondary.store.get(strarray[1]);
		}
		return res;
	}

	public synchronized String QueryXML(String key) throws ParserConfigurationException, SAXException, IOException,
			XPathExpressionException {
		String reString = null;
		DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = builderFactory.newDocumentBuilder();
		Document document = builder.parse(new File("localstore2.xml"));
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
		document = builder.parse(new File("localstore2.xml"));
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
		File file = new File("localstore2.xml");
		if (!file.exists()) {
			file.createNewFile();
		}
		FileOutputStream outputStream = new FileOutputStream(file);
		StreamResult xmlResult = new StreamResult(outputStream);
		transformer.transform(domSource, xmlResult);
	}

	public void ReSend(String[] arrStrings) throws UnknownHostException, IOException {

		Socket socket = new Socket("localhost", 9020);
		ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
		out.writeObject(arrStrings);
		out.flush();
		out.close();
		socket.close();
	}

}
