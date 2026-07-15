package pl.app.service;

import pl.database.*;
import pl.database.enums.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class RentalService {
    private final InvoiceService invoiceService = new InvoiceService();

    public Long startRentalFromReservation(Long idRezerwacji, LocalDate dataWyp, LocalDate planowanyZwrot) {
        return DatabaseService.tx(em -> {
            Rezerwacja r = em.find(Rezerwacja.class, idRezerwacji);
            if (r == null) throw new IllegalArgumentException("Nie znaleziono rezerwacji: " + idRezerwacji);

            if (r.getStatusRezerwacji() != StatusRezerwacji.ZATWIERDZONA)
                throw new IllegalStateException("Rezerwacja musi być zatwierdzona");

            Pojazd pojazd = r.getPojazd();
            if (pojazd.getStatusPojazdu() != StatusPojazdu.DOSTEPNY)
                throw new IllegalStateException("Pojazd nie jest dostępny");

            Wypozyczenie w = new Wypozyczenie();
            w.setKlient(r.getKlient());
            w.setPojazd(pojazd);
            w.setDataWypozyczenia(dataWyp);
            w.setPlanowanaDataZwrotu(planowanyZwrot);
            w.setStatusWypozyczenia(StatusWypozyczenia.AKTYWNE);

            w.dodajRezerwacje(r);

            em.persist(w);

            pojazd.setStatusPojazdu(StatusPojazdu.WYNAJETY);

            em.persist(new ZdarzeniePojazdu(LocalDate.now(), TypZdarzenia.WYPOZYCZENIE,
                    "Rozpoczęto wypożyczenie (id=" + w.getId() + ")", pojazd));

            return w.getId();
        });
    }

    public void finishRental(Long idWyp, LocalDate zwrot) {
        DatabaseService.tx(em -> {
            Wypozyczenie w = em.find(Wypozyczenie.class, idWyp);
            if (w == null) throw new IllegalArgumentException("Nie znaleziono wypożyczenia: " + idWyp);

            w.setRzeczywistaDataZwrotu(zwrot);

            boolean opoznienie = zwrot.isAfter(w.getPlanowanaDataZwrotu());
            w.setStatusWypozyczenia(opoznienie ? StatusWypozyczenia.OPOZNIENIE : StatusWypozyczenia.ZAKONCZONE);

            Pojazd pojazd = w.getPojazd();
            pojazd.setStatusPojazdu(StatusPojazdu.DOSTEPNY);

            em.persist(new ZdarzeniePojazdu(LocalDate.now(), TypZdarzenia.ZWROT,
                    "Zakończono wypożyczenie (id=" + w.getId() + ")", pojazd));

            if (w.getFaktura() == null) {
                int dni = (int) (ChronoUnit.DAYS.between(w.getDataWypozyczenia(), zwrot) + 1);
                Faktura f = invoiceService.issueInvoiceForRental(em, w, dni);
                w.setFaktura(f);
            }

            return null;
        });
    }
}
