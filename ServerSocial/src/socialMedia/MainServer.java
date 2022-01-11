package socialMedia;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Properties;
import java.io.*;

public class MainServer {

	public static void main(String[] args) {
		// impostazioni server
		String server, tcpPort, udpPort, multiCast, multiCastPort, registryHost, registryPortStr, timeout, serverName;
		Properties prop = new Properties();
		try {
			FileInputStream input = new FileInputStream("C:\\Users\\Davide Bulotta\\eclipse-workspace\\ServerSocial\\src\\socialMedia\\ServerConfig.properties");
			prop.load(input);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			System.err.println("IOException");
			e.printStackTrace();
		}
		
		// Prendo gli argomenti dal file di configurazione
		// .getProperty cerca la parola chiave
		// restituisce la String dell'argomento
		server = prop.getProperty("SERVER");
		tcpPort = prop.getProperty("TCPPORT");
		udpPort = prop.getProperty("UDPPORT");
		multiCast = prop.getProperty("MULTICAST");
		multiCastPort = prop.getProperty("MCASTPORT");
		registryHost = prop.getProperty("REGISTRYHOST");
		registryPortStr = prop.getProperty("REGISTRYPORT");
		timeout = prop.getProperty("TIMEOUT");
		serverName = prop.getProperty("SERVERNAME");
		
		int registryPortInt = Integer.parseInt(registryPortStr);
		
		//Creazione del server
		
		
		//Inizio RMI
		// Creazione dello stub
		try {
			SocialSystem socialSystem = new SocialSystem();
			SocialSystemRemoteMethods stub = (SocialSystemRemoteMethods) 
					UnicastRemoteObject.exportObject(socialSystem, 0);
			
			LocateRegistry.createRegistry(registryPortInt);
			Registry registro = LocateRegistry.getRegistry(registryPortInt);
			
			String host = "rmi://" + registryHost + ":" + registryPortStr + "/" + serverName;
			
			registro.rebind(serverName, stub);
			System.err.println("Server pronto");
		} catch (RemoteException e) {
			// Errore in caso di RemoteException
			System.err.println("Errore di comunicazione ");
		}
		
		
	}

}
