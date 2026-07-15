package pl.app.service;

import pl.database.*;
import pl.database.enums.StatusPojazdu;
import pl.database.enums.StatusRezerwacji;
import pl.database.enums.TypZdarzenia;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class ReservationService {
    public List<Rezerwacja> listPending() {
        return DatabaseService.tx(em -> {
            var cb = em.getCriteriaBuilder();
            var query = cb.createQuery(Rezerwacja.class);
            var reservation = query.from(Rezerwacja.class);

            query.select(reservation)
                    .where(cb.equal(reservation.get("statusRezerwacji"), StatusRezerwacji.OCZEKUJE))
                    .orderBy(cb.asc(reservation.<LocalDateTime>get("dataUtworzenia")));

            return em.createQuery(query).getResultList();
        });
    }

    public Long createReservation(String klientPesel, String nrRej, LocalDate od, LocalDate doDaty) {
        if (od == null || doDaty == null) throw new IllegalArgumentException("Daty są wymagane");
        if (doDaty.isBefore(od)) throw new IllegalArgumentException("Data do nie może być przed datą od");

        String nr = nrRej.trim().toUpperCase();
        String pesel = klientPesel.trim();

        return DatabaseService.tx(em -> {
            Klient klient = em.find(Klient.class, pesel);
            if (klient == null) throw new IllegalArgumentException("Nie znaleziono klienta: " + pesel);

            Pojazd pojazd = findVehicleByRegistration(em, nr)
                    .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono pojazdu: " + nr));

            if (pojazd.getStatusPojazdu() != StatusPojazdu.DOSTEPNY)
                throw new IllegalStateException("Pojazd nie jest dostępny. Aktualny status: " + friendlyStatus(pojazd.getStatusPojazdu()));

            Long conflicts = countReservationConflicts(em, pojazd, od, doDaty);

            if (conflicts > 0)
                throw new IllegalStateException("Wybrany termin koliduje z inną rezerwacją pojazdu " + nr);

            Rezerwacja r = new Rezerwacja();
            r.setKlient(klient);
            r.setPojazd(pojazd);
            r.setDataOd(od);
            r.setDataDo(doDaty);
            r.setDataUtworzenia(LocalDateTime.now());
            r.setStatusRezerwacji(StatusRezerwacji.OCZEKUJE);

            em.persist(r);

            em.persist(new ZdarzeniePojazdu(LocalDate.now(), TypZdarzenia.REZERWACJA,
                    "Utworzono rezerwację (id=" + r.getId() + ")", pojazd));

            return r.getId();
        });
    }

    public void cancelReservation(Long id) {
        DatabaseService.tx(em -> {
            Rezerwacja r = em.find(Rezerwacja.class, id);
            if (r == null) throw new IllegalArgumentException("Nie znaleziono rezerwacji: " + id);

            r.setStatusRezerwacji(StatusRezerwacji.ANULOWANA);

            em.persist(new ZdarzeniePojazdu(LocalDate.now(), TypZdarzenia.REZERWACJA,
                    "Anulowano rezerwację ( id=" + r.getId() + ")", r.getPojazd()));

            return null;
        });
    }

    public void approveReservation(Long id, String pracownikPesel) {
        DatabaseService.tx(em -> {
            Rezerwacja r = em.find(Rezerwacja.class, id);
            if (r == null) throw new IllegalArgumentException("Nie znaleziono rezerwacji: " + id);

            Pracownik p = em.find(Pracownik.class, pracownikPesel.trim());
            if (p == null) throw new IllegalArgumentException("Nie znaleziono pracownika: " + pracownikPesel);

            r.setStatusRezerwacji(StatusRezerwacji.ZATWIERDZONA);
            r.setZatwierdzilPracownik(p);

            em.persist(new ZdarzeniePojazdu(LocalDate.now(), TypZdarzenia.REZERWACJA,
                    "Zatwierdzono rezerwację  (id=" + r.getId() + ")", r.getPojazd()));

            return null;
        });
    }

    private java.util.Optional<Pojazd> findVehicleByRegistration(jakarta.persistence.EntityManager em, String nrRej) {
        var cb = em.getCriteriaBuilder();
        var query = cb.createQuery(Pojazd.class);
        var vehicle = query.from(Pojazd.class);

        query.select(vehicle)
                .where(cb.equal(cb.upper(vehicle.<String>get("numerRejestracyjny")), nrRej.trim().toUpperCase()));

        return em.createQuery(query)
                .setMaxResults(1)
                .getResultStream()
                .findFirst();
    }

    private Long countReservationConflicts(jakarta.persistence.EntityManager em, Pojazd pojazd, LocalDate od, LocalDate doDaty) {
        var cb = em.getCriteriaBuilder();
        var query = cb.createQuery(Long.class);
        var reservation = query.from(Rezerwacja.class);

        query.select(cb.count(reservation))
                .where(
                        cb.equal(reservation.get("pojazd"), pojazd),
                        cb.notEqual(reservation.get("statusRezerwacji"), StatusRezerwacji.ANULOWANA),
                        cb.lessThanOrEqualTo(reservation.<LocalDate>get("dataOd"), doDaty),
                        cb.greaterThanOrEqualTo(reservation.<LocalDate>get("dataDo"), od)
                );

        return em.createQuery(query).getSingleResult();
    }

    private static String friendlyStatus(StatusPojazdu status) {
        return switch (status) {
            case DOSTEPNY -> "dostępny";
            case WYNAJETY -> "wynajęty";
            case W_SERWISIE -> "w serwisie";
            case WYCOFANY -> "wycofany";
        };
    }
}
