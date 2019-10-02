# ABSTRACT FCA

## Descrizione

Definizione del servizio [FCA](https://github.com/agenziaentrateriscossione/docway-fca) (File Conversion Agent) per indicizzazione e conversione (in PDF) di documenti. 

Il progetto contiene l'intera logica di gestione del pool di host [FCS](https://github.com/agenziaentrateriscossione/docway-fcs) (selezione RoundRobin o tramite analisi della coda delle richieste). Per poter essere utilizzato necessita l'estensione della classe astratta _Fca_ con relativa implementazione dei metodi

- __public List<FcsRequest> loadFcsPendingRequests() throws Exception__: Recupero di tutti i documenti per i quali è richiesta l'attività di indicizzazione o conversione in PDF;
- __public void onRunException(Exception e)__: Metodo invocato in caso di catch di una eccezione bloccante (stop del servizio) su FCA;
- __public void onRunFinally()__: Metodo invocato sul finally dell'eccezione bloccante. Su questo metodo è possibile richiamare tutte le azioni da compiere prima dello stop del servizio.

__N.B.__: Per avviare il processo di elaborazione di FCA occorre invocare all'interno del _main()_ il metodo __run()__ della classe implementata che estende _Fca_.

### Esempio di estensione di Fca

```
public class DummyFca extends Fca {

  private static final Logger logger = LogManager.getLogger(DummyFca.class.getName());
	
  public static void main(String[] args) {
    try {
      DummyFca dummyFca = new DummyFca();
      dummyFca.run();
      if (logger.isInfoEnabled())
        logger.info("DummyFca.main(): shutdown...");
      System.exit(0);
    }
    catch(Exception e) {
      logger.error("DummyFca.main(): got exception... " + e.getMessage(), e);
      System.exit(1);
    }
  }
  
  public DummyFca() throws Exception {
    super();
    // TODO eventuale codice specifico dell'implementazione
  }

  @Override
  public List<FcsRequest> loadFcsPendingRequests() throws Exception {
    List<FcsRequest> reqs = new ArrayList<FcsRequest>();
    // TODO recupero dei documenti da elaborare
    return reqs;
  }

  @Override
  public void onRunException(Exception e) {
    // TODO da completare
  }

  @Override
  public void onRunFinally() {
    // TODO azioni da compiere prima della chiusura del servizio
  }
  
}

```


## Prerequisiti

1. _Java8_


## Configurazione

Per configurare l'FCA occorre settare le properties presenti all'interno del file _it.tredi.abstract-fca.properties_. 

Oltre alla definizione del pool di FCS e le modalità di selezione di un host FCS, all'interno del file sono definite diverse properties tramite le quali è possibile "istruire" gli host FCS (es. dimensione massima dei file da elaborare, eventuale limitazione alle estensioni supportate da FCS, timeout massimo di elaborazione, ecc.).

__N.B.__: Per maggiori informazioni sulla configurazione si rimanda ai commenti presenti nel file di properties.
