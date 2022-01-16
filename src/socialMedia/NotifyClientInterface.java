package socialMedia;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

public interface NotifyClientInterface extends Remote {
	public String getClientId() throws RemoteException;
	
	void notifyFollowers(ArrayList<String> followersCallBack) throws RemoteException;
	
	void notifyPost(ArrayList<Post> postsCallBack) throws RemoteException;
	
	void notifyFollowing(ArrayList<String> followingCallBack) throws RemoteException;
	
	void notifyUsersList(ArrayList<User> usersListCallBack) throws RemoteException;
	
	void notifyFeedList(ArrayList<Post> usersFeedListCallBack) throws RemoteException;
}
