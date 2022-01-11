package socialMedia;

import java.util.*;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.*;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.Naming;

public class ClientMain {

	public static void main(String[] args) {
		
		String registryHost, registryPortStr, serverName;
		Properties prop = new Properties();
		
		try {
			FileInputStream input = new FileInputStream("C:\\Users\\Davide Bulotta\\eclipse-workspace\\ServerSocial\\src\\socialMedia\\ServerConfig.properties");
			prop.load(input);
			
			registryHost = prop.getProperty("SERVER");
			registryPortStr = prop.getProperty("REGISTRYPORT");
			serverName = prop.getProperty("SERVERNAME");
			
			int registryPortInt = Integer.parseInt(registryPortStr);
			
			Registry registry = LocateRegistry.getRegistry(registryPortInt);
			
			SocialSystemRemoteMethods serverStub = (SocialSystemRemoteMethods) registry.lookup(serverName);
			
			System.out.println("Client connesso");
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

}
