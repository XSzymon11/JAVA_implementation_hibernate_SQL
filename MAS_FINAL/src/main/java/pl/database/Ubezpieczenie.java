package pl.database;

import jakarta.persistence.*;
import pl.database.enums.StatusUbezpieczenia;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ubezpieczenie",
        uniqueConstraints = @UniqueConstraint(name = "uk_ubezpieczenie_nr_polisy", columnNames = "numer_polisy"))
public class Ubezpieczenie {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ubezpieczenie_seq")
    @SequenceGenerator(name = "ubezpieczenie_seq", sequenceName = "ubezpieczenie_seq", allocationSize = 50)
    private Long id;

    @Column(name = "numer_polisy", nullable = false, length = 60)
    private String numerPolisy;

    @Column(nullable = false, length = 120)
    private String firma;

    @Column(nullable = false, length = 500)
    private String opis;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal cena = BigDecimal.ZERO;

    @Column(nullable = false)
    private LocalDate dataRozpoczecia;

    @Column(nullable = false)
    private LocalDate dataZakonczenia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusUbezpieczenia statusUbezpieczenia = StatusUbezpieczenia.SZKIC;

    @OneToMany(mappedBy = "ubezpieczenie", fetch = FetchType.LAZY)
    private List<PojazdUbezpieczenie> przypisaniaPojazdow = new ArrayList<>();

    protected Ubezpieczenie() {}

    public Ubezpieczenie(String numerPolisy, String firma, String opis, BigDecimal cena,
                         LocalDate dataRozpoczecia, LocalDate dataZakonczenia) {
        this.numerPolisy = numerPolisy;
        this.firma = firma;
        this.opis = opis;
        this.cena = cena;
        this.dataRozpoczecia = dataRozpoczecia;
        this.dataZakonczenia = dataZakonczenia;
    }

    @PrePersist
    @PreUpdate
    private void validate() {
        EntityValidation.requireNonBlank(numerPolisy, "Numer polisy jest wymagany");
        EntityValidation.requireNonBlank(firma, "Firma ubezpieczenia jest wymagana");
        EntityValidation.requireNonBlank(opis, "Opis ubezpieczenia jest wymagany");
        EntityValidation.requireMaxLength(numerPolisy, 60, "Numer polisy jest za dlugi");
        EntityValidation.requireMaxLength(firma, 120, "Nazwa firmy ubezpieczenia jest za dluga");
        EntityValidation.requireMaxLength(opis, 500, "Opis ubezpieczenia jest za dlugi");
        EntityValidation.requireNonNegative(cena, "Cena ubezpieczenia nie moze byc ujemna");
        EntityValidation.requireDateRange(dataRozpoczecia, dataZakonczenia, "Niepoprawny zakres dat ubezpieczenia");
        EntityValidation.requireNonNull(statusUbezpieczenia, "Status ubezpieczenia jest wymagany");
    }

    public Long getId() { return id; }
    public String getNumerPolisy() { return numerPolisy; }
    public void setNumerPolisy(String numerPolisy) { this.numerPolisy = numerPolisy; }

    public String getFirma() { return firma; }
    public void setFirma(String firma) { this.firma = firma; }

    public String getOpis() { return opis; }
    public void setOpis(String opis) { this.opis = opis; }

    public BigDecimal getCena() { return cena; }
    public void setCena(BigDecimal cena) { this.cena = cena; }

    public LocalDate getDataRozpoczecia() { return dataRozpoczecia; }
    public void setDataRozpoczecia(LocalDate dataRozpoczecia) { this.dataRozpoczecia = dataRozpoczecia; }

    public LocalDate getDataZakonczenia() { return dataZakonczenia; }
    public void setDataZakonczenia(LocalDate dataZakonczenia) { this.dataZakonczenia = dataZakonczenia; }

    public StatusUbezpieczenia getStatusUbezpieczenia() { return statusUbezpieczenia; }
    public void setStatusUbezpieczenia(StatusUbezpieczenia statusUbezpieczenia) { this.statusUbezpieczenia = statusUbezpieczenia; }

    public BigDecimal wyliczKwoteSkladki() {
        return cena;
    }

    public boolean sprawdzWaznosc() {
        LocalDate dzisiaj = LocalDate.now();
        return statusUbezpieczenia == StatusUbezpieczenia.AKTYWNE
                && (dataRozpoczecia == null || !dzisiaj.isBefore(dataRozpoczecia))
                && (dataZakonczenia == null || !dzisiaj.isAfter(dataZakonczenia));
    }

    public void zmienStatus(StatusUbezpieczenia status) {
        this.statusUbezpieczenia = status;
    }

    public void dodajPrzypisaniePojazdu(PojazdUbezpieczenie przypisanie) {
        if (przypisanie == null) return;
        if (!przypisaniaPojazdow.contains(przypisanie)) {
            przypisaniaPojazdow.add(przypisanie);
        }
        if (przypisanie.getUbezpieczenie() != this) {
            przypisanie.setUbezpieczenie(this);
        }
    }

    public List<PojazdUbezpieczenie> getPrzypisaniaPojazdow() {
        return przypisaniaPojazdow;
    }
}
