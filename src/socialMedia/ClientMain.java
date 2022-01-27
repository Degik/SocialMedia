package socialMedia;

import java.util.*;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.*;
import java.rmi.server.RemoteObject;
import java.rmi.server.UnicastRemoteObject;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import org.apache.commons.lang3.StringUtils;

@SuppressWarnings("unused")
public class ClientMain extends RemoteObject implements NotifyClientInterface{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static SocketChannel socketChannel;
	private ArrayList<String> followers; // followersCallBack lista degli utenti che mi seguono
	private ArrayList<Post> posts;	 	 // postsCallBack lista dei post pubblicati dall'utente 
	private ArrayList<User> usersList;	 // usersListCallBack lista degki utenti del feed
	private ArrayList<String> following; // followingCallBack lista degli utenti che seguo
	private ArrayList<Post> userFeedList; // userFeedList lista dei post degli utenti che segui
	private ArrayList<Double> history;
	private String username;
	private String clientId;
	
	public ClientMain() {
		super();
		followers = null;
		posts = null;
		usersList = null;
		following = null;
		clientId = null;
		history = null;
	}
	
	public void start() {
		String serverHostStr, registryPortStr, serverName, mulPortStr, mulIpStr, tcpPortStr;
		Properties prop = new Properties();
		
		boolean state = true; // Serve per uscire dal while del menu client
		
		try {
			
			FileInputStream input = new FileInputStream("ClientConfig.properties");
			prop.load(input);
			
			serverHostStr = prop.getProperty("SERVER");
			tcpPortStr = prop.getProperty("TCPPORT");
			mulIpStr = prop.getProperty("MULTICAST");
			mulPortStr = prop.getProperty("MCASTPORT");
			registryPortStr = prop.getProperty("REGISTRYPORT");
			serverName = prop.getProperty("SERVERNAME");
			
			int registryPortInt = Integer.parseInt(registryPortStr);
			int tcpPortInt = Integer.parseInt(tcpPortStr);
			int mulPortInt = Integer.parseInt(mulPortStr);
			
			
			// Creazione delle callBack
			ServerInterface serverStub;
			NotifyClientInterface callObj; //
			NotifyClientInterface callStub;//
			Registry registry = LocateRegistry.getRegistry(registryPortInt);
			serverStub = (ServerInterface) registry.lookup(serverName);
			callObj = this;
			callStub = (NotifyClientInterface) UnicastRemoteObject.exportObject((Remote) callObj, 0);
			
			System.out.println("Client connesso");
			
			try {
				socketChannel = SocketChannel.open();
				socketChannel.connect(new InetSocketAddress(serverHostStr, tcpPortInt));
			} catch(ClosedChannelException e1) {
				System.err.println("Socket error!");
				e1.printStackTrace();
				return;
			}
			
			//Creo il thread CalculateClient
			Thread t = null;
			
			System.out.println("Benvenuto su Social\n\n");
			@SuppressWarnings("resource") // sc is not closed
			Scanner sc = new Scanner(System.in);
			help();
			System.out.println();
			
			while(state) {
				String request;
				System.out.print("> ");
				String inputCommand = sc.nextLine() + " " + username;
				String[] command = inputCommand.split("\\s+");
				if(command[0].isEmpty()) {
					System.out.println("< Devi inserire un comando");
					continue;
				}
				
				switch(command[0]) {
				case "register":
					if(username != null) {
						System.out.println("< Per registrarti esegui il logout");
						break;
					}
					if(command.length < 4) {
						System.out.println("< Devi usare register <username> <password> <tags>");
						break;
					}
					if(command[1].equals(command[2])) {
						System.out.println("< username e password devono essere diversi");
						break;
					}
					// Inserimento dei tag
					LinkedList<String> tags = new LinkedList<>();
					int count = command.length - 4; // Ricavo il numero di tag inseriti
					if(count == 0) {
						System.out.println("< Devi inserire almenu un tag");
						break;
					}else if(count > 5) {
						System.out.println("< Devi inserire un numero massimo di 5 tag");
						break;
					}else {
						// Inserisco tag nella lista tags
						int j = 3;
						for(int i = 0; i < count; i++) {
							tags.add(command[j].toLowerCase());
							j++;
						}
						if(!serverStub.registerUser(command[1], command[2], tags)) {
							System.out.println("< Username gia' in uso");
							break;
						} else {
							System.out.println("< Registrazione avvenuta con successo");
							break;
						}
					}
				case "login":
					if(username != null) {
						System.out.println("< Per entrare con un altro utente esegui il logout");
						break;
					}
					if(command.length < 3) {
						System.out.println("< Devi usare login <username> <password>");
						break;
					}
					serverStub.registerForCallBacks(callStub);
					clientId = serverStub.updateClientIdList(command[1]);
					if(clientId.equals("nessuno")) {
						System.out.println("< Username non valido");
						clientId = null;
						break;
					}
					request = requestCommand(inputCommand);
					if(!request.contains("errat")) {
						System.out.println("< " + request);
						username = command[1];
					} else {
						serverStub.unregisterForCallBacks(callStub);
						System.out.println("< " + request);
						clientId = null;
					}
					// Avvio il thread CalculateClient
					t = new Thread(new CalculateClient(mulPortInt, mulIpStr));
					t.start();
					//
					break;
				case "list":
					if(username == null) {
						System.out.println("< Per usare questo comando devi aver effettuato l'accesso");
						break;
					}
					if(command.length < 2) {
						System.out.println("< Devi usare list user | followers | following");
						break;
					}
					switch(command[1]) {
					case "user":
						request = requestCommand(inputCommand);
						if(usersList.isEmpty()) { // Risolvere il problema del se stesso
							System.out.println("< Non ci sono utenti con tag in comune");
						} else {
							System.out.println("< " + request);
							System.out.println("<   Utente     |    Tag");
							System.out.println("< ------------------------------------------");
							for(User user : usersList) {
								System.out.println(user);
							}
						}
						break;
					case "followers":
						request = requestCommand(inputCommand);
						if(followers.isEmpty()) {
							System.out.println("< Non ti segue nessuno");
						} else {
							System.out.println("< " + request);
							System.out.println("<      Utente");
							System.out.println("< ----------------");
							for(String user : followers) {
								System.out.println("<   " + user);
							}
						}
						break;
					case "following":
						request = requestCommand(inputCommand);
						if(following.isEmpty()) {
							System.out.println("< Non segui nessuno");
						} else {
							System.out.println("< " + request);
							System.out.println("<      Utente");
							System.out.println("< ----------------");
							for(String user : following) {
								System.out.println("<   " + user);
							}
						}
						break;
					default:
						System.out.println("< Devi usare list user | followers | following");
						break;
					}
					break;
				case "follow":
					if(username == null) {
						System.out.println("< Per userare questo comando devi aver effettuato l'accesso");
						break;
					}
					if(command.length < 2) {
						System.out.println("< Devi usare follow <username>");
						break;
					}
					if(command[1].equals(username)) {
						System.out.println("< Non puoi seguire te stesso");
						break;
					}
					request = requestCommand(inputCommand);
					System.out.println("< " + request);
					break;
				case "unfollow":
					if(username == null) {
						System.out.println("< Per userare questo comando devi aver effettuato l'accesso");
						break;
					}
					if(command.length < 2) {
						System.out.println("< Devi usare unfollow <username>");
						break;
					}
					if(command[1].equals(username)) {
						System.out.println("< Non puoi smettere di seguire te stesso");
					}
					request = requestCommand(inputCommand);
					System.out.println("< " + request);
					break;
				case "logout":
					if(username == null) {
						System.out.println("< Per effettuare il logout devi prima accedere");
						break;
					}
					if(command.length < 1) {
						System.out.println("< Devi scrivere logout");
						break;
					}
					serverStub.resetClientId(username);
					serverStub.unregisterForCallBacks(callStub);
					request = requestCommand(inputCommand);
					username = null;
					System.out.println("< " + request);
					// Interrompo il thread
					t.interrupt();
					System.out.println("< Logout effettuato");
					break;
				case "blog":
					if(username == null) {
						System.out.println("< Per usare blog devi prima accedere");
						break;
					}
					if(command.length < 1) {
						System.out.println("< Devi scrivere blog");
						break;
					}
					request = requestCommand(inputCommand);
					if(posts.isEmpty()) {
						System.out.println("< Non hai pubblicato niente");
					} else {
						System.out.println("< " + request);
						System.out.println("< Id	  |   Autore  |   Titolo ");
						System.out.println("< -----------------------------------------------------------------------");
						for(Post p: posts) {
							System.out.println("< " + p.getPostId() + "	  | " + p.getAuthor() + " | " + p.getTitle());
						}
					}
					break;
				case "post":
					if(username == null) {
						System.out.println("< Per effettuare il logout devi prima accedere");
						break;
					}
					if(command.length < 3) {
						System.out.println("< Devi scrivere blog <title> <content>");
						break;
					}
					request = requestCommand(inputCommand);
					System.out.println("< " + request);
					break;
				case "show":
					if(username == null) {
						System.out.println("< Per effettuare il logout devi prima accedere");
						break;
					}
					if(command.length < 2) {
						System.out.println("< Devi scrivere show feed | post");
						break;
					}
					switch(command[1]) {
					case "feed":
						request = requestCommand(inputCommand);
						if(userFeedList.isEmpty()) {
							System.out.println("< Il feed e' vuoto");
						} else {
							System.out.println("< " + request);
							System.out.println("< Id	  |   Autore  |   Titolo ");
							System.out.println("< -----------------------------------------------------------------------");
							for(Post p: userFeedList) {
								System.out.println("< " + p.getPostId() + "	  | " + p.getAuthor() + " | " + p.getTitle());
							}
						}
						break;
					case "post":
						if(command.length < 4) {
							System.out.println("< Devi scrivere show post <idPost>");
							break;
						}
						int idPost = Integer.parseInt(command[2]);
						Post p = null;
						p = serverStub.showPost(idPost);
						if(p == null) {
							System.out.println("< Post non trovato");
						} else {
							System.out.println(p);
						}
						break;
					default:
						System.out.println("< Devi scrivere show feed | post");
						break;
					}
					break;
				case "delete":
					if(username == null) {
						System.out.println("< Per cancellare devi prima accedere");
						break;
					}
					if(command.length < 3) {
						System.out.println("< Devi scrivere delete <idPost>");
						break;
					}
					request = requestCommand(inputCommand);
					System.out.println("< " + request);
					break;
				case "rewin":
					if(username == null) {
						System.out.println("< Per effettuare il logout devi prima accedere");
						break;
					}
					if(command.length < 2) {
						System.out.println("< Devi scrivere delete <idPost>");
						break;
					}
					request = requestCommand(inputCommand);
					System.out.println("< " + request);
					break;
				case "rate":
					//
					if(username == null) {
						System.out.println("< Per effettuare il logout devi prima accedere");
						break;
					}
					if(command.length < 3) {
						System.out.println("< Devi scrivere rate <idPost> <vote>");
						break;
					}
					request = requestCommand(inputCommand);
					System.out.println("< " + request);
					break;
				case "comment":
					if(username == null) {
						System.out.println("< Per effettuare il logout devi prima accedere");
						break;
					}
					if(command.length < 3) {
						System.out.println("< Devi scrivere comment <idPost> <comment>");
						break;
					}
					request = requestCommand(inputCommand);
					System.out.println("< " + request);
					break;
				case "wallet":
					//
					if(username == null) {
						System.out.println("< Per effettuare il logout devi prima accedere");
						break;
					}
					if(command[1].equals("btc")) {
						request = requestCommand(inputCommand);
						System.out.println("< " + request);
					} else {
						request = requestCommand(inputCommand);
						System.out.println("< " + request);
						if(history.isEmpty()) {
							System.out.println("< Non ci sono transazioni");
						} else {
							for(Double h : history) {
								System.out.println("< Sono stati aggiunti " + h + " nel tuo portafogli");
							}
						}
					}
					break;
				case "help":
					help();
					break;
				case "quit":
					if(username != null) {
						System.out.println("< Devi prima sloggare ");
						break;
					}
					System.exit(0);
					break;
				default:
					System.out.println("< Comando non valido!");
					break;
				}
				
			}
			
		} catch (RemoteException e) {
			System.err.println("Errore di comunicazione con il server!");
			e.printStackTrace();
		} catch (NotBoundException e) {
			System.err.println("NotBoundException");
			e.printStackTrace();
		} catch(FileNotFoundException e) {
			System.err.println("File non trovato");
		} catch(IOException e) {
			System.err.println("IOException");
			e.printStackTrace();
		}
	}
	
	public static void help() {
		System.out.println("< Ecco la lista dei comandi:");
		System.out.println("     register <username> <password> <tags> [per registarti l'utente]");
		System.out.println("     login <username> <password> [per accedere con il tuo account]");
		System.out.println("     logout [per scollegarti]");
		System.out.println("     list users [questo comando restituisce la lista degli utenti con almeno un tag in comune]");
		System.out.println("     list followers [restituisce la lista dei propri followers]");
		System.out.println("     list following [restituisce la lista degli utenti di cui sei follower]");
		System.out.println("     follow <username> [permette di seguire l'utente <username>]");
		System.out.println("     unfollow <username> [permette di non seguire piu' l'utente <username>]");
		System.out.println("     blog [restituisce la lista dei post di cui l'utente e' autore]");
		System.out.println("     post <title> <content> [crea un post con <title> e <content>]");
		System.out.println("     show feed [Mostra la lista dei post nel proprio feed]");
		System.out.println("     show post <id> [mostra il post con postId <id>]");
		System.out.println("     delete <idPost> [cancella il post]");
		System.out.println("     rewin <idPost> [pubblica il post sul proprio blog]");
		System.out.println("     rate <idPost> <vote> [assegna un voto al post, voto negativo -1, voto positivo +1]");
		System.out.println("     comment <idPost> <comment> [commenta il post]");
		System.out.println("     wallet [restituisce il totale e la storia delle transazioni]");
		System.out.println("     wallet btc [restituisce il valore del proprio portafoglio in bitcoin]");
		System.out.println("     help [mostra la lista dei comandi]");
		System.out.println("     quit [termina il programma]");
	}
	
	// Questo metodo prepare ed invia il buffer contenente l'array String di commandi
	// Ovviamente vengono spediti come String command
	// Invia il comando
	// Aspetta la risposta
	public String requestCommand(String command) throws IOException {
		ByteBuffer buffSend = ByteBuffer.allocate(256); // allocate 256 byte
		buffSend.clear(); 								// set pos 0
		buffSend.put(command.getBytes(StandardCharsets.US_ASCII));
		buffSend.flip();
		// Mando il buffer al server
		socketChannel.write(buffSend);
		// Creo un secondo buffer per ricevere la risposta dal server
		ByteBuffer buffRecv = ByteBuffer.allocate(256);
		socketChannel.read(buffRecv);
		buffRecv.flip();
		return StandardCharsets.US_ASCII.decode(buffRecv).toString();	
	}

	public static void main(String[] args) {
		ClientMain client = new ClientMain();
		client.start();
	}

	@Override
	public void notifyFollowers(ArrayList<String> followersCallBack) throws RemoteException {
		this.followers = followersCallBack;
	}

	@Override
	public void notifyPost(ArrayList<Post> postsCallBack) throws RemoteException {
		this.posts = postsCallBack;
	}

	@Override
	public String getClientId() throws RemoteException {
		return clientId;
	}

	@Override
	public void notifyFollowing(ArrayList<String> followingCallBack) throws RemoteException {
		this.following = followingCallBack;
	}

	@Override
	public void notifyUsersList(ArrayList<User> usersListCallBack) throws RemoteException {
		this.usersList = usersListCallBack;
	}

	@Override
	public void notifyFeedList(ArrayList<Post> usersFeedListCallBack) throws RemoteException {
		this.userFeedList = usersFeedListCallBack;
	}

	@Override
	public void notifyHistory(ArrayList<Double> historyCallBack) throws RemoteException {
		this.history = historyCallBack;
	}
	
}
