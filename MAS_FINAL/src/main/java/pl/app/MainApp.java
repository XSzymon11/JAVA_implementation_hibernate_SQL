package pl.app;

import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import pl.app.ui.DashboardView;
import pl.app.ui.VehicleInsuranceView;
import pl.auth.Session;
import pl.database.DatabaseService;
import pl.database.SeedData;

public class MainApp extends Application {
    private static final double MAX_MAIN_SIZE = 1040;

    private double loginSize = 520;
    private double mainSize = 1040;

    private boolean squareGuard = false;

    @Override
    public void start(Stage stage) {
        stage.setTitle("KaRent");
        applyAppIcon(stage);
        computeUiSizes();
        enforceSquareStage(stage);
        ensureSeeded();
        showLogin(stage);
        stage.show();
    }

    private void enforceSquareStage(Stage stage) {
        stage.widthProperty().addListener((obs, oldV, newV) -> {
            if (squareGuard) return;
            squareGuard = true;
            stage.setHeight(newV.doubleValue());
            squareGuard = false;
        });
        stage.heightProperty().addListener((obs, oldV, newV) -> {
            if (squareGuard) return;
            squareGuard = true;
            stage.setWidth(newV.doubleValue());
            squareGuard = false;
        });
    }

    private void computeUiSizes() {
        Rectangle2D vb = Screen.getPrimary().getVisualBounds();
        double maxSquare = Math.min(vb.getWidth(), vb.getHeight());
        mainSize = Math.min(MAX_MAIN_SIZE, Math.floor(maxSquare * 0.90));
        loginSize = Math.floor(mainSize / 2.0);
    }

    private void showLogin(Stage stage) {
        Session.clear();

        Scene scene = new Scene(new LoginView(() -> showDashboard(stage)), loginSize, loginSize);

        var css = getClass().getResource("/styles/style.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }

        stage.setScene(scene);
        stage.setWidth(loginSize);
        stage.setHeight(loginSize);
        stage.setMinWidth(loginSize);
        stage.setMinHeight(loginSize);
        stage.setResizable(true);
        stage.centerOnScreen();
    }

    private void showDashboard(Stage stage) {
        stage.getScene().setRoot(new DashboardView(
                () -> showVehicleInsurance(stage),
                () -> showLogin(stage)
        ));
        stage.setWidth(mainSize);
        stage.setHeight(mainSize);
        stage.setMinWidth(mainSize);
        stage.setMinHeight(mainSize);
        stage.centerOnScreen();
    }

    private void showVehicleInsurance(Stage stage) {
        VehicleInsuranceView view = new VehicleInsuranceView(() -> showDashboard(stage));
        stage.getScene().setRoot(view.getView());

        stage.setWidth(mainSize);
        stage.setHeight(mainSize);
        stage.setMinWidth(mainSize);
        stage.setMinHeight(mainSize);
        stage.centerOnScreen();
    }

    private void ensureSeeded() {
        DatabaseService.tx(em -> {
            SeedData.seed(em);
            return null;
        });
    }

    public static void main(String[] args) {
        launch();
    }

    private void applyAppIcon(Stage stage) {
        try {
            var is = getClass().getResourceAsStream("/karent.png");
            if (is != null) {
                stage.getIcons().add(new Image(is));
            }
        } catch (Exception ignored) {

        }
    }
}
