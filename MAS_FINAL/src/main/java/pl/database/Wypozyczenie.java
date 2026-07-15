package pl.database;

import jakarta.persistence.*;
import pl.database.enums.StatusPojazdu;
import pl.database.enums.StatusWypozyczenia;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "wypozyczenie")
public class Wypozyczenie {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "wypozyczenie_seq")
    @SequenceGenerator(name = "wypozyczenie_seq", sequenceName = "wypozyczenie_seq", allocationSize = 50)
    private Long id;

    @Column(nullable = false)
    private LocalDate dataWypozyczenia;

    @Column(nullable = false)
    private LocalDate planowanaDataZwrotu;

    @Column(nullable = true)
    private LocalDate rzeczywistaDataZwrotu;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusWypozyczenia statusWypozyczenia = StatusWypozyczenia.AKTYWNE;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pojazd_id", nullable = false)
    private Pojazd pojazd;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "klient_pesel", nullable = false)
    private Klient klient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "obslugujacy_pracownik_pesel")
    private Pracownik obslugujacyPracownik;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "wypozyczenie_rezerwacja",
            joinColumns = @JoinColumn(name = "wypozyczenie_id"),
            inverseJoinColumns = @JoinColumn(name = "rezerwacja_id")
    )
    private Set<Rezerwacja> rezerwacje = new HashSet<>();

    @OneToOne(mappedBy = "wypozyczenie", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    private Faktura faktura;

    public Wypozyczenie() {}

    public Wypozyczenie(Klient klient, Pojazd pojazd, LocalDate dataWypozyczenia, LocalDate planowanaDataZwrotu) {
        setKlient(klient);
        setPojazd(pojazd);
        this.dataWypozyczenia = dataWypozyczenia;
        this.planowanaDataZwrotu = planowanaDataZwrotu;
    }

    public Wypozyczenie(Klient klient, Pojazd pojazd, LocalDate dataWypozyczenia, LocalDate planowanaDataZwrotu, LocalDate rzeczywistaDataZwrotu) {
        this(klient, pojazd, dataWypozyczenia, planowanaDataZwrotu);
        this.rzeczywistaDataZwrotu = rzeczywistaDataZwrotu;
    }

    @PrePersist
    @PreUpdate
    private void validate() {
        EntityValidation.requireNonNull(klient, "Wypozyczenie musi miec klienta");
        EntityValidation.requireNonNull(pojazd, "Wypozyczenie musi miec pojazd");
        EntityValidation.requireDateRange(dataWypozyczenia, planowanaDataZwrotu, "Niepoprawny zakres dat wypozyczenia");
        if (rzeczywistaDataZwrotu != null && rzeczywistaDataZwrotu.isBefore(dataWypozyczenia)) {
            throw new IllegalArgumentException("Rzeczywista data zwrotu nie moze byc przed data wypozyczenia");
        }
        EntityValidation.requireNonNull(statusWypozyczenia, "Status wypozyczenia jest wymagany");
    }

    public Long getId() { return id; }

    public LocalDate getDataWypozyczenia() { return dataWypozyczenia; }
    public void setDataWypozyczenia(LocalDate dataWypozyczenia) { this.dataWypozyczenia = dataWypozyczenia; }

    public LocalDate getPlanowanaDataZwrotu() { return planowanaDataZwrotu; }
    public void setPlanowanaDataZwrotu(LocalDate planowanaDataZwrotu) { this.planowanaDataZwrotu = planowanaDataZwrotu; }

    public LocalDate getRzeczywistaDataZwrotu() { return rzeczywistaDataZwrotu; }
    public void setRzeczywistaDataZwrotu(LocalDate rzeczywistaDataZwrotu) { this.rzeczywistaDataZwrotu = rzeczywistaDataZwrotu; }

    public StatusWypozyczenia getStatusWypozyczenia() { return statusWypozyczenia; }
    public void setStatusWypozyczenia(StatusWypozyczenia statusWypozyczenia) { this.statusWypozyczenia = statusWypozyczenia; }

    public Pojazd getPojazd() { return pojazd; }
    public Klient getKlient() { return klient; }

    public void setKlient(Klient klient) {
        if (this.klient == klient) return;

        Klient poprzedni = this.klient;
        this.klient = klient;

        if (poprzedni != null) {
            poprzedni.getWypozyczeniaList().remove(this);
        }
        if (klient != null && !klient.getWypozyczeniaList().contains(this)) {
            klient.dodajWypozyczenie(this);
        }
    }

    public void setPojazd(Pojazd pojazd) {
        if (this.pojazd == pojazd) return;

        Pojazd poprzedni = this.pojazd;
        this.pojazd = pojazd;

        if (poprzedni != null) {
            poprzedni.getWypozyczenia().remove(this);
        }
        if (pojazd != null && !pojazd.getWypozyczenia().contains(this)) {
            pojazd.dodajWypozyczenie(this);
        }
    }

    public Pracownik getObslugujacyPracownik() { return obslugujacyPracownik; }
    public void setObslugujacyPracownik(Pracownik obslugujacyPracownik) {
        if (this.obslugujacyPracownik == obslugujacyPracownik) return;

        Pracownik poprzedni = this.obslugujacyPracownik;
        this.obslugujacyPracownik = obslugujacyPracownik;

        if (poprzedni != null) {
            poprzedni.getObslugiwaneWypozyczenia().remove(this);
        }
        if (obslugujacyPracownik != null && !obslugujacyPracownik.getObslugiwaneWypozyczenia().contains(this)) {
            obslugujacyPracownik.dodajObslugiwaneWypozyczenie(this);
        }
    }

    public BigDecimal wyliczKoszt() {
        if (pojazd == null || dataWypozyczenia == null || planowanaDataZwrotu == null) {
            return BigDecimal.ZERO;
        }
        LocalDate dataDo = rzeczywistaDataZwrotu == null ? planowanaDataZwrotu : rzeczywistaDataZwrotu;
        long dni = Math.max(1, ChronoUnit.DAYS.between(dataWypozyczenia, dataDo));
        return pojazd.obliczKosztWypozyczenia(dni);
    }

    public Faktura utworzFakture() {
        if (faktura == null) {
            faktura = new Faktura("FV-" + System.currentTimeMillis(), wyliczKoszt(), this);
        }
        return faktura;
    }

    public void zakonczWypozyczenie(LocalDate dataZwrotu) {
        this.rzeczywistaDataZwrotu = dataZwrotu;
        this.statusWypozyczenia = dataZwrotu != null
                && planowanaDataZwrotu != null
                && dataZwrotu.isAfter(planowanaDataZwrotu)
                ? StatusWypozyczenia.OPOZNIENIE
                : StatusWypozyczenia.ZAKONCZONE;
        if (pojazd != null) {
            pojazd.setStatusPojazdu(StatusPojazdu.DOSTEPNY);
        }
    }

    public Set<Rezerwacja> getRezerwacje() { return rezerwacje; }

    public void dodajRezerwacje(Rezerwacja rezerwacja) {
        if (rezerwacja == null) return;
        if (rezerwacje.add(rezerwacja)) {
            rezerwacja.dodajWypozyczenie(this);
        }
    }

    public Faktura getFaktura() { return faktura; }
    public void setFaktura(Faktura faktura) {
        if (this.faktura == faktura) return;

        Faktura poprzednia = this.faktura;
        this.faktura = faktura;

        if (poprzednia != null && poprzednia.getWypozyczenie() == this) {
            poprzednia.setWypozyczenie(null);
        }
        if (faktura != null && faktura.getWypozyczenie() != this) {
            faktura.setWypozyczenie(this);
        }
    }
}
