package socialMedia;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface SocialSystemRemoteMethods extends Remote {
	public boolean addUser(User u) throws RemoteException;
}
