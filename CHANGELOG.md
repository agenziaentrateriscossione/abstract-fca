# Change Log

## [6.0.3] - 2018-12-21

### Changed
- Aggiunti metodi abstract per il recupero della versione e data di rilascio dell'applicazione (scrittura su log della versione dell'implementazione di FCA)

### Fixed
- Corretto il controllo su porta socket (verifica su altra istanza di FCA gia' in esecuzione)

## [6.0.2] - 2018-02-08

### Fixed
- Corretta configurazione log4j2 per elminazione di vecchie copie dei file di log

## [6.0.1] - 2017-09-22

### Fixed
- Corretto bug in caricamento dei parametri di attivazione (lettura sempre da singleton di configurazione)
- Chiamata a isAliveFcsHost ad ogni nuova richiesta in fase di individuazione dell'host FCS da chiamare (problemi in caso di riavvio di un host FCS)

## [6.0.0] - 2017-09-05

### Added
- Ridefinizione di FCA (File Conversion Agent) per indicizzazione e conversione (in PDF) di documenti
- Implementazione indipendente dalla sorgente dati
- Definizione di una classe astratta FCA che deve essere implementata per esporre i servizi di indicizzazione (e conversione in PDF) di files per una specifica applicazione 
