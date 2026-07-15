package pl.database;

import jakarta.persistence.*;
import pl.database.enums.StatusRezerwacji;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "rezerwacja")
public class Rezerwacja {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "rezerwacja_seq")
    @SequenceGenerator(name = "rezerwacja_seq", sequenceName = "rezerwacja_seq", allocationSize = 50)
    private Long id;

    @Column(nullable = false)
    private LocalDate dataOd;

    @Column(nullable = false)
    private LocalDate dataDo;

    @Column(nullable = false)
    private LocalDateTime dataUtworzenia = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusRezerwacji statusRezerwacji = StatusRezerwacji.OCZEKUJE;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "klient_pesel", nullable = false)
    private Klient klient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pojazd_id", nullable = false)
    private Pojazd pojazd;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zatwierdzil_pracownik_pesel")
    private Pracownik zatwierdzilPracownik;

    @ManyToMany(mappedBy = "rezerwacje", fetch = FetchType.LAZY)
    private Set<Wypozyczenie> wypozyczenia = new HashSet<>();

    public Rezerwacja() {}

    public Rezerwacja(Klient klient, Pojazd pojazd, LocalDate dataOd, LocalDate dataDo) {
        setKlient(klient);
        setPojazd(pojazd);
        this.dataOd = dataOd;
        this.dataDo = dataDo;
    }

    @PrePersist
    @PreUpdate
    private void validate() {
        EntityValidation.requireNonNull(klient, "Rezerwacja musi miec klienta");
        EntityValidation.requireNonNull(pojazd, "Rezerwacja musi miec pojazd");
        EntityValidation.requireDateRange(dataOd, dataDo, "Niepoprawny zakres dat rezerwacji");
        EntityValidation.requireNonNull(dataUtworzenia, "Data utworzenia rezerwacji jest wymagana");
        EntityValidation.requireNonNull(statusRezerwacji, "Status rezerwacji jest wymagany");
        if (statusRezerwacji == StatusRezerwacji.ZATWIERDZONA && zatwierdzilPracownik == null) {
            throw new IllegalArgumentException("Zatwierdzona rezerwacja musi wskazywac pracownika");
        }
    }

    public Long getId() { return id; }

    public LocalDate getDataOd() { return dataOd; }
    public void setDataOd(LocalDate dataOd) { this.dataOd = dataOd; }

    public LocalDate getDataDo() { return dataDo; }
    public void setDataDo(LocalDate dataDo) { this.dataDo = dataDo; }

    public LocalDateTime getDataUtworzenia() { return dataUtworzenia; }

    public void setDataUtworzenia(LocalDateTime dataUtworzenia) {this.dataUtworzenia = dataUtworzenia;}

    public StatusRezerwacji getStatusRezerwacji() { return statusRezerwacji; }
    public void setStatusRezerwacji(StatusRezerwacji statusRezerwacji) { this.statusRezerwacji = statusRezerwacji; }

    public Klient getKlient() { return klient; }
    public void setKlient(Klient klient) {
        if (this.klient == klient) return;

        Klient poprzedni = this.klient;
        this.klient = klient;

        if (poprzedni != null) {
            poprzedni.getRezerwacje().remove(this);
        }
        if (klient != null && !klient.getRezerwacje().contains(this)) {
            klient.dodajRezerwacje(this);
        }
    }

    public Pojazd getPojazd() { return pojazd; }
    public void setPojazd(Pojazd pojazd) {
        if (this.pojazd == pojazd) return;

        Pojazd poprzedni = this.pojazd;
        this.pojazd = pojazd;

        if (poprzedni != null) {
            poprzedni.getRezerwacje().remove(this);
        }
        if (pojazd != null && !pojazd.getRezerwacje().contains(this)) {
            pojazd.dodajRezerwacje(this);
        }
    }

    public Pracownik getZatwierdzilPracownik() { return zatwierdzilPracownik; }
    public void setZatwierdzilPracownik(Pracownik zatwierdzilPracownik) {
        if (this.zatwierdzilPracownik == zatwierdzilPracownik) return;

        Pracownik poprzedni = this.zatwierdzilPracownik;
        this.zatwierdzilPracownik = zatwierdzilPracownik;

        if (poprzedni != null) {
            poprzedni.getZatwierdzoneRezerwacje().remove(this);
        }
        if (zatwierdzilPracownik != null && !zatwierdzilPracownik.getZatwierdzoneRezerwacje().contains(this)) {
            zatwierdzilPracownik.dodajZatwierdzonaRezerwacje(this);
        }
    }

    public BigDecimal wyliczKoszt() {
        if (pojazd == null || dataOd == null || dataDo == null) {
            return BigDecimal.ZERO;
        }
        long dni = Math.max(1, ChronoUnit.DAYS.between(dataOd, dataDo));
        return pojazd.obliczKosztWypozyczenia(dni);
    }

    public void zatwierdzRezerwacje(Pracownik pracownik) {
        this.zatwierdzilPracownik = pracownik;
        this.statusRezerwacji = StatusRezerwacji.ZATWIERDZONA;
    }

    public void anuluj() {
        this.statusRezerwacji = StatusRezerwacji.ANULOWANA;
    }

    void dodajWypozyczenie(Wypozyczenie wypozyczenie) {
        if (wypozyczenie == null) return;
        if (wypozyczenia.add(wypozyczenie) && !wypozyczenie.getRezerwacje().contains(this)) {
            wypozyczenie.dodajRezerwacje(this);
        }
    }

    public Set<Wypozyczenie> getWypozyczenia() { return wypozyczenia; }
}
