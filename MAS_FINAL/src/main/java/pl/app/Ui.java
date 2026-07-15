package pl.app;

import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;

import java.io.PrintWriter;
import java.io.StringWriter;

public final class Ui {
    private Ui() {}

    static void alertInfo(String title, String content) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(title);
        a.setContentText(content);
        a.showAndWait();
    }

    static void alertException(String title, String content, Exception ex) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(title);
        a.setContentText(content);

        StringWriter sw = new StringWriter();
        ex.printStackTrace(new PrintWriter(sw));
        TextArea area = new TextArea(sw.toString());
        area.setEditable(false);
        area.setWrapText(false);
        area.setPrefWidth(820);
        area.setPrefHeight(320);
        a.getDialogPane().setExpandableContent(area);

        a.showAndWait();
    }
}