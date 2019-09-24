package it.tredi.fca.entity;

public class FcsRequest {
	
	public static final String CONVERSION_TO_PDF = "pdf";

	private String docId; // TODO l'identificativo del record da indicizzare/convertire potrebbe essere definito come object generico
	private String convTo;

	/**
	 * Eventuali parametri aggiuntivi da inviare all'host FCS per il completamento delle attivita' di indicizzazione
	 * e/o conversione di files
	 */
	private String additionalParameters;
	
	public FcsRequest(String docId) {
		this(docId, CONVERSION_TO_PDF);
	}
	
	public FcsRequest(String docId, String convTo) {
		this.docId = docId;
		this.convTo = convTo;
		if (this.convTo == null || this.convTo.isEmpty())
			this.convTo = CONVERSION_TO_PDF;
	}
	
	public String getConversionTo() {
		return convTo;
	}

	public String getDocId() {
		return docId;
	}
	
	public String getAdditionalParameters() {
		return (additionalParameters != null) ? additionalParameters : "";
	}

	public void setAdditionalParameters(String additionalParameters) {
		this.additionalParameters = additionalParameters;
	}
	
	@Override
	public String toString() {
		return "[ID = " + docId + ", convTo = " + convTo + "]";
	}
}
