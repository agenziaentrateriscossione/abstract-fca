package it.tredi.fca;

/**
 * Definizione di un host FCS (con indicazione del numero massimo di richieste che possono essere gestite sull'host)
 * @author mbernardini
 */
public class FcsHost {

	private String host;
	private int port;
	private int queueMaxSize; // numero massimo di richieste concorrenti
	private int queueInProgressSize; // numero di richieste attualmente in fase di processo

	public FcsHost(String host, int port, int queueMaxSize) {
		this.host = host;
		this.port = port;
		
		this.queueMaxSize = queueMaxSize;
		this.queueInProgressSize = 0;
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	public int getQueueMaxSize() {
		return queueMaxSize;
	}

	/**
	 * Setta il completamento dell'elaborazione sull'host corrente per uno specifico documento. Elimina l'identificativo del documento
	 * dalla lista contenente le lavorazioni correnti.
	 * @param docId
	 */
	public void setRequestCompleted(String docId) {
		if (docId != null && !docId.isEmpty()) {
			InProgressRequests.getInstance().removeRequest(docId);			
			decrementQueueInProgressSize();
		}
	}
	
	/**
	 * Restituisce il numero totale di elaborazioni che risultano attualmente in corso sull'host FCS
	 * @return
	 */
	public int getQueueInProgressSize() {
		return queueInProgressSize;
	}

	/**
	 * Incrementa di 1 il numero di elaborazioni in corso sull'host FCS
	 */
	public void incrementQueueInProgressSize() {
		this.queueInProgressSize++;
	}
	
	/**
	 * Decrementa di 1 il numero di elaborazioni in corso sull'host FCS
	 */
	public void decrementQueueInProgressSize() {
		if (this.queueInProgressSize > 0)
			this.queueInProgressSize--;
	}

	@Override
	public String toString() {
		return host + ":" + port + " [size: " + queueInProgressSize + "]";
	}

}
