package it.tredi.fca;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Singleton di aggancio e verifica della porta Socket per il servizio FCA
 */
public class FcaSocket {

	private int port;
	private ServerSocket fcaSocket;
	
	private static final Logger logger = LogManager.getLogger(FcaSocket.class.getName());

	// Singleton
    private static FcaSocket instance = null;
    
    /**
     * Costruttore privato
     */
    private FcaSocket(int port) throws IOException {
    	this.port = port;
    	if (this.port != 0) // e' stata specificata la porta di verifica dell'istanza di FCA
    		this.initSocket();
    }
	
    /**
     * Ritorna l'oggetto contenente il Socket del servizio FCA
	 * @return
	 */
	public static FcaSocket getInstance(int port) throws IOException {
		if (instance == null) {
			synchronized (FcaSocket.class) {
				if (instance == null) {
					if (logger.isInfoEnabled())
						logger.info("FcaSocket instance is null... create one");
					instance = new FcaSocket(port);
				}
			}
		}
		return instance;
	}
	
	/**
	 * Inizializzazione del socket di MSA
	 * @throws IOException
	 */
	private void initSocket() throws IOException {
		InetSocketAddress testSocketAddress = new InetSocketAddress("127.0.0.1", port);
		fcaSocket = new ServerSocket();
		fcaSocket.bind(testSocketAddress);
	}
	
}
