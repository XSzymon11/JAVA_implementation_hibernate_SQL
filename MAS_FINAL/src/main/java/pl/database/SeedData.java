package pl.database;

import jakarta.persistence.EntityManager;
import pl.auth.PasswordHasher;
import pl.database.enums.StatusFaktury;
import pl.database.enums.StatusPojazdu;
import pl.database.enums.StatusRezerwacji;
import pl.database.enums.StatusUbezpieczenia;
import pl.database.enums.StatusWypozyczenia;
import pl.database.enums.StatusZgloszenia;
import pl.database.enums.TypNadwozia;
import pl.database.enums.TypUmowy;
import pl.database.enums.TypZdarzenia;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class SeedData {
    public static void seed(EntityManager em) {
        LocalDate today = LocalDate.now();

        Osoba adminOs = getOrCreateOsoba(em, "00000000000", "Admin", "Główny", "000000000", "admin@karent.pl");
        adminOs.setLogin(ensureUniqueLogin(em, "admin", adminOs.getPesel()));
        adminOs.setHaslo(PasswordHasher.hash("admin"));

        Osoba prac1Os = getOrCreateOsoba(em, "11111111111", "Pracownik", "Pracowniczy", "9999998997", "jan@karent.pl");
        prac1Os.setLogin(ensureUniqueLogin(em, "pracownik", prac1Os.getPesel()));
        prac1Os.setHaslo(PasswordHasher.hash("pracownik"));
        Pracownik prac1 = getOrCreatePracownik(em, prac1Os, "Kierownik zmiany");
        prac1.setDataZatrudnienia(today.minusYears(4));
        Zatrudnienie zatr1 = getOrCreateZatrudnienie(em, prac1);
        zatr1.setTypUmowy(TypUmowy.UOP);
        zatr1.setDataZawarcia(today.minusYears(4));
        zatr1.setOkres(48);
        zatr1.setWynagrodzenieNetto(bd(7300));

        Osoba prac2Os = getOrCreateOsoba(em, "22222222222", "Anna", "Nowak", "888777666", "anna@karent.pl");
        prac2Os.setLogin(ensureUniqueLogin(em, "anna", prac2Os.getPesel()));
        prac2Os.setHaslo(PasswordHasher.hash("anna"));
        Pracownik prac2 = getOrCreatePracownik(em, prac2Os, "Specjalista ds floty");
        prac2.setDataZatrudnienia(today.minusYears(2));
        Zatrudnienie zatr2 = getOrCreateZatrudnienie(em, prac2);
        zatr2.setTypUmowy(TypUmowy.B2B);
        zatr2.setDataZawarcia(today.minusYears(2));
        zatr2.setOkres(24);
        zatr2.setWynagrodzenieNetto(bd(8900));

        getOrCreateKlient(em, prac1Os);

        Klient[] klienci = new Klient[10];
        for (int i = 0; i < 10; i++) {
            String pesel = String.format("500000000%02d", i);

            String imie = switch (i) {
                case 0 -> "Piotr";
                case 1 -> "Katarzyna";
                case 2 -> "Michał";
                case 3 -> "Agnieszka";
                case 4 -> "Tomasz";
                case 5 -> "Monika";
                case 6 -> "Paweł";
                case 7 -> "Julia";
                case 8 -> "Marcin";
                default -> "Natalia";
            };
            String nazwisko = switch (i) {
                case 0 -> "Zieliński";
                case 1 -> "Wiśniewska";
                case 2 -> "Wójcik";
                case 3 -> "Kowalczyk";
                case 4 -> "Kamiński";
                case 5 -> "Lewandowska";
                case 6 -> "Dąbrowski";
                case 7 -> "Król";
                case 8 -> "Szymański";
                default -> "Nowicka";
            };

            Osoba os = getOrCreateOsoba(
                    em,
                    pesel,
                    imie,
                    nazwisko,
                    String.format("6007007%02d", i),
                    (i % 2 == 0) ? ("klient" + (i + 1) + "@karent.pl") : null
            );

            String login = "klient" + (i + 1);
            os.setLogin(ensureUniqueLogin(em, login, os.getPesel()));
            os.setHaslo(PasswordHasher.hash(login));

            Klient k = getOrCreateKlient(em, os);
            klienci[i] = k;
        }

        Osobowy corolla = ensureOsobowy(em, "WA1234A", "Toyota", "Corolla", 2022,
                45000, bd(190.00), today.minusMonths(2), 5, TypNadwozia.SEDAN, true, StatusPojazdu.DOSTEPNY);

        Osobowy octavia = ensureOsobowy(em, "WW1111B", "Skoda", "Octavia", 2019,
                98000, bd(160.00), today.minusMonths(8), 5, TypNadwozia.KOMBI, true, StatusPojazdu.DOSTEPNY);

        Osobowy golf = ensureOsobowy(em, "PO2222C", "Volkswagen", "Golf", 2021,
                52000, bd(175.00), today.minusMonths(4), 5, TypNadwozia.HATCHBACK, true, StatusPojazdu.WYNAJETY);

        Osobowy x5 = ensureOsobowy(em, "WA3333D", "BMW", "X5", 2023,
                31000, bd(320.00), today.minusMonths(1), 5, TypNadwozia.SUV, true, StatusPojazdu.DOSTEPNY);

        ensureOsobowy(em, "KR4444E", "Audi", "A4", 2020,
                76000, bd(210.00), today.minusMonths(10), 5, TypNadwozia.SEDAN, true, StatusPojazdu.DOSTEPNY);

        ensureOsobowy(em, "LU5555F", "Toyota", "Yaris", 2018,
                112000, bd(120.00), today.minusMonths(7), 5, TypNadwozia.HATCHBACK, false, StatusPojazdu.WYCOFANY);

        ensureOsobowy(em, "WR6666G", "Opel", "Insignia", 2022,
                40000, bd(185.00), today.minusMonths(3), 5, TypNadwozia.SEDAN, true, StatusPojazdu.DOSTEPNY);

        Osobowy sportage = ensureOsobowy(em, "GDA7777H", "Kia", "Sportage", 2021,
                69000, bd(220.00), today.minusMonths(5), 5, TypNadwozia.SUV, true, StatusPojazdu.W_SERWISIE);

        Dostawczy sprinter = ensureDostawczy(em, "KR9999K", "Mercedes", "Sprinter", 2023,
                18000, bd(380.00), today.minusMonths(2), 1400, 14000, StatusPojazdu.DOSTEPNY);

        Dostawczy transit = ensureDostawczy(em, "WA1112L", "Ford", "Transit", 2020,
                135000, bd(320.00), today.minusMonths(11), 1200, 12000, StatusPojazdu.WYNAJETY);

        ensureDostawczy(em, "PO1314M", "Iveco", "Daily", 2022,
                54000, bd(350.00), today.minusMonths(6), 1600, 16000, StatusPojazdu.DOSTEPNY);

        Osobowy mazda = ensureOsobowy(em, "LU2020N", "Mazda", "3", 2020,
                61000, bd(170.00), today.minusMonths(9), 5, TypNadwozia.SEDAN, true, StatusPojazdu.DOSTEPNY);

        ensureOsobowy(em, "PO8080R", "Renault", "Clio", 2021,
                43000, bd(140.00), today.minusMonths(6), 5, TypNadwozia.HATCHBACK, true, StatusPojazdu.DOSTEPNY);
        ensureDostawczy(em, "KR7070T", "Fiat", "Ducato", 2019,
                155000, bd(310.00), today.minusMonths(9), 1300, 13000, StatusPojazdu.DOSTEPNY);

        Ubezpieczenie u10001 = ensureUbezpieczenie(em, "10001", "PZU", "OC", bd(650),
                today.minusMonths(1), today.plusMonths(11), StatusUbezpieczenia.AKTYWNE);
        Ubezpieczenie u10002 = ensureUbezpieczenie(em, "10002", "PZU", "AC", bd(900),
                today.minusMonths(2), today.plusMonths(10), StatusUbezpieczenia.AKTYWNE);
        Ubezpieczenie u10003 = ensureUbezpieczenie(em, "10003", "Warta", "OC + Assistance", bd(720),
                today.minusMonths(1), today.plusMonths(11), StatusUbezpieczenia.AKTYWNE);
        Ubezpieczenie u10004 = ensureUbezpieczenie(em, "10004", "Allianz", "OC+AC", bd(1400),
                today.minusMonths(3), today.plusMonths(9), StatusUbezpieczenia.AKTYWNE);
        Ubezpieczenie u10005 = ensureUbezpieczenie(em, "10005", "Ergo Hestia", "OC", bd(610),
                today.minusMonths(4), today.plusMonths(8), StatusUbezpieczenia.AKTYWNE);

        Ubezpieczenie u10010 = ensureUbezpieczenie(em, "10010", "PZU", "Assistance", bd(260),
                today.minusDays(10), today.plusMonths(12), StatusUbezpieczenia.AKTYWNE);
        Ubezpieczenie u10011 = ensureUbezpieczenie(em, "10011", "Allianz", "OC", bd(640),
                today.minusMonths(1), today.plusMonths(11), StatusUbezpieczenia.AKTYWNE);
        Ubezpieczenie u10012 = ensureUbezpieczenie(em, "10012", "Warta", "OC", bd(625),
                today.minusMonths(2), today.plusMonths(10), StatusUbezpieczenia.AKTYWNE);

        ensureUbezpieczenie(em, "10020", "PZU", "OC - wolna polisa", bd(690),
                today.minusDays(5), today.plusMonths(12), StatusUbezpieczenia.AKTYWNE);
        ensureUbezpieczenie(em, "10021", "Warta", "AC - wolna polisa", bd(1100),
                today.minusDays(3), today.plusMonths(12), StatusUbezpieczenia.AKTYWNE);
        ensureUbezpieczenie(em, "10022", "Allianz", "OC+AC - wolna polisa", bd(1450),
                today, today.plusMonths(12), StatusUbezpieczenia.AKTYWNE);
        ensureUbezpieczenie(em, "10023", "UNIQA", "Assistance - wolna polisa", bd(310),
                today.minusDays(7), today.plusMonths(11), StatusUbezpieczenia.AKTYWNE);
        ensureUbezpieczenie(em, "10024", "Ergo Hestia", "OC dostawcze - wolna polisa", bd(780),
                today.minusDays(1), today.plusMonths(12), StatusUbezpieczenia.AKTYWNE);

        ensureUbezpieczenie(em, "20001", "PZU", "OC (wygasłe)", bd(500),
                today.minusMonths(14), today.minusMonths(2), StatusUbezpieczenia.WYGASLE);
        ensureUbezpieczenie(em, "30001", "Allianz", "Szkic – do uzupełnienia", bd(0),
                today, today.plusMonths(12), StatusUbezpieczenia.SZKIC);
        ensureUbezpieczenie(em, "40001", "UNIQA", "Usunięte (demo)", bd(0),
                today.minusMonths(1), today.plusMonths(11), StatusUbezpieczenia.USUNIETE);

        ensurePrzypisanie(em, corolla, u10001, today.minusDays(20), today.plusMonths(11), "OC - standard");
        ensurePrzypisanie(em, corolla, u10002, today.minusDays(40), today.plusMonths(10), "AC - rozszerzone");
        ensurePrzypisanie(em, corolla, u10003, today.minusDays(10), today.plusMonths(11), "Assistance");
        ensurePrzypisanie(em, sprinter, u10004, today.minusMonths(2), today.plusMonths(9), "Flota dostawcza");
        ensurePrzypisanie(em, x5, u10005, today.minusMonths(3), today.plusMonths(8), "OC");
        ensurePrzypisanie(em, octavia, u10010, today.minusDays(5), today.plusMonths(12), "Assistance");
        ensurePrzypisanie(em, transit, u10011, today.minusMonths(1), today.plusMonths(11), "OC");
        ensurePrzypisanie(em, mazda, u10012, today.minusMonths(2), today.plusMonths(10), "OC");

        if (count(em, ZdarzeniePojazdu.class) < 3) {
            em.persist(new ZdarzeniePojazdu(today.minusDays(30), TypZdarzenia.PRZEGLAD, "Przegląd okresowy wykonany", corolla));
            em.persist(new ZdarzeniePojazdu(today.minusDays(12), TypZdarzenia.UBEZPIECZENIE, "Dodano pakiet OC/AC", corolla));
            em.persist(new ZdarzeniePojazdu(today.minusDays(3), TypZdarzenia.SERWIS, "Wizyta w serwisie – diagnostyka", sportage));
        }

        if (count(em, ZgloszenieSerwisowe.class) < 3) {
            ZgloszenieSerwisowe z1 = new ZgloszenieSerwisowe(today.minusDays(20), "Wymiana klocków hamulcowych", sportage);
            z1.setStatusZgloszenia(StatusZgloszenia.W_REALIZACJI);
            z1.setPrzypisanyPracownik(prac2);
            em.persist(z1);

            ZgloszenieSerwisowe z2 = new ZgloszenieSerwisowe(today.minusDays(8), "Kontrola układu klimatyzacji", corolla);
            z2.setStatusZgloszenia(StatusZgloszenia.UTWORZONE);
            z2.setPrzypisanyPracownik(prac1);
            em.persist(z2);

            ZgloszenieSerwisowe z3 = new ZgloszenieSerwisowe(today.minusDays(60), "Wymiana opon", octavia);
            z3.setStatusZgloszenia(StatusZgloszenia.ZAKONCZONE);
            z3.setPrzypisanyPracownik(prac2);
            em.persist(z3);
        }

        if (count(em, Rezerwacja.class) < 3) {
            Rezerwacja r1 = new Rezerwacja(klienci[0], corolla, today.plusDays(2), today.plusDays(6));
            r1.setStatusRezerwacji(StatusRezerwacji.OCZEKUJE);
            em.persist(r1);

            Rezerwacja r2 = new Rezerwacja(klienci[1], x5, today.plusDays(7), today.plusDays(10));
            r2.setStatusRezerwacji(StatusRezerwacji.ZATWIERDZONA);
            r2.setZatwierdzilPracownik(prac1);
            em.persist(r2);

            Rezerwacja r3 = new Rezerwacja(klienci[2], sprinter, today.plusDays(1), today.plusDays(3));
            r3.setStatusRezerwacji(StatusRezerwacji.ANULOWANA);
            em.persist(r3);
        }

        if (count(em, Wypozyczenie.class) < 3) {
            Wypozyczenie w1 = new Wypozyczenie(klienci[3], transit, today.minusDays(2), today.plusDays(2));
            w1.setStatusWypozyczenia(StatusWypozyczenia.AKTYWNE);
            w1.setObslugujacyPracownik(prac1);
            em.persist(w1);

            Wypozyczenie w2 = new Wypozyczenie(klienci[4], golf, today.minusDays(12), today.minusDays(7), today.minusDays(7));
            w2.setStatusWypozyczenia(StatusWypozyczenia.ZAKONCZONE);
            w2.setObslugujacyPracownik(prac2);
            em.persist(w2);

            Wypozyczenie w3 = new Wypozyczenie(klienci[5], octavia, today.minusDays(10), today.minusDays(1));
            w3.setStatusWypozyczenia(StatusWypozyczenia.OPOZNIENIE);
            w3.setObslugujacyPracownik(prac1);
            em.persist(w3);

            ensureFakturaFor(em, w1, nextInvoiceNumber(em), bd(1200), StatusFaktury.WYSTAWIONA);
            ensureFakturaFor(em, w2, nextInvoiceNumber(em), bd(980), StatusFaktury.OPLACONA);
            ensureFakturaFor(em, w3, nextInvoiceNumber(em), bd(1500), StatusFaktury.WYSTAWIONA);
        }

        ensureMinInvoices(em, 3);

        ensureMinRentalReservationLinks(em, 3);
    }

    public static void seed() {
        DatabaseService.tx(em -> {
            seed(em);
            return null;
        });
    }

    private static BigDecimal bd(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP);
    }

    private static <T> long count(EntityManager em, Class<T> entity) {
        var cb = em.getCriteriaBuilder();
        var query = cb.createQuery(Long.class);
        var root = query.from(entity);

        query.select(cb.count(root));

        return em.createQuery(query).getSingleResult();
    }

    private static Osoba getOrCreateOsoba(EntityManager em, String pesel, String imie, String nazwisko, String tel, String email) {
        Osoba o = em.find(Osoba.class, pesel);
        if (o == null) {
            o = new Osoba(pesel, imie, nazwisko, tel, email);
            em.persist(o);
            return o;
        }

        if (o.getImie() == null) o.setImie(imie);
        if (o.getNazwisko() == null) o.setNazwisko(nazwisko);
        if (o.getNumerTelefonu() == null) o.setNumerTelefonu(tel);
        if (o.getAdresEmail() == null && email != null) o.setAdresEmail(email);
        return o;
    }

    private static String ensureUniqueLogin(EntityManager em, String desiredLogin, String pesel) {
        String base = desiredLogin == null ? "" : desiredLogin.trim();
        if (base.isBlank()) base = "user";

        if (!loginExists(em, base, pesel)) return base;

        String suffix = (pesel != null && pesel.length() >= 2) ? pesel.substring(pesel.length() - 2) : "xx";
        String alt = base + "_" + suffix;
        if (!loginExists(em, alt, pesel)) return alt;

        return alt + "_1";
    }

    private static boolean loginExists(EntityManager em, String login, String ignoredPesel) {
        var cb = em.getCriteriaBuilder();
        var query = cb.createQuery(Long.class);
        var osoba = query.from(Osoba.class);

        var loginPredicate = cb.equal(osoba.get("login"), login);
        var ownerPredicate = ignoredPesel == null
                ? cb.conjunction()
                : cb.notEqual(osoba.get("pesel"), ignoredPesel);

        query.select(cb.count(osoba))
                .where(cb.and(loginPredicate, ownerPredicate));

        Long count = em.createQuery(query).getSingleResult();
        return count != null && count > 0;
    }

    private static Pracownik getOrCreatePracownik(EntityManager em, Osoba osoba, String stanowisko) {
        Pracownik p = em.find(Pracownik.class, osoba.getPesel());
        if (p == null) {
            p = new Pracownik(osoba, stanowisko);
            em.persist(p);
            return p;
        }

        if (p.getStanowisko() == null) p.setStanowisko(stanowisko);
        return p;
    }

    private static Zatrudnienie getOrCreateZatrudnienie(EntityManager em, Pracownik pracownik) {
        Zatrudnienie z = em.find(Zatrudnienie.class, pracownik.getPesel());
        if (z == null) {
            z = new Zatrudnienie(pracownik);
            em.persist(z);
        }
        return z;
    }

    private static Klient getOrCreateKlient(EntityManager em, Osoba osoba) {
        Klient k = em.find(Klient.class, osoba.getPesel());
        if (k == null) {
            k = new Klient(osoba);
            em.persist(k);
        }
        return k;
    }

    private static Pojazd findPojazdByNr(EntityManager em, String nr) {
        var cb = em.getCriteriaBuilder();
        var query = cb.createQuery(Pojazd.class);
        var pojazd = query.from(Pojazd.class);

        query.select(pojazd)
                .where(cb.equal(pojazd.get("numerRejestracyjny"), nr));

        return em.createQuery(query)
                .setMaxResults(1)
                .getResultStream()
                .findFirst()
                .orElse(null);
    }

    private static Osobowy ensureOsobowy(EntityManager em, String nr, String marka, String model, int rok,
                                         double przebieg, BigDecimal cenaZaDobe, LocalDate przeglad,
                                         int miejsca, TypNadwozia typ, Boolean klima, StatusPojazdu status) {
        Pojazd existing = findPojazdByNr(em, nr);
        if (existing instanceof Osobowy o) {
            o.setMarka(marka);
            o.setModel(model);
            o.setRokProdukcji(rok);
            o.setPrzebieg(przebieg);
            o.setCenaZaDobe(cenaZaDobe);
            o.setDataPrzegladu(przeglad);
            o.setStatusPojazdu(status);
            o.setLiczbaMiejsc(miejsca);
            o.setTypNadwozia(typ);
            o.setKlimatyzacja(klima);
            return o;
        }
        Osobowy o = new Osobowy(nr, marka, model, rok, przebieg, cenaZaDobe, przeglad, miejsca, typ, klima);
        o.setStatusPojazdu(status);
        em.persist(o);
        return o;
    }

    private static Dostawczy ensureDostawczy(EntityManager em, String nr, String marka, String model, int rok,
                                             double przebieg, BigDecimal cenaZaDobe, LocalDate przeglad,
                                             double ladownosc, double pojemnosc, StatusPojazdu status) {
        Pojazd existing = findPojazdByNr(em, nr);
        if (existing instanceof Dostawczy d) {
            d.setMarka(marka);
            d.setModel(model);
            d.setRokProdukcji(rok);
            d.setPrzebieg(przebieg);
            d.setCenaZaDobe(cenaZaDobe);
            d.setDataPrzegladu(przeglad);
            d.setStatusPojazdu(status);
            d.setLadownosc(ladownosc);
            d.setPojemnosc(pojemnosc);
            return d;
        }
        Dostawczy d = new Dostawczy(nr, marka, model, rok, przebieg, cenaZaDobe, przeglad, ladownosc, pojemnosc);
        d.setStatusPojazdu(status);
        em.persist(d);
        return d;
    }

    private static Ubezpieczenie findUbezpieczenieByNrPolisy(EntityManager em, String nrPolisy) {
        var cb = em.getCriteriaBuilder();
        var query = cb.createQuery(Ubezpieczenie.class);
        var ubezpieczenie = query.from(Ubezpieczenie.class);

        query.select(ubezpieczenie)
                .where(cb.equal(ubezpieczenie.get("numerPolisy"), nrPolisy));

        return em.createQuery(query)
                .setMaxResults(1)
                .getResultStream()
                .findFirst()
                .orElse(null);
    }

    private static Ubezpieczenie ensureUbezpieczenie(EntityManager em, String nrPolisy, String firma, String opis,
                                                     BigDecimal cena, LocalDate od, LocalDate dd, StatusUbezpieczenia status) {
        Ubezpieczenie u = findUbezpieczenieByNrPolisy(em, nrPolisy);
        if (u != null) {
            u.setFirma(firma);
            u.setOpis(opis);
            u.setCena(cena);
            u.setDataRozpoczecia(od);
            u.setDataZakonczenia(dd);
            u.setStatusUbezpieczenia(status);
            return u;
        }
        u = new Ubezpieczenie(nrPolisy, firma, opis, cena, od, dd);
        u.setStatusUbezpieczenia(status);
        em.persist(u);
        return u;
    }

    private static boolean isInsuranceAlreadyAssigned(EntityManager em, Ubezpieczenie u) {
        var cb = em.getCriteriaBuilder();
        var query = cb.createQuery(Long.class);
        var assignment = query.from(PojazdUbezpieczenie.class);

        query.select(cb.count(assignment))
                .where(cb.equal(assignment.get("ubezpieczenie"), u));

        Long c = em.createQuery(query).getSingleResult();
        return c != null && c > 0;
    }

    private static void ensurePrzypisanie(EntityManager em, Pojazd pojazd, Ubezpieczenie ubezpieczenie,
                                          LocalDate od, LocalDate dd, String uwagi) {
        if (isInsuranceAlreadyAssigned(em, ubezpieczenie)) return;
        em.persist(new PojazdUbezpieczenie(pojazd, ubezpieczenie, od, dd, uwagi));
    }

    private static String nextInvoiceNumber(EntityManager em) {
        long next = count(em, Faktura.class) + 1000;
        return "FV-" + next;
    }

    private static void ensureFakturaFor(EntityManager em, Wypozyczenie w, String numer, BigDecimal netto, StatusFaktury status) {
        if (hasInvoiceFor(em, w)) return;

        Faktura f = new Faktura(numer, netto, w);
        f.setStatusFaktury(status);
        em.persist(f);
        w.setFaktura(f);
    }

    private static void ensureMinInvoices(EntityManager em, int min) {
        long faktury = count(em, Faktura.class);
        if (faktury >= min) return;

        var rentals = rentalsWithoutInvoice(em);

        long next = count(em, Faktura.class) + 1000;
        for (Wypozyczenie w : rentals) {
            if (count(em, Faktura.class) >= min) break;

            if (hasInvoiceFor(em, w)) continue;

            Faktura f = new Faktura("FV-" + next++, bd(999.99), w);
            f.setStatusFaktury(StatusFaktury.WYSTAWIONA);
            em.persist(f);
            w.setFaktura(f);
        }
    }

    private static void ensureMinRentalReservationLinks(EntityManager em, int min) {
        Long links = countRentalReservationLinks(em);
        if (links != null && links >= min) return;

        var rentals = allRentals(em);
        var reservations = allReservations(em);
        if (rentals.isEmpty() || reservations.isEmpty()) return;

        int added = 0;
        for (Wypozyczenie w : rentals) {
            for (Rezerwacja r : reservations) {
                if (added >= min) return;

                if (isRentalReservationLinked(w, r)) continue;

                w.dodajRezerwacje(r);
                added++;
            }
        }
    }

    private static boolean hasInvoiceFor(EntityManager em, Wypozyczenie rental) {
        var cb = em.getCriteriaBuilder();
        var query = cb.createQuery(Long.class);
        var invoice = query.from(Faktura.class);

        query.select(cb.count(invoice))
                .where(cb.equal(invoice.get("wypozyczenie"), rental));

        Long count = em.createQuery(query).getSingleResult();
        return count != null && count > 0;
    }

    private static java.util.List<Wypozyczenie> rentalsWithoutInvoice(EntityManager em) {
        return allRentals(em).stream()
                .filter(rental -> rental.getFaktura() == null)
                .toList();
    }

    private static java.util.List<Wypozyczenie> allRentals(EntityManager em) {
        var cb = em.getCriteriaBuilder();
        var query = cb.createQuery(Wypozyczenie.class);
        var rental = query.from(Wypozyczenie.class);

        query.select(rental)
                .orderBy(cb.asc(rental.get("id")));

        return em.createQuery(query).getResultList();
    }

    private static java.util.List<Rezerwacja> allReservations(EntityManager em) {
        var cb = em.getCriteriaBuilder();
        var query = cb.createQuery(Rezerwacja.class);
        var reservation = query.from(Rezerwacja.class);

        query.select(reservation)
                .orderBy(cb.asc(reservation.get("id")));

        return em.createQuery(query).getResultList();
    }

    private static Long countRentalReservationLinks(EntityManager em) {
        return allRentals(em).stream()
                .mapToLong(rental -> rental.getRezerwacje().size())
                .sum();
    }

    private static boolean isRentalReservationLinked(Wypozyczenie rental, Rezerwacja reservation) {
        Long reservationId = reservation.getId();
        return rental.getRezerwacje().stream()
                .anyMatch(existing -> existing.getId() != null && existing.getId().equals(reservationId));
    }
}
