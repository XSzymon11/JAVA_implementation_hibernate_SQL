# KaRent

KaRent to desktopowa aplikacja wspierająca zarządzanie wypożyczalnią samochodów. Projekt łączy interfejs graficzny wykonany w JavaFX z relacyjną bazą PostgreSQL oraz warstwą persystencji opartą na Jakarta Persistence i Hibernate. Aplikacja odwzorowuje procesy biznesowe wypożyczalni: obsługę floty, klientów i pracowników, rezerwacji, wypożyczeń, faktur, ubezpieczeń oraz zdarzeń serwisowych.

Projekt powstał jako praktyczna implementacja rozbudowanego modelu obiektowo-relacyjnego. Szczególny nacisk położyłem na poprawne odwzorowanie relacji między encjami, dwukierunkową synchronizację asocjacji, walidację reguł biznesowych, transakcyjność operacji oraz rozdzielenie interfejsu użytkownika od logiki aplikacji i dostępu do danych.

## Najważniejsze funkcje

Aktualnie dostępny interfejs aplikacji umożliwia:

- logowanie użytkownika i przechowywanie informacji o aktywnej sesji,
- wyświetlanie floty pojazdów w tabeli,
- wyszukiwanie pojazdu po numerze rejestracyjnym,
- filtrowanie pojazdów według statusu,
- podgląd podstawowych danych i statusu wybranego pojazdu,
- przeglądanie polis przypisanych do pojazdu,
- tworzenie nowej polisy ubezpieczeniowej,
- przypisywanie dostępnej, aktywnej polisy do pojazdu wraz z okresem obowiązywania i uwagami,
- usuwanie przypisania polisy z pojazdu,
- walidację formularzy i prezentowanie komunikatów potwierdzających lub informujących o błędach,
- wylogowanie i bezpieczne wyczyszczenie bieżącej sesji.

Warstwa domenowa i serwisowa obsługuje również:

- tworzenie, zatwierdzanie i anulowanie rezerwacji,
- kontrolę kolizji terminów rezerwacji,
- rozpoczęcie wypożyczenia na podstawie zatwierdzonej rezerwacji,
- zakończenie wypożyczenia i automatyczną aktualizację statusu pojazdu,
- obliczanie kosztu wynajmu na podstawie liczby dni i stawki dobowej,
- wystawianie i obsługę faktur,
- zmianę statusu pojazdu i wycofanie go z floty,
- rejestrowanie historii zdarzeń pojazdu oraz zgłoszeń serwisowych.

> Część modułów domenowych posiada gotową logikę i serwisy, natomiast ich ekrany w panelu pracownika są oznaczone jako kolejne etapy rozwoju. W pełni dostępny w UI jest obecnie moduł zarządzania ubezpieczeniami pojazdów.

## Technologie

| Technologia | Zastosowanie |
|---|---|
| Java 17 | główny język aplikacji i implementacja logiki domenowej |
| JavaFX 17 | desktopowy interfejs użytkownika, formularze, tabele i nawigacja |
| CSS | spójny wygląd kontrolek i ekranów aplikacji |
| Maven | zarządzanie zależnościami, kompilacja i uruchamianie projektu |
| Jakarta Persistence 3.1 | standard mapowania obiektowo-relacyjnego |
| Hibernate ORM 6.4 | implementacja JPA, zarządzanie encjami i schematem bazy |
| PostgreSQL 16 | relacyjna baza danych aplikacji |
| Docker Compose | powtarzalne uruchamianie lokalnej instancji PostgreSQL |
| Criteria API | bezpieczne typowo budowanie zapytań do bazy |
| SHA-256 | haszowanie haseł użytkowników przed zapisaniem w bazie |

## Architektura projektu

Kod został podzielony na warstwy o jasno określonych odpowiedzialnościach:

```text
src/main/java/pl
├── app
│   ├── MainApp.java             # punkt wejścia i nawigacja między ekranami
│   ├── LoginView.java           # ekran logowania
│   ├── service                  # logika przypadków użycia i operacje transakcyjne
│   └── ui                       # widoki, dialogi i komponenty JavaFX
├── auth
│   ├── PasswordHasher.java      # haszowanie i weryfikacja haseł
│   └── Session.java             # stan zalogowanego użytkownika
└── database
    ├── enums                    # statusy i typy wykorzystywane w modelu
    ├── DatabaseService.java     # EntityManagerFactory i obsługa transakcji
    ├── EntityValidation.java    # wspólne reguły walidacji encji
    ├── SeedData.java            # idempotentne dane demonstracyjne
    └── *.java                   # encje modelu domenowego
```

Zasoby aplikacji znajdują się w `src/main/resources`:

- `META-INF/persistence.xml` zawiera konfigurację połączenia i Hibernate,
- `styles/style.css` definiuje wygląd aplikacji.

### Przepływ operacji

Widoki JavaFX odpowiadają za prezentację danych i obsługę zdarzeń użytkownika. Operacje biznesowe delegowane są do klas serwisowych, które wykonują zapytania i modyfikacje encji w ramach transakcji udostępnianych przez `DatabaseService`. Hibernate mapuje encje na tabele PostgreSQL, zarządza relacjami i synchronizuje zmiany z bazą.

## Model danych

Model obejmuje najważniejsze obszary działania wypożyczalni:

- `Osoba` — wspólne dane osobowe oraz dane logowania,
- `Klient` i `Pracownik` — role powiązane z osobą relacją jeden-do-jednego,
- `Zatrudnienie` — dane dotyczące umowy pracownika,
- `Pojazd` — abstrakcyjna encja bazowa floty,
- `Osobowy` i `Dostawczy` — wyspecjalizowane typy pojazdów,
- `Rezerwacja` — termin, klient, pojazd, status oraz zatwierdzający pracownik,
- `Wypozyczenie` — rzeczywisty okres wynajmu i pracownik obsługujący,
- `Faktura` — dokument finansowy powiązany z wypożyczeniem,
- `Ubezpieczenie` — dane polisy i jej status,
- `PojazdUbezpieczenie` — encja asocjacyjna przechowująca okres i szczegóły przypisania polisy,
- `ZdarzeniePojazdu` — historia przeglądów, serwisu i ubezpieczeń,
- `ZgloszenieSerwisowe` — obsługa prac serwisowych pojazdu.

Dziedziczenie `Pojazd -> Osobowy/Dostawczy` zostało odwzorowane strategią `JOINED`. Dzięki temu wspólne atrybuty znajdują się w tabeli bazowej, a dane charakterystyczne dla konkretnego rodzaju pojazdu w tabelach specjalizowanych.

W modelu wykorzystałem relacje `OneToOne`, `OneToMany`, `ManyToOne` i `ManyToMany`, ładowanie `LAZY`, kaskadowanie wybranych operacji, usuwanie osieroconych rekordów, ograniczenia unikalności oraz sekwencje PostgreSQL. Metody pomocnicze encji utrzymują spójność obu stron relacji dwukierunkowych.

## Reguły biznesowe i jakość danych

Walidacja wykonywana przed zapisem i aktualizacją encji (`@PrePersist`, `@PreUpdate`) zabezpiecza m.in. przed:

- brakiem wymaganych powiązań i wartości,
- niepoprawnymi zakresami dat,
- ujemnymi cenami, przebiegami i parametrami pojazdów,
- zatwierdzeniem rezerwacji bez wskazania pracownika,
- przypisaniem nieaktywnej lub usuniętej polisy,
- ponownym przypisaniem tej samej polisy,
- rezerwacją niedostępnego pojazdu lub konfliktem terminów.

Statusy biznesowe zapisuję jako czytelne wartości tekstowe przy użyciu `EnumType.STRING`. Operacje wieloetapowe są wykonywane transakcyjnie — w przypadku wyjątku następuje rollback, co ogranicza ryzyko częściowego zapisu danych.

## Uruchomienie projektu

### Wymagania

- JDK 17,
- Maven 3.8 lub nowszy,
- Docker z obsługą Docker Compose.

### 1. Uruchom bazę danych

W katalogu projektu wykonaj:

```bash
docker compose up -d
```

Kontener uruchamia PostgreSQL na lokalnym porcie `5433`. Konfiguracja developerska:

```text
baza:     karent
użytkownik: karent
hasło:    karent
adres:    jdbc:postgresql://127.0.0.1:5433/karent
```

### 2. Uruchom aplikację

```bash
mvn clean javafx:run
```

Przy pierwszym uruchomieniu Maven pobierze zależności. Hibernate utworzy lub zaktualizuje wymagane tabele, a aplikacja automatycznie doda demonstracyjne dane startowe.

### 3. Zaloguj się

Przykładowe konto administratora:

```text
login: admin
hasło: admin
```

Dostępne są także konta demonstracyjne `pracownik / pracownik` oraz `anna / anna`.

### Zatrzymanie bazy

```bash
docker compose down
```

Dane PostgreSQL pozostają w nazwanym wolumenie `karent_pgdata`. Aby rozpocząć pracę z całkowicie pustą bazą, wolumen trzeba usunąć osobno.

## Dane demonstracyjne

`SeedData` tworzy zestaw przykładowych pracowników, klientów, samochodów osobowych i dostawczych, polis, przypisań ubezpieczeń, rezerwacji, wypożyczeń, faktur oraz zgłoszeń serwisowych. Seed jest idempotentny — kolejne uruchomienie aplikacji aktualizuje lub uzupełnia dane zamiast bezwarunkowo tworzyć duplikaty.

## Najważniejsze elementy techniczne

- model domenowy zawierający dziedziczenie, asocjacje z atrybutami i relacje dwukierunkowe,
- własny wrapper transakcyjny zapewniający commit, rollback i zamykanie `EntityManager`,
- zapytania budowane przy użyciu JPA Criteria API,
- osobna warstwa serwisowa dla pojazdów, ubezpieczeń, rezerwacji, wypożyczeń i faktur,
- automatyczna synchronizacja statusów rezerwacji, wypożyczeń i pojazdów,
- dynamiczne filtrowanie danych prezentowanych w `TableView`,
- własne okna modalne, potwierdzenia i obsługa błędów,
- responsywne dopasowanie rozmiaru okna do dostępnej przestrzeni ekranu,
- automatyczne przygotowanie środowiska demonstracyjnego.

## Możliwe kierunki rozwoju

- udostępnienie w UI istniejących operacji rezerwacji, wypożyczeń i faktur,
- rozbudowa ekranów historii pojazdu i zgłoszeń serwisowych,
- role i uprawnienia dla administratora, pracownika oraz klienta,
- eksport faktur i raportów do PDF,
- raportowanie wykorzystania floty i przychodów,
- testy jednostkowe oraz integracyjne z bazą uruchamianą w Testcontainers,
- przeniesienie danych połączenia do zmiennych środowiskowych,
- zastosowanie silnego, adaptacyjnego algorytmu haszowania haseł, np. Argon2 lub BCrypt.

## Autor

Projekt zrealizowany jako samodzielna aplikacja do portfolio, prezentująca praktyczne wykorzystanie języka Java, programowania obiektowego, relacyjnych baz danych, ORM, projektowania interfejsów desktopowych oraz modelowania procesów biznesowych.
