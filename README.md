### Progetto Programmazione III / LAB.
## _**GESTIONE ORDINI RISTORANTE**_
---

## *Componenti*:

# Biancarosa Pasquale (2098)
# Militerno Eugenio (2454)
# Ruotolo Pasquale (2322)

---

### Link Presentazione [CANVA](https://www.canva.com/design/DAGGsYUxV4U/bw9aB1-Ws4m5a5DG0E8pKA/view?utm_content=DAGGsYUxV4U&utm_campaign=designshare&utm_medium=link&utm_source=editor)

---

## **DESCRIZIONE**:

Il progetto richiedeva di gestire gli *ordini di un ristorante*.
 * Il _cliente_ si siede al tavolo libero, e procede ad ordinare **all'arrivo del cameriere**.
 * Una volta che l'ordine è pronto, il cameriere lo porta al tavolo, e lo contrassegna come **consegnato**
 * Il cliente può quindi consumare l'ordine, e quando finisce di ordinare e mangiare, procede al _pagamento_, che viene eseguito **dall'operatore in cassa**
 * Una volta che il cliente paga (in contanti, carta o bancomat), l'ordine è contrassegnato come **completato**, e viene rimosso dalla lista di ordini, e viene generato lo scontrino.
---
 * Il ristorante prevede **M** tavoli, ciascuno dei quali può assumere 3 stati:
     * Libero : Al tavolo non sono sedute persone
     * Occupato : Al tavolo sono seduti clienti, che stanno ordinando
     * Pagamento : Il tavolo è ancora occupato, ma i clienti stanno pagando e usciranno dal locale, liberandolo.
 * Il ristorante impiega **N** camerieri, che si occupano di consegnare gli ordini ai tavoli, e segnare lo stato di ciascun ordine, e vi è un **Admin**, che può gestire il menù e far effettuare il pagamento.
---

## **IMPLEMENTAZIONE**

_Il progetto prevede due tipologie di accesso_:
    * **CAMERIERE** : 
        - Prende le ordinazioni dai clienti
        - Annulla l'ultima ordinazione effettuata
        - Consegnare l'ordine al cliente
    * **ADMIN** :
        - Inserire o cambiare un piatto o bibita nel menù
        - Procedere al pagamento del cliente in base al codice del tavolo

Il database è stato creato usando MySQL, e viene hostato su un DB SQL in localhost (*inizialmente su RDS di Amazon AWS*). Il **frontend** è stato fatto usando *JavaFX*, mentre il **backend** è stato sviluppato in *Java*.
L'ordinazione viene effettuata scegliendo un piatto dal menù, *potendo inserire modifiche tramite **note** al piatto*.

---

# PATTERN:

Ciascun utente del DB è contrassegnato da una flag *Waiter* o *Admin*, e ha il proprio accesso.
Durante tutta l'implementazione del progetto, sono stati usati *sei* Design Patterns, ed è stata seguita la logica dei principi **SOLID**

    * Strategy : Permette di effettuare l'azione richiesta facendo la scelta più opportuna (*Payment*, *Login*)
    * Decorator : Permette di aggiungere funzionalità agli oggetti senza modificarne la struttura (*Order*)
    * Observer : Relazione 1:N fra oggetti in modo da notificare i cambiamenti di stato di un oggetto (*Order*, *Waiter*)
    * Factory : Interfaccia che permette di creare oggetti in una superclasse, ma le sottoclassi possono alterarne il tipo (*Connection*)
    * DAO : Astrazione per l'accesso ai dati, separando Buiseness Logic e Dati (*CompleteOrder*)
    * Facade : Fornisce un'interfaccia semplificata ad un insieme complesso di classi, librerie o framework (*Connection*)

Inoltre, sono stati usati i _**record**_, per gestire oggetti immutabili, possedendo *componenti* implicitamente dichiarati **final**, ed avere accesso rapido ai dati.

Usati per evitare *boilerplate code*, ovvero codice ripetuto molte volte senza modifiche; i record permettono di evitare di ridefinire lo stesso codice varie volte, facendo direttamente riferimento ad una struttura dati immutabile.

---
