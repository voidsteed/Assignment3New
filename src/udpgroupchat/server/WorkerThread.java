package udpgroupchat.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class WorkerThread extends Thread {

	private DatagramPacket rxPacket;
	private DatagramSocket socket;
	
	//available Client and ClientEndPoint in this workerThread;
	private Client availableClient = null;
	private ClientEndPoint cep = null;

	public WorkerThread(DatagramPacket packet, DatagramSocket socket) {
		this.rxPacket = packet;
		this.socket = socket;
		
		cep = new ClientEndPoint(this.rxPacket.getAddress(),this.rxPacket.getPort());
		//find the current client and clientEndPoint
		for(Client client : Server.idsToClientsMap.values()){
			if(client.isActive() && cep.hashCode() == client.getClientEndPoint().hashCode()){
				availableClient = client;
				break;
			}
		}

		
	}

	@Override
	public void run() {
		// convert the rxPacket's payload to a string
		String payload = new String(rxPacket.getData(), 0, rxPacket.getLength())
				.trim();

		// dispatch request handler functions based on the payload's prefix
		
		//REGISTER:CLIENT_NAME
		if (payload.startsWith("REGISTER")) {
			onRegisterRequested(payload);
			return;
		}
		//UNREGISTER:CLIENT_NAME
		if (payload.startsWith("UNREGISTER")) {
			onUnregisterRequested(payload);
			return;
		}

		//SEND:GROUP_NAME:MESSAGE
		if (payload.startsWith("SEND")) {
			onSendRequested(payload);
			return;
		}

		//CREATE_GROUP:GROUPNAME
		//CREATE_GROUP:GROUPNAME:MAXSIZE
		if (payload.startsWith("CREATE_GROUP")) {
			onCreateGroupRequested(payload);
			return;
		}
		//JOIN:GROUP_NAME
		if (payload.startsWith("JOIN")) {
			onJoinRequested(payload);
			return;
		}
		//LIST_GROUP
		if (payload.startsWith("LIST_GROUP")) {
			onListGroupRequested(payload);
			return;
		}
		
		if (payload.startsWith("POLL")) {
			onPollRequested(payload);
			return;
		}
		
		if (payload.startsWith("ACK")) {
			onACKRequested(payload);
			return;
		}

		if (payload.startsWith("SHUTDOWN")) {
			onShutDownRequested(payload);
			return;
		}
		
		if (payload.startsWith("QUIT")) {
			onQuitRequested(payload);
			return;
		}
		// if we got here, it must have been a bad request, so we tell the
		// client about it
		onBadRequest(payload);
	}

	public void sendACK(String payload, InetAddress address, int port)
			throws IOException {

		int ackID = (int) (Math.random() * 1000 );
		final String ack = "ACK:" + ackID;
		payload = ack + " " + payload;
		final DatagramPacket txPacket = new DatagramPacket(payload.getBytes(),
				payload.length(), address, port);

		final Timer timer = new Timer();
		Server.ackToTimerMap.put(ack, timer);

		timer.scheduleAtFixedRate(new TimerTask() {
			  @Override
			  public void run() {
					try {
						WorkerThread.this.socket.send(txPacket);
						//WorkerThread.this.socket.receive(rPacket);

					} catch (IOException e) {
						e.printStackTrace();
					}

			  }
			}, 0, (long) (20*1000)); ///TEST 3*60*1000);
	}

	// send a string, wrapped in a UDP packet, to the specified remote endpoint
	public void send(String payload, InetAddress address, int port)
			throws IOException {
		DatagramPacket txPacket = new DatagramPacket(payload.getBytes(),
				payload.length(), address, port);
		this.socket.send(txPacket);
	}
	
	public void sendToClient(String str){
		try {
			sendACK(str, this.rxPacket.getAddress(),
					this.rxPacket.getPort());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void sendMsgToServer(String message) throws SocketException,IOException{
		DatagramPacket txPacket = new DatagramPacket(message.getBytes(), message.length(), Server.serverSktAddress);
		socket.send(txPacket);
	}
	

	private void onRegisterRequested(String payload) {		
		String[] tokens = payload.split(":");
		String name = tokens[1];
		boolean exist = false;
		String str = "";
		
		for (Client c : Server.idsToClientsMap.values()){
			if(c.getName().equals(name)){
				exist = true;
				str = "Name: " + name + " already exists!\n";
			}
			// create a client object, and put it in the map that assigns names
			// to client objects
			
		}
		if(!exist){
			Client client = new Client(name,cep);
			Server.clientEndPoints.add(client.getClientEndPoint());
			Server.idsToClientsMap.put(client.getUniqueID(),client);
			str = "REGISTERED " + client.getName() + " as a user!\n";
		}
		sendToClient(str);
	}

	private void onUnregisterRequested(String payload) {

		String[] tokens = payload.split(":");
		String name = tokens[1];
		String str = "";
		boolean exist = false;
		
		for(Client c : Server.idsToClientsMap.values()){
			if(c.getName().equals(name)){
				Server.idsToClientsMap.remove(c.getUniqueID());
				availableClient.setInactive();
				str = "Client "+ c.getName() +" quit!\n";
				break;
			}
		}
		if(!exist){
			str = "CLIENT NOT REGISTERED\n";
		}
		// check if client is in the set of registered clientEndPoints
		/*if (Server.idsToClientsMap.containsValue(name)) {
			Server.idsToClientsMap.remove(name);
			str = "UNREGISTERED " + name +"\n";
		} else {
			str = "CLIENT NOT REGISTERED\n";
		}*/
		sendToClient(str);
	}

	private void onCreateGroupRequested(String payload) {
		String[] tokens = payload.split(":");
		String groupName = tokens[1];
		boolean exist = false;
		String str = "";
		
		if(tokens != null && (tokens.length == 2 || tokens.length == 3)){
			for (Group g : Server.groups){
				if(g.getGroupName().equals(groupName)){
					exist = true;
					str = "Group Name: " + groupName + " already exists!\n";
					break;
				}
				}
				// create a client object, and put it in the map that assigns names
				// to client objects
			if(!exist){
				Group group;
				if(tokens.length == 2){
					group = new Group(tokens[1]);
				}
				else{//create with groupName and maxSize
					group = new Group(tokens[1],Integer.parseInt(tokens[2]));
				}
				group.add(availableClient);
				Server.groups.add(group);
				str = "CREATED THE GROUP: " + groupName + "\n"; 
			}
			
		}
		else{
			str = "INVALID REQUEST!";
		}
		// note that calling clientEndPoints.add() with the same endpoint info
		// (address and port)
		// multiple times will not add multiple instances of ClientEndPoint to
		// the set, because ClientEndPoint.hashCode() is overridden. See
		// http://docs.oracle.com/javase/7/docs/api/java/util/Set.html for
		// details.

		// tell client we're OK
		sendToClient(str);
		
	}
	//SEND:GROUP_NAME:MESSAGE
	private void onSendRequested(String payload) {
		
		String[] tokens = payload.split(":");
		boolean exist = false;
		String msg = "";
		String str = "";
		
		if(tokens.length == 3){
			String groupName = tokens[1];
			String message = tokens[2];
			for (Group g : Server.groups){
				if(g.getGroupName().equals( groupName) && g.hasClient(availableClient)){
					exist = true;
					msg = "MSG FROM " + availableClient.getName() + " TO " + groupName + ": " + message + "\n";
					for(Client c : g.getList()){
						if(c.isActive()){
							try{
								send(msg,c.getClientEndPoint().address,c.getClientEndPoint().port);
							}
							catch(IOException e){
								e.printStackTrace();
							}
						}
						else{
							
							c.addMessage(msg);
							
						}
						Server.idsToMsgsMap.put(c.getUniqueID(), msg);
					}
					//break;
				}
				
			}
				if(!exist){
					str = "YOU MUST JOIN THE GROUP "+ groupName +" OR THE GROUP DOESN'T EXIST!\n";
			}
		}
		else{
			str = "INVALID REQUEST!";
		}
		sendToClient(str);
	}
	

	//JOIN:GROUP_NAME
	private void onJoinRequested(String payload) {
		String[] tokens = payload.split(":");
		String str = "";
		boolean exist = false;
		
		if(tokens.length != 0 && tokens.length == 2){
			String groupName = tokens[1];
			for(Group g : Server.groups){
				if(g.getGroupName().equals(groupName)){
					exist = true;
					//if group success added the client
					if(g.hasClient(availableClient)){
						str = availableClient.getName() +" IN "+ groupName +" \n";
					}
					else if (g.add(availableClient)){
						str = availableClient.getName() + " JOINED GROUP: " + g.getGroupName() + "\n";
					}

					else{
						str = availableClient.getName() + " CAN'T JOIN THE GROUP: " + groupName + " is full\n";
					}
					break;
				}
			}
			if(!exist){
				str = groupName + " doesn't exist!\n";
			}
			
		}
		else{
			str = "Invalid request\n";
		}
		
		sendToClient(str);
	}
	//POLL:NAME
	private void onPollRequested(String payload) {		
		StringBuilder str = new StringBuilder();
		String[] tokens = payload.split(":");
		String name = tokens[1];
		boolean exist = false;
		UUID id = null;


			System.out.println("yo dogggg");
			for(Client c: Server.idsToClientsMap.values()){
				if(c.getName().equals(name)){
					System.out.println("yo dogggg if");
					exist = true;
					
					for (UUID key  : Server.idsToClientsMap.keySet()) {
						if(Server.idsToClientsMap.get(key).getName().equals(name)){
							id = key;
							System.out.println(id.toString());
						}
					}	
				}
			}

		for(UUID uuid: Server.idsToMsgsMap.keySet()){
			if(uuid.equals(id)){
				String msg = Server.idsToMsgsMap.get(uuid);
				System.out.println(msg);
				sendToClient(msg);
			}
		}
	}
		

	//ACK:UNIQUE_ID
	private void onACKRequested(String payload) {
		//get the payload information from payload
		if(Server.ackToTimerMap.containsKey(payload)){
			Server.ackToTimerMap.get(payload).cancel();
			}
		
		
	}

	private void onShutDownRequested(String payload) {
		if(cep.address.equals("127.0.0.1")){
			socket.close();
			
		}
		sendToClient("SERVER SHUTDOWN\n");
		
	}
	//QUIT:GROUP_NAME
	private void onQuitRequested(String payload) {
		//get the payload information from payload
		String[] tokens = payload.split(":");
		String groupName = tokens[1];
		String str = "";
		boolean exist = false;
		
		for(Group g : Server.groups){
			if (g.getGroupName().equals(groupName)){
				exist = true;
				if(g.hasClient(availableClient)){
					String name = availableClient.getName();
					g.remove(availableClient);
					str = name + " QUIT GROUP " +groupName+"\n";
					break;
				}
				else{
					str = "NO CLIENT: " + availableClient.getName() + " IN THE GROUP " + groupName;
				}
			}
		}
		if(!exist){
			str = "GROUP " + groupName + " DOENS'T EXIST!";
		}
		sendToClient(str);

		
	}
	//LIST_GROUP
	private void onListGroupRequested(String payload) {
		StringBuilder  str = new StringBuilder();
		for(Group g : Server.groups){
			str.append(g.getGroupName() + "\n");
		}
		sendToClient(str.toString());
	}
	
	private void onBadRequest(String payload) {
		try {
			sendACK("BAD REQUEST\n", this.rxPacket.getAddress(),
					this.rxPacket.getPort());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
