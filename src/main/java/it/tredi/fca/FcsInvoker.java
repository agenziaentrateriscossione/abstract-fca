package it.tredi.fca;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.tredi.fca.entity.FcsRequest;
import it.tredi.fcs.socket.commands.HeaderRequest;
import it.tredi.fcs.socket.commands.HeaderResponse;
import it.tredi.fcs.socket.commands.Protocol;

/**
 * Thread di invocazione di un host FCS
 * @author mbernardini
 */
public class FcsInvoker implements Runnable {

	private static final Logger logger = LogManager.getLogger(Fca.class.getName());

	private static final int FCS_WORK_TIMEOUT_SOCKET_ADDON = 5000;

	private String id;
	private int fcsWorkTimeout = 0;
	private BlockingQueue<FcsRequest> fcsRequestQueue = null;

	/**
	 * Costruttore
	 * @param id
	 * @param fcsRequestQueue
	 */
	public FcsInvoker(BlockingQueue<FcsRequest> fcsRequestQueue) {
		this.id = Thread.currentThread().getName();
		this.fcsRequestQueue = fcsRequestQueue;

		try {
			int workTimeout = Fca.getFcaConfig().getFcsWorkTimeout();
			if (workTimeout > 0)
				this.fcsWorkTimeout = workTimeout + FCS_WORK_TIMEOUT_SOCKET_ADDON;
		}
		catch(Exception e) {
			logger.error("FcsInvoker[" + getIdentifier() + "]: unable to get FCS work timeout... " + e.getMessage());
		}
	}

	@Override
	public void run() {
		try {
			if (logger.isInfoEnabled())
				logger.info("FcsInvoker[" + getIdentifier() + "]: thread started...");
			
			while(true) {
				FcsRequest fcsRequest = fcsRequestQueue.take();
				if (logger.isInfoEnabled())
					logger.info("FcsInvoker[" + getIdentifier() + "]: took request " + (fcsRequest != null ? fcsRequest.getDocId() : "NULL") + " from blocking queue...");
				
				FcsHost fcsHost = null;
				int attemps = 1;
				
				while(fcsHost == null) {
					fcsHost = Fca.getTargetFcs();
					
					if(fcsHost == null) {
						//TODO mail di notifica all'amministratore che ci sono dei servizi fcs previsti inattivi
						if (Fca.getFcaConfig().getFcaWaitingStep() > 0) {
							logger.warn("FcsInvoker[" + fcsRequest.getDocId() + "]: No host found (attempt " + attemps + ")... wait for " + Fca.getFcaConfig().getFcaWaitingStep() + " millis...");
							try {
								Thread.sleep(Fca.getFcaConfig().getFcaWaitingStep());
							} 
							catch (InterruptedException e) {
								logger.error("FcsInvoker[" + fcsRequest.getDocId() + "]: No host found error on sleep", e);
							}
							attemps++;
						}
					}
				}
				
				if (logger.isInfoEnabled())
					logger.info("FcsInvoker[" + getIdentifier() + "]: New Request -> " + fcsRequest.toString());
				
				Socket client = null;
				DataOutputStream dos = null;
				DataInputStream dis = null;
				try {
					client = new Socket();
					if (fcsWorkTimeout > 0)
						client.setSoTimeout(fcsWorkTimeout);
					client.connect(new InetSocketAddress(fcsHost.getHost(), fcsHost.getPort()), Fca.SOCKET_CONNECT_TIMEOUT_DEFAULT_VALUE);

					if (logger.isInfoEnabled())
						logger.info("FcsInvoker[" + getIdentifier() + "]: client " + fcsHost.getHost() + ":" + fcsHost.getPort() + " ready!");

					dos = new DataOutputStream(client.getOutputStream());
		            dis = new DataInputStream(client.getInputStream());

		            boolean success = false;

		            long startTime = System.currentTimeMillis();

		            // init del dialogo socket di indicizzazione/conversione
		            Protocol protocol = new Protocol(dis, dos);
					protocol.sendHeader(HeaderRequest.INIT_HEADER.bytes());
					HeaderResponse response = HeaderResponse.getHeaderResponse(protocol.receiveHeader()); // attesa di risposta da parte dell'host FCS
					if (response == HeaderResponse.ACK_HEADER) {

						// invio a FCS la tipologia di comando da eseguire...
						protocol.sendHeader(HeaderRequest.FCA_HEADER.bytes());
						response = HeaderResponse.getHeaderResponse(protocol.receiveHeader()); // attesa di risposta da parte dell'host FCS
						if (response == HeaderResponse.ACK_HEADER) {
							// comando riconosciuto correttamente da parte di FCS...

							// invio di tutti i parametri necessari a portare a termine la richiesta
							protocol.sendString(fcsRequest.getDocId()); // invio dell'identificativo del documento da elaborare
							protocol.sendString(fcsRequest.getConversionTo()); // definisce l'estensione (o estensioni) di destinazione delle conversioni dei file del documento
							protocol.sendString(fcsRequest.getAdditionalParameters()); // eventuali parametri aggiuntivi da inviare all'host FCS

							// client socket in attesa della risposta da parte dell'host FCS...
							response = HeaderResponse.getHeaderResponse(protocol.receiveHeader());
							if (response == HeaderResponse.DONE_HEADER)
								success = true;
						}
					}

		        	if (success) {
		        		if (logger.isInfoEnabled())
		        			logger.info("FcsInvoker[" + getIdentifier() + "]: Request COMPLETED in " + (System.currentTimeMillis()-startTime) + " millis.!");
		        	}
					else
						logger.warn("FcsInvoker[" + getIdentifier() + "]: Request FAILED in " + (System.currentTimeMillis()-startTime) + " millis.!");
				}
				catch(Exception e) {
					logger.error("FcsInvoker[" + getIdentifier() + "]: Request FAILED! Got exception on socket dialogue... " + e.getMessage(), e);
				}
				finally {
					// chiusura dei buffer e del socket
					try {
						if (dis != null)
							dis.close();
					}
					catch(Exception e) {
						logger.warn("FcsInvoker[" + getIdentifier() + "]: unable to close InputStream... " + e.getMessage());
					}
					try {
						if (dos != null)
							dos.close();
					}
					catch(Exception e) {
						logger.warn("FcsInvoker[" + getIdentifier() + "]: unable to close OutputStream... " + e.getMessage());
					}
					try {
						if (client != null)
							client.close();
					}
					catch(Exception e) {
						logger.warn("FcsInvoker[" + getIdentifier() + "]: unable to close socket connection... " + e.getMessage(), e);
					}
				}

				fcsHost.setRequestCompleted(fcsRequest.getDocId());
			}
		} 
		catch (Exception e) {
			// TODO mail di notifica all'amministratore che ci sono dei servizi fcs previsti inattivi
			logger.error("FcsInvoker[" + getIdentifier() + "]: FATAL, unable to read requestsQueue (quit thread)... " + e.getMessage(), e);
		}
	}

	public String getIdentifier() {
		return id;
	}

}
