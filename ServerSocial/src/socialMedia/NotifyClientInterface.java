package socialMedia;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

public interface NotifyClientInterface extends Remote {
	void notifyUsers(ArrayList<String> users) throws RemoteException;
	
	void notifyPost(ArrayList<String> posts) throws RemoteException;
}
