package com.adgsoftware.mydomo.engine.connector.impl;

import com.adgsoftware.mydomo.engine.Command;
import com.adgsoftware.mydomo.engine.Log;
import com.adgsoftware.mydomo.engine.connector.CommandListener;
import com.adgsoftware.mydomo.engine.connector.CommandResult;
import com.adgsoftware.mydomo.engine.connector.CommandResultStatus;

/**
 * Manage the connection in a new thread to be asynchrone and avoid blocking process. 
 *
 */
public class OpenWebCommandThread implements Runnable {
	
//	Logger log = Logger.getLogger(OpenWebCommandThread.class.getName());
	Log log = new Log();
	private OpenWebCommanderImpl commander;
	private String command;
	private CommandListener resultListener;
	
	public OpenWebCommandThread(OpenWebCommanderImpl commander, String command, CommandListener resultListener) {
		this.commander = commander;
		this.command = command;
		this.resultListener = resultListener;
	}
	
	public void run() {
		sendCommand(command, resultListener);
	}

	public void stop() {
	}
		
	public void sendCommand(String command, CommandListener resultListener) { 
		synchronized (OpenWebCommanderImpl.class) { // mutex on the main thread: only one connection or send message at the same time!
			if (commander.isConnected()) { // Test again since with the lock, maybe a previous thread has closed the connection!

				commander.writeMessage(command);
				String msg = commander.readMessage();
				
		    	if(msg == null){
		    		log.severe(Log.Session.Command, "Command failed.");
		    		if (resultListener != null) {
		    			resultListener.onCommand(new CommandResult(null, CommandResultStatus.error));
		    		}
		    		return;
		    	}
		
		    	if (Command.ACK.equals(msg)){
		    		log.finest(Log.Session.Command, "Command sent.");
		    		if (resultListener != null) {
		    			resultListener.onCommand(new CommandResult(Command.ACK, CommandResultStatus.ok));
		    		}
		    		return;
		    	} else if (Command.NACK.equals(msg)){
		    		log.severe(Log.Session.Command, "Command failed.");
		    		if (resultListener != null) {
		    			resultListener.onCommand(new CommandResult(Command.NACK, CommandResultStatus.nok));
		    		}
		    		return;
		    	} else { // First return was information. The next should be acknowledgment
		    		String actionReturn = msg;
		    		msg = commander.readMessage();
		    		if(Command.ACK.equals(msg)){
		    			log.finest(Log.Session.Command, "Command sent.");
		    			if (resultListener != null) {
		    				resultListener.onCommand(new CommandResult(actionReturn, CommandResultStatus.ok));
		    			}
		    			return;
		    		} 
		
					log.severe(Log.Session.Command, "Command failed.");
					if (resultListener != null) {
						resultListener.onCommand(new CommandResult(actionReturn, CommandResultStatus.error));
					}
					return;
				}
			} else { // connection closed...
				log.severe(Log.Session.Command, "Command failed (Connection closed).");
				if (resultListener != null) {
					resultListener.onCommand(new CommandResult(Command.NACK, CommandResultStatus.nok));
				}
				return;
			}
		}
	}
}
