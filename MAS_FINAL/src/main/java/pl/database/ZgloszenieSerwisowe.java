package pl.database;

import jakarta.persistence.*;
import pl.database.enums.StatusZgloszenia;

import java.time.LocalDate;

@Entity
@Table(name = "zgloszenie_serwisowe")
public class ZgloszenieSerwisowe {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "zgloszenie_seq")
    @SequenceGenerator(name = "zgloszenie_seq", sequenceName = "zgloszenie_seq", allocationSize = 50)
    private Long id;

    @Column(nullable = false)
    private LocalDate dataZgloszenia;

    @Column(nullable = false, length = 100)
    private String powod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusZgloszenia statusZgloszenia = StatusZgloszenia.UTWORZONE;

    @Column
    private LocalDate dataZakonczenia;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pojazd_id", nullable = false)
    private Pojazd pojazd;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "przypisany_pracownik_pesel")
    private Pracownik przypisanyPracownik;

    protected ZgloszenieSerwisowe() {}

    public ZgloszenieSerwisowe(LocalDate dataZgloszenia, String powod, Pojazd pojazd) {
        this.dataZgloszenia = dataZgloszenia;
        this.powod = powod;
        setPojazd(pojazd);
    }

    @PrePersist
    @PreUpdate
    private void validate() {
        EntityValidation.requireNonNull(dataZgloszenia, "Data zgloszenia serwisowego jest wymagana");
        EntityValidation.requireNonBlank(powod, "Powod zgloszenia serwisowego jest wymagany");
        EntityValidation.requireMaxLength(powod, 100, "Powod zgloszenia serwisowego jest za dlugi");
        EntityValidation.requireNonNull(statusZgloszenia, "Status zgloszenia serwisowego jest wymagany");
        EntityValidation.requireNonNull(pojazd, "Zgloszenie serwisowe musi dotyczyc pojazdu");
        if (dataZakonczenia != null && dataZakonczenia.isBefore(dataZgloszenia)) {
            throw new IllegalArgumentException("Data zakonczenia zgloszenia nie moze byc przed data zgloszenia");
        }
        if (statusZgloszenia == StatusZgloszenia.ZAKONCZONE) {
            EntityValidation.requireNonNull(dataZakonczenia, "Data zakonczenia jest wymagana dla zakonczonego zgloszenia");
        }
    }

    public Long getId() { return id; }
    public LocalDate getDataZgloszenia() { return dataZgloszenia; }
    public void setDataZgloszenia(LocalDate dataZgloszenia) { this.dataZgloszenia = dataZgloszenia; }

    public String getPowod() { return powod; }
    public void setPowod(String powod) { this.powod = powod; }

    public StatusZgloszenia getStatusZgloszenia() { return statusZgloszenia; }
    public void setStatusZgloszenia(StatusZgloszenia statusZgloszenia) { zmienStatus(statusZgloszenia); }

    public LocalDate getDataZakonczenia() { return dataZakonczenia; }
    public void setDataZakonczenia(LocalDate dataZakonczenia) { this.dataZakonczenia = dataZakonczenia; }

    public void zmienStatus(StatusZgloszenia status) {
        this.statusZgloszenia = status;
        if (status == StatusZgloszenia.ZAKONCZONE && dataZakonczenia == null) {
            dataZakonczenia = LocalDate.now();
        }
    }

    public void przypiszPracownika(Pracownik pracownik) {
        setPrzypisanyPracownik(pracownik);
    }

    public Pojazd getPojazd() { return pojazd; }
    public void setPojazd(Pojazd pojazd) {
        if (this.pojazd == pojazd) return;

        Pojazd poprzedni = this.pojazd;
        this.pojazd = pojazd;

        if (poprzedni != null) {
            poprzedni.getZgloszeniaSerwisowe().remove(this);
        }
        if (pojazd != null && !pojazd.getZgloszeniaSerwisowe().contains(this)) {
            pojazd.getZgloszeniaSerwisowe().add(this);
        }
    }

    public Pracownik getPrzypisanyPracownik() { return przypisanyPracownik; }
    public void setPrzypisanyPracownik(Pracownik przypisanyPracownik) {
        if (this.przypisanyPracownik == przypisanyPracownik) return;

        Pracownik poprzedni = this.przypisanyPracownik;
        this.przypisanyPracownik = przypisanyPracownik;

        if (poprzedni != null) {
            poprzedni.getZgloszenia().remove(this);
        }
        if (przypisanyPracownik != null && !przypisanyPracownik.getZgloszenia().contains(this)) {
            przypisanyPracownik.dodajZgloszenie(this);
        }
    }
}
