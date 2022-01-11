package socialMedia;

import java.util.*;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.*;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;

public class ClientMain {
	
	private static SocketChannel socketChannel;
	
	public static void start() {
		String serverHostStr, registryPortStr, serverName, tcpPortStr;
		Properties prop = new Properties();
		
		boolean state = true; // Serve per uscire dal while del menu client
		boolean isLogin = false; // Serve per verificare se l'utente e' loggato
		// Questo mi permette di limitare la richiesta dei comandi
		
		try {
			
			FileInputStream input = new FileInputStream("C:\\Users\\Davide Bulotta\\eclipse-workspace\\ServerSocial\\src\\socialMedia\\ServerConfig.properties");
			prop.load(input);
			
			serverHostStr = prop.getProperty("SERVER");
			tcpPortStr = prop.getProperty("TCPPORT");
			registryPortStr = prop.getProperty("REGISTRYPORT");
			serverName = prop.getProperty("SERVERNAME");
			
			int registryPortInt = Integer.parseInt(registryPortStr);
			int serverHostInt = Integer.parseInt(serverHostStr);
			int tcpPortInt = Integer.parseInt(tcpPortStr);
			
			Registry registry = LocateRegistry.getRegistry(registryPortInt);
			
			SocialSystemRemoteMethods serverStub = (SocialSystemRemoteMethods) registry.lookup(serverName);
			
			System.out.println("Client connesso");
			
			try {
				socketChannel = SocketChannel.open();
				socketChannel.connect(new InetSocketAddress(serverHostStr, tcpPortInt));
			} catch(ClosedChannelException e1) {
				System.err.println("Socket error!");
				e1.printStackTrace();
				return;
			}
			
			while(state) {
				Scanner sc = new Scanner(System.in);
				
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
		}
	}

	public static void main(String[] args) {
		
	}

}
