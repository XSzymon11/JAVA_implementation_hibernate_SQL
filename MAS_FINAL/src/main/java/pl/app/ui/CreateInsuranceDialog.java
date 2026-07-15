package pl.app.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import pl.app.service.VehicleInsuranceService;
import pl.database.Ubezpieczenie;
import pl.database.enums.StatusUbezpieczenia;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CreateInsuranceDialog extends Stage {
    private boolean saved = false;
    private String createdPolicyNumber;

    public CreateInsuranceDialog(Window owner, VehicleInsuranceService service) {
        initOwner(owner);
        initModality(Modality.WINDOW_MODAL);
        setTitle("Nowe ubezpieczenie");

        applyIcon();

        TextField numerPolisy = new TextField();
        numerPolisy.setPromptText("Numer polisy (unikalny)");

        TextField firma = new TextField();
        firma.setPromptText("Firma");

        TextArea opis = new TextArea();
        opis.setPromptText("Opis");
        opis.setPrefRowCount(3);

        TextField cena = new TextField();
        cena.setPromptText("Cena (np. 1200,00)");

        DatePicker od = new DatePicker();
        od.setPromptText("Data rozpoczęcia");

        DatePicker doDp = new DatePicker();
        doDp.setPromptText("Data zakończenia");

        ComboBox<StatusUbezpieczenia> status = new ComboBox<>();
        status.getItems().addAll(StatusUbezpieczenia.values());
        status.setValue(StatusUbezpieczenia.AKTYWNE);
        status.setCellFactory(cb -> new ListCell<>() {
            @Override
            protected void updateItem(StatusUbezpieczenia item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : friendlyStatus(item));
            }
        });
        status.setButtonCell(status.getCellFactory().call(null));

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.addRow(0, new Label("Numer polisy"), numerPolisy);
        grid.addRow(1, new Label("Firma"), firma);
        grid.addRow(2, new Label("Cena"), cena);
        grid.addRow(3, new Label("Ważność od"), od);
        grid.addRow(4, new Label("Ważność do"), doDp);
        grid.addRow(5, new Label("Status"), status);
        grid.addRow(6, new Label("Opis"), opis);

        ColumnConstraints c1 = new ColumnConstraints();
        c1.setMinWidth(140);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c1, c2);

        Button cancel = new Button("Anuluj");
        cancel.getStyleClass().add("button-secondary");
        cancel.setOnAction(e -> close());

        Button save = new Button("Zapisz");
        save.getStyleClass().add("button-primary");
        save.setDefaultButton(true);
        save.setOnAction(e -> {
            try {
                BigDecimal price = parsePrice(cena.getText());
                LocalDate d1 = od.getValue();
                LocalDate d2 = doDp.getValue();

                Ubezpieczenie created = service.createInsurance(
                        numerPolisy.getText(),
                        firma.getText(),
                        opis.getText(),
                        price,
                        d1,
                        d2,
                        status.getValue()
                );

                saved = true;
                createdPolicyNumber = created.getNumerPolisy();
                close();
            } catch (Exception ex) {
                ModalDialogs.error(getOwner(), "Błąd", ex.getMessage() == null ? "Nie udało się utworzyć ubezpieczenia" : ex.getMessage());
            }
        });

        HBox buttons = new HBox(12, cancel, save);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        VBox card = new VBox(14,
                new Label("Utwórz ubezpieczenie"),
                grid,
                buttons
        );
        card.getStyleClass().add("card");

        VBox root = new VBox(card);
        root.getStyleClass().addAll("page", "page-content");
        root.setPadding(new Insets(24));
        root.setAlignment(Pos.TOP_CENTER);

        Scene scene = new Scene(root, 520, 520);
        var css = getClass().getResource("/styles/style.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());
        setScene(scene);

        setResizable(false);
    }

    public boolean isSaved() {
        return saved;
    }

    public String getCreatedPolicyNumber() {
        return createdPolicyNumber;
    }

    private static BigDecimal parsePrice(String txt) {
        String s = txt == null ? "" : txt.trim().replace(",", ".");
        if (s.isBlank()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(s);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Cena musi być poprawną liczbą");
        }
    }

    private static String friendlyStatus(StatusUbezpieczenia status) {
        return switch (status) {
            case SZKIC -> "Szkic";
            case AKTYWNE -> "Aktywne";
            case WYGASLE -> "Wygasłe";
            case USUNIETE -> "Usunięte";
        };
    }

    private void applyIcon() {
        try {
            var is = getClass().getResourceAsStream("/karent.png");
            if (is != null) {
                getIcons().add(new Image(is));
            }
        } catch (Exception ignored) {
        }
    }
}
