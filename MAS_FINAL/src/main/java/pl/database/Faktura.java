package pl.database;

import jakarta.persistence.*;
import pl.database.enums.StatusFaktury;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Entity
@Table(name = "faktura",
        uniqueConstraints = @UniqueConstraint(name = "uk_faktura_numer", columnNames = "numer_faktury"))
public class Faktura {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "faktura_seq")
    @SequenceGenerator(name = "faktura_seq", sequenceName = "faktura_seq", allocationSize = 50)
    private Long id;

    @Column(name = "numer_faktury", nullable = false, length = 60)
    private String numerFaktury;

    @Column(nullable = false)
    private final LocalDate dataWystawienia = LocalDate.now();

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal kwotaNetto = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusFaktury statusFaktury = StatusFaktury.WYSTAWIONA;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wypozyczenie_id", nullable = false, unique = true)
    private Wypozyczenie wypozyczenie;

    public Faktura() {}

    public Faktura(String numerFaktury, BigDecimal kwotaNetto, Wypozyczenie wypozyczenie) {
        this.numerFaktury = numerFaktury;
        this.kwotaNetto = kwotaNetto == null ? BigDecimal.ZERO : kwotaNetto;
        setWypozyczenie(wypozyczenie);
    }

    public Faktura(Integer numerFaktury, BigDecimal kwotaBrutto, Wypozyczenie wypozyczenie) {
        this(String.valueOf(numerFaktury), nettoFromBrutto(kwotaBrutto), wypozyczenie);
    }

    @PrePersist
    @PreUpdate
    private void validate() {
        EntityValidation.requireNonBlank(numerFaktury, "Numer faktury jest wymagany");
        EntityValidation.requireMaxLength(numerFaktury, 60, "Numer faktury jest za dlugi");
        EntityValidation.requireNonNull(dataWystawienia, "Data wystawienia faktury jest wymagana");
        EntityValidation.requireNonNegative(kwotaNetto, "Kwota netto faktury nie moze byc ujemna");
        EntityValidation.requireNonNull(statusFaktury, "Status faktury jest wymagany");
        EntityValidation.requireNonNull(wypozyczenie, "Faktura musi dotyczyc wypozyczenia");
    }

    public BigDecimal wyliczKwoteBrutto() {
        return kwotaNetto.multiply(new BigDecimal("1.23")).setScale(2, RoundingMode.HALF_UP);
    }

    public void anuluj() {
        statusFaktury = StatusFaktury.ANULOWANA;
    }

    public boolean zweryfikujPlatnosc() {
        return statusFaktury == StatusFaktury.OPLACONA;
    }

    public String pobierzSzczegolyRachunku() {
        return "Faktura " + numerFaktury
                + ", netto: " + kwotaNetto
                + ", brutto: " + wyliczKwoteBrutto()
                + ", status: " + statusFaktury;
    }

    private static BigDecimal nettoFromBrutto(BigDecimal kwotaBrutto) {
        if (kwotaBrutto == null) return BigDecimal.ZERO;
        return kwotaBrutto.divide(new BigDecimal("1.23"), 2, RoundingMode.HALF_UP);
    }

    public Long getId() { return id; }
    public String getNumerFaktury() { return numerFaktury; }
    public void setNumerFaktury(String numerFaktury) { this.numerFaktury = numerFaktury; }
    public void setNumerFaktury(Integer numerFaktury) { this.numerFaktury = String.valueOf(numerFaktury); }

    public LocalDate getDataWystawienia() { return dataWystawienia; }

    public BigDecimal getKwotaNetto() { return kwotaNetto; }
    public void setKwotaNetto(BigDecimal kwotaNetto) {
        this.kwotaNetto = kwotaNetto == null ? BigDecimal.ZERO : kwotaNetto;
    }

    @Transient
    public BigDecimal getKwotaBrutto() {
        return wyliczKwoteBrutto();
    }

    public void setKwotaBrutto(BigDecimal kwotaBrutto) {
        this.kwotaNetto = nettoFromBrutto(kwotaBrutto);
    }

    public StatusFaktury getStatusFaktury() { return statusFaktury; }
    public void setStatusFaktury(StatusFaktury statusFaktury) { this.statusFaktury = statusFaktury; }

    public Wypozyczenie getWypozyczenie() { return wypozyczenie; }
    public void setWypozyczenie(Wypozyczenie wypozyczenie) {
        if (this.wypozyczenie == wypozyczenie) return;

        Wypozyczenie poprzednie = this.wypozyczenie;
        this.wypozyczenie = wypozyczenie;

        if (poprzednie != null && poprzednie.getFaktura() == this) {
            poprzednie.setFaktura(null);
        }
        if (wypozyczenie != null && wypozyczenie.getFaktura() != this) {
            wypozyczenie.setFaktura(this);
        }
    }
}
