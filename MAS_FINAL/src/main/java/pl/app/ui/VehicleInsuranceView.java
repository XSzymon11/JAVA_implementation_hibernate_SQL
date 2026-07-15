package pl.app.ui;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.stage.Window;
import pl.app.service.VehicleInsuranceService;
import pl.database.Pojazd;
import pl.database.PojazdUbezpieczenie;
import pl.database.Ubezpieczenie;
import pl.database.enums.StatusPojazdu;
import pl.database.enums.StatusUbezpieczenia;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class VehicleInsuranceView {
    private final BorderPane root = new BorderPane();
    private final VehicleInsuranceService service = new VehicleInsuranceService();
    private final Runnable onBack;

    private final ObservableList<Pojazd> vehicles = FXCollections.observableArrayList();
    private List<Pojazd> allVehiclesCache = List.of();

    public VehicleInsuranceView(Runnable onBack) {
        this.onBack = Objects.requireNonNull(onBack);
        showVehiclesScreen();
    }

    public Parent getView() {
        return root;
    }

    private void showVehiclesScreen() {
        allVehiclesCache = service.listAllVehicles();
        vehicles.setAll(allVehiclesCache);

        root.setTop(buildNavHeader("Powrót", onBack));

        Label title = new Label("Pojazdy");
        title.getStyleClass().add("app-title");

        ComboBox<StatusPojazdu> statusFilter = new ComboBox<>();
        statusFilter.getItems().add(null);
        statusFilter.getItems().addAll(StatusPojazdu.values());
        statusFilter.setValue(null);
        statusFilter.setPromptText("Status");
        statusFilter.setCellFactory(cb -> new ListCell<>() {
            @Override
            protected void updateItem(StatusPojazdu item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else if (item == null) {
                    setText("Wszystkie");
                } else {
                    setText(friendlyStatus(item));
                }
            }
        });
        statusFilter.setButtonCell(statusFilter.getCellFactory().call(null));

        TextField search = new TextField();
        search.setPromptText("Szukaj numeru rejestracyjnego");
        search.setMinWidth(260);

        HBox filters = new HBox(12, statusFilter, search);
        filters.setAlignment(Pos.CENTER_LEFT);

        TableView<Pojazd> table = buildVehiclesTable();
        table.setItems(vehicles);

        Runnable applyFilter = () -> {
            StatusPojazdu st = statusFilter.getValue();
            String q = search.getText() == null ? "" : search.getText().trim().toLowerCase();

            List<Pojazd> filtered = allVehiclesCache.stream()
                    .filter(p -> st == null || p.getStatusPojazdu() == st)
                    .filter(p -> q.isEmpty() || (p.getNumerRejestracyjny() != null && p.getNumerRejestracyjny().toLowerCase().contains(q)))
                    .collect(Collectors.toList());

            vehicles.setAll(filtered);
        };

        statusFilter.valueProperty().addListener((obs, o, n) -> applyFilter.run());
        search.textProperty().addListener((obs, o, n) -> applyFilter.run());

        table.setRowFactory(tv -> {
            TableRow<Pojazd> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2 && !row.isEmpty()) {
                    showVehicleDetailsScreen(row.getItem());
                }
            });
            return row;
        });

        VBox content = new VBox(14, title, filters, table);
        content.getStyleClass().addAll("page", "page-content");
        content.setAlignment(Pos.TOP_CENTER);

        ScrollPane scroller = new ScrollPane(content);
        scroller.setFitToWidth(true);
        scroller.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroller.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroller.getStyleClass().add("transparent-scroll-pane");

        root.setCenter(scroller);
    }

    private void showVehicleDetailsScreen(Pojazd vehicle) {
        root.setTop(buildNavHeader("Powrót", this::showVehiclesScreen));

        Label title = new Label(vehicle.getMarka() + " " + vehicle.getModel() + " (" + vehicle.getNumerRejestracyjny() + ")");
        title.getStyleClass().add("app-title");

        Label rok = new Label("Rok: " + vehicle.getRokProdukcji());
        Label przebieg = new Label("Przebieg: " + vehicle.getPrzebieg() + " km");

        HBox statusRow = new HBox(8, new Label("Status:"), pill(friendlyStatus(vehicle.getStatusPojazdu()), statusPillClass(vehicle.getStatusPojazdu())));
        statusRow.setAlignment(Pos.CENTER_LEFT);

        VBox meta = new VBox(6, rok, przebieg, statusRow);
        meta.setPadding(new Insets(0, 0, 8, 0));

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab insuranceTab = new Tab("Ubezpieczenia");
        Tab historyTab = new Tab("Historia", new Label("(opcjonalnie)"));
        Tab eventsTab = new Tab("Zdarzenia", new Label("(opcjonalnie)"));

        VBox insuranceContent = new VBox(12);
        insuranceContent.setPadding(new Insets(14, 0, 0, 0));

        TableView<PojazdUbezpieczenie> assignmentsTable = buildAssignmentsTable(vehicle);
        assignmentsTable.setItems(FXCollections.observableArrayList(loadAssignments(vehicle)));

        Button add = new Button("Dodaj ubezpieczenie");
        add.getStyleClass().add("button-primary");
        add.setOnAction(e -> showAddInsuranceScreen(vehicle));
        add.setMaxWidth(260);

        VBox addRow = new VBox(add);
        addRow.setAlignment(Pos.CENTER);
        addRow.setPadding(new Insets(8, 0, 0, 0));

        insuranceContent.getChildren().addAll(assignmentsTable, addRow);
        insuranceTab.setContent(insuranceContent);
        tabs.getTabs().addAll(insuranceTab, historyTab, eventsTab);

        VBox content = new VBox(14, title, meta, tabs);
        content.getStyleClass().addAll("page", "page-content");
        content.setAlignment(Pos.TOP_CENTER);

        ScrollPane scroller = new ScrollPane(content);
        scroller.setFitToWidth(true);
        scroller.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroller.getStyleClass().add("transparent-scroll-pane");
        root.setCenter(scroller);
    }

    private void showAddInsuranceScreen(Pojazd vehicle) {
        root.setTop(buildNavHeader("Powrót", () -> showVehicleDetailsScreen(vehicle)));

        Label title = new Label("Dodaj ubezpieczenie");
        title.getStyleClass().add("app-title");

        Label pickLabel = new Label("Wybierz ubezpieczenie");
        pickLabel.getStyleClass().add("section-title");

        Button createNew = new Button("Utwórz nowe ubezpieczenie");
        createNew.getStyleClass().add("button-secondary");

        TableView<Ubezpieczenie> available = buildAvailableInsurancesTable();
        available.setItems(FXCollections.observableArrayList(loadAvailableActiveInsurances()));
        available.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        createNew.setOnAction(e -> {
            Window owner = windowOf(root);
            CreateInsuranceDialog dlg = new CreateInsuranceDialog(owner, service);
            dlg.showAndWait();

            if (dlg.isSaved()) {
                available.setItems(FXCollections.observableArrayList(loadAvailableActiveInsurances()));
                String createdNr = dlg.getCreatedPolicyNumber();
                if (createdNr != null) {
                    available.getItems().stream()
                            .filter(u -> createdNr.equals(u.getNumerPolisy()))
                            .findFirst()
                            .ifPresent(u -> available.getSelectionModel().select(u));
                }
            }
        });

        HBox pickHeader = new HBox(12, pickLabel, new Region(), createNew);
        pickHeader.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(pickHeader.getChildren().get(1), Priority.ALWAYS);

        DatePicker od = new DatePicker();
        od.setPromptText("Data od");
        DatePicker doDp = new DatePicker();
        doDp.setPromptText("Data do");

        HBox dates = new HBox(12, od, doDp);
        dates.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(od, Priority.ALWAYS);
        HBox.setHgrow(doDp, Priority.ALWAYS);
        od.setMaxWidth(Double.MAX_VALUE);
        doDp.setMaxWidth(Double.MAX_VALUE);

        TextArea notes = new TextArea();
        notes.setPromptText("Uwagi (opcjonalnie)");
        notes.setPrefRowCount(4);

        Button cancel = new Button("Anuluj");
        cancel.getStyleClass().add("button-secondary");

        Button ok = new Button("Zatwierdź");
        ok.getStyleClass().add("button-primary");

        HBox buttons = new HBox(12, cancel, ok);
        buttons.setAlignment(Pos.CENTER);

        cancel.setOnAction(e -> {
            Window owner = windowOf(root);
            boolean yes = ModalDialogs.confirm(owner,
                    "Dodaj ubezpieczenie",
                    "Czy chcesz zrezygnować z wypełniania wniosku o polisę?",
                    "Tak",
                    "Nie"
            );
            if (yes) showVehicleDetailsScreen(vehicle);
        });

        ok.setOnAction(e -> {
            Window owner = windowOf(root);

            Ubezpieczenie sel = available.getSelectionModel().getSelectedItem();
            LocalDate d1 = od.getValue();
            LocalDate d2 = doDp.getValue();

            if (sel == null || d1 == null || d2 == null || d2.isBefore(d1)) {
                ModalDialogs.error(owner, "Błąd", "Proszę uzupełnić poprawnie wszystkie pola formularza");
                return;
            }

            try {
                service.addInsuranceToVehicle(vehicle.getId(), sel.getId(), d1, d2, notes.getText());
                ModalDialogs.info(owner, "Ubezpieczenie dodane", "Ubezpieczenie zostało pomyślnie dodane do pojazdu");
                showVehicleDetailsScreen(refreshVehicle(vehicle));
            } catch (Exception ex) {
                ModalDialogs.error(owner, "Błąd", ex.getMessage() == null ? "Nie udało się dodać ubezpieczenia" : ex.getMessage());
            }
        });

        VBox card = new VBox(12,
                pickHeader,
                available,
                new Separator(),
                dates,
                notes,
                buttons
        );
        card.getStyleClass().add("card");
        card.setMaxWidth(860);

        VBox content = new VBox(14, title, card);
        content.getStyleClass().addAll("page", "page-content");
        content.setAlignment(Pos.TOP_CENTER);

        ScrollPane scroller = new ScrollPane(content);
        scroller.setFitToWidth(true);
        scroller.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroller.getStyleClass().add("transparent-scroll-pane");
        root.setCenter(scroller);
    }


    private HBox buildNavHeader(String backText, Runnable onBack) {
        Button back = new Button("←  " + backText);
        back.getStyleClass().add("back-button");
        back.setOnAction(e -> onBack.run());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        StackPane avatar = new StackPane(new Label("👤"));
        avatar.getStyleClass().add("avatar");

        HBox header = new HBox(12, back, spacer, avatar);
        header.getStyleClass().add("header-bar");
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    private TableView<Pojazd> buildVehiclesTable() {
        TableView<Pojazd> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(420);

        TableColumn<Pojazd, String> nr = new TableColumn<>("NR REJ");
        nr.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getNumerRejestracyjny()));

        TableColumn<Pojazd, String> marka = new TableColumn<>("MARKA");
        marka.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getMarka()));

        TableColumn<Pojazd, String> model = new TableColumn<>("MODEL");
        model.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getModel()));

        TableColumn<Pojazd, StatusPojazdu> status = new TableColumn<>("STATUS");
        status.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getStatusPojazdu()));
        status.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(StatusPojazdu item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                setGraphic(pill(friendlyStatus(item), statusPillClass(item)));
                setText(null);
            }
        });

        TableColumn<Pojazd, Void> go = new TableColumn<>("");
        go.setMaxWidth(48);
        go.setMinWidth(48);
        go.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("›");
            {
                btn.getStyleClass().add("table-arrow-button");
                btn.setOnAction(e -> {
                    Pojazd v = getTableView().getItems().get(getIndex());
                    showVehicleDetailsScreen(v);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btn);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        table.getColumns().addAll(nr, marka, model, status, go);
        return table;
    }

    private TableView<PojazdUbezpieczenie> buildAssignmentsTable(Pojazd vehicle) {
        TableView<PojazdUbezpieczenie> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(360);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy");

        TableColumn<PojazdUbezpieczenie, String> polisa = new TableColumn<>("POLISA");
        polisa.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getUbezpieczenie().getNumerPolisy()));

        TableColumn<PojazdUbezpieczenie, String> firma = new TableColumn<>("FIRMA");
        firma.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getUbezpieczenie().getFirma()));

        TableColumn<PojazdUbezpieczenie, String> status = new TableColumn<>("STATUS");
        status.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(assignmentStatusText(c.getValue())));
        status.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                boolean active = "Aktywne".equals(item);
                String cls = active ? "pill-green" : "pill-gray";
                setGraphic(pill(item, cls));
                setText(null);
            }
        });

        TableColumn<PojazdUbezpieczenie, String> okres = new TableColumn<>("OKRES");
        okres.setCellValueFactory(c -> {
            LocalDate d1 = c.getValue().getDataOd();
            LocalDate d2 = c.getValue().getDataDo();
            String s = (d1 == null || d2 == null) ? "" : (fmt.format(d1) + "–" + fmt.format(d2));
            return new ReadOnlyObjectWrapper<>(s);
        });

        TableColumn<PojazdUbezpieczenie, Void> del = new TableColumn<>("");
        del.setMaxWidth(120);
        del.setMinWidth(120);
        del.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button();
            {
                btn.getStyleClass().addAll("button-secondary", "compact-table-button");
                btn.setOnAction(e -> {
                    PojazdUbezpieczenie a = getTableView().getItems().get(getIndex());
                    if (a == null || a.isUsuniete()) return;

                    Window owner = windowOf(root);
                    boolean yes = ModalDialogs.confirm(owner,
                            "Usuń ubezpieczenie",
                            "Czy chcesz usunąć ubezpieczenie z pojazdu?\nPolisa wróci do listy dostępnych",
                            "Usuń",
                            "Anuluj"
                    );
                    if (!yes) return;

                    try {
                        service.removeInsuranceFromVehicle(a.getId());
                        a.setUsuniete(true);
                        getTableView().setItems(FXCollections.observableArrayList(loadAssignments(vehicle)));
                    } catch (Exception ex) {
                        ModalDialogs.error(owner, "Błąd", ex.getMessage() == null ? "Nie udało się usunąć ubezpieczenia" : ex.getMessage());
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                PojazdUbezpieczenie a = getTableView().getItems().get(getIndex());
                boolean removed = a == null || a.isUsuniete();
                btn.setText(removed ? "Usunięte" : "Usuń");
                btn.setDisable(removed);
                setGraphic(btn);
                setAlignment(Pos.CENTER);
            }
        });

        table.getColumns().addAll(polisa, firma, status, okres, del);
        return table;
    }

    private TableView<Ubezpieczenie> buildAvailableInsurancesTable() {
        TableView<Ubezpieczenie> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(220);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy");

        TableColumn<Ubezpieczenie, String> polisa = new TableColumn<>("POLISA");
        polisa.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getNumerPolisy()));

        TableColumn<Ubezpieczenie, String> firma = new TableColumn<>("FIRMA");
        firma.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getFirma()));

        TableColumn<Ubezpieczenie, String> waznosc = new TableColumn<>("WAŻNOŚĆ");
        waznosc.setCellValueFactory(c -> {
            LocalDate d1 = c.getValue().getDataRozpoczecia();
            LocalDate d2 = c.getValue().getDataZakonczenia();
            String s = (d1 == null || d2 == null) ? "" : (fmt.format(d1) + "–" + fmt.format(d2));
            return new ReadOnlyObjectWrapper<>(s);
        });

        TableColumn<Ubezpieczenie, StatusUbezpieczenia> st = new TableColumn<>("STATUS");
        st.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getStatusUbezpieczenia()));
        st.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(StatusUbezpieczenia item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                boolean active = item == StatusUbezpieczenia.AKTYWNE;
                setGraphic(pill(active ? "Aktywne" : "Wygasłe", active ? "pill-green" : "pill-gray"));
                setText(null);
            }
        });

        table.getColumns().addAll(polisa, firma, waznosc, st);
        return table;
    }

    private Label pill(String text, String styleClass) {
        Label l = new Label(text);
        l.getStyleClass().addAll("pill", styleClass);
        return l;
    }

    private String statusPillClass(StatusPojazdu s) {
        return switch (s) {
            case DOSTEPNY -> "pill-green";
            default -> "pill-gray";
        };
    }

    private static String friendlyStatus(StatusPojazdu s) {
        return switch (s) {
            case DOSTEPNY -> "Dostępny";
            case WYNAJETY -> "Wynajęty";
            case W_SERWISIE -> "W serwisie";
            case WYCOFANY -> "Wycofany";
        };
    }

    private static String assignmentStatusText(PojazdUbezpieczenie a) {
        if (a == null) return "";
        if (a.isUsuniete()) return "Usunięte";

        LocalDate now = LocalDate.now();
        if (a.getUbezpieczenie().getStatusUbezpieczenia() == StatusUbezpieczenia.WYGASLE) return "Wygasłe";

        LocalDate from = a.getDataOd();
        LocalDate to = a.getDataDo();
        boolean active = (now.isEqual(from) || now.isAfter(from)) && (now.isEqual(to) || now.isBefore(to));
        return active ? "Aktywne" : "Wygasłe";
    }

    private static Window windowOf(Parent p) {
        Scene sc = p.getScene();
        return sc == null ? null : sc.getWindow();
    }

    private List<PojazdUbezpieczenie> loadAssignments(Pojazd vehicle) {
        List<PojazdUbezpieczenie> list = new ArrayList<>(vehicle.getPrzypisaniaUbezpieczen());
        list.sort(Comparator
                .comparing(PojazdUbezpieczenie::isUsuniete)
                .thenComparing(PojazdUbezpieczenie::getDataOd, Comparator.nullsLast(Comparator.reverseOrder())));
        return list;
    }

    private Pojazd refreshVehicle(Pojazd vehicle) {
        if (vehicle == null || vehicle.getId() == null) return vehicle;
        return service.findVehicleWithInsuranceAssignments(vehicle.getId()).orElse(vehicle);
    }

    private List<Ubezpieczenie> loadAvailableActiveInsurances() {
        return service.listAvailableActiveInsurances();
    }
}
