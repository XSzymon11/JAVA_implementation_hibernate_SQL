package pl.database;

import jakarta.persistence.*;
import pl.database.enums.TypNadwozia;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "pojazd_osobowy")
public class Osobowy extends Pojazd {

    @Column(nullable = false)
    private int liczbaMiejsc;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TypNadwozia typNadwozia;

    @Column(nullable = true)
    private Boolean klimatyzacja;

    protected Osobowy() {}

    public Osobowy(String nrRej, String marka, String model, int rok, double przebieg,
                   BigDecimal cenaZaDobe, LocalDate dataPrzegladu,
                   int liczbaMiejsc, TypNadwozia typNadwozia) {
        super(nrRej, marka, model, rok, przebieg, cenaZaDobe, dataPrzegladu);
        this.liczbaMiejsc = liczbaMiejsc;
        this.typNadwozia = typNadwozia;
        this.klimatyzacja = null;
    }

    public Osobowy(String nrRej, String marka, String model, int rok, double przebieg,
                   BigDecimal cenaZaDobe, LocalDate dataPrzegladu,
                   int liczbaMiejsc, TypNadwozia typNadwozia, Boolean klimatyzacja) {
        super(nrRej, marka, model, rok, przebieg, cenaZaDobe, dataPrzegladu);
        this.liczbaMiejsc = liczbaMiejsc;
        this.typNadwozia = typNadwozia;
        this.klimatyzacja = klimatyzacja;
    }

    public int getLiczbaMiejsc() { return liczbaMiejsc; }
    public void setLiczbaMiejsc(int liczbaMiejsc) { this.liczbaMiejsc = liczbaMiejsc; }

    public TypNadwozia getTypNadwozia() { return typNadwozia; }
    public void setTypNadwozia(TypNadwozia typNadwozia) { this.typNadwozia = typNadwozia; }

    public Boolean getKlimatyzacja() { return klimatyzacja; }
    public void setKlimatyzacja(Boolean klimatyzacja) { this.klimatyzacja = klimatyzacja; }

    @Override
    protected void validateVehicleType() {
        if (liczbaMiejsc <= 0) {
            throw new IllegalArgumentException("Liczba miejsc musi byc dodatnia");
        }
        EntityValidation.requireNonNull(typNadwozia, "Typ nadwozia jest wymagany");
    }

    @Override
    public BigDecimal obliczKosztWypozyczenia() {
        return obliczKosztWypozyczenia(1);
    }

    @Override
    public java.math.BigDecimal obliczKosztWypozyczenia(long liczbaDni) {
        java.math.BigDecimal base = super.obliczKosztWypozyczenia(liczbaDni);
        java.math.BigDecimal factor;
        switch (getTypNadwozia()) {
            case SUV -> factor = new java.math.BigDecimal("1.15");
            case KOMBI -> factor = new java.math.BigDecimal("1.05");
            case SEDAN -> factor = new java.math.BigDecimal("1.00");
            case HATCHBACK -> factor = new java.math.BigDecimal("0.95");
            default -> factor = java.math.BigDecimal.ONE;
        }
        return base.multiply(factor).setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
