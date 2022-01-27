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
	private int rateRewardAuthor;
	
	public CalculateSystem(ArrayList<User> users, int timeSleep, ObjectMapper objectMapper, File usersJson, int mulPort, String mulIp, int rateRewardAuthor) {
		this.users = users;
		this.timeSleep = timeSleep;
		this.objectMapper = objectMapper;
		this.usersJson = usersJson;
		this.mulPort = mulPort;
		this.mulIp = mulIp;
		this.rateRewardAuthor = rateRewardAuthor;
	}
	
	@Override
	public void run() {
		while(true) {
			sleep(timeSleep); // Per tot tempo dorme e dopo calcola nuovamente
			double reward, rewardAuthor, rewardCurator;
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
						double sum = 0;
						for(String u : usersComment) {
							sum = sum + calculateRewardOnlyComment(getUserNumberComments(u, p));
						}
						reward = calculateRewardOnlyLikes(p.getNewPeopleLikes()) + Math.log(sum + 1)/p.getNumberIter();
					} else {
						// Calcoliamo il reward in base solo ai like e lo divido per il numero di iterazioni
						reward = (calculateRewardOnlyLikes((p.getNewPeopleLikes())) + Math.log(1))/p.getNumberIter();
					}
					// Calcolo la percentuale dell'autore
					rewardAuthor = (rateRewardAuthor * reward) / 100;
					// Aggiungo la ricompensa al portafogli dell'autore
					userAuthor.addWallet(rewardAuthor);
					// Aggiorno lo storico
					userAuthor.addHistory(rewardAuthor);
					// Calcolo rewardCurator
					rewardCurator = ((100 - rateRewardAuthor) * reward) / 100;
					// Ne ricavo gli user per ottenere maggiori informazioni
					ArrayList<User> usersListReward = getUsersListReward(p.getRewardsList());
					// Suddivido le ricompense per ogni utente e le assegno
					rewardCurator = rewardCurator / usersListReward.size();
					for(User u : usersListReward) {
						u.addWallet(rewardCurator);
						u.addHistory(rewardCurator);
					}
					// Aumenti numero iterazioni
					p.addNumberIter();
					// Resetto tutti gli indicatori
					p.resetNewPeopleComment();
					p.resetNewPeopleCommenting();
					p.resetNewPeopleLikes();
					p.resetRewardsList();
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
	
	public ArrayList<User> getUsersListReward(ArrayList<String> usersReward) {
		ArrayList<User> usersListReward = new ArrayList<User>();
		for(String nameUser : usersReward) {
			for(User u : users) {
				if(nameUser.equals(u.getUsername())) {
					usersListReward.add(u);
				}
			}
		}
		return usersListReward;
	}
	
	public int getUserNumberComments(String username, Post p) {
		int count = 0;
		ArrayList<Comment> comments = p.getComments();
		for(Comment c : comments) {
			if(c.getAuthorComment().equals(username)) {
				count++;
			}
		}
		return count;
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
		
		reward = 2/(1 + (Math.exp((numberComments-1)*-1)));
		
		return reward;
	}
	
	public double calculateRewardOnlyLikes(int newPeopleLikes) {
		double reward = 0;
		
		reward = Math.log(Math.max(newPeopleLikes,0)+1);
		
		return reward;
	}
	
}
