package pl.database;

import jakarta.persistence.*;
import pl.database.enums.StatusPojazdu;
import pl.database.enums.TypZdarzenia;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pojazd", uniqueConstraints = @UniqueConstraint(name = "uk_pojazd_rejestracja", columnNames = "numer_rejestracyjny"))
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Pojazd {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pojazd_seq")
    @SequenceGenerator(name = "pojazd_seq", sequenceName = "pojazd_seq", allocationSize = 50)
    private Long id;

    @Column(name = "numer_rejestracyjny", nullable = false, length = 16)
    private String numerRejestracyjny;

    @Column(nullable = false, length = 60)
    private String marka;

    @Column(nullable = false, length = 60)
    private String model;

    @Column(nullable = false)
    private int rokProdukcji;

    @Column(nullable = false)
    private double przebieg;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal cenaZaDobe = BigDecimal.ZERO;

    @Column(nullable = false)
    private LocalDate dataPrzegladu;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusPojazdu statusPojazdu = StatusPojazdu.DOSTEPNY;

    @OneToMany(mappedBy = "pojazd", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PojazdUbezpieczenie> przypisaniaUbezpieczen = new ArrayList<>();

    @OneToMany(mappedBy = "pojazd", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ZdarzeniePojazdu> zdarzenia = new ArrayList<>();

    @OneToMany(mappedBy = "pojazd", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ZgloszenieSerwisowe> zgloszeniaSerwisowe = new ArrayList<>();

    @OneToMany(mappedBy = "pojazd", fetch = FetchType.LAZY)
    private List<Rezerwacja> rezerwacje = new ArrayList<>();

    @OneToMany(mappedBy = "pojazd", fetch = FetchType.LAZY)
    private List<Wypozyczenie> wypozyczenia = new ArrayList<>();

    protected Pojazd() {}

    public Pojazd(String numerRejestracyjny, String marka, String model, int rokProdukcji,
                  double przebieg, BigDecimal cenaZaDobe, LocalDate dataPrzegladu) {
        this.numerRejestracyjny = numerRejestracyjny;
        this.marka = marka;
        this.model = model;
        this.rokProdukcji = rokProdukcji;
        this.przebieg = przebieg;
        this.cenaZaDobe = cenaZaDobe;
        this.dataPrzegladu = dataPrzegladu;
    }

    @PrePersist
    @PreUpdate
    private void validate() {
        EntityValidation.requireNonBlank(numerRejestracyjny, "Numer rejestracyjny jest wymagany");
        EntityValidation.requireMaxLength(numerRejestracyjny, 16, "Numer rejestracyjny jest za dlugi");
        EntityValidation.requireNonBlank(marka, "Marka pojazdu jest wymagana");
        EntityValidation.requireMaxLength(marka, 60, "Marka pojazdu jest za dluga");
        EntityValidation.requireNonBlank(model, "Model pojazdu jest wymagany");
        EntityValidation.requireMaxLength(model, 60, "Model pojazdu jest za dlugi");
        if (rokProdukcji < 1886 || rokProdukcji > LocalDate.now().getYear()) {
            throw new IllegalArgumentException("Rok produkcji pojazdu jest poza dozwolonym zakresem");
        }
        EntityValidation.requireNonNegative(przebieg, "Przebieg nie moze byc ujemny");
        EntityValidation.requireNonNegative(cenaZaDobe, "Cena za dobe nie moze byc ujemna");
        EntityValidation.requireNonNull(dataPrzegladu, "Data przegladu jest wymagana");
        EntityValidation.requireNonNull(statusPojazdu, "Status pojazdu jest wymagany");
        validateVehicleType();
    }

    protected void validateVehicleType() {
    }

    public Long getId() { return id; }
    public String getNumerRejestracyjny() { return numerRejestracyjny; }
    public void setNumerRejestracyjny(String v) { this.numerRejestracyjny = v; }

    public String getMarka() { return marka; }
    public void setMarka(String marka) { this.marka = marka; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public int getRokProdukcji() { return rokProdukcji; }
    public void setRokProdukcji(int rokProdukcji) { this.rokProdukcji = rokProdukcji; }

    public double getPrzebieg() { return przebieg; }
    public void setPrzebieg(double przebieg) { this.przebieg = przebieg; }

    public BigDecimal getCenaZaDobe() { return cenaZaDobe; }
    public void setCenaZaDobe(BigDecimal cenaZaDobe) { this.cenaZaDobe = cenaZaDobe; }

    public LocalDate getDataPrzegladu() { return dataPrzegladu; }
    public void setDataPrzegladu(LocalDate dataPrzegladu) { this.dataPrzegladu = dataPrzegladu; }

    public StatusPojazdu getStatusPojazdu() { return statusPojazdu; }
    public void setStatusPojazdu(StatusPojazdu statusPojazdu) { this.statusPojazdu = statusPojazdu; }

    public BigDecimal obliczKosztWypozyczenia() {
        return obliczKosztWypozyczenia(1);
    }

    public java.math.BigDecimal obliczKosztWypozyczenia(long liczbaDni) {
        if (liczbaDni <= 0) return java.math.BigDecimal.ZERO;
        return getCenaZaDobe().multiply(java.math.BigDecimal.valueOf(liczbaDni));
    }

    public List<ZdarzeniePojazdu> pobierzHistorieZdarzen() {
        return zdarzenia;
    }

    public void zarejestrujNaprawe() {
        ZgloszenieSerwisowe zgloszenie = new ZgloszenieSerwisowe(LocalDate.now(), "Naprawa pojazdu", this);
        ZdarzeniePojazdu zdarzenie = new ZdarzeniePojazdu(LocalDate.now(), TypZdarzenia.SERWIS,
                "Zarejestrowano naprawę pojazdu", this);
        dodajZgloszenieSerwisowe(zgloszenie);
        dodajZdarzenie(zdarzenie);
        statusPojazdu = StatusPojazdu.W_SERWISIE;
    }

    public List<Pojazd> wyswietlDostepnePojazdy() {
        return statusPojazdu == StatusPojazdu.DOSTEPNY ? List.of(this) : List.of();
    }

    public void dodajPrzypisanieUbezpieczenia(PojazdUbezpieczenie przypisanie) {
        if (przypisanie == null) return;
        if (!przypisaniaUbezpieczen.contains(przypisanie)) {
            przypisaniaUbezpieczen.add(przypisanie);
        }
        if (przypisanie.getPojazd() != this) {
            przypisanie.setPojazd(this);
        }
    }

    public void usunPrzypisanieUbezpieczenia(PojazdUbezpieczenie przypisanie) {
        if (przypisanie == null) return;
        if (przypisaniaUbezpieczen.remove(przypisanie) && przypisanie.getPojazd() == this) {
            przypisanie.setPojazd(null);
        }
    }

    public void dodajZdarzenie(ZdarzeniePojazdu zdarzenie) {
        if (zdarzenie == null) return;
        if (!zdarzenia.contains(zdarzenie)) {
            zdarzenia.add(zdarzenie);
        }
        if (zdarzenie.getPojazd() != this) {
            zdarzenie.setPojazd(this);
        }
    }

    public void dodajZgloszenieSerwisowe(ZgloszenieSerwisowe zgloszenie) {
        if (zgloszenie == null) return;
        if (!zgloszeniaSerwisowe.contains(zgloszenie)) {
            zgloszeniaSerwisowe.add(zgloszenie);
        }
        if (zgloszenie.getPojazd() != this) {
            zgloszenie.setPojazd(this);
        }
    }

    void dodajRezerwacje(Rezerwacja rezerwacja) {
        if (rezerwacja == null) return;
        if (!rezerwacje.contains(rezerwacja)) {
            rezerwacje.add(rezerwacja);
        }
        if (rezerwacja.getPojazd() != this) {
            rezerwacja.setPojazd(this);
        }
    }

    void dodajWypozyczenie(Wypozyczenie wypozyczenie) {
        if (wypozyczenie == null) return;
        if (!wypozyczenia.contains(wypozyczenie)) {
            wypozyczenia.add(wypozyczenie);
        }
        if (wypozyczenie.getPojazd() != this) {
            wypozyczenie.setPojazd(this);
        }
    }

    public List<PojazdUbezpieczenie> getPrzypisaniaUbezpieczen() { return przypisaniaUbezpieczen; }
    public List<ZdarzeniePojazdu> getZdarzenia() { return zdarzenia; }
    public List<ZgloszenieSerwisowe> getZgloszeniaSerwisowe() { return zgloszeniaSerwisowe; }
    public List<Rezerwacja> getRezerwacje() { return rezerwacje; }
    public List<Wypozyczenie> getWypozyczenia() { return wypozyczenia; }
}
