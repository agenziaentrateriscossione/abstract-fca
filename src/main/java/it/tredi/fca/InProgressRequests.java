package it.tredi.fca;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Mantiene la lista di richieste pendenti a FCA (richieste in attesa di elaborazione, aggiunte alla blocking queue o in fase di elaborazione su un host FCS)
 * @author mbernardini
 */
public class InProgressRequests {
	
	private static final Logger logger = LogManager.getLogger(Fca.class.getName());

	// Singleton
    private static InProgressRequests instance = null;
    
    private List<String> inprogressIds;
    
    /**
     * Costruttore privato
     */
    private InProgressRequests() {
    	this.inprogressIds = new ArrayList<String>();
    }
	
    /**
     * Ritorna l'oggetto contenente la lista di richieste pendenti
	 * @return
	 */
	public static InProgressRequests getInstance() {
		if (instance == null) {
			synchronized (InProgressRequests.class) {
				if (instance == null) {
					if (logger.isInfoEnabled())
						logger.info("InProgressRequests instance is null... create one");
					instance = new InProgressRequests();
				}
			}
		}
		return instance;
	}
	
	/**
	 * Aggiunta di una richiesta alla lista di richieste pendenti
	 * @param idReq Identificativo della richiesta da aggiungere (nuova richiesta da elaborare)
	 */
	public synchronized void addRequest(String idReq) {
		if (idReq != null && !idReq.isEmpty())
			inprogressIds.add(idReq);
	}
	
	/**
	 * Eliminazione di una richiesta alla lista di richieste pendenti
	 * @param idReq Identificativo della richiesta da rimuovere (richiesta per la quale e' stata completata l'elaborazione)
	 */
	public synchronized void removeRequest(String idReq) {
		if (idReq != null && !idReq.isEmpty() && inprogressIds.contains(idReq))
			inprogressIds.remove(idReq);
		else
			logger.warn("InProgressRequests.removeRequest(): Req " + idReq + " not contained in in-progress list");
	}
	
	/**
	 * Ritorna true se la richiesta specificata risulta gia' presente fra quelle pendenti, false altrimenti
	 * @param idReq Identificativo della richiesta da controllare
	 * @return
	 */
	public boolean containsRequest(String idReq) {
		if (idReq != null && !idReq.isEmpty())
			return inprogressIds.contains(idReq);
		else
			return false;
	}
	
	/**
	 * Ritorna il numero di richieste pendenti
	 * @return
	 */
	public int countRequests() {
		return inprogressIds.size();
	}
	
	/**
	 * Ritorna l'elenco delle richieste pendenti
	 * @return
	 */
	public List<String> listRequests() {
		return inprogressIds;
	}
	
	
}
