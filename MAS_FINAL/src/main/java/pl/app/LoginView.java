package pl.app;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import pl.auth.Session;
import pl.database.DatabaseService;
import pl.database.Osoba;

import java.util.Optional;

public class LoginView extends BorderPane {
    private final Runnable onLoginSuccess;

    public LoginView(Runnable onLoginSuccess) {
        this.onLoginSuccess = onLoginSuccess;
        buildUi();
    }

    private void buildUi() {
        Label title = new Label("KaRent – logowanie");
        title.getStyleClass().add("app-title");

        TextField loginField = new TextField();
        loginField.setPromptText("Login (np. admin)");

        PasswordField passField = new PasswordField();
        passField.setPromptText("Hasło (np. admin)");

        Button loginBtn = new Button("Zaloguj");
        loginBtn.setDefaultButton(true);

        VBox box = new VBox(10,
                title,
                new Label("Login"), loginField,
                new Label("Hasło"), passField,
                loginBtn
        );
        box.setPadding(new Insets(24));
        box.setMaxWidth(420);

        box.getStyleClass().add("card");

        StackPane center = new StackPane(box);
        center.setPadding(new Insets(24));

        setCenter(center);

        loginBtn.setOnAction(e -> {
            String login = loginField.getText() == null ? "" : loginField.getText().trim();
            String pass = passField.getText() == null ? "" : passField.getText();

            if (login.isBlank()) {
                Ui.alertInfo("Nie wpisano loginu", "Wpisz login, aby się zalogować");
                return;
            }

            try {
                Optional<Osoba> osoba = authenticate(login, pass);
                if (osoba.isPresent()) {
                    Session.setCurrent(osoba.get());
                    onLoginSuccess.run();
                } else {
                    Ui.alertInfo("Niepoprawne dane", "Niepoprawny login lub hasło");
                }
            } catch (Exception ex) {
                Ui.alertException("Błąd logowania", "Nie udało się zweryfikować danych w bazie", ex);
            }
        });
    }

    private Optional<Osoba> authenticate(String login, String password) {
        return DatabaseService.tx(em -> {
            var cb = em.getCriteriaBuilder();
            var query = cb.createQuery(Osoba.class);
            var osoba = query.from(Osoba.class);

            query.select(osoba)
                    .where(cb.equal(osoba.get("login"), login));

            return em.createQuery(query)
                    .getResultStream()
                    .findFirst()
                    .filter(o -> o.zaloguj(login, password));
        });
    }
}
