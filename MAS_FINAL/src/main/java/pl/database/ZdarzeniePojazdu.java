package pl.database;

import jakarta.persistence.*;
import pl.database.enums.TypZdarzenia;

import java.time.LocalDate;

@Entity
@Table(name = "zdarzenie_pojazdu")
public class ZdarzeniePojazdu {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "zdarzenie_seq")
    @SequenceGenerator(name = "zdarzenie_seq", sequenceName = "zdarzenie_seq", allocationSize = 50)
    private Long id;

    @Column(nullable = false)
    private LocalDate dataZdarzenia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TypZdarzenia typZdarzenia;

    @Column(nullable = false, length = 500)
    private String opis;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pojazd_id", nullable = false)
    private Pojazd pojazd;

    protected ZdarzeniePojazdu() {}

    public ZdarzeniePojazdu(LocalDate dataZdarzenia, TypZdarzenia typZdarzenia, String opis, Pojazd pojazd) {
        this.dataZdarzenia = dataZdarzenia;
        this.typZdarzenia = typZdarzenia;
        this.opis = opis;
        setPojazd(pojazd);
    }

    @PrePersist
    @PreUpdate
    private void validate() {
        EntityValidation.requireNonNull(dataZdarzenia, "Data zdarzenia pojazdu jest wymagana");
        EntityValidation.requireNonNull(typZdarzenia, "Typ zdarzenia pojazdu jest wymagany");
        EntityValidation.requireNonBlank(opis, "Opis zdarzenia pojazdu jest wymagany");
        EntityValidation.requireMaxLength(opis, 500, "Opis zdarzenia pojazdu jest za dlugi");
        EntityValidation.requireNonNull(pojazd, "Zdarzenie musi dotyczyc pojazdu");
    }

    public Long getId() { return id; }
    public LocalDate getDataZdarzenia() { return dataZdarzenia; }
    public void setDataZdarzenia(LocalDate dataZdarzenia) { this.dataZdarzenia = dataZdarzenia; }

    public TypZdarzenia getTypZdarzenia() { return typZdarzenia; }
    public void setTypZdarzenia(TypZdarzenia typZdarzenia) { this.typZdarzenia = typZdarzenia; }

    public String getOpis() { return opis; }
    public void setOpis(String opis) { this.opis = opis; }

    public Pojazd getPojazd() { return pojazd; }
    public void setPojazd(Pojazd pojazd) {
        if (this.pojazd == pojazd) return;

        Pojazd poprzedni = this.pojazd;
        this.pojazd = pojazd;

        if (poprzedni != null) {
            poprzedni.getZdarzenia().remove(this);
        }
        if (pojazd != null && !pojazd.getZdarzenia().contains(this)) {
            pojazd.getZdarzenia().add(this);
        }
    }
}
