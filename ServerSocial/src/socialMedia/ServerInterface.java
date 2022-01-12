package socialMedia;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.LinkedList;

public interface ServerInterface extends Remote {
	public boolean registerUser(String username, String password, LinkedList<String> tags) throws RemoteException;
	
	public void registerForCallBacks(NotifyClientInterface clientInterface) throws RemoteException;
	
	public void unregisterForCallBaks(NotifyClientInterface clientInterface) throws RemoteException;
}
