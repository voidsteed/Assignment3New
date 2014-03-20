package udpgroupchat.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class Server {

	// constants
	public static final int DEFAULT_PORT = 20000;
	public static final int MAX_PACKET_SIZE = 512;
	
	public static final int MAX_ARQ_ATTEMPTS = 10;
	
	
	public static final Timer timer = new Timer();
	
	// port number to listen on
	protected static int port;
	public static InetSocketAddress serverSktAddress;
	

	// set of clientEndPoints
	// note that this is synchronized, i.e. safe to be read/written from
	// concurrent threads without additional locking
	protected static final Set<ClientEndPoint> clientEndPoints = 
			Collections.synchronizedSet(new HashSet<ClientEndPoint>());
	
	protected static final Set<Group> groups = 
			Collections.synchronizedSet(new HashSet<Group>());
	
	protected static final Map<UUID,Client> idsToClientsMap = 
			Collections.synchronizedMap(new HashMap<UUID,Client>());
	
	protected static final Map<UUID, TimerTask> idsToTimerMap = 
			Collections.synchronizedMap(new HashMap<UUID, TimerTask>());
	
	protected static final Map<UUID, String> idsToMsgsMap = 
			Collections.synchronizedMap(new HashMap<UUID, String>());

	protected static final Map<String, Timer> ackToTimerMap = 
			Collections.synchronizedMap(new HashMap<String, Timer>());
	
	// constructor
	Server(int port) throws UnknownHostException {
		this.port = port;
		this.serverSktAddress = new InetSocketAddress(InetAddress.getLocalHost(),port);
	}

	// start up the server
	public void start() {
		DatagramSocket socket = null;
		try {
			// create a datagram socket, bind to port port. See
			// http://docs.oracle.com/javase/tutorial/networking/datagrams/ for
			// details.

			socket = new DatagramSocket(port);

			// receive packets in an infinite loop
			while (true) {
				// create an empty UDP packet
				byte[] buf = new byte[Server.MAX_PACKET_SIZE];
				DatagramPacket packet = new DatagramPacket(buf, buf.length);
				// call receive (this will poulate the packet with the received
				// data, and the other endpoint's info)
				socket.receive(packet);
				// start up a worker thread to process the packet (and pass it
				// the socket, too, in case the
				// worker thread wants to respond)
				WorkerThread t = new WorkerThread(packet, socket);
				t.start();
			}
		} catch (IOException e) {
			// we jump out here if there's an error, or if the worker thread (or
			// someone else) closed the socket
			e.printStackTrace();
		} finally {
			if (socket != null && !socket.isClosed())
				socket.close();
		}
	}

	// main method
	public static void main(String[] args) throws UnknownHostException {
		int port = Server.DEFAULT_PORT;

		// check if port was given as a command line argument
		if (args.length > 0) {
			try {
				port = Integer.parseInt(args[0]);
			} catch (Exception e) {
				System.out.println("Invalid port specified: " + args[0]);
				System.out.println("Using default port " + port);
			}
		}

		// instantiate the server
		Server server = new Server(port);

		System.out
				.println("Starting server. Connect with netcat (nc -u localhost "
						+ port
						+ ") or start multiple instances of the client app to test the server's functionality.");

		// start it
		server.start();

	}

}
