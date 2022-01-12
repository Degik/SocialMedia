package socialMedia;

import java.util.*;
import java.rmi.*;

public class SocialSystem {
	private Set<User> users = null;
	
	public SocialSystem() {
		users = new HashSet<>();
	}
	
	public boolean registerUser(String username, String password, LinkedList<String> tags) throws RemoteException {
		if(username == null || password == null || tags == null) {
			throw new NullPointerException();
		}
		User u = new User(username, password, tags);
		if(users.add(u)) {
			System.out.println("Registrazione utente " + username + " avvuta con successo" + u);
			return true;
		}
		return false;
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
				return null;
			}
		}else {
			System.out.println("Un guest ha provato ad effettuare il login (utente non registrato)");
			return null;
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
