package pl.database;

import jakarta.persistence.*;
import pl.database.enums.StatusRezerwacji;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "pracownik")
public class Pracownik {

    @Id
    @Column(name = "pesel", length = 11)
    private String pesel;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "pesel")
    private Osoba osoba;

    @Column(nullable = false, length = 80)
    private String stanowisko;

    @Column(nullable = false)
    private LocalDate dataZatrudnienia;

    @OneToOne(mappedBy = "pracownik", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true, optional = false)
    private Zatrudnienie zatrudnienie;

    @OneToMany(mappedBy = "zatwierdzilPracownik", fetch = FetchType.LAZY)
    private List<Rezerwacja> zatwierdzoneRezerwacje = new ArrayList<>();

    @OneToMany(mappedBy = "obslugujacyPracownik", fetch = FetchType.LAZY)
    private List<Wypozyczenie> obslugiwaneWypozyczenia = new ArrayList<>();

    @OneToMany(mappedBy = "przypisanyPracownik", fetch = FetchType.LAZY)
    private List<ZgloszenieSerwisowe> zgloszenia = new ArrayList<>();

    protected Pracownik() {}

    public Pracownik(Osoba osoba, String stanowisko) {
        powiazZOsoba(osoba);
        this.stanowisko = stanowisko;
        this.dataZatrudnienia = LocalDate.now();
        ustawZatrudnienie(new Zatrudnienie(this));
    }

    @PrePersist
    @PreUpdate
    private void validate() {
        EntityValidation.requireNonNull(osoba, "Pracownik musi byc powiazany z osoba");
        EntityValidation.requireNonBlank(stanowisko, "Stanowisko jest wymagane");
        EntityValidation.requireMaxLength(stanowisko, 80, "Stanowisko jest za dlugie");
        EntityValidation.requireNonNull(dataZatrudnienia, "Data zatrudnienia jest wymagana");
        if (dataZatrudnienia.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Data zatrudnienia nie moze byc z przyszlosci");
        }
        EntityValidation.requireNonNull(zatrudnienie, "Zatrudnienie pracownika jest wymagane");
    }

    @Transient
    public int getStaz() {
        return wyliczStaz();
    }

    @Transient
    public int wyliczStaz() {
        if (dataZatrudnienia == null) {
            return 0;
        }
        return (int) ChronoUnit.MONTHS.between(dataZatrudnienia, LocalDate.now());
    }

    public Rezerwacja rezerwujPojazd(Pojazd pojazd, LocalDate dataOd, LocalDate dataDo) {
        if (osoba == null || osoba.getKlient() == null) {
            throw new IllegalStateException("Pracownik musi mieć rolę klienta, aby rezerwować pojazd");
        }
        return osoba.getKlient().rezerwujPojazd(pojazd, dataOd, dataDo);
    }

    public void zatwierdzRezerwacje(Rezerwacja rezerwacja) {
        if (rezerwacja == null) return;
        rezerwacja.setStatusRezerwacji(StatusRezerwacji.ZATWIERDZONA);
        rezerwacja.setZatwierdzilPracownik(this);
    }

    public Klient zarejestrujKlienta(Osoba osoba) {
        if (osoba == null) {
            throw new IllegalArgumentException("Osoba jest wymagana do rejestracji klienta");
        }
        if (osoba.getKlient() != null) {
            return osoba.getKlient();
        }
        return new Klient(osoba);
    }

    public Faktura wystawFakture(Wypozyczenie wypozyczenie) {
        if (wypozyczenie == null) {
            throw new IllegalArgumentException("Wypożyczenie jest wymagane do wystawienia faktury");
        }
        Faktura faktura = new Faktura("FV-" + System.currentTimeMillis(), wypozyczenie.wyliczKoszt(), wypozyczenie);
        wypozyczenie.setFaktura(faktura);
        return faktura;
    }

    public String getPesel() { return pesel; }
    public Osoba getOsoba() { return osoba; }

    public String getStanowisko() { return stanowisko; }
    public void setStanowisko(String stanowisko) { this.stanowisko = stanowisko; }

    public LocalDate getDataZatrudnienia() { return dataZatrudnienia; }
    public void setDataZatrudnienia(LocalDate dataZatrudnienia) { this.dataZatrudnienia = dataZatrudnienia; }

    public Zatrudnienie getZatrudnienie() { return zatrudnienie; }
    public List<Rezerwacja> getZatwierdzoneRezerwacje() { return zatwierdzoneRezerwacje; }
    public List<Wypozyczenie> getObslugiwaneWypozyczenia() { return obslugiwaneWypozyczenia; }
    public List<ZgloszenieSerwisowe> getZgloszenia() { return zgloszenia; }

    private void powiazZOsoba(Osoba osoba) {
        this.osoba = osoba;
        this.pesel = osoba == null ? null : osoba.getPesel();
        if (osoba != null && osoba.getPracownik() != this) {
            osoba.ustawPracownika(this);
        }
    }

    void ustawZatrudnienie(Zatrudnienie zatrudnienie) {
        this.zatrudnienie = zatrudnienie;
        if (zatrudnienie != null && zatrudnienie.getPracownik() != this) {
            zatrudnienie.setPracownik(this);
        }
    }

    void dodajZatwierdzonaRezerwacje(Rezerwacja rezerwacja) {
        if (rezerwacja == null) return;
        if (!zatwierdzoneRezerwacje.contains(rezerwacja)) {
            zatwierdzoneRezerwacje.add(rezerwacja);
        }
        if (rezerwacja.getZatwierdzilPracownik() != this) {
            rezerwacja.setZatwierdzilPracownik(this);
        }
    }

    void dodajObslugiwaneWypozyczenie(Wypozyczenie wypozyczenie) {
        if (wypozyczenie == null) return;
        if (!obslugiwaneWypozyczenia.contains(wypozyczenie)) {
            obslugiwaneWypozyczenia.add(wypozyczenie);
        }
        if (wypozyczenie.getObslugujacyPracownik() != this) {
            wypozyczenie.setObslugujacyPracownik(this);
        }
    }

    void dodajZgloszenie(ZgloszenieSerwisowe zgloszenie) {
        if (zgloszenie == null) return;
        if (!zgloszenia.contains(zgloszenie)) {
            zgloszenia.add(zgloszenie);
        }
        if (zgloszenie.getPrzypisanyPracownik() != this) {
            zgloszenie.setPrzypisanyPracownik(this);
        }
    }
}
