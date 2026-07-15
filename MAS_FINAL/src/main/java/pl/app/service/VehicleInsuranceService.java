package pl.app.service;

import jakarta.persistence.criteria.JoinType;
import pl.database.DatabaseService;
import pl.database.Pojazd;
import pl.database.PojazdUbezpieczenie;
import pl.database.Ubezpieczenie;
import pl.database.enums.StatusUbezpieczenia;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class VehicleInsuranceService {
    private static String normalizeNotes(String notes) {
        if (notes == null) return null;
        String n = notes.trim();
        return n.isBlank() ? null : n;
    }

    public List<Pojazd> listAllVehicles() {
        return DatabaseService.tx(em -> {
            var cb = em.getCriteriaBuilder();
            var query = cb.createQuery(Pojazd.class);
            var pojazd = query.from(Pojazd.class);
            var assignments = pojazd.fetch("przypisaniaUbezpieczen", JoinType.LEFT);
            assignments.fetch("ubezpieczenie", JoinType.LEFT);

            query.select(pojazd)
                    .distinct(true)
                    .orderBy(cb.asc(pojazd.get("numerRejestracyjny")));

            return em.createQuery(query).getResultList();
        });
    }

    public Optional<Pojazd> findVehicleWithInsuranceAssignments(Long vehicleId) {
        Objects.requireNonNull(vehicleId);

        return DatabaseService.tx(em -> {
            var cb = em.getCriteriaBuilder();
            var query = cb.createQuery(Pojazd.class);
            var pojazd = query.from(Pojazd.class);
            var assignments = pojazd.fetch("przypisaniaUbezpieczen", JoinType.LEFT);
            assignments.fetch("ubezpieczenie", JoinType.LEFT);

            query.select(pojazd)
                    .distinct(true)
                    .where(cb.equal(pojazd.get("id"), vehicleId));

            return em.createQuery(query)
                    .getResultStream()
                    .findFirst();
        });
    }

    public List<Ubezpieczenie> listAvailableActiveInsurances() {
        return DatabaseService.tx(em -> {
            var cb = em.getCriteriaBuilder();
            var query = cb.createQuery(Ubezpieczenie.class);
            var insurance = query.from(Ubezpieczenie.class);
            var assigned = query.subquery(Integer.class);
            var assignment = assigned.from(PojazdUbezpieczenie.class);

            assigned.select(cb.literal(1))
                    .where(
                            cb.equal(assignment.get("ubezpieczenie"), insurance),
                            cb.isFalse(assignment.<Boolean>get("usuniete"))
                    );

            query.select(insurance)
                    .where(
                            cb.equal(insurance.get("statusUbezpieczenia"), StatusUbezpieczenia.AKTYWNE),
                            cb.not(cb.exists(assigned))
                    )
                    .orderBy(
                            cb.asc(insurance.<String>get("firma")),
                            cb.asc(insurance.<String>get("numerPolisy"))
                    );

            return em.createQuery(query).getResultList();
        });
    }

    public Ubezpieczenie createInsurance(String numerPolisy,
                                         String firma,
                                         String opis,
                                         BigDecimal cena,
                                         LocalDate dataRozpoczecia,
                                         LocalDate dataZakonczenia,
                                         StatusUbezpieczenia status) {

        String nr = numerPolisy == null ? "" : numerPolisy.trim();
        String f = firma == null ? "" : firma.trim();
        String o = opis == null ? "" : opis.trim();

        if (nr.isBlank()) throw new IllegalArgumentException("Numer polisy jest wymagany");
        if (f.isBlank()) throw new IllegalArgumentException("Firma jest wymagana");
        if (o.isBlank()) throw new IllegalArgumentException("Opis jest wymagany");
        if (dataRozpoczecia == null || dataZakonczenia == null || dataZakonczenia.isBefore(dataRozpoczecia)) {
            throw new IllegalArgumentException("Niepoprawny zakres dat");
        }
        if (cena == null || cena.signum() < 0) {
            throw new IllegalArgumentException("Cena nie może być ujemna");
        }
        StatusUbezpieczenia st = status == null ? StatusUbezpieczenia.AKTYWNE : status;

        return DatabaseService.tx(em -> {
            if (insuranceNumberExists(em, nr)) {
                throw new IllegalArgumentException("Ubezpieczenie o takim numerze polisy już istnieje");
            }

            Ubezpieczenie u = new Ubezpieczenie(nr, f, o, cena, dataRozpoczecia, dataZakonczenia);
            u.setStatusUbezpieczenia(st);
            em.persist(u);
            return u;
        });
    }

    public void addInsuranceToVehicle(long vehicleId,
                                      long insuranceId,
                                      LocalDate dataOd,
                                      LocalDate dataDo,
                                      String uwagi) {

        if (dataOd == null || dataDo == null || dataDo.isBefore(dataOd)) {
            throw new IllegalArgumentException("Niepoprawny zakres dat");
        }

        DatabaseService.tx(em -> {
            Pojazd vehicle = em.find(Pojazd.class, vehicleId);
            if (vehicle == null) throw new IllegalArgumentException("Nie znaleziono pojazdu");

            Ubezpieczenie insurance = em.find(Ubezpieczenie.class, insuranceId);
            if (insurance == null) throw new IllegalArgumentException("Nie znaleziono ubezpieczenia");
            if (insurance.getStatusUbezpieczenia() != StatusUbezpieczenia.AKTYWNE) {
                throw new IllegalArgumentException("Wybrane ubezpieczenie nie jest aktywne");
            }

            if (insurance.getDataRozpoczecia() != null && dataOd.isBefore(insurance.getDataRozpoczecia())) {
                throw new IllegalArgumentException("Data od jest wcześniejsza niż początek ważności polisy");
            }
            if (insurance.getDataZakonczenia() != null && dataDo.isAfter(insurance.getDataZakonczenia())) {
                throw new IllegalArgumentException("Data do wykracza poza ważność polisy");
            }

            if (isInsuranceAssigned(em, insuranceId)) {
                throw new IllegalArgumentException("Wybrane ubezpieczenie jest już przypisane do innego pojazdu");
            }

            PojazdUbezpieczenie assignment = new PojazdUbezpieczenie(
                    vehicle, insurance, dataOd, dataDo, normalizeNotes(uwagi)
            );
            vehicle.dodajPrzypisanieUbezpieczenia(assignment);
            em.persist(assignment);

            return null;
        });
    }

    public void removeInsuranceFromVehicle(long assignmentId) {
        DatabaseService.tx(em -> {
            PojazdUbezpieczenie a = em.find(PojazdUbezpieczenie.class, assignmentId);
            if (a == null) throw new IllegalArgumentException("Nie znaleziono przypisania ubezpieczenia");
            if (a.isUsuniete()) return null;

            a.setUsuniete(true);

            String old = a.getUwagi() == null ? "" : a.getUwagi().trim();
            String marker = "[USUNIĘTO " + LocalDate.now() + "]";
            a.setUwagi(old.isBlank() ? marker : (old + "\n" + marker));

            try {
                var opis = "Usunięto ubezpieczenie (polisa: " + a.getUbezpieczenie().getNumerPolisy() + ")";
                var zd = new pl.database.ZdarzeniePojazdu(
                        LocalDate.now(),
                        pl.database.enums.TypZdarzenia.UBEZPIECZENIE,
                        opis,
                        a.getPojazd()
                );
                em.persist(zd);
                a.getPojazd().dodajZdarzenie(zd);
            } catch (Exception ignored) {}

            return null;
        });
    }

    private boolean insuranceNumberExists(jakarta.persistence.EntityManager em, String policyNumber) {
        var cb = em.getCriteriaBuilder();
        var query = cb.createQuery(Long.class);
        var insurance = query.from(Ubezpieczenie.class);

        query.select(cb.count(insurance))
                .where(cb.equal(insurance.get("numerPolisy"), policyNumber));

        Long count = em.createQuery(query).getSingleResult();
        return count != null && count > 0;
    }

    private boolean isInsuranceAssigned(jakarta.persistence.EntityManager em, long insuranceId) {
        var cb = em.getCriteriaBuilder();
        var query = cb.createQuery(Long.class);
        var assignment = query.from(PojazdUbezpieczenie.class);

        query.select(cb.count(assignment))
                .where(
                        cb.equal(assignment.get("ubezpieczenie").get("id"), insuranceId),
                        cb.isFalse(assignment.<Boolean>get("usuniete"))
                );

        Long count = em.createQuery(query).getSingleResult();
        return count != null && count > 0;
    }
}
