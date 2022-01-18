package socialMedia;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class CalculateClient implements Runnable {

	private static final int sizeBuff = 1024;
	private int mulPort;
	private String mulIp;
	boolean stop = false;
	
	public CalculateClient(int mulPort, String mulIp) {
		this.mulPort = mulPort;
		this.mulIp = mulIp;
	}
	
	@Override
	public void run() {
		try (MulticastSocket socket = new MulticastSocket(mulPort)) {
			InetAddress group = InetAddress.getByName(mulIp);
			if(!group.isMulticastAddress()) {
				System.err.println("< Non riesco a ricevere messaggi dal CalculateSystem");
			}
			socket.joinGroup(group);
			
			while(!Thread.currentThread().isInterrupted()) {
				byte[] buff = new byte[sizeBuff];
				DatagramPacket packet = new DatagramPacket(buff, sizeBuff);
				socket.receive(packet);
				if(!Thread.currentThread().isInterrupted()) {
					System.out.println();
					System.out.println("< " + new String(packet.getData()));
					System.out.print("> ");
				}
			}
			
		} catch(Exception e) {
			System.err.println("< Errore CalculateClient");
			e.printStackTrace();
		}
	}

}
