package com.adgsoftware.mydomo.engine.connector.impl;

/*
 * #%L
 * MyDomoEngine
 * %%
 * Copyright (C) 2011 - 2013 A. de Giuli
 * %%
 * This file is part of MyDomo done by A. de Giuli (arnaud.degiuli(at)free.fr).
 * 
 *     MyDomo is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     MyDomo is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with MyDomo.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

import com.adgsoftware.mydomo.engine.Command;
import com.adgsoftware.mydomo.engine.Log;
import com.adgsoftware.mydomo.engine.connector.ConnectionListener;
import com.adgsoftware.mydomo.engine.connector.ConnectionStatusEnum;

/**
 * Manage the connection in a new thread to be asynchrone and avoid blocking process. 
 *
 */
public class OpenWebConnectThread implements Runnable {
	
//	Logger log = Logger.getLogger(OpenWebConnectThread.class.getName());
	Log log = new Log();
	private OpenWebCommanderImpl commander;
	
	public OpenWebConnectThread(OpenWebCommanderImpl commander) {
		this.commander = commander;
	}

	public void run() {
		connect();
	}

	public void stop() {
	}
		
	public boolean connect() {
		synchronized (commander) { // mutex on the main thread: only one connection or send message at the same time!
			if (!commander.isConnected()) { // Test again since with the lock, maybe a previous thread has opened the connection!
				
				try {
					String ip = commander.getIp();
					int port = commander.getPort();
					
					log.fine(Log.Session.Command, "Connect to socket ["+ ip +":"+ port+"]");
					if (ip == null || port == 0) {
						return false;
					}
					
					commander.socket = new Socket();
					InetSocketAddress address = new InetSocketAddress(ip, port);
					commander.socket.connect(address, commander.getTimeout());
					commander.input= new BufferedReader(new InputStreamReader(commander.socket.getInputStream()));
					log.finest(Log.Session.Command, "InputReader created.");
					commander.output = new PrintWriter(commander.socket.getOutputStream(),true);
					log.finest(Log.Session.Command, "PrintWriter created.");
				} catch (Exception e){
					log.severe(Log.Session.Command, "Impossible to connect to the server ["+ commander.getIp() +":"+ commander.getPort()+"]:" + e.getMessage());
					if (commander.socket!=null && commander.socket.isConnected()) {
						commander.close();
					}
					return false;
				}
				
				if(commander.socket != null) {
						
					log.finest(Log.Session.Command, " ----- Step Connection ----- ");
					String msg = commander.readMessage();
		            if (!Command.ACK.equals(msg)) {
		            	// Bad return message
		                log.severe(Log.Session.Command, "Bad message [" + msg + "] received from [" + commander.getIp() + "]");
		                callOpenWebConnectionListenerConnect(ConnectionStatusEnum.WrongAcknowledgement);
		                commander.close();
		                return false;
		            }
					
		            log.finest(Log.Session.Command, "----- Step Identification -----");
		        	commander.writeMessage(Command.COMMAND_SESSION);
		            
					if(commander.usePassword){
						log.finest(Log.Session.Command, "----- Step authentification -----");
						msg = commander.readMessage();
			            
				    	long password = 0; //gestPassword.applicaAlgoritmo(passwordOpen, msg); TODO manage password
				    	String passwordMsg = "*#"+password+"##"; 
				    	log.finest(Log.Session.Command, "Tx: " + passwordMsg);
				    	commander.writeMessage(passwordMsg);		    	
		        	} 
					
					log.finest(Log.Session.Command, "----- Step Final -----");
					msg = commander.readMessage();
			    	
					if (!Command.ACK.equals(msg)) {		       	
				        log.severe(Log.Session.Command, "Problem during connection to [" + commander.getIp() + "] with message [" + msg + "]");
				        callOpenWebConnectionListenerConnect(ConnectionStatusEnum.WrongAcknowledgement);
				        commander.close();
				        return false;
				    }
					
					log.fine(Log.Session.Command, "Connection OK");
					callOpenWebConnectionListenerConnect(ConnectionStatusEnum.Connected);
			        return true;
		
				} else {
					log.severe(Log.Session.Command, "No socket... Impossible to connect");
					callOpenWebConnectionListenerConnect(ConnectionStatusEnum.NoSocket);
					return false;
				}
			}
			
			return true;
		}
	}
	
	private void callOpenWebConnectionListenerConnect(ConnectionStatusEnum connection) {
		for (ConnectionListener connectionListener : commander.connectionListenerList) {
			try {
				connectionListener.onConnect(connection);
			} catch (Exception e) {
				log.severe(Log.Session.Command, "ConnectionListener raise an error: " + e.getMessage());
			}
		}
	}
}
