package socialMedia;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteObject;
import java.rmi.server.UnicastRemoteObject;

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

public class MainServer extends RemoteObject implements ServerInterface{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -297225371107007564L;
	private static ArrayList<NotifyClientInterface> clients = null;
	private ArrayList<User> users = null;
	private HashMap<String, ArrayList<String>> followers = null;
	private final File backupDir;
	private final File usersJson;
	private final ObjectMapper objectMapper;
	private int nextClientId = 0;
	
	public static void main(String[] args) {
		MainServer serverTh = new MainServer();
		start(serverTh);
	}

	public MainServer() {
		super();
		clients = new ArrayList<>(); // Lista dei client connessi al server
		users = new ArrayList<>(); 	 // Set contenente gli utenti registrati al sistema
		followers = new HashMap<>();
		backupDir = new File("backup");
		usersJson = new File("backup/users.json");
		objectMapper = new ObjectMapper();
		objectMapper.enable(SerializationFeature.INDENT_OUTPUT);			// https://fasterxml.github.io/jackson-databind/javadoc/2.6/com/fasterxml/jackson/databind/SerializationFeature.html#INDENT_OUTPUT
		objectMapper.enable(SerializationFeature.FLUSH_AFTER_WRITE_VALUE);	// https://fasterxml.github.io/jackson-databind/javadoc/2.6/com/fasterxml/jackson/databind/SerializationFeature.html#FLUSH_AFTER_WRITE_VALUE
		objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
	}
	
	public static void start(MainServer serverTh) {
		// impostazioni server
		@SuppressWarnings("unused") // not used
		String server, tcpPortStr, udpPort, multiCast, multiCastPort, registryHost, registryPortStr, timeout, serverName, serverClassName;
		Properties prop = new Properties();
		try {
			FileInputStream input = new FileInputStream("C:\\Users\\Davide Bulotta\\git\\SocialGit\\SocialMedia\\ServerSocial\\src\\socialMedia\\ServerConfig.properties");
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
		timeout = prop.getProperty("TIMEOUT");
		serverName = prop.getProperty("SERVERNAME");
		serverClassName = prop.getProperty("CLASSNAME");
				
		int registryPortInt = Integer.parseInt(registryPortStr);
		int tcpPortInt = Integer.parseInt(tcpPortStr);
				
		//Creazione del server
		if(!serverTh.manageBackup()) {
			System.err.println("Server [Errore di caricamento del backup]");
		}
		System.out.println("Server [Backup pronto]");
		
		//Inizio RMI
		// Creazione dello stub
		try {
			ServerInterface stub;
			stub = (ServerInterface) UnicastRemoteObject.exportObject(serverTh, 0);
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
						String result = serverTh.startCommand(command);
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
					e.printStackTrace();
					return;
				}
			}
			
		}
	}

	@Override
	public boolean registerUser(String username, String password, LinkedList<String> tags) throws RemoteException {
		// TODO Auto-generated method stub
		for(User u : users) {
			if(u.getUsername().equals(username)) {
				System.out.println("Server [registrazione fallita nickname gia' in uso]");
				return false;
			}
		}
		User user = new User(username, password, tags);
		if(!users.add(user)) {
			System.out.println("Server [registrazione fallita]");
			return false;
		}
		try {
			objectMapper.writeValue(usersJson, users);
		} catch(IOException e) {
			System.err.println("Server [Errore creazione backup di " + user.getUsername() + "]");
		}
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
	public synchronized void unregisterForCallBaks(NotifyClientInterface clientInterface) throws RemoteException {
		if(clients.contains(clientInterface)) {
			clients.remove(clientInterface);
			System.out.println("Server [Client rimosso]");
		}else {
			System.out.println("Server [Client non trovato]");
		}
	}
	
	private boolean manageBackup() {
		if(!backupDir.exists()) {
			System.out.println("Server [Nessun backup trovato]");
			if(!backupDir.mkdir()) {
				System.err.println("Server [Errore nella creazione della cartella di backup]");
				return false;
			}
			System.out.println("Server [Cartella backup creata]");
			try {
				usersJson.createNewFile();
				System.out.println("Server [Nuovo file di backup creato]");
			} catch(IOException e) {
				System.err.println("Server [Errore creazione del file di backup]");
				return false;
			}
			users = new ArrayList<>();
		} else {
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
			} else {
				users = new ArrayList<>();
			}
		}
		return true;
	}
	
	public String startCommand(String[] command) {
		switch(command[0]) {
		case "login":
			String username = command[1];
			String password = command[2];
			for(User user : users) {
				if(user.getUsername().equals(username)) {
					if(user.getPassword().equals(password)) {
						System.out.println("Server [Un utente ha effettuato l'accesso " + user.getUsername() + "]");
						updateCallBack(username);
						return "Accesso effettuato come " + username;
					} else {
						System.out.println("Server [Un utente ha inserito la password sbagliata]");
						return null;
					}
				} else {
					System.out.println("Server [Un utente ha inserito il nickname errato]");
					return null;
				}
			}
			System.out.println("Server [Un guest ha provato ad accedere]");
			return null;
		case "logout":
			return null;
		}
		return null;
	}
	
	public void updateCallBack(String username) {
		for(String k : followers.keySet()) { // Aggiorno la lista globale dei followers per ogni utente
			User user = getUser(k);
			ArrayList<String> followersList = user.getFollowers();
			followers.put(k, followersList);
		}
		NotifyClientInterface logout = null;
		for(NotifyClientInterface client : clients) {
			for(User user : users) {
				try {
					if(client.getClientId().equals(user.getClientId())) {
						client.notifyFollowers(user.getFollowers()); // Lista delle persone che seguono user
					}
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		System.out.println("Server [CallBack " + username + " aggiornati");
	}
	
	@Override
	public String updateClientIdList(String username) {
		User user = getUser(username);
		String userClientId = user.getClientId();
		if(userClientId == null) {
			String clientId = Integer.toString(nextClientId);
			nextClientId++;
			user.setClientId(clientId);
			return clientId;
		}
		return userClientId;
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
	
}
