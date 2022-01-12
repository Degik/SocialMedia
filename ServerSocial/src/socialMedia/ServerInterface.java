package socialMedia;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.LinkedList;

public interface ServerInterface extends Remote {
	// Registrazione
	public boolean registerUser(String username, String password, LinkedList<String> tags) throws RemoteException;
	// login
	public boolean login(String username, String password) throws RemoteException;
	// logout
	public boolean logout(String username) throws RemoteException;
	//
	public void registerForCallBacks(NotifyClientInterface clientInterface) throws RemoteException;
	
	public void unregisterForCallBaks(NotifyClientInterface clientInterface) throws RemoteException;
}
