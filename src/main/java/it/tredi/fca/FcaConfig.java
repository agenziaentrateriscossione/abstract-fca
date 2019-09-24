package it.tredi.fca;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.tredi.fcs.socket.commands.entity.FcsActivationParams;
import it.tredi.utils.properties.PropertiesReader;

/**
 * Parametri di configurazione di FCA
 * @author mbernardini
 */
public class FcaConfig {

	private static final Logger logger = LogManager.getLogger(Fca.class.getName());

	private static final String PROPERTIES_FILE_NAME = "it.tredi.abstract-fca.properties";

	private static final String FCA_PRESENCE_PORT_PROPERTY = "fca.presence.port";
	private static final String FCA_WAITING_STEP_PROPERTY = "fca.waiting.step";
	private static final String FCA_REFRESH_DELAY_PROPERTY = "fca.refresh.delay";

	private static final String FCS_POOL_PROPERTY = "fcs.pool";
	private static final String FCS_SELECTION_MODE_PROPERTY = "fcs.selection.mode";
	private static final String FCS_ALIVE_TIMEOUT_PROPERTY = "fcs.alive.timeout";
	private static final String FCS_WORK_TIMEOUT_PROPERTY = "fcs.work.timeout";

	private static final String FCS_INDEX_ENABLED_PROPERTY = "fcs.index.enabled";
	private static final String FCS_INDEX_OCR_PROPERTY = "fcs.index.ocr";
	private static final String FCS_INDEX_OCR_FILE_TYPES_EXCLUDE_PROPERTY = "fcs.index.ocr.fileTypes.exclude";
	private static final String FCS_INDEX_MAX_FILE_SIZE_PROPERTY = "fcs.index.maxFileSize";
	private static final String FCS_INDEX_FILE_TYPES_INCLUDE_PROPERTY = "fcs.index.fileTypes.include";
	private static final String FCS_INDEX_FILE_TYPES_EXCLUDE_PROPERTY = "fcs.index.fileTypes.exclude";
	private static final String FCS_INDEX_MAX_CHARS_PROPERTY = "fcs.index.maxChars";

	private static final String FCS_CONVERT_ENABLED_PROPERTY = "fcs.convert.enabled";
	private static final String FCS_CONVERT_MAX_FILE_SIZE_PROPERTY = "fcs.convert.maxFileSize";
	private static final String FCS_CONVERT_FILE_TYPES_PROPERTY = "fcs.convert.fileTypes";

	private static final int FCA_PRESENCE_PORT_DEFAULT_VALUE = 0;
	private static final int FCA_WAITING_STEP_DEFAULT_VALUE = 200;
	private static final int FCA_REFRESH_DELAY_DEFAULT_VALUE = 20000;

	private static final int FCS_ALIVE_TIMEOUT_DEFAULT_VALUE = 2000;
	private static final int FCS_WORK_TIMEOUT_DEFAULT_VALUE = 0;

	private static final FcsSelectionMode FCS_SELECTION_MODE_DEFAULT_VALUE = FcsSelectionMode.QUEUE_SIZE;

	private int fcaPresencePort = FCA_PRESENCE_PORT_DEFAULT_VALUE;
	private int fcaWaitingStep = FCA_WAITING_STEP_DEFAULT_VALUE;
	private int fcaRefreshDelay = FCA_REFRESH_DELAY_DEFAULT_VALUE;

	private List<FcsHost> fcsPool = new ArrayList<FcsHost>();
	private FcsSelectionMode fcsSelectionMode = FCS_SELECTION_MODE_DEFAULT_VALUE;
	private int fcsAliveTimeout = FCS_ALIVE_TIMEOUT_DEFAULT_VALUE;
	private int fcsWorkTimeout = FCS_WORK_TIMEOUT_DEFAULT_VALUE;

	private FcsActivationParams fcsConfig = null;

	// Singleton
    private static FcaConfig instance = null;

    /**
     * Costruttore privato
     */
    private FcaConfig() throws Exception {
    	PropertiesReader propertiesReader = new PropertiesReader(PROPERTIES_FILE_NAME);

    	this.fcaPresencePort = propertiesReader.getIntProperty(FCA_PRESENCE_PORT_PROPERTY, FCA_PRESENCE_PORT_DEFAULT_VALUE);
    	this.fcaWaitingStep = propertiesReader.getIntProperty(FCA_WAITING_STEP_PROPERTY, FCA_WAITING_STEP_DEFAULT_VALUE);
    	if (this.fcaWaitingStep < 0) {
    		logger.warn("FcaConfig: value " + this.fcaWaitingStep + " NOT valid for property " + FCA_WAITING_STEP_PROPERTY + ". Assign default value " + FCA_WAITING_STEP_DEFAULT_VALUE);
    		this.fcaWaitingStep = FCA_WAITING_STEP_DEFAULT_VALUE;
    	}
    	this.fcaRefreshDelay = propertiesReader.getIntProperty(FCA_REFRESH_DELAY_PROPERTY, FCA_REFRESH_DELAY_DEFAULT_VALUE);
    	if (this.fcaRefreshDelay < 0) {
    		logger.warn("FcaConfig: value " + this.fcaRefreshDelay + " NOT valid for property " + FCA_REFRESH_DELAY_PROPERTY + ". Assign default value " + FCA_REFRESH_DELAY_DEFAULT_VALUE);
    		this.fcaRefreshDelay = FCA_REFRESH_DELAY_DEFAULT_VALUE;
    	}

    	this.fcsAliveTimeout = propertiesReader.getIntProperty(FCS_ALIVE_TIMEOUT_PROPERTY, FCS_ALIVE_TIMEOUT_DEFAULT_VALUE);
    	this.fcsWorkTimeout = propertiesReader.getIntProperty(FCS_WORK_TIMEOUT_PROPERTY, FCS_WORK_TIMEOUT_DEFAULT_VALUE);

    	String[] strFcsPool = propertiesReader.getProperty(FCS_POOL_PROPERTY, "").split(",");
    	if (strFcsPool != null && strFcsPool.length > 0) {
    		if (this.fcsPool == null)
    			this.fcsPool = new ArrayList<FcsHost>();
    		for (int i=0; i<strFcsPool.length; i++) {
    			String[] fcs = strFcsPool[i].split(":");
    			if (fcs != null && fcs.length == 3) {
    				try {
    					this.fcsPool.add(new FcsHost(fcs[0], Integer.parseInt(fcs[1]), Integer.parseInt(fcs[2])));
    				}
    				catch(Exception e) {
    					logger.error("FcaConfig: got exception on fcs pool building... " + e.getMessage(), e);
    					throw e;
    				}
    			}
    		}
    	}

    	String strFcsSelectionMode = propertiesReader.getProperty(FCS_SELECTION_MODE_PROPERTY, null);
    	if (strFcsSelectionMode != null && strFcsSelectionMode.toLowerCase().equals("roundrobin"))
    		this.fcsSelectionMode = FcsSelectionMode.ROUNDROBIN;
    	else
    		this.fcsSelectionMode = FcsSelectionMode.QUEUE_SIZE;

    	this.fcsAliveTimeout = propertiesReader.getIntProperty(FCS_ALIVE_TIMEOUT_PROPERTY, FCS_ALIVE_TIMEOUT_DEFAULT_VALUE);
    	if (this.fcsAliveTimeout <= 0) {
    		logger.warn("FcaConfig: value " + this.fcsAliveTimeout + " NOT valid for property " + FCS_ALIVE_TIMEOUT_PROPERTY + ". Assign default value " + FCS_ALIVE_TIMEOUT_DEFAULT_VALUE);
    		this.fcsAliveTimeout = FCS_ALIVE_TIMEOUT_DEFAULT_VALUE;
    	}

    	this.fcsConfig = new FcsActivationParams();
    	this.fcsConfig.setWorkTimeout(this.fcsWorkTimeout);
    	this.fcsConfig.setIndexEnabled(propertiesReader.getBooleanProperty(FCS_INDEX_ENABLED_PROPERTY, true));
    	this.fcsConfig.setOcrEnabled(propertiesReader.getBooleanProperty(FCS_INDEX_OCR_PROPERTY, true));

    	String ocrFileTypesExclude = propertiesReader.getProperty(FCS_INDEX_OCR_FILE_TYPES_EXCLUDE_PROPERTY, "");
    	if (!ocrFileTypesExclude.isEmpty())
    		this.fcsConfig.setOcrFileTypesExclude(Arrays.asList(ocrFileTypesExclude.toLowerCase().split(",")));

    	this.fcsConfig.setIndexMaxFileSize(propertiesReader.getLongProperty(FCS_INDEX_MAX_FILE_SIZE_PROPERTY, 0));
    	String indexFileTypesInclude = propertiesReader.getProperty(FCS_INDEX_FILE_TYPES_INCLUDE_PROPERTY, "");
    	if (!indexFileTypesInclude.isEmpty())
    		this.fcsConfig.setIndexFileTypesInclude(Arrays.asList(indexFileTypesInclude.toLowerCase().split(",")));
    	String indexFileTypesExclude = propertiesReader.getProperty(FCS_INDEX_FILE_TYPES_EXCLUDE_PROPERTY, "");
    	if (!indexFileTypesExclude.isEmpty())
    		this.fcsConfig.setIndexFileTypesExclude(Arrays.asList(indexFileTypesExclude.toLowerCase().split(",")));
    	this.fcsConfig.setIndexMaxChars(propertiesReader.getIntProperty(FCS_INDEX_MAX_CHARS_PROPERTY, -1));
    	this.fcsConfig.setConvertEnabled(propertiesReader.getBooleanProperty(FCS_CONVERT_ENABLED_PROPERTY, true));
    	this.fcsConfig.setConvertMaxFileSize(propertiesReader.getLongProperty(FCS_CONVERT_MAX_FILE_SIZE_PROPERTY, 0));
    	String convertFileTypes = propertiesReader.getProperty(FCS_CONVERT_FILE_TYPES_PROPERTY, "");
    	if (!convertFileTypes.isEmpty())
    		this.fcsConfig.setConvertFileTypes(Arrays.asList(convertFileTypes.toLowerCase().split(",")));

    	if (logger.isDebugEnabled()) {
    		logger.debug("------------------- FCA CONFIGURATION PARAMETERS -------------------");
    		logger.debug(FCA_PRESENCE_PORT_PROPERTY + " = " + this.fcaPresencePort);
    		logger.debug(FCA_WAITING_STEP_PROPERTY + " = " + this.fcaWaitingStep);
    		logger.debug(FCA_REFRESH_DELAY_PROPERTY + " = " + this.fcaRefreshDelay);

    		logger.debug(FCS_POOL_PROPERTY + " = " + String.join(", ", strFcsPool));
    		logger.debug(FCS_SELECTION_MODE_PROPERTY + " = " + this.fcsSelectionMode);
    		logger.debug(FCS_ALIVE_TIMEOUT_PROPERTY + " = " + this.fcsAliveTimeout);
    		logger.debug(FCS_WORK_TIMEOUT_PROPERTY + " = " + this.fcsWorkTimeout);

    		logger.debug(FCS_INDEX_ENABLED_PROPERTY + " = " + this.fcsConfig.isIndexEnabled());
    		logger.debug(FCS_INDEX_OCR_PROPERTY + " = " + this.fcsConfig.isOcrEnabled());
    		logger.debug(FCS_INDEX_OCR_FILE_TYPES_EXCLUDE_PROPERTY + " = " + String.join(", ", this.fcsConfig.getOcrFileTypesExclude()));
    		logger.debug(FCS_INDEX_MAX_FILE_SIZE_PROPERTY + " = " + this.fcsConfig.getIndexMaxFileSize());
    		logger.debug(FCS_INDEX_FILE_TYPES_INCLUDE_PROPERTY + " = " + String.join(", ", this.fcsConfig.getIndexFileTypesInclude()));
    		logger.debug(FCS_INDEX_FILE_TYPES_EXCLUDE_PROPERTY + " = " + String.join(", ", this.fcsConfig.getIndexFileTypesExclude()));
    		logger.debug(FCS_INDEX_MAX_CHARS_PROPERTY + " = " + this.fcsConfig.getIndexMaxChars());

    		logger.debug(FCS_CONVERT_ENABLED_PROPERTY + " = " + this.fcsConfig.isConvertEnabled());
    		logger.debug(FCS_CONVERT_MAX_FILE_SIZE_PROPERTY + " = " + this.fcsConfig.getConvertMaxFileSize());
    		logger.debug(FCS_CONVERT_FILE_TYPES_PROPERTY + " = " + String.join(", ", this.fcsConfig.getConvertFileTypes()));
    	}
    }

    /**
     * Ritorna l'oggetto contenente tutte le configurazioni di FCA (e parametri di indicizzazione/conversione da inviare agli host FCS)
	 * @return
	 */
	public static FcaConfig getInstance() throws Exception {
		if (instance == null) {
			synchronized (FcaConfig.class) {
				if (instance == null) {
					if (logger.isInfoEnabled())
						logger.info("FcaConfig instance is null... create one");
					instance = new FcaConfig();
				}
			}
		}
		return instance;
	}

	public int getFcaPresencePort() {
		return fcaPresencePort;
	}

	public int getFcaWaitingStep() {
		return fcaWaitingStep;
	}

	public int getFcaRefreshDelay() {
		return fcaRefreshDelay;
	}

	public List<FcsHost> getFcsPool() {
		return fcsPool;
	}

	public FcsSelectionMode getFcsSelectionMode() {
		return fcsSelectionMode;
	}

	public int getFcsAliveTimeout() {
		return fcsAliveTimeout;
	}

	public int getFcsWorkTimeout() {
		return fcsWorkTimeout;
	}

	public FcsActivationParams getFcsConfig() {
		return fcsConfig;
	}

	/**
	 * Restituisce la configurazione di FCS (parametri di attivazione di FCS definiti sul file di properties di FCA) in formato JSON
	 * @return
	 * @throws Exception
	 */
	public String getJsonFcsConfig() throws Exception {
		String json = null;
		if (fcsConfig != null)
			json = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(fcsConfig);
		return json;
	}

}
