package udpgroupchat.server;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Group {
	private String groupName;
	private int maxSize = 500;
	private Set<Client> clientsList = Collections.synchronizedSet(new HashSet<Client>());
	
	//overload
	public Group(String name){
		this.groupName = name;
	}
	//specify a group with max member
	public Group(String name, int max){
		this.groupName = name;
		this.maxSize = max;
	}
	//getters and setters
	public String getGroupName(){
		return groupName;
	}
	public int getMaxSize(){
		return maxSize;
	}
	public int getSize(){
		return clientsList.size();
	}
	public Set<Client> getList(){
		return clientsList;
	}
	public void setGroupName(String name){
		this.groupName = name;
	}
	public void setGroupMaxSize(int size){
		this.maxSize = size;
	}
	public boolean isFull(){
		return (getSize()>=maxSize);
	}
	public boolean isEmpty(){
		return (getSize()<maxSize);
	}
	//add, remove
	public boolean add(Client client){
		if (isFull()){
			return false;
		}
		return clientsList.add(client);
	}
	public boolean remove(Client client){
		if (!isEmpty()){
			clientsList.remove(client);
		}
		return false;
	}
	public boolean hasClient(Client client){
		return clientsList.contains(client);
	}
	
	
}
