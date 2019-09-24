package it.tredi.fca;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.security.AccessControlException;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.tredi.fca.entity.FcsRequest;
import it.tredi.fcs.socket.commands.HeaderRequest;
import it.tredi.fcs.socket.commands.HeaderResponse;
import it.tredi.fcs.socket.commands.Protocol;

/**
 * Classe astratta FCA: Smistamento richieste verso pool FCS
 * @author mbernardini
 */
public abstract class Fca {

	private static final Logger logger = LogManager.getLogger(Fca.class.getName());
	
	public static final int SOCKET_CONNECT_TIMEOUT_DEFAULT_VALUE = 2000;

	private static int lastIndex = 0;
	
	/**
	 * Coda condivisa (e sincronizzata) fra main FCA (che popola la coda con le richieste di indicizzazione/conversione) e vari thread di invoker (che prelevano le richieste
	 * ed invocano un host FCS)
	 */
	private BlockingQueue<FcsRequest> fcsRequestQueue = null;

	/** Shutdown hook thread instance */
	private FcaShutdownHook shutdownHook;

	public Fca() throws Exception {

		if (logger.isInfoEnabled()) {
			logger.info(" _____    ____      _    ");
			logger.info("|  ___|  / ___|    / \\   ");
			logger.info("| |_    | |       / _ \\  ");
			logger.info("|  _|   | |___   / ___ \\ ");
			logger.info("|_|      \\____| /_/   \\_\\");
			logger.info("FCA version: " + getAppVersion() + " " + getAppBuildDate());
		}
		
		if (FcaConfig.getInstance().getFcsPool() == null || FcaConfig.getInstance().getFcsPool().isEmpty())
			throw new Exception("ERROR: No FCS hosts defined!");

		// istanzia la lista di richieste pendenti
		InProgressRequests.getInstance();
	}
	
	/**
	 * Ritorna la versione dell'applicazione
	 * @return
	 */
	public abstract String getAppVersion();
	
	/**
	 * Ritorna la data di rilascio dell'applicazione
	 * @return
	 */
	public abstract String getAppBuildDate();

	/**
	 * Esecuzione del servizio di FCA (controllo documenti da indicizzare e/o
	 * convertire ed invio ad host FCS)
	 *
	 * @throws Exception
	 */
	public void run() throws Exception {
		this.shutdownHook = new FcaShutdownHook();
		try {
			Runtime.getRuntime().addShutdownHook(shutdownHook);
			if (logger.isInfoEnabled())
				logger.info("FCA: FcaShutdownHook registered!");
		}
		catch (AccessControlException e) {
			logger.error("FCA: Could not register shutdown hook... " + e.getMessage(), e);
		}
		
		// mbernardini 21/12/2018 : corretto controllo su porta socket occupata
		try {
			FcaSocket.getInstance(FcaConfig.getInstance().getFcaPresencePort());
		}
		catch (Exception e) {
			if (logger.isInfoEnabled()) {
				logger.info("Another instance of FCA already running on port [" + FcaConfig.getInstance().getFcaPresencePort() + "]");
				logger.info("FCA Service NOT Started!");
			}
			throw e;
		}

		try {
			if (logger.isInfoEnabled())
				logger.info("FCA: Service Started!");

			// Inizializzazione degli host FCS
			//initializeFcsHosts();

			// Assegnazione delle dimensione alla coda condivisa (e sincronizzata). La dimensione corrisponde al numero totale di thread di indicizzazione/conversione
			// configurati sugli host FCS
			int queueSize = 0;
			for (FcsHost fcsHost : FcaConfig.getInstance().getFcsPool()) {
				queueSize += fcsHost.getQueueMaxSize();
			}
			fcsRequestQueue = new ArrayBlockingQueue<FcsRequest>(queueSize);
			if (logger.isInfoEnabled())
				logger.info("FCA: blocking queue size = " + queueSize);

			// Per ogni thread definito per gli host FCS viene generato un thread di invoker che si mette in attesa di leggere delle richieste di indicizzazione/conversione
			// dalla coda condivisa
			for(int i = 0; i < queueSize; i++) {
				FcsInvoker fcsInvoker = new FcsInvoker(fcsRequestQueue);
				new Thread(fcsInvoker, "Invoker" + i).start();
			}

			while (true) {
				try {
					List<FcsRequest> fcsRequests = loadFcsPendingRequests();
					if (logger.isInfoEnabled())
						logger.info("FCA: found " + (fcsRequests != null ? fcsRequests.size() : "0") + " FCS requests!");

					if (fcsRequests != null && fcsRequests.size() > 0) {
						for (FcsRequest fcsRequest : fcsRequests) {
							// Verifico che la richista corrente non risulti gia' presa in carico da uno specifico host FCS
							if (!isInProgressFcsRequest(fcsRequest)) {
								// Inserisco la request nella coda. In caso di cosa piena il processo main si mette in attesa che una delle richieste pendenti venga presa in carico da
								// un threa Invoker
								fcsRequestQueue.put(fcsRequest);
								if (logger.isInfoEnabled())
									logger.info("FCA: put " + fcsRequest.getDocId() + " request on blocking queue...");
								InProgressRequests.getInstance().addRequest(fcsRequest.getDocId());
							}
						}
					}
					else {
						// sleep di FCA prima di verificare la presenza di nuovi documenti da elaborare (indicizzazione/conversione)
						if (logger.isInfoEnabled())
							logger.info("FCA: sleep for " + FcaConfig.getInstance().getFcaRefreshDelay() + " millis...");
						Thread.sleep(FcaConfig.getInstance().getFcaRefreshDelay());
					}
				}
				catch (Exception e) {
					logger.error("FCA: Got FATAL exception... " + e.getMessage() + "! Wait " + FcaConfig.getInstance().getFcaRefreshDelay() + " ms. and retry...", e);
					Thread.sleep(FcaConfig.getInstance().getFcaRefreshDelay());
				}
			}
		}
		catch (Exception e) {
			onRunException(e);
			logger.error("FCA: [FATAL ERROR] -> " + e.getMessage() + "... FCA service is down!", e);
			throw e;
		}
		finally {
			onRunFinally();

			if (shutdownHook != null) {
				try {
					Runtime.getRuntime().removeShutdownHook(shutdownHook);
				}
				catch (IllegalStateException e) {
					// May fail if the JVM is already shutting down
					logger.warn("FCA: FcaShutdown already shutting down! " + e.getMessage());
				}
				this.shutdownHook = null;
			}
		}
	}

	/**
	 * Metodo chiamato per arrestare il servizio
	 */
	public static void stop(String[] args) {
		logger.info("Fca.stop(): exit method now call System.exit(0)");
		System.exit(0);
	}

	/**
	 * Eventuali azioni da compiere in caso di eccezione su RUN di FCA
	 * @param e
	 */
	public abstract void onRunException(Exception e);

	/**
	 * Eventuali azioni da compiere su finally del RUN di FCA (chiusura del servizio)
	 */
	public abstract void onRunFinally();

	/**
	 * Caricamento di tutte le richieste di indicizzazione/conversione pendenti
	 * @return
	 * @throws Exception
	 */
	public abstract List<FcsRequest> loadFcsPendingRequests() throws Exception;

	/**
	 * Inizializzazione di tutti gli host FCS: Verifica se effettivamente attivi (server socket in ascolto) ed invio di tutti i parametri di
	 * configurazione
	 * @throws Exception Eccezione in caso di host che non risponde
	 * @deprecated Chiamata a isAliveFcsHost ad ogni nuova richiesta in fase di individuazione dell'host FCS da chiamare (problemi in caso di riavvio di un host FCS)
	 */
	private void initializeFcsHosts() throws Exception {
		for (FcsHost fcsHost : FcaConfig.getInstance().getFcsPool()) {
			if (!isAliveFcsHost(fcsHost))
				throw new Exception("ERROR: Host " + fcsHost.toString() + "is not ALIVE... FCA stops execution!");
		}
	}

	/**
	 * Restituisce l'elenco dei documenti che sono attualmente in fase di
	 * elaborazione da parte degli host FCS
	 * @return
	 */
	public List<String> getInProgressRequests() {
		return InProgressRequests.getInstance().listRequests();
	}

	/**
	 * Verifica che l'host FCS selezionato sia effettivamente attivo (server
	 * socket in ascolto)
	 * @param host
	 * @return
	 */
	private static boolean isAliveFcsHost(FcsHost host) {
		long startTime = System.currentTimeMillis();
		boolean isAlive = false;
		Socket client = null;
		DataInputStream dis = null;
		DataOutputStream dos = null;
		try {
			client = new Socket();
			if (FcaConfig.getInstance().getFcsAliveTimeout() > 0)
				client.setSoTimeout(FcaConfig.getInstance().getFcsAliveTimeout());
			client.connect(new InetSocketAddress(host.getHost(), host.getPort()), SOCKET_CONNECT_TIMEOUT_DEFAULT_VALUE);

			dis = new DataInputStream(client.getInputStream());
			dos = new DataOutputStream(client.getOutputStream());

			Protocol protocol = new Protocol(dis, dos);
			protocol.sendHeader(HeaderRequest.ALIVE_HEADER.bytes());

			// attesa di risposta da parte dell'host FCS
			HeaderResponse response = HeaderResponse.getHeaderResponse(protocol.receiveHeader());
			if (response == HeaderResponse.ACK_HEADER) {
				isAlive = true; // risposta positiva del server (server attivo e
								// correttamente configurato)
			}
			else if (response == HeaderResponse.TO_CONFIG_HEADER) {
				// server attivo, ma mancano i parametri di attivazione... occorre spedirli al server

				// invio dei parametri di configurazione di FCS (recuperati da file di properties di FCA)
				protocol.sendString(FcaConfig.getInstance().getJsonFcsConfig());
				protocol.sendHeader(HeaderRequest.FCS_CONF_HEADER.bytes());

				// attesa di risposta da parte dell'host FCS
				response = HeaderResponse.getHeaderResponse(protocol.receiveHeader());
				if (response == HeaderResponse.ACK_HEADER)
					isAlive = true; // risposta positiva del server (server attivo e correttamente configurato)
				else // riscontrato problema durante la fase di invio configurazioni a server FCS
					logger.error("Fca.isAliveHost(): unexpected server configuration response... " + response.header());

			}
			else {
				// rispsta inaspettata da perte del server
				logger.error("Fca.isAliveHost(): unable to recognize server response... " + response.header());
			}
		}
		catch (SocketTimeoutException e) {
			logger.error("Fca.isAliveFcsHost(): got socket timeout exception on " + host.getHost() + ":" + host.getPort() + "... " + e.getMessage());
		}
		catch (Exception e) {
			logger.error("Fca.isAliveFcsHost(): got exception on " + host.getHost() + ":" + host.getPort() + "... " + e.getMessage());
		}
		finally {
			// chiusura dei buffer e del socket
			try {
				if (dis != null)
					dis.close();
			}
			catch (Exception e) {
				logger.warn("Fca.isAliveFcsHost(): unable to close InputStream... " + e.getMessage());
			}
			try {
				if (dos != null)
					dos.close();
			}
			catch (Exception e) {
				logger.warn("Fca.isAliveFcsHost(): unable to close OutputStream... " + e.getMessage());
			}
			try {
				if (client != null)
					client.close();
			}
			catch (Exception e) {
				logger.warn("Fca.isAliveFcsHost(): unable to close socket connection... " + e.getMessage(), e);
			}
		}

		if (logger.isDebugEnabled())
			logger.debug("Fca.isAliveFcsHost() - host= " + host.getHost() + ":" + host.getPort() + " time required: " + (System.currentTimeMillis() - startTime));
		return isAlive;
	}

	/**
	 * Ritorna la configurazione di FCA
	 * @return
	 * @throws Exception
	 */
	public static FcaConfig getFcaConfig() throws Exception {
		return FcaConfig.getInstance();
	}

	/**
	 * Ritorna la lista degli Host fcs configurati
	 * @return
	 * @throws Exception
	 */
	public static List<FcsHost> getFcsHosts() throws Exception {
		return FcaConfig.getInstance().getFcsPool();
	}

	/**
	 * Caricamento di un host FCS di destinazione dal pool a disposizione di FCA
	 * @return
	 * @throws Exception
	 */
	public static synchronized FcsHost getTargetFcs() throws Exception {
		if (FcaConfig.getInstance().getFcsSelectionMode() == FcsSelectionMode.ROUNDROBIN)
			return getTargetFcsRoundRobin();
		else
			return getTargetFcsByQueue();
	}

	/**
	 * Caricamento di un host FCS di destinazione tramite RoundRobin
	 * @return
	 * @throws Exception
	 */
	private static FcsHost getTargetFcsRoundRobin() throws Exception {
		FcsHost fcs = null;

		if (FcaConfig.getInstance().getFcsPool() != null) {
			for (int i = 0; i < FcaConfig.getInstance().getFcsPool().size(); i++) {
				lastIndex = ++lastIndex % FcaConfig.getInstance().getFcsPool().size();

				fcs = FcaConfig.getInstance().getFcsPool().get(lastIndex);
				if (!isFullQueue(fcs) && isAliveFcsHost(fcs))
					break;
				else
					fcs = null;
			}
		}

		if (fcs != null) {
			fcs.incrementQueueInProgressSize();
			if (logger.isDebugEnabled())
				logger.debug("Fca.getTargetFcsRoundRobin(): Found host " + fcs.getHost() + ":" + fcs.getPort() + " -> new in-progress size = " + fcs.getQueueInProgressSize());
		}
		return fcs;
	}

	/**
	 * Caricamento di un host FCS di destinazione tramite analisi della coda
	 * delle richieste (viene assegnato il piu' scarico)
	 * @return
	 * @throws Exception
	 */
	private static FcsHost getTargetFcsByQueue() throws Exception {
		FcsHost min = null;
		int index = 0;

		for (FcsHost fcsHost : FcaConfig.getInstance().getFcsPool()) {
			if (!isFullQueue(fcsHost) && isAliveFcsHost(fcsHost)) {
				if (min == null || fcsHost.getQueueInProgressSize() < min.getQueueInProgressSize()) {
					min = fcsHost;
					lastIndex = index;
				}
			}
			index++;
		}

		if (min != null) {
			min.incrementQueueInProgressSize();
			if (logger.isDebugEnabled())
				logger.debug("Fca.getTargetFcsByQueue(): Found host " + min.getHost() + ":" + min.getPort() + " -> new in-progress size = " + min.getQueueInProgressSize());
		}
		return min;
	}

	/**
	 * Verifica se la coda di processi di un host e' completamente impegnata o
	 * se ci sono thread disponibili
	 *
	 * @param host
	 * @return
	 */
	private static boolean isFullQueue(FcsHost host) {
		return host.getQueueInProgressSize() == host.getQueueMaxSize();
	}

	/**
	 * Ritorna true se la richiesta corrente risulta gia' presa in carica da un host FCS. Recupero di un documento attualmente ancora in fase di elaborazione
	 * da parte di un host (estrazione del testo o conversione di files ancora in corso)
	 * @param fcsRequest
	 * @return
	 */
	private boolean isInProgressFcsRequest(FcsRequest fcsRequest) {
		boolean inProgress = false;
		if (fcsRequest != null) {
			inProgress = InProgressRequests.getInstance().containsRequest(fcsRequest.getDocId());
			if (inProgress) {
				if (logger.isInfoEnabled())
					logger.info("FCA: docId " + fcsRequest.getDocId() + " already in progress (blocking queue or FCS Host)... ");
			}
		}
		return inProgress;
	}

	/**
	 * Called on shutdown. This gives use a chance to store the keys and to optimize even if the cache manager's shutdown method was not called
	 * manually.
	 */
	class FcaShutdownHook extends Thread {

		/**
		 * This will persist the keys on shutdown.
		 * @see java.lang.Thread#run()
		 */
		@Override
		public void run() {
			if (logger.isInfoEnabled())
				logger.info("FcaShutdownHook hook ACTIVATED. Shutdown was not called. CALL onRunFinally().");

			try {
				onRunFinally();
			}
			catch (Exception e) {
				logger.error("FcaShutdownHook: got exception on fca closure... " + e.getMessage(), e);
			}
		}
	}


}
