package pl.database;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "pojazd_ubezpieczenie")
public class PojazdUbezpieczenie {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pojazd_ubez_seq")
    @SequenceGenerator(name = "pojazd_ubez_seq", sequenceName = "pojazd_ubez_seq", allocationSize = 50)
    private Long id;

    @Column(nullable = false)
    private LocalDate dataOd;

    @Column(nullable = false)
    private LocalDate dataDo;

    @Column(length = 500)
    private String uwagi;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean usuniete = false;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pojazd_id", nullable = false)
    private Pojazd pojazd;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ubezpieczenie_id", nullable = false)
    private Ubezpieczenie ubezpieczenie;

    public PojazdUbezpieczenie() {}

    public PojazdUbezpieczenie(Pojazd pojazd, Ubezpieczenie ubezpieczenie, LocalDate dataOd, LocalDate dataDo) {
        this(pojazd, ubezpieczenie, dataOd, dataDo, null);
    }

    public PojazdUbezpieczenie(Pojazd pojazd, Ubezpieczenie ubezpieczenie, LocalDate dataOd, LocalDate dataDo, String uwagi) {
        setPojazd(pojazd);
        setUbezpieczenie(ubezpieczenie);
        this.dataOd = dataOd;
        this.dataDo = dataDo;
        this.uwagi = uwagi;
    }

    @PrePersist
    @PreUpdate
    private void validate() {
        EntityValidation.requireNonNull(pojazd, "Przypisanie musi wskazywac pojazd");
        EntityValidation.requireNonNull(ubezpieczenie, "Przypisanie musi wskazywac ubezpieczenie");
        EntityValidation.requireDateRange(dataOd, dataDo, "Niepoprawny zakres dat przypisania ubezpieczenia");
        EntityValidation.requireMaxLength(uwagi, 500, "Uwagi przypisania sa za dlugie");

        if (ubezpieczenie != null && dataOd != null && dataDo != null) {
            if (ubezpieczenie.getDataRozpoczecia() != null
                    && dataOd.isBefore(ubezpieczenie.getDataRozpoczecia())) {
                throw new IllegalArgumentException("Data od przypisania nie moze byc przed poczatkiem polisy");
            }
            if (ubezpieczenie.getDataZakonczenia() != null
                    && dataDo.isAfter(ubezpieczenie.getDataZakonczenia())) {
                throw new IllegalArgumentException("Data do przypisania nie moze byc po koncu polisy");
            }
        }
    }

    public Long getId() { return id; }

    public LocalDate getDataOd() { return dataOd; }
    public void setDataOd(LocalDate dataOd) { this.dataOd = dataOd; }

    public LocalDate getDataDo() { return dataDo; }
    public void setDataDo(LocalDate dataDo) { this.dataDo = dataDo; }

    public String getUwagi() { return uwagi; }
    public void setUwagi(String uwagi) { this.uwagi = uwagi; }

    public boolean isUsuniete() { return usuniete; }
    public void setUsuniete(boolean usuniete) { this.usuniete = usuniete; }

    public Pojazd getPojazd() { return pojazd; }
    public void setPojazd(Pojazd pojazd) {
        if (this.pojazd == pojazd) return;

        Pojazd poprzedni = this.pojazd;
        this.pojazd = pojazd;

        if (poprzedni != null) {
            poprzedni.getPrzypisaniaUbezpieczen().remove(this);
        }
        if (pojazd != null && !pojazd.getPrzypisaniaUbezpieczen().contains(this)) {
            pojazd.getPrzypisaniaUbezpieczen().add(this);
        }
    }

    public Ubezpieczenie getUbezpieczenie() { return ubezpieczenie; }
    public void setUbezpieczenie(Ubezpieczenie ubezpieczenie) {
        if (this.ubezpieczenie == ubezpieczenie) return;

        Ubezpieczenie poprzednie = this.ubezpieczenie;
        this.ubezpieczenie = ubezpieczenie;

        if (poprzednie != null) {
            poprzednie.getPrzypisaniaPojazdow().remove(this);
        }
        if (ubezpieczenie != null && !ubezpieczenie.getPrzypisaniaPojazdow().contains(this)) {
            ubezpieczenie.getPrzypisaniaPojazdow().add(this);
        }
    }
}
