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

@SuppressWarnings("unused")
public class ClientMain extends RemoteObject implements NotifyClientInterface{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static SocketChannel socketChannel;
	private User user = null;
	private ArrayList<String> users;
	private ArrayList<String> posts;
	private String username;
	private String clientId;
	
	public ClientMain() {
		super();
		users = null;
		posts = null;
		username = null; // Un utente che non ha effettuato il login
		clientId = null;
	}
	
	public void start() {
		String serverHostStr, registryPortStr, serverName, tcpPortStr, serverClassName;
		Properties prop = new Properties();
		
		boolean state = true; // Serve per uscire dal while del menu client
		
		try {
			
			FileInputStream input = new FileInputStream("C:\\Users\\Davide Bulotta\\eclipse-workspace\\ServerSocial\\src\\socialMedia\\ServerConfig.properties");
			prop.load(input);
			
			serverHostStr = prop.getProperty("SERVER");
			tcpPortStr = prop.getProperty("TCPPORT");
			registryPortStr = prop.getProperty("REGISTRYPORT");
			serverName = prop.getProperty("SERVERNAME");
			
			int registryPortInt = Integer.parseInt(registryPortStr);
			int tcpPortInt = Integer.parseInt(tcpPortStr);
			
			
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
			
			System.out.println("Benvenuto su Social\n\n");
			@SuppressWarnings("resource") // sc is not closed
			Scanner sc = new Scanner(System.in);
			help();
			System.out.println();
			
			while(state) {
				System.out.print("> ");
				String inputCommand = sc.nextLine() + " " + clientId;
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
					int count = command.length - 3; // Ricavo il numero di tag inseriti
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
							tags.add(command[j]);
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
					String request = requestCommand(inputCommand);
					if(request != null) {
						System.out.println("< " + request);
						username = command[1];
					} else {
						serverStub.unregisterForCallBaks(callStub);
						System.out.println("< Username e password errati");
					}
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
					// Da continuare
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
			System.err.println("Fine non trovato");
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
		System.out.println("     rewin <idPost> [pubblica il posto sul proprio blog]");
		System.out.println("     rate <idPost> <vote> [assegna un voto al post, voto negativo -1, voto positivo +1]");
		System.out.println("     comment <idPost> <comment> [commenta il post]");
		System.out.println("     wallet [restituisce il totale e la storia delle transazioni]");
		System.out.println("     wallet btc [restituisce il valore del proprio portafoglio in bitcoin]");
		System.out.println("     help [mostra la lista dei comandi]");
		System.out.println("     quit [termina il programma]");
	}
	
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
		this.users = users;
	}

	@Override
	public void notifyPost(ArrayList<String> posts) throws RemoteException {
		this.posts = posts;
	}

	@Override
	public String getClientId() throws RemoteException {
		return clientId;
	}

}
