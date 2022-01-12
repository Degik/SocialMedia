package socialMedia;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteObject;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Set;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;

public class MainServer extends RemoteObject implements ServerInterface{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -297225371107007564L;
	private static ArrayList<NotifyClientInterface> clients = null;
	private Set<User> users = null;
	
	public static void main(String[] args) {
		MainServer serverTh = new MainServer();
		start(serverTh);
	}

	public MainServer() {
		super();
		clients = new ArrayList<>(); // Lista dei client connessi al server
		users = new HashSet<>(); 	 // Set contenente gli utenti registrati al sistema
	}
	
	public static void start(MainServer serverTh) {
		// impostazioni server
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
		SocialSystem socialSystem = new SocialSystem();
		
		//Inizio RMI
		// Creazione dello stub
		try {
			ServerInterface stub;
			stub = (ServerInterface) UnicastRemoteObject.exportObject(serverTh, 0);
			LocateRegistry.createRegistry(registryPortInt);
			Registry registro = LocateRegistry.getRegistry(registryPortInt);
			registro.rebind(serverClassName, stub);
			System.err.println("Server pronto");
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
			if(u.getUsername().equals(username))
				System.out.println("Server [registrazione fallita nickname gia' in uso]");
				return false;
		}
		User user = new User(username, password, tags);
		if(!users.add(user)) {
			System.out.println("Server [registrazione fallita]");
			return false;
		}
		System.out.println("Server [Registazione avvenuta con successo " + user.getUsername() + " id (" + user.getUserId() + ")]\n" );
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
	
	
}
