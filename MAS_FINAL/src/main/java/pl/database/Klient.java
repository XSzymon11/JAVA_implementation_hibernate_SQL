package pl.database;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "klient")
public class Klient {

    @Id
    @Column(name = "pesel", length = 11)
    private String pesel;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "pesel")
    private Osoba osoba;

    @Column(nullable = false)
    private LocalDate dataRejestracji;

    @OneToMany(mappedBy = "klient", fetch = FetchType.LAZY)
    private List<Rezerwacja> rezerwacje = new ArrayList<>();

    @OneToMany(mappedBy = "klient", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Wypozyczenie> wypozyczenia = new ArrayList<>();

    protected Klient() {}

    public Klient(Osoba osoba) {
        powiazZOsoba(osoba);
        this.dataRejestracji = LocalDate.now();
    }

    @PrePersist
    @PreUpdate
    private void validate() {
        EntityValidation.requireNonNull(osoba, "Klient musi byc powiazany z osoba");
        EntityValidation.requireNonNull(dataRejestracji, "Data rejestracji klienta jest wymagana");
    }

    public String getPesel() { return pesel; }
    public Osoba getOsoba() { return osoba; }

    public LocalDate getDataRejestracji() { return dataRejestracji; }

    private void powiazZOsoba(Osoba osoba) {
        this.osoba = osoba;
        this.pesel = osoba == null ? null : osoba.getPesel();
        if (osoba != null && osoba.getKlient() != this) {
            osoba.ustawKlienta(this);
        }
    }

    public int wyliczLiczbePojazdow() {
        return wypozyczenia.size();
    }

    public Rezerwacja rezerwujPojazd(Pojazd pojazd, LocalDate dataOd, LocalDate dataDo) {
        Rezerwacja rezerwacja = new Rezerwacja(this, pojazd, dataOd, dataDo);
        dodajRezerwacje(rezerwacja);
        return rezerwacja;
    }

    public void anulujRezerwacje(Rezerwacja rezerwacja) {
        if (rezerwacja != null && rezerwacje.contains(rezerwacja)) {
            rezerwacja.anuluj();
        }
    }

    public List<Rezerwacja> getRezerwacje() { return rezerwacje; }
    public List<Wypozyczenie> getWypozyczeniaList() { return wypozyczenia; }

    void dodajRezerwacje(Rezerwacja rezerwacja) {
        if (rezerwacja == null) return;
        if (!rezerwacje.contains(rezerwacja)) {
            rezerwacje.add(rezerwacja);
        }
        if (rezerwacja.getKlient() != this) {
            rezerwacja.setKlient(this);
        }
    }

    void dodajWypozyczenie(Wypozyczenie wypozyczenie) {
        if (wypozyczenie == null) return;
        if (!wypozyczenia.contains(wypozyczenie)) {
            wypozyczenia.add(wypozyczenie);
        }
        if (wypozyczenie.getKlient() != this) {
            wypozyczenie.setKlient(this);
        }
    }
}
