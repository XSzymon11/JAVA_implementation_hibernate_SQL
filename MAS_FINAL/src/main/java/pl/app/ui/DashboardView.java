package pl.app.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class DashboardView extends BorderPane {
    public DashboardView(Runnable onVehicleInsurance, Runnable onLogout) {
        setTop(buildHeader());
        setCenter(buildContent(onVehicleInsurance, onLogout));
    }

    private HBox buildHeader() {
        Label title = new Label("Karen’t – panel pracownika");
        title.getStyleClass().add("app-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        StackPane avatar = new StackPane(new Label("👤"));
        avatar.getStyleClass().add("avatar");

        HBox header = new HBox(12, title, spacer, avatar);
        header.getStyleClass().add("header-bar");
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    private VBox buildContent(Runnable onVehicleInsurance, Runnable onLogout) {
        Label section = new Label("Zarządzanie");
        section.getStyleClass().add("section-title");

        Button manageInsurance = menuButton("Zarządzaj ubezpieczeniami pojazdów", false);
        manageInsurance.setOnAction(e -> onVehicleInsurance.run());

        Button approveReservations = menuButton("Zatwierdź rezerwacje", false);
        approveReservations.setDisable(true);

        Button vehicleHistory = menuButton("Historia pojazdów", false);
        vehicleHistory.setDisable(true);

        Button serviceRequests = menuButton("Zgłoszenia serwisowe", false);
        serviceRequests.setDisable(true);

        Button logout = new Button("Wyloguj");
        logout.getStyleClass().add("button-secondary");
        logout.setOnAction(e -> onLogout.run());

        VBox menu = new VBox(10, manageInsurance, approveReservations, vehicleHistory, serviceRequests);
        menu.setFillWidth(true);

        VBox content = new VBox(14, section, menu, new Region(), logout);
        VBox.setVgrow(content.getChildren().get(content.getChildren().size() - 2), Priority.ALWAYS);

        content.getStyleClass().addAll("page", "page-content");
        content.setAlignment(Pos.TOP_CENTER);
        BorderPane.setAlignment(content, Pos.TOP_CENTER);
        BorderPane.setMargin(content, new Insets(0));
        return content;
    }

    private Button menuButton(String text, boolean primary) {
        Label label = new Label(text);
        Label arrow = new Label("›");
        arrow.getStyleClass().add("menu-arrow");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox graphic = new HBox(12, label, spacer, arrow);
        graphic.setAlignment(Pos.CENTER_LEFT);

        Button b = new Button();
        b.setMaxWidth(Double.MAX_VALUE);
        b.getStyleClass().add("menu-button");
        if (primary) b.getStyleClass().add("menu-button-primary");
        b.setGraphic(graphic);
        b.setMnemonicParsing(false);
        return b;
    }
}
