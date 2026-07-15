package pl.app.service;

import jakarta.persistence.EntityManager;
import pl.database.*;
import pl.database.enums.StatusFaktury;

import java.math.BigDecimal;

public class InvoiceService {
    Faktura issueInvoiceForRental(EntityManager em, Wypozyczenie w, int dni) {

        var cb = em.getCriteriaBuilder();
        var query = cb.createQuery(Long.class);
        var invoice = query.from(Faktura.class);

        query.select(cb.count(invoice));

        Long count = em.createQuery(query).getSingleResult();
        String numer = "FV-" + ((count == null ? 0 : count) + 1);

        BigDecimal netto = w.getPojazd().obliczKosztWypozyczenia(dni);

        Faktura f = new Faktura(numer, netto, w);
        f.setStatusFaktury(StatusFaktury.WYSTAWIONA);

        em.persist(f);
        return f;
    }
}
