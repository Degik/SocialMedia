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
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.*;

public class MainServer extends RemoteObject implements ServerInterface{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -297225371107007564L;
	private static ArrayList<NotifyClientInterface> clients = null;
	private ArrayList<User> users = null;
	private final File backupDir;
	private final File usersJson;
	private final ObjectMapper objectMapper;
	
	public static void main(String[] args) {
		MainServer serverTh = new MainServer();
		start(serverTh);
	}

	public MainServer() {
		super();
		clients = new ArrayList<>(); // Lista dei client connessi al server
		users = new ArrayList<>(); 	 // Set contenente gli utenti registrati al sistema
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

	@Override
	public boolean login(String username, String password) throws RemoteException {
		if(username == null || password == null) {
			throw new NullPointerException();
		}
		for(User user : users) {
			if(user.getUsername().equals(username)) {
				if(user.getPassword().equals(password)) {
					System.out.println("Server [Un utente ha effettuato l'accesso " + user.getUsername() + "]");
					return true;
				} else {
					System.out.println("Server [Un utente ha inserito la password sbagliata]");
				}
			} else {
				System.out.println("Server [Un utente ha inserito il nickname errato]");
			}
		}
		System.out.println("Server [Un guest ha provato ad accedere]");
		return false;
	}

	@Override
	public boolean logout(String username) throws RemoteException {
		//
		return false;
	}
	
	
}
