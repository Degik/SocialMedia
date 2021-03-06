package socialMedia;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteObject;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.ConnectException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper; // Grazie a questa libreria posso gestire i json in maniera veloce
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.lang3.StringUtils;

public class MainServer extends RemoteObject implements ServerInterface{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -297225371107007564L;
	private ArrayList<NotifyClientInterface> clients = null;
	private ArrayList<User> users = null;
	private HashMap<String, ArrayList<String>> followers = null;	// Esempio: Degik e' seguito da questi utenti
	private HashMap<String, ArrayList<User>> usersListTag = null;   // Esempio: Utenti del feed di Degik
	private Set<Post> posts = null;
	private final File backupDir;
	private final File usersJson;
	private final File postIdJson;
	private final File commentIdJson;
	private final File userIdJson;
	private final ObjectMapper objectMapper;
	private int nextClientId = 0;
	private int nextCommentId;
	private int nextPostId;
	private int nextUserId;
	private double  btcValue;
	
	public static void main(String[] args) {
		MainServer serverTh = new MainServer();
		serverTh.start();
	}

	public MainServer() {
		super();
		clients = new ArrayList<>(); // Lista dei client connessi al server
		users = new ArrayList<>(); 	 // Set contenente gli utenti registrati al sistema
		followers = new HashMap<>();
		usersListTag = new HashMap<>();
		posts = new HashSet<>();
		backupDir = new File("backup");
		usersJson = new File("backup/users.json");
		postIdJson = new File("backup/nextpostid.json");
		commentIdJson = new File("backup/nextcommentid.json");
		userIdJson = new File("backup/usernextid.json");
		objectMapper = new ObjectMapper();
		objectMapper.enable(SerializationFeature.INDENT_OUTPUT);			// https://fasterxml.github.io/jackson-databind/javadoc/2.6/com/fasterxml/jackson/databind/SerializationFeature.html#INDENT_OUTPUT
		objectMapper.enable(SerializationFeature.FLUSH_AFTER_WRITE_VALUE);	// https://fasterxml.github.io/jackson-databind/javadoc/2.6/com/fasterxml/jackson/databind/SerializationFeature.html#FLUSH_AFTER_WRITE_VALUE
		objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
		btcValue = generateRandom();
	}
	
	public void start() {
		// impostazioni server
		@SuppressWarnings("unused")// not used
		String server, tcpPortStr, udpPort, multiCast, multiCastPort, registryHost, registryPortStr, sleepTime, serverName, serverClassName, rateRewardAuthorStr;
		Properties prop = new Properties();
		try {
			FileInputStream input = new FileInputStream("ServerConfig.properties");
			prop.load(input);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("IOException");
			e.printStackTrace();
		}
		
		// Prendo gli argomenti dal file di configurazione
		// .getProperty cerca la parola chiave
		// restituisce la String dell'argomento
		server = prop.getProperty("SERVER");
		tcpPortStr = prop.getProperty("TCPPORT");
		udpPort = prop.getProperty("UDPPORT");
		multiCast = prop.getProperty("MULTICAST");
		multiCastPort = prop.getProperty("MCASTPORT");
		registryHost = prop.getProperty("REGISTRYHOST");
		registryPortStr = prop.getProperty("REGISTRYPORT");
		sleepTime = prop.getProperty("SLEEPTIME");
		serverName = prop.getProperty("SERVERNAME");
		serverClassName = prop.getProperty("CLASSNAME");
		rateRewardAuthorStr = prop.getProperty("RATEREWARDAUTHOR");
				
		int multiCastPortInt = Integer.parseInt(multiCastPort);
		int registryPortInt = Integer.parseInt(registryPortStr);
		int tcpPortInt = Integer.parseInt(tcpPortStr);
		int rateRewardAuthorInt = Integer.parseInt(rateRewardAuthorStr);
		
		if(rateRewardAuthorInt < 0 || rateRewardAuthorInt > 100) {
			throw new IllegalArgumentException("Il valore dev'essere compreso tra 0 e 100");
		}
				
		//Creazione del server
		if(!manageBackup()) {
			System.err.println("Server [Errore di caricamento del backup]");
		}
		System.out.println("Server [Backup pronto]");
		
		//Inizio RMI
		// Creazione dello stub
		try {
			ServerInterface stub;
			stub = (ServerInterface) UnicastRemoteObject.exportObject(this, 0);
			LocateRegistry.createRegistry(registryPortInt);
			Registry registro = LocateRegistry.getRegistry(registryPortInt);
			registro.rebind(serverName, stub);
			//System.out.println("Server pronto");
		} catch (RemoteException e) {
			// Errore in caso di RemoteException
			System.err.println("Errore di comunicazione ");
			e.printStackTrace();
			return;
		}	
				
		ServerSocketChannel serverSocketChannel;
		try {
			serverSocketChannel = ServerSocketChannel.open();
			serverSocketChannel.socket().bind(new InetSocketAddress(tcpPortInt));
			serverSocketChannel.configureBlocking(false);
		} catch(IOException e) {
			System.err.println("IOException (SocketServerChannel)");
			e.printStackTrace();
			return;
		}
				
		Selector selector;
		try {
			selector = Selector.open();
			serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
		}catch(IOException e) {
			System.err.println("IOException (Select)");
			e.printStackTrace();
			return;
		}
		
		resetClientIdStatus();
		try {
			objectMapper.writeValue(usersJson, users);
		} catch(IOException e) {
			System.err.println("Server [Errore creazione backup]");
		}
		
		// Avvio il thread a parte che calcola ogni tot timeout le ricompense
		Thread t = new Thread(new CalculateSystem(users, Integer.parseInt(sleepTime), objectMapper, usersJson, multiCastPortInt, multiCast, rateRewardAuthorInt));
		t.start();
		
		
		System.out.println("Server [online]");
		
		while(true) {
			try {
				selector.select();
			} catch(IOException e) {
				System.out.println("Selector IOException");
				e.printStackTrace();
				break;
			}
			
			Set<SelectionKey> readKeys = selector.selectedKeys();
			Iterator<SelectionKey> iterator = readKeys.iterator();
			
			while(iterator.hasNext()) {
				SelectionKey key = iterator.next();
				iterator.remove();
				try {
					if(key.isAcceptable()) {
						ServerSocketChannel serverSocket = (ServerSocketChannel) key.channel();
						SocketChannel clientSocket = serverSocket.accept();
						clientSocket.configureBlocking(false);
						clientSocket.register(selector, SelectionKey.OP_READ);
					} else if (key.isReadable()) {
						SocketChannel clientSocket = (SocketChannel) key.channel();
						ByteBuffer buffRecv = ByteBuffer.allocate(256);
						clientSocket.read(buffRecv);
						buffRecv.flip();
						String request = StandardCharsets.US_ASCII.decode(buffRecv).toString(); // Trasformo in stringa i byte ricevuti
						String[] command = request.split("\\s+");
						if(command[0].equals("quit")) {
							key.channel().close();
							key.cancel();
							continue;
						}
						String result = startCommand(command);
						ByteBuffer buffSend = ByteBuffer.allocate(256);
						buffSend.put(result.getBytes());
						buffSend.flip();
						key.interestOps(SelectionKey.OP_WRITE);
						key.attach(buffSend);
					} else if (key.isWritable()) {
						SocketChannel clientSocket = (SocketChannel) key.channel();
						ByteBuffer buffSend = (ByteBuffer) key.attachment();
						clientSocket.write(buffSend);
						key.interestOps(SelectionKey.OP_READ);
						key.attach(null);
					}
				} catch(IOException e) {
					key.cancel();
					try {
						key.channel().close();
					} catch(IOException e1) {
						System.err.println("Server [Connessione interrotta con un client]");
					}
				}
			}
			
		}
	}
	
	// RMI
	@Override
	public synchronized boolean registerUser(String username, String password, LinkedList<String> tags) throws RemoteException {
		// TODO Auto-generated method stub
		for(User u : users) {
			if(u.getUsername().equals(username)) {
				System.out.println("Server [registrazione fallita nickname gia' in uso]");
				return false;
			}
		}
		User user = new User(username, password, tags, nextUserId);
		nextUserId++;
		if(!users.add(user)) {
			System.out.println("Server [registrazione fallita]");
			return false;
		}
		try {
			objectMapper.writeValue(usersJson, users);
			objectMapper.writeValue(userIdJson, nextUserId);
		} catch(IOException e) {
			System.err.println("Server [Errore creazione backup di " + user.getUsername() + "]");
		}
		updateUsersListTag(username);
		System.out.println("Server [Registazione avvenuta con successo " + user.getUsername() + " id (" + user.getUserId() + ")]" );
		return true;
	}

	@Override
	public synchronized void registerForCallBacks(NotifyClientInterface clientInterface) throws RemoteException {
		if(!clients.contains(clientInterface)) {		
			clients.add(clientInterface);
			System.out.println("Server [Nuovo client registrato]");
		}
	}

	@Override
	public synchronized void unregisterForCallBacks(NotifyClientInterface clientInterface) throws RemoteException {
		if(clients.contains(clientInterface)) {
			clients.remove(clientInterface);
			System.out.println("Server [Client rimosso]");
		}else {
			System.out.println("Server [Client non trovato]");
		}
	}
	
	public synchronized Post showPost(int idPost) throws RemoteException {
		if(!checkPost(idPost)) {
			return null;
		}
		for(Post p : posts) {
			if(p.getPostId() == idPost) {
				return p;
			}
		}
		return null;
	}
	
	private boolean manageBackup() {
		// Verifico se esite la cartella backup
		if(!backupDir.exists()) {
			// Se non esiste la creo
			System.out.println("Server [Nessun backup trovato]");
			if(!backupDir.mkdir()) {
				System.err.println("Server [Errore nella creazione della cartella di backup]");
				return false;
			}
			System.out.println("Server [Cartella backup creata]");
			try {
				// Creo al suo interno tutti i file Json
				usersJson.createNewFile();
				postIdJson.createNewFile();
				commentIdJson.createNewFile();
				userIdJson.createNewFile();
				System.out.println("Server [Nuovo file di backup creato]");
			} catch(IOException e) {
				System.err.println("Server [Errore creazione del file di backup]");
				return false;
			}
			users = new ArrayList<>();
			nextPostId = 0;
			nextCommentId = 0;
			nextUserId = 0;
		} else {
			// Se la lunghezza di ogni file e' diversa da 0 allora vuol dire che c'e' contenuto
			// Prendo il contenuto e ripristino lo stato della della struttura users
			if(usersJson.length() != 0) {
				try {
					//users = objectMapper.readValue(usersJson, objectMapper.getTypeFactory().constructCollectionType(ArrayList.class, User.class));
					users = new ArrayList<>(Arrays.asList(objectMapper.readValue(usersJson, User[].class)));
				} catch (JsonParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (JsonMappingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			// Altrimenti il file e' vuoto, quindi inizializzo la struttura
			} else {
				users = new ArrayList<>();
			}
			if(postIdJson.length() != 0) {
				try {
					nextPostId = objectMapper.readValue(postIdJson, Integer.class);
				} catch (JsonParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (JsonMappingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				nextPostId = 0;
			}
			if(commentIdJson.length() != 0) {
				try {
					nextCommentId = objectMapper.readValue(commentIdJson, Integer.class);
				} catch (JsonParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (JsonMappingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				nextCommentId = 0;
			}
			if(userIdJson.length() != 0) {
				try {
					nextUserId = objectMapper.readValue(userIdJson, Integer.class);
				} catch (JsonParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (JsonMappingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				nextUserId = 0;
			}
		}
		return true;
	}
	
	
	//StartCommand raccoglie una array String di comandi e si comporta di conseguenza aggiornando le informazioni dei vari client
	// Effettua aggiornamenti dei backup e ritorna una risposta al client
	// Il client potrebbe anche sfruttare come risposta le varizioni che il metodo updateCallBack (chiamato piu' volte in startCommand) crea nei propri callBack
	public String startCommand(String[] command) throws RemoteException {
		User user1 = null;
		User user2 = null;
		switch(command[0]) {
		// case login
		// Verifica che l'utente esista davvero altrimenti ritorna un errore
		case "login":
			String username = command[1];
			String password = command[2];
			for(User user : users) {
				if(user.getUsername().equals(username)) {
					if(user.getPassword().equals(password)) {
						System.out.println("Server [Un utente ha effettuato l'accesso " + user.getUsername() + "]");
						updateCallBack(username);
						updateUsersListTag(username);
						try {
							objectMapper.writeValue(usersJson, users);
						} catch(IOException e) {
							System.err.println("Server [Errore creazione backup di " + user.getUsername() + "]");
						}
						System.out.println("Server [Backup effettuato]");
						return "Accesso effettuato come " + username;
					} else {
						System.out.println("Server [Un utente ha inserito la password sbagliata]");
						return "Password errata";
					}
				}
			}
			System.out.println("Server [Un utente ha inserito il nickname errato]");
			return "Username errato";
			// Command list 
			// A prescindere dal case list aggiorna le callBack
			// Questo per essere sempre sicuro che anche i client siano al passo con le modifiche del social
		case "list":
			switch(command[1]) {
			case "user":
				System.out.println("Server [Un utente ha richiesto la lista utenti " + command[2] + "]");
				updateCallBack(command[2]);
				//
				try {
					objectMapper.writeValue(usersJson, users);
				} catch(IOException e) {
					System.err.println("Server [Errore creazione backup]");
				}
				System.out.println("Server [Backup effettuato]");
				return "Ecco la lista degli utenti";
			case "followers":
				System.out.println("Server [Un utente ha richiesto la lista followers" + command[2] + "]");
				updateCallBack(command[2]);
				//
				try {
					objectMapper.writeValue(usersJson, users);
				} catch(IOException e) {
					System.err.println("Server [Errore creazione backup]");
				}
				System.out.println("Server [Backup effettuato]");
				return "Ecco la lista dei followers";
			case "following":
				System.out.println("Server [Un utente ha richiesto la lista following" + command[2] + "]");
				updateCallBack(command[2]);
				//
				try {
					objectMapper.writeValue(usersJson, users);
				} catch(IOException e) {
					System.err.println("Server [Errore creazione backup]");
				}
				System.out.println("Server [Backup effettuato]");
				return "Ecco la lista following";
			}
			// case follow
			// Verifica che l'utente che vogliamo seguire esista, altrimenti ritorna errore
			// Ovviamente se dovessimo gia' seguire quell'utente ritornerebbe sempre un messaggio di errore
		case "follow":
			if(!isRegister(command[1])) {
				System.out.println("Server [L'utente " + command[2] + " ha provato a seguire " + command[1] + ", ma l'utente non esiste]");
				return "Utente non trovato";
			}
			user1 = getUser(command[2]); // Utente che inizia a seguire
			user2 = getUser(command[1]); // Utente che viene seguito
			if(user2.getFollowers().contains(command[2])){ // Se l'utente e' gia' seguito
				System.out.println("Server [L'utente " + command[2] + " ha provato a seguire " + command[1] + ", ma gia' lo segue]");
				return "Segui gia' questo utente";
			}
			user2.addFollowers(user1.getUsername());
			user1.addFollowing(user2.getUsername());
			updateCallBack(command[2]);
			try {
				objectMapper.writeValue(usersJson, users);
			} catch(IOException e) {
				System.err.println("Server [Errore creazione backup]");
			}
			System.out.println("Server [Backup effettuato]");
			System.out.println("Server [L'utente " + command[2] + " ha iniziato a seguire " + command[1] + "]");
			return "Ora segui " + command[1];
			// case unfollow
			// Verifica che l'utente che vogliamo smettere di seguire esista, altrimenti ritorna errore
			// Controlla se l'utente che non vogliamo seguire avesse noi come follower
		case "unfollow":
			if(!isRegister(command[1])) {
				System.out.println("Server [L'utente " + command[2] + " voleva smettere di seguire " + command[1] + ", ma l'utente non esiste]");
				return "Utente non trovato";
			}
			user1 = getUser(command[2]); // Utente che smette di seguire
			user2 = getUser(command[1]); // Utente che non viene piu' seguito
			if(!user2.getFollowers().contains(command[2])){ // Se l'utente e' gia' seguito
				System.out.println("Server [L'utente " + command[2] + " ha provato a smetter di seguire " + command[1] + ", ma gia' non lo segue]");
				return "Non segui questo utente";
			}
			user2.removeFollowers(user1.getUsername()); // Utente2 non viene piu' seguito da utente1
			user1.removeFollowing(user2.getUsername()); // Utente1 smette di seguire utente2
			updateCallBack(command[2]);
			try {
				objectMapper.writeValue(usersJson, users);
			} catch(IOException e) {
				System.err.println("Server [Errore creazione backup]");
			}
			System.out.println("Server [Backup effettuato]");
			System.out.println("Server [L'utente " + command[2] + " ha smesso di seguire " + command[1] + "]");
			return "Ora segui non segui piu' " + command[1];
		case "blog":
			updateCallBack(command[1]);
			return "Ecco la lista dei tuoi post";
			// case post
			// Mi permette di creare un post con un titolo ed il testo
		case "post":
			// Non uso command[3] poiche' corrisponde a " "
			String union = StringUtils.join(command, " ");
			String[] post = union.split("\"");
			user1 = getUserPost(post[4]); // Ricavo l'utente
			Post p = new Post(post[1], post[3], user1.getUsername(), nextPostId); // Creo un nuovo post
			nextPostId++;
			user1.addPost(p); // Aggiungo il post nella lista posts dell'utente
			updateCallBack(user1.getUsername());
			try {
				objectMapper.writeValue(usersJson, users);
				objectMapper.writeValue(postIdJson, nextPostId);
			} catch(IOException e) {
				System.err.println("Server [Errore creazione backup]");
			}
			return "Nuovo post creato (" + p.getPostId() + ")"; 
		case "logout":
			try {
				objectMapper.writeValue(usersJson, users);
			} catch(IOException e) {
				System.err.println("Server [Errore creazione backup]");
			}
			System.out.println("Server [Un utente ha effettuato il logout " + command[1] + "]");
			return "Salvataggio delle informazioni completato";
		case "show":
			switch(command[1]) {
			case "feed":
				updateCallBack(command[2]);
				try {
					objectMapper.writeValue(usersJson, users);
				} catch(IOException e) {
					System.err.println("Server [Errore creazione backup]");
				}
				System.out.println("Server [Un utente ha effettuato il logout " + command[1] + "]");
				return "Ecco il tuo feed";
				//
			}
		case "delete":
			int idPost = Integer.parseInt(command[1]);
			user1 = getUser(command[2]);
			ArrayList<Post> postList = user1.PostsWhitoutNew();
			for(Post post1 : postList) {
				if(post1.getPostId() == idPost) {
					postList.remove(post1);
					updateCallBack(command[2]);
					try {
						objectMapper.writeValue(usersJson, users);
					} catch(IOException e) {
						System.err.println("Server [Errore creazione backup]");
					}
					System.out.println("Server [Un utente ha cancellato un post " + user1.getUsername() + " (" + idPost + ")]");
					return "Post cancellato (" + idPost + ")";
				}
			}
			return "Non ho trovato nessun post con questo idPost";
			// case rewin
			// Questo crea una copia del post di un certo utente da mettere sul mio blog
			// In questo caso io divento il proprietario della copia di tale post
		case "rewin":
			int idPost1 = Integer.parseInt(command[1]);
			user1 = getUser(command[2]);
			ArrayList<Post> postList1 = user1.PostsWhitoutNew();
			if(!checkPost(idPost1)) {
				return "Non esiste un post con questo id";
			}
			for(Post post1 : postList1) {
				if(post1.getPostId() == idPost1) {
					return "Questo post e' gia' nel tuo blog";
				}
			}
			for(Post p1 : posts) {
				if(p1.getPostId() == idPost1) {
					Post newPost = new Post(p1.getTitle(), p1.getText(), user1.getUsername(), nextPostId);
					nextPostId++;
					postList1.add(newPost);
					updateCallBack(user1.getUsername());
					try {
						objectMapper.writeValue(usersJson, users);
						objectMapper.writeValue(postIdJson, nextPostId);
					} catch(IOException e) {
						System.err.println("Server [Errore creazione backup]");
					}
				}
			}
			return "Post aggiunto al tuo blog";
			// case rate
			// In base ai casi aumento o dominuisci il voto del contatore newPeopleLikes
		case "rate":
			int idPost2 = Integer.parseInt(command[1]);
			if(!checkPost(idPost2)) {
				return "Non esiste un post con questo id";
			}
			switch(command[2]) {
			case "+1":
				for(Post postQuote : posts) {
					if(postQuote.getPostId() == idPost2) {
						if(postQuote.getAuthor().equals(command[3])) {
							return "Non puoi aggiungere un buon voto al tuo post";
						}
						if(postQuote.checkGoodQuotes(command[3])) {
							return "Hai gia' dato un voto positivo a questo post";
						}
						if(postQuote.checkBadQuotes(command[3])) {
							postQuote.removeBadQuotes(command[3]);
							postQuote.addNewPeopleLikes(); // Aggiungo +1 ovvero quello rimosso in precedenza
						}
						postQuote.addRewardsList(command[3]); // Aggiungo l'utente alla lista ricompense del post
						postQuote.addGoodQuotes(command[3]);
						postQuote.addNewPeopleLikes();
						System.out.println("Server [L'utente " + command[3] + " ha aggiunto un buon voto al post (" + postQuote.getPostId() +  ")");
						try {
							objectMapper.writeValue(usersJson, users);
						} catch(IOException e) {
							System.err.println("Server [Errore creazione backup]");
						}
						return "Voto positivo aggiunto al post (" + postQuote.getPostId() + ")";
					}
				}
			case "-1":
				for(Post postQuote : posts) {
					if(postQuote.getPostId() == idPost2) {
						if(postQuote.getAuthor().equals(command[3])) {
							return "Non puoi aggiungere un cattivo voto al tuo post";
						}
						if(postQuote.checkBadQuotes(command[3])) {
							return "Hai gia' dato un voto negativo a questo post";
						}
						if(!postQuote.checkComment(command[3])) { // Controllo se l'utente ha commentato il post di recente
							postQuote.removeRewardsList(command[3]); // In questo caso mi assicuro di rimuoverlo dalla rewardsList
						}
						if(postQuote.checkGoodQuotes(command[3])) {
							postQuote.removeGoodQuotes(command[3]);
							postQuote.removeNewPeopleLikes(); // -1 nei people likes
						}
						postQuote.addBadQuotes(command[3]);
						postQuote.addNewPeopleLikes();
						System.out.println("Server [L'utente " + command[3] + " ha aggiunto un cattivo voto al post (" + postQuote.getPostId() +  ")");
						try {
							objectMapper.writeValue(usersJson, users);
						} catch(IOException e) {
							System.err.println("Server [Errore creazione backup]");
						}
						return "Voto negativo aggiunto al post (" + postQuote.getPostId() + ")";
					}
				}
			default:
				return "Voto non valido";
			}
		case "comment":
			int idPost3 = Integer.parseInt(command[1]);
			String unionText = StringUtils.join(command, " ");
			String[] comment = unionText.split("\"");
			if(!checkPost(idPost3)) {
				return "Non esiste un post con questo id";
			}
			for(Post postComment : posts) {
				if(postComment.getPostId() == idPost3) {
					String name = comment[2].replaceAll("\\s+", "");
					Comment c = new Comment(name, comment[1], nextCommentId);
					nextCommentId++;
					// Verifico se l'utente e' gia' presente nella lista newPeopleComment
					if(!postComment.checkComment(name)) {
						// Se e' la prima volta che commenta il post dall'ultimo calcolo delle ricompense si conta
						postComment.addNewPeopleComment(name);
					}
					postComment.addRewardsList(name); // Aggiungo l'utente alla reward, non rischio duplicati
					postComment.addNewPeopleCommenting(); // Aggiorno comunque il contatore del numero totale di nuovi commenti
					postComment.addComment(c);
					// Ricavo l'utente dal nome
					User u = getUser(name);
					// Aggiungo +1 al counter del numero di commenti
					u.addNumberComments();
					try {
						objectMapper.writeValue(usersJson, users);
						objectMapper.writeValue(commentIdJson, nextCommentId);
					} catch(IOException e) {
						System.err.println("Server [Errore creazione backup]");
					}
					System.out.println("Server [Un utente " + name + " ha commentato un post (" + idPost3 + ")]");
					return "Commento aggiunto (" + postComment.getPostId() + ")";
				}
			}
		case "wallet":
			//
			if(command[1].equals("btc")) {
				return "Questo e' il tuo saldo convertito: " + getUser(command[2]).getWallet()*btcValue;
			} else {
				updateCallBack(command[1]);
				System.out.println("Server [Un utente " + command[1] + " ha chiesto la lista delle transazioni]");
				return " Ecco il tuo saldo : " + getUser(command[1]).getWallet();
			}
		}
		return "";
	}
	
	public boolean isRegister(String username) {
		for(User user : users) {
			if(user.getUsername().equals(username))
				return true;
		}
		return false;
	}
	
	public void resetClientIdStatus(){
		for(User user : users) {
			if(user.getClientId() != null) {
				user.setClientId(null);
			}
		}
	}
	
	public void updateCallBack(String username) throws RemoteException {
		for(String k : followers.keySet()) { // Aggiorno la lista globale dei followers per ogni utente
			User user = getUser(k);
			ArrayList<String> followersList = user.getFollowers();
			followers.put(k, followersList);
		}
		NotifyClientInterface logout = null;
		for(NotifyClientInterface client : clients) {
			try {
				for(User user : users) {
					if(client.getClientId().equals(user.getClientId())) {
						client.notifyFollowers(user.getFollowers()); // Lista delle persone che seguono user
						client.notifyUsersList(usersListTag.get(user.getUsername())); // Lista degli utenti con tag in comune
						client.notifyFollowing(user.getFollowing());
						client.notifyPost(user.getPosts()); // Restituisce la lista dei post dell'utente
						client.notifyFeedList(updateFeedList(user.getUsername())); // Restituisce la lista feed dell'utente
						client.notifyHistory(user.getHistory()); // Restituisce la lista transazioni
					}
				}
			} catch(ConnectException e) {
				logout = client;
			}
		}
		for(User user : users) {
			ArrayList<Post> userPosts = user.getPosts();
			for(Post p : userPosts) {
				posts.add(p);
			}
		}
		if(logout != null) {
			unregisterForCallBacks(logout);
		}
		
		System.out.println("Server [CallBack " + username + " aggiornati]");
	}
	
	public void updateUsersListTag(String username) {
		usersListTag.put(username, checkTags(username));
	}
	
	// checkTags restituisce la lista degli utenti con i tag in comune
	public ArrayList<User> checkTags(String username){
		User user = getUser(username);
		ArrayList<String> userTag = new ArrayList<>(user.getTags());
		ArrayList<User> result = new ArrayList<>();
		for(User u : users) {
			ArrayList<String> uTag = new ArrayList<>(u.getTags());
			for(String t1 : userTag) {
				if(!t1.equals("null")) {
					for(String t2 : uTag) {
						if(t1.equals(t2)) {
							if(!result.contains(u)) {
								result.add(u);
							}
							break;
						}
					}
				}
			}
		}
		if(result.contains(user)) {
			result.remove(user);
		}
		return result;
	}
	
	// Questo metodo e' molto importante perche' assegna un clientId a tutti quei client che effettuano il login
	// Il clientId permette di sapere cosa inviare e a chi inviare determinate informazioni
	@Override
	public String updateClientIdList(String username) {
		User user = getUser(username);
		if(user == null) {
			return "nessuno";
		}
		String userClientId = user.getClientId();
		String clientId;
		if(userClientId == null) {
			clientId = Integer.toString(nextClientId);
			nextClientId++;
			user.setClientId(clientId);
		} else {
			resetClientId(username);
			clientId = Integer.toString(nextClientId);
			nextClientId++;
			user.setClientId(clientId);
		}
		return clientId;
	}
	
	@Override
	public void resetClientId(String username) {
		if(username == null) {
			throw new NullPointerException();
		}
		User user = getUser(username);
		user.setClientId(null);
	}
	
	public User getUserPost(String username) { // Per risolvere il probleme dello spazio uso questo metodo
		if(username == null)
			throw new NullPointerException();
		for(User user : users) {
			String name = " " + user.getUsername();
			if(name.equals(username))
				return user;
		}
		return null;
	}
	
	public User getUser(String username) {
		if(username == null)
			throw new NullPointerException();
		for(User user : users) {
			if(user.getUsername().equals(username))
				return user;
		}
		return null;
	}// Ricaviamo l'oggetto User da username
	
	public boolean checkPost(int idPost) {
		for(Post p : posts) {
			if(p.getPostId() == idPost) {
				return true;
			}
		}
		return false;
	}
	
	public ArrayList<Post> updateFeedList(String username) {	// Esempio: Post che visualizza Degik nel suo feed
		ArrayList<Post> feedPost = new ArrayList<>();
		for(User user : users) {
			if(user.getFollowing().contains(username)) {
				ArrayList<Post> userPost = new ArrayList<>();
				for(Post post : userPost) {
					feedPost.add(post);
				}
			}
		}
		return feedPost;
	}
	
	public double generateRandom() {
		return ThreadLocalRandom.current().nextDouble();
	}
	
}
