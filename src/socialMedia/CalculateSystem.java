package socialMedia;

import java.util.ArrayList;
import java.util.concurrent.locks.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class CalculateSystem implements Runnable{
	
	private Lock lock = new ReentrantLock();
	
	private ArrayList<User> users = null;
	private final ObjectMapper objectMapper;
	private File usersJson = null;
	private int timeSleep;
	private int mulPort;
	private String mulIp;
	
	public CalculateSystem(ArrayList<User> users, int timeSleep, ObjectMapper objectMapper, File usersJson, int mulPort, String mulIp) {
		this.users = users;
		this.timeSleep = timeSleep;
		this.objectMapper = objectMapper;
		this.usersJson = usersJson;
		this.mulPort = mulPort;
		this.mulIp = mulIp;
	}
	
	@Override
	public void run() {
		while(true) {
			sleep(timeSleep); // Per tot tempo dorme e dopo calcola nuovamente
			double reward;
			lock.lock();
			// Prendo tutti i post
			ArrayList<Post> posts = takeAllPosts();
			// Controlliamo ogni singolo post
			for(Post p : posts) {
				// Controlliamo se sono state effettuati dei cambiamenti nel post dall'ultimo check
				if(p.getNewPeopleCommenting() > 0 || p.getNewPeopleLikes() != 0) {
					// Ricaviamo lo UserAuthor
					User userAuthor = getAuthorUser(p.getAuthor());
					// Controllo se sono stati effettuati dei commenti, risparmia molte risorse altrimenti
					if(p.getNewPeopleCommenting() > 0) {
						// Ricavo la lista nomi delle nuove persone che hanno commentato
						ArrayList<String> usersComment = p.getNewPeopleComment();
						// Ne recavo gli user per ottenre maggiori informazioni
						ArrayList<User> usersListComment = getUsersListComment(usersComment);
						double sum = 0;
						for(User u : usersListComment) {
							sum = sum + calculateRewardOnlyComment(u.getNumberComments());
						}
						reward = calculateRewardOnlyLikes(p.getNewPeopleLikes()) + Math.log(sum + 1);
						// Aggiungo la ricompensa al portafogli dell'autore
						userAuthor.addWallet(reward);
						// Aggiungo la ricompensa allo storico
						userAuthor.addHistory(reward);
						// Resetto tutti gli indicatori
						p.resetNewPeopleComment();
						p.resetNewPeopleCommenting();
						p.resetNewPeopleLikes();
					} else {
						// Calcoliamo il reward in base solo ai like e lo divido per il numero di iterazioni
						reward = (calculateRewardOnlyLikes((p.getNewPeopleLikes())) + Math.log(1))/p.getNumberIter();
						// Aumento il numero di iterazioni
						p.addNumberIter();
						// Aggiungo la ricompensa al portafogli dell'autore
						userAuthor.addWallet(reward);
						// Aggiungo la ricompensa allo storico
						userAuthor.addHistory(reward);
						// Resetto tutti gli indicatori
						p.resetNewPeopleLikes();
					}
				}
			}
			try(DatagramSocket socket = new DatagramSocket()) {
				InetAddress group = InetAddress.getByName(mulIp);
				if(!group.isMulticastAddress()) {
					System.err.println("Server [Non sono presenti gruppi multicast]");
				}
				String message = "Server [Ho appena aggiornato le ricompense]";
				byte[] content = message.getBytes();
				DatagramPacket packet = new DatagramPacket(content, content.length, group, mulPort);
				socket.send(packet);
				System.out.println("Server [Ho appena notificato ai client]");
			} catch(Exception e) {
				System.err.println("Server [Non sono riuscito a notificare]");
			}
			try {
				objectMapper.writeValue(usersJson, users);
			} catch(IOException e) {
				System.err.println("Server [Errore creazione backup]");
			}
			lock.unlock();
			System.out.println("Server [Ho completato il calcolo delle ricompense]");
		}
		
	}
	
	public ArrayList<Post> takeAllPosts(){
		ArrayList<Post> posts = new ArrayList<>();
		for(User u : users) {
			for(Post p : u.postWithoutNew()) {
				posts.add(p);
			}
		}
		return posts;
	}
	
	public User getAuthorUser(String author) {
		for(User u : users) {
			if(u.getUsername().equals(author)) {
				return u;
			}
		}
		return null;
	}
	
	public ArrayList<User> getUsersListComment(ArrayList<String> usersComment) {
		ArrayList<User> usersListComment = new ArrayList<User>();
		for(String nameUser : usersComment) {
			for(User u : users) {
				if(nameUser.equals(u.getUsername())) {
					usersListComment.add(u);
				}
			}
		}
		return usersListComment;
	}
	
	public static void sleep(int timeSleep) {
		for(int i = 0; i < timeSleep; i++) {
			try {
				Thread.sleep(1000);
			} catch(InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	

	public double calculateRewardOnlyComment(int numberComments) {
		double reward = 0;
		
		reward = (2/(1 + (exponential(10,numberComments-1))));
		
		return reward;
	}
	
	public double calculateRewardOnlyLikes(int newPeopleLikes) {
		double reward = 0;
		
		reward = Math.log(Math.max(newPeopleLikes,0)+1);
		
		return reward;
	}
	
	public float exponential(int n , float x) {
		float sum = 1;
		  
        for (int i = n - 1; i > 0; --i )
            sum = 1 + x * sum / i;
  
        return sum;
	}
	
}
