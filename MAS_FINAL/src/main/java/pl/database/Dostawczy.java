package pl.database;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "pojazd_dostawczy")
public class Dostawczy extends Pojazd {

    @Column(nullable = false)
    private double ladownosc;

    @Column(nullable = false)
    private double pojemnosc;

    protected Dostawczy() {}

    public Dostawczy(String nrRej, String marka, String model, int rok, double przebieg,
                     BigDecimal cenaZaDobe, LocalDate dataPrzegladu,
                     double ladownosc, double pojemnosc) {
        super(nrRej, marka, model, rok, przebieg, cenaZaDobe, dataPrzegladu);
        this.ladownosc = ladownosc;
        this.pojemnosc = pojemnosc;
    }

    public double getLadownosc() { return ladownosc; }
    public void setLadownosc(double ladownosc) { this.ladownosc = ladownosc; }

    public double getPojemnosc() { return pojemnosc; }
    public void setPojemnosc(double pojemnosc) { this.pojemnosc = pojemnosc; }

    @Override
    protected void validateVehicleType() {
        EntityValidation.requirePositive(ladownosc, "Ladownosc musi byc dodatnia");
        EntityValidation.requirePositive(pojemnosc, "Pojemnosc musi byc dodatnia");
    }

    @Override
    public BigDecimal obliczKosztWypozyczenia() {
        return obliczKosztWypozyczenia(1);
    }

    @Override
    public java.math.BigDecimal obliczKosztWypozyczenia(long liczbaDni) {
        java.math.BigDecimal base = super.obliczKosztWypozyczenia(liczbaDni);
        java.math.BigDecimal cap = java.math.BigDecimal.valueOf(getPojemnosc())
                .divide(new java.math.BigDecimal("10000"), 4, java.math.RoundingMode.HALF_UP);
        java.math.BigDecimal load = java.math.BigDecimal.valueOf(getLadownosc())
                .divide(new java.math.BigDecimal("10000"), 4, java.math.RoundingMode.HALF_UP);
        java.math.BigDecimal factor = java.math.BigDecimal.ONE.add(cap).add(load);
        return base.multiply(factor).setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
