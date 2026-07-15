package pl.database;

import jakarta.persistence.*;
import pl.auth.PasswordHasher;

import java.time.LocalDate;

@Entity
@Table(name = "osoba",
        uniqueConstraints = @UniqueConstraint(name = "uk_osoba_login", columnNames = "login"))
public class Osoba {

    @Id
    @Column(name = "pesel", nullable = false, length = 11, updatable = false)
    private String pesel;

    @Column(nullable = false, length = 60)
    private String imie;

    @Column(nullable = false, length = 80)
    private String nazwisko;

    @Column(nullable = false, length = 30)
    private String numerTelefonu;

    @Column(length = 120)
    private String adresEmail;

    @Column(nullable = false, length = 60)
    private String login;

    @Column(nullable = false, length = 200)
    private String haslo;

    @OneToOne(mappedBy = "osoba", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    private Klient klient;

    @OneToOne(mappedBy = "osoba", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    private Pracownik pracownik;

    protected Osoba() {}

    public Osoba(String pesel, String imie, String nazwisko, String numerTelefonu, String adresEmail) {
        this(pesel, imie, nazwisko, numerTelefonu, adresEmail, pesel, PasswordHasher.hash(pesel));
    }

    public Osoba(String pesel, String imie, String nazwisko, String numerTelefonu,
                 String adresEmail, String login, String haslo) {
        this.pesel = pesel;
        this.imie = imie;
        this.nazwisko = nazwisko;
        this.numerTelefonu = numerTelefonu;
        this.adresEmail = adresEmail;
        this.login = login;
        this.haslo = haslo;
    }

    public Osoba(String pesel, String imie, String nazwisko, String numerTelefonu) {
        this(pesel, imie, nazwisko, numerTelefonu, null);
    }

    @PrePersist
    @PreUpdate
    private void validate() {
        EntityValidation.requireNonBlank(pesel, "PESEL jest wymagany");
        if (!pesel.matches("\\d{11}")) {
            throw new IllegalArgumentException("PESEL musi miec 11 cyfr");
        }

        EntityValidation.requireNonBlank(imie, "Imie jest wymagane");
        EntityValidation.requireNonBlank(nazwisko, "Nazwisko jest wymagane");
        EntityValidation.requireNonBlank(numerTelefonu, "Numer telefonu jest wymagany");
        EntityValidation.requireNonBlank(login, "Login jest wymagany");
        EntityValidation.requireNonBlank(haslo, "Haslo jest wymagane");

        EntityValidation.requireMaxLength(imie, 60, "Imie jest za dlugie");
        EntityValidation.requireMaxLength(nazwisko, 80, "Nazwisko jest za dlugie");
        EntityValidation.requireMaxLength(numerTelefonu, 30, "Numer telefonu jest za dlugi");
        EntityValidation.requireMaxLength(login, 60, "Login jest za dlugi");
        EntityValidation.requireMaxLength(haslo, 200, "Haslo jest za dlugie");
        EntityValidation.requireMaxLength(adresEmail, 120, "Adres email jest za dlugi");

        if (adresEmail != null && !adresEmail.isBlank() && !adresEmail.contains("@")) {
            throw new IllegalArgumentException("Adres email musi zawierac znak @");
        }
    }

    public String getPesel() { return pesel; }

    public String getImie() { return imie; }
    public void setImie(String imie) { this.imie = imie; }

    public String getNazwisko() { return nazwisko; }
    public void setNazwisko(String nazwisko) { this.nazwisko = nazwisko; }

    public String getNumerTelefonu() { return numerTelefonu; }
    public void setNumerTelefonu(String numerTelefonu) { this.numerTelefonu = numerTelefonu; }

    public String getAdresEmail() { return adresEmail; }
    public void setAdresEmail(String adresEmail) { this.adresEmail = adresEmail; }

    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }

    public String getHaslo() { return haslo; }
    public void setHaslo(String haslo) { this.haslo = haslo; }

    public boolean zaloguj(String login, String haslo) {
        return this.login != null
                && this.login.equals(login)
                && PasswordHasher.matches(haslo, this.haslo);
    }

    public Rezerwacja rezerwujPojazd(Pojazd pojazd, LocalDate dataOd, LocalDate dataDo) {
        if (klient == null) {
            throw new IllegalStateException("Tylko osoba z rolą klienta może zarezerwować pojazd");
        }
        return klient.rezerwujPojazd(pojazd, dataOd, dataDo);
    }

    void ustawKlienta(Klient klient) {
        this.klient = klient;
    }

    void ustawPracownika(Pracownik pracownik) {
        this.pracownik = pracownik;
    }

    public Klient getKlient() { return klient; }
    public Pracownik getPracownik() { return pracownik; }
}
