package pl.database;

import jakarta.persistence.*;
import pl.database.enums.TypUmowy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Entity
@Table(name = "zatrudnienie")
public class Zatrudnienie {

    @Id
    @Column(name = "pesel", length = 11)
    private String pesel;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "pesel")
    private Pracownik pracownik;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TypUmowy typUmowy = TypUmowy.UOP;

    @Column(nullable = false)
    private LocalDate dataZawarcia = LocalDate.now();

    @Column(nullable = false)
    private int okres = 12;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal wynagrodzenieNetto = BigDecimal.ZERO;

    protected Zatrudnienie() {}

    public Zatrudnienie(Pracownik pracownik) {
        setPracownik(pracownik);
    }

    @PrePersist
    @PreUpdate
    private void validate() {
        EntityValidation.requireNonNull(pracownik, "Zatrudnienie musi wskazywac pracownika");
        EntityValidation.requireNonNull(typUmowy, "Typ umowy jest wymagany");
        EntityValidation.requireNonNull(dataZawarcia, "Data zawarcia umowy jest wymagana");
        if (okres <= 0) {
            throw new IllegalArgumentException("Okres zatrudnienia musi byc dodatni");
        }
        EntityValidation.requireNonNegative(wynagrodzenieNetto, "Wynagrodzenie netto nie moze byc ujemne");
    }

    @Transient
    public LocalDate getDataZakonczenia() {
        return wyliczDateZakonczenia();
    }

    public LocalDate wyliczDateZakonczenia() {
        return dataZawarcia.plusMonths(okres);
    }

    public BigDecimal wyliczWynagrodzenieBrutto() {
        return wynagrodzenieNetto
                .multiply(new BigDecimal("1.23"))
                .setScale(2, RoundingMode.HALF_UP);
    }

    public String getPesel() { return pesel; }
    public Pracownik getPracownik() { return pracownik; }

    public void setPracownik(Pracownik pracownik) {
        if (this.pracownik == pracownik) return;
        this.pracownik = pracownik;
        this.pesel = pracownik == null ? null : pracownik.getPesel();
        if (pracownik != null && pracownik.getZatrudnienie() != this) {
            pracownik.ustawZatrudnienie(this);
        }
    }

    public TypUmowy getTypUmowy() { return typUmowy; }
    public void setTypUmowy(TypUmowy typUmowy) { this.typUmowy = typUmowy; }

    public LocalDate getDataZawarcia() { return dataZawarcia; }
    public void setDataZawarcia(LocalDate dataZawarcia) { this.dataZawarcia = dataZawarcia; }

    public int getOkres() { return okres; }
    public void setOkres(int okres) { this.okres = okres; }

    public BigDecimal getWynagrodzenieNetto() { return wynagrodzenieNetto; }
    public void setWynagrodzenieNetto(BigDecimal wynagrodzenieNetto) {
        this.wynagrodzenieNetto = wynagrodzenieNetto == null ? BigDecimal.ZERO : wynagrodzenieNetto;
    }

    @Transient
    public BigDecimal getWynagrodzenieBrutto() {
        return wyliczWynagrodzenieBrutto();
    }

    public void setWynagrodzenieBrutto(double wynagrodzenieBrutto) {
        this.wynagrodzenieNetto = BigDecimal.valueOf(wynagrodzenieBrutto)
                .divide(new BigDecimal("1.23"), 2, RoundingMode.HALF_UP);
    }
}
