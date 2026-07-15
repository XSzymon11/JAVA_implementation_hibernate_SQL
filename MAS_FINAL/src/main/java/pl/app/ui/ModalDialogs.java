package pl.app.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

public final class ModalDialogs {
    private ModalDialogs() {}

    public static void info(Window owner, String title, String message) {
        show(owner, true, title, message, "Zamknij");
    }

    public static void error(Window owner, String title, String message) {
        show(owner, false, title, message, "Wróć");
    }

    public static boolean confirm(Window owner, String title, String message, String yesText, String noText) {
        Stage stage = baseStage(owner);

        Label icon = new Label("?");
        icon.getStyleClass().addAll("modal-icon", "modal-icon-error");

        Label t = new Label(title);
        t.getStyleClass().add("modal-title");

        Label msg = new Label(message);
        msg.getStyleClass().add("modal-message");
        msg.setWrapText(true);

        Button yes = new Button(yesText);
        yes.getStyleClass().add("button-primary");
        Button no = new Button(noText);
        no.getStyleClass().add("button-secondary");

        final boolean[] result = {false};
        yes.setOnAction(e -> {
            result[0] = true;
            stage.close();
        });
        no.setOnAction(e -> {
            result[0] = false;
            stage.close();
        });

        HBox buttons = new HBox(12, yes, no);
        buttons.setAlignment(Pos.CENTER);

        VBox card = new VBox(10,
                new HBox(10, icon, t),
                msg,
                new Region(),
                buttons
        );
        VBox.setVgrow(card.getChildren().get(2), Priority.ALWAYS);
        card.getStyleClass().add("modal-card");
        card.setMaxWidth(440);

        Scene scene = buildOverlayScene(card);
        stage.setScene(scene);
        stage.showAndWait();
        return result[0];
    }

    private static void show(Window owner, boolean success, String title, String message, String buttonText) {
        Stage stage = baseStage(owner);

        Label icon = new Label(success ? "✓" : "!");
        icon.getStyleClass().addAll("modal-icon", success ? "modal-icon-success" : "modal-icon-error");

        Label t = new Label(title);
        t.getStyleClass().add("modal-title");

        Label msg = new Label(message);
        msg.getStyleClass().add("modal-message");
        msg.setWrapText(true);

        Button close = new Button(buttonText);
        close.getStyleClass().add("button-primary");
        close.setOnAction(e -> stage.close());

        HBox btnRow = new HBox(close);
        btnRow.setAlignment(Pos.CENTER);

        VBox card = new VBox(10,
                new HBox(10, icon, t),
                msg,
                btnRow
        );
        card.getStyleClass().add("modal-card");
        card.setMaxWidth(440);

        Scene scene = buildOverlayScene(card);
        stage.setScene(scene);
        stage.showAndWait();
    }

    private static Stage baseStage(Window owner) {
        Stage stage = new Stage(StageStyle.TRANSPARENT);
        stage.initModality(Modality.WINDOW_MODAL);
        if (owner != null) stage.initOwner(owner);
        stage.setResizable(false);
        return stage;
    }

    private static Scene buildOverlayScene(VBox card) {
        StackPane overlay = new StackPane(card);
        overlay.getStyleClass().add("overlay");
        overlay.setAlignment(Pos.CENTER);
        StackPane.setMargin(card, new Insets(0));

        Scene scene = new Scene(overlay);
        scene.setFill(Color.TRANSPARENT);

        var css = ModalDialogs.class.getResource("/styles/style.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());
        return scene;
    }
}
