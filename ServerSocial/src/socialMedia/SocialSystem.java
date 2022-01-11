package socialMedia;

import java.util.*;
import java.rmi.*;

public class SocialSystem implements SocialSystemRemoteMethods {
	private Set<User> users = null;
	
	public SocialSystem() {
		users = new HashSet<>();
	}
	
	public boolean addUser(String username, String password) throws RemoteException {
		if(username == null || password == null) {
			throw new NullPointerException();
		}
		User u = new User(password, password);
		return users.add(u);
	}//Metodo RMI che permette attraverso una richiesta del client di registrare un utente
	
	public User login(String username, String password) {
		if(username == null || password == null) {
			throw new NullPointerException();
		}
		if(isRegister(username)) {
			User user = getUser(username);
			if(user != null) { // Se getUser restituisce qualcosa
				if(password.equals(user.getPassword())) { // Se la password e' corretta
					System.out.println("Un utente ha effettuato il login: " + user);
					return user;
				}else { // Se la password non e' corretta
					System.out.println("Un utente ha provato ad effettuare il login (password errata): " + user);
					return null;
				}
			}else {
				System.out.println("Un guest ha provato ad effettuare il login (utente non trovato)");
			}
		}else {
			System.out.println("Un guest ha provato ad effettuare il login (utente non registrato)");
		}
	}//Metodo RMI che permette attraverso una richiesta del client di eseguire il login
	
	public boolean isRegister(String username) {
		for(User u : users) {
			if(u.getUsername().equals(username)) {
				return true;
			}
		}
		return false;
	}
	
	public User getUser(String username) {
		for(User u : users) {
			if(u.getUsername().equals(username)) {
				return u;
			}
		}
		return null;
	}
}
