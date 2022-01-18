package socialMedia;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.LinkedList;

public interface ServerInterface extends Remote {
	// Registrazione
	public boolean registerUser(String username, String password, LinkedList<String> tags) throws RemoteException;
	//
	public String updateClientIdList(String username) throws RemoteException;
	//
	public Post showPost(int idPost) throws RemoteException;
	//
	public void resetClientId(String username) throws RemoteException;
	
	void registerForCallBacks(NotifyClientInterface clientInterface) throws RemoteException;
	
	void unregisterForCallBacks(NotifyClientInterface clientInterface) throws RemoteException;
}
