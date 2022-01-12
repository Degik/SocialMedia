package socialMedia;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Set;

public interface NotifyClientInterface extends Remote {
	void notifyUsers(Set<String> users) throws RemoteException;
	
	void notifyPost(Set<String> posts) throws RemoteException;
}
