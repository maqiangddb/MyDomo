package com.adgsoftware.mydomo.engine.connector.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import com.adgsoftware.mydomo.engine.Log;
import com.adgsoftware.mydomo.engine.connector.CommandListener;
import com.adgsoftware.mydomo.engine.connector.Commander;
import com.adgsoftware.mydomo.engine.connector.ConnectionListener;
import com.adgsoftware.mydomo.engine.controller.Controller;
import com.adgsoftware.mydomo.engine.controller.Status;

public class OpenWebCommanderImpl implements Commander {
	
	Socket socket;
//	Logger log = Logger.getLogger(OpenWebCommanderImpl.class.getName());
	Log log = new Log();
	BufferedReader input = null;
	PrintWriter output = null;
	private String ip;
	private int port;
	private int timeout = 5000;
//	private long passwordOpen;
	boolean usePassword = false; // TODO manage password
	List<ConnectionListener> connectionListenerList = new ArrayList<ConnectionListener>();
	
	
	/**
	 * 
	 * @param ip the ip or dns name of the open server
	 * @param port the port number of the open server
	 * @param passwordOpen 
	 */
	public OpenWebCommanderImpl(String ip, int port, long passwordOpen) {
		this.ip = ip;
		this.port = port;
//		this.passwordOpen = passwordOpen;
	}
	
	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
		close();
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
		close();
	}
	
	/**
	 * Asynchrone connection
	 */
	public void connect() { 
		new Thread(new OpenWebConnectThread(this)).start(); // Make connection in thread to avoid blocking the user!
		
	}
	
	@Override
	public void close() {
		if(socket != null){
			try {
				socket.close();
				socket = null;
				for (ConnectionListener connectionListener : connectionListenerList) {
					try {
						connectionListener.onClose();
					} catch (Exception e) {
						log.severe(Log.Session.Command, "ConnectionListener raise an error: " + e.getMessage());
					}
				}
						
			} catch (IOException e) {				
				log.severe(Log.Session.Command, "Error during closing the socket: " + e.getMessage());
			}
		}
	}
	
	@Override
	public void sendCommand(String command, CommandListener resultListener){

//		try { TODO check command!!!!
//			openWebNet = new OpenWebNet(comandoOpen);
//			if(openWebNet.isErrorFrame()){
//				ClientFrame.scriviSulLog("ERRATA frame open "+comandoOpen+", la invio comunque!!!",1,0,0);
//			}else{
//				ClientFrame.scriviSulLog("CREATO oggetto OpenWebNet "+openWebNet.getFrameOpen(),1,0,0);
//			}
//		}catch(Exception e){
//			ClientFrame.scriviSulLog("ERRORE nella creazione dell'oggetto OpenWebNet "+comandoOpen,1,0,0);
//			System.out.println("Eccezione in GestioneSocketComandi durante la creazione del'oggetto OpenWebNet");
//			e.printStackTrace();
//		}

		if (!isConnected()) { // If socket close? => init connection.
			new Thread(new OpenWebConnectThread(this)).start(); // Open connection in thread to avoid blocking user!
		}
		// Send asynchronously the command!
		new Thread(new OpenWebCommandThread(this, command, resultListener)).start(); 
	}

	void writeMessage(String message) {
		if (output != null) { // No output can mean no server is responding
			output.write(message);
			output.flush();
			log.fine(Log.Session.Command, "TO   COMMAND SERVER: " + message);
		}
	}
	
    String readMessage(){
	    int indice = 0;
	    boolean exit = false;
	    char respond[] = new char[1024];
		char c = ' ';
		int ci = 0;
		String responseString = null;
		
    	try{
	    	do { 
	    		if(socket != null && !socket.isInputShutdown()) {
	    			ci = input.read();		    		
		    		if (ci == -1) {
			  			log.finest(Log.Session.Command, "End of read from command server socket.");
			  			socket = null;
			  			break;
			        } else { 
			        	c = (char) ci;			        				        
					    if (c == '#' && indice > 1 && '#' == respond[indice-1]) {
					    	respond[indice] = c;
					    	exit = true;
					    	log.finest(Log.Session.Command, "End of message from command server socket [" + new String(respond) + "].");
					    	break;
					    } else {
					    	respond[indice] = c;
					    	indice = indice + 1;
					    } 
			        }
	    		} else {
	    			close();
	    			break;
	    		}
	        } while(true); 
		}catch(IOException e){
			log.severe(Log.Session.Command, "Socket not available: " + e.getMessage());
	    }
		
		if (exit == true){
			responseString = new String(respond,0,indice+1);
		}
		
		log.fine(Log.Session.Command, "FROM COMMAND SERVER: " + responseString);
		
		return responseString;
    }

	@Override
	public void addControllerToExecute(Controller<? extends Status> controller) {
		controller.setServer(this);
		
	}
	
	@Override
	public boolean isConnected() {
		if(socket != null){
			if (socket.isConnected()) {
				return true;		
			} else {
				close();
				return false;
			}
		} else {
			return false;
		}
	}

	@Override
	public void addConnectionListener(
			ConnectionListener connectionListener) {
		connectionListenerList.add(connectionListener);
	}

	@Override
	public int getTimeout() {
		return timeout;
	}

	@Override
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}
}
