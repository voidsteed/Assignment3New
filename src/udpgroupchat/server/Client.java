package udpgroupchat.server;

import java.util.Date;
import java.util.Queue;
import java.util.UUID;

public class Client {
	//every client has a clientEndPoint which includes address and port
	private ClientEndPoint clientEndPoint = null;
	//unique ID for each client
	private UUID uniqueID;
	private String name;
	//Client is active or not
	private boolean aFlag;
	private Date timestamp;
	private Queue<String> messages;
	
	
	//Ctor
	public Client(String name, ClientEndPoint cep){
		this.name = name;
		uniqueID = UUID.randomUUID();
		this.aFlag = true;
		clientEndPoint = cep;
	}
	
	//getters
	public String getName(){
		return name;
	}
	public UUID getUniqueID(){
		return uniqueID;
	}
	public boolean isActive(){
		return aFlag;
	}
	public ClientEndPoint getClientEndPoint(){
		return clientEndPoint;
	}
	public Queue<String> getMessage(){
		return messages;
	}
	//setters

	public void setActive(ClientEndPoint cep){
		 aFlag = true;
		 addClientEndPoint(cep);
	}
	public void setInactive(){
		aFlag = false;
		Server.clientEndPoints.remove(clientEndPoint);
		clientEndPoint = null;
	}
	//adding this client to the server EndClientPoint set
	public void addClientEndPoint(ClientEndPoint cep){
		this.clientEndPoint = cep;
		Server.clientEndPoints.add(cep);
	}
	//add sent message to user's queue
	public void addMessage(String msg){
		messages.add(msg);
	}
	
	public void removeMessage(UUID id){
		for(String str: messages){
			if(Server.idsToMsgsMap.get(id).equals( str)){
				messages.remove(str);
				break;
			}
		}
	}
	
	
}
