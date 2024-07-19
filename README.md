### Progetto Programmazione III / LAB.
## _**GESTIONE ORDINI RISTORANTE**_
---

## Componenti:

# Biancarosa Pasquale (2098)
# Militerno Eugenio (2454)
# Ruotolo Pasquale (2322)

---

### Link Presentazione [CANVA](https://www.canva.com/design/DAGGsYUxV4U/bw9aB1-Ws4m5a5DG0E8pKA/view?utm_content=DAGGsYUxV4U&utm_campaign=designshare&utm_medium=link&utm_source=editor)

---
## DESCRIZIONE:
Il progetto richiedeva di gestire gli *ordini di un ristorante*.
 * Il _cliente_ si siede al tavolo libero, e procede ad ordinare **all'arrivo del cameriere**.
 * Una volta che l'ordine è pronto, il cameriere lo porta al tavolo, e lo contrassegna come **consegnato**
 * Il cliente può quindi consumare l'ordine, e quando finisce di ordinare e mangiare, procede al _pagamento_, che viene eseguito **dall'operatore in cassa**
 * Una volta che il cliente paga (in contanti, carta o bancomat), l'ordine è contrassegnato come **completato**, e viene rimosso dalla lista di ordini, e viene generato lo scontrino.
---
 * Il ristorante prevede **N** tavoli, ciascuno dei quali può assumere 3 stati:
     * Libero : Al tavolo non sono sedute persone
     * Occupato : Al tavolo sono seduti clienti, che stanno ordinando
     * Pagamento : Il tavolo è ancora occupato, ma i clienti stanno pagando e usciranno dal locale, liberandolo.
 * Il ristorante impiega **M** camerieri, che si occupano di consegnare gli ordini ai tavoli, e segnare lo stato di ciascun ordine, e vi è un **Admin**, che può gestire il menù e far effettuare il pagamento.
 
