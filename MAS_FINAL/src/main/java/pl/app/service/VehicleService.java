package pl.app.service;

import pl.database.DatabaseService;
import pl.database.Pojazd;
import pl.database.enums.StatusPojazdu;

import java.util.List;
import java.util.Optional;

public class VehicleService {
    public List<Pojazd> listAll() {
        return DatabaseService.tx(em -> {
            var cb = em.getCriteriaBuilder();
            var query = cb.createQuery(Pojazd.class);
            var pojazd = query.from(Pojazd.class);

            query.select(pojazd)
                    .orderBy(cb.asc(pojazd.get("numerRejestracyjny")));

            return em.createQuery(query).getResultList();
        });
    }

    public List<Pojazd> listAvailable() {
        return DatabaseService.tx(em -> {
            var cb = em.getCriteriaBuilder();
            var query = cb.createQuery(Pojazd.class);
            var pojazd = query.from(Pojazd.class);

            query.select(pojazd)
                    .where(cb.equal(pojazd.get("statusPojazdu"), StatusPojazdu.DOSTEPNY))
                    .orderBy(cb.asc(pojazd.get("numerRejestracyjny")));

            return em.createQuery(query).getResultList();
        });
    }

    public Optional<Pojazd> findByRegistration(String nrRej) {
        if (nrRej == null || nrRej.isBlank()) return Optional.empty();

        return DatabaseService.tx(em -> findByRegistration(em, nrRej));
    }

    public void changeStatus(String nrRej, StatusPojazdu status) {
        DatabaseService.tx(em -> {
            Pojazd p = findByRegistration(em, nrRej).orElseThrow();
            p.setStatusPojazdu(status);
            return null;
        });
    }

    public void removeFromFleet(String nrRej) {
        DatabaseService.tx(em -> {
            Pojazd p = findByRegistration(em, nrRej).orElseThrow();
            em.remove(p);
            return null;
        });
    }

    private Optional<Pojazd> findByRegistration(jakarta.persistence.EntityManager em, String nrRej) {
        var cb = em.getCriteriaBuilder();
        var query = cb.createQuery(Pojazd.class);
        var pojazd = query.from(Pojazd.class);
        String normalized = nrRej.trim().toLowerCase();

        query.select(pojazd)
                .where(cb.equal(cb.lower(pojazd.<String>get("numerRejestracyjny")), normalized));

        return em.createQuery(query)
                .setMaxResults(1)
                .getResultStream()
                .findFirst();
    }
}
