package mapademo.util;

import java.io.File;
import java.util.Optional;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import upv.ipc.sportlib.MapRegion;
import upv.ipc.sportlib.SportActivityApp;


// Helper para anadir un MapRegion a la libreria desde un dialogo modal.
// Lo usamos tanto desde Main (botones Agregar/Cambiar mapa) como desde Historial
// (boton "Anadir mapa" que aparece cuando una actividad no tiene mapa).
public final class MapDialogs {

    private MapDialogs() {}

    // Abre el dialogo de "Anadir mapa". Devuelve true si el usuario lo
    // confirmo y el mapa se guardo correctamente en la BBDD.
    public static boolean showAddMapDialog(Window owner) {

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Anadir mapa");
        if (owner != null) dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setResizable(true);

        ButtonType okBtn = new ButtonType("Anadir", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okBtn, ButtonType.CANCEL);

        Label header = new Label("Selecciona el JPG del mapa e introduce las\n"
                + "coordenadas del bounding box (las imprime el script Python).");
        header.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        header.setWrapText(true);

        // --- nombre ---
        Label lName = new Label("Nombre:");
        lName.setMinWidth(110);
        TextField nameField = new TextField();
        nameField.setPromptText("Ej: Montanejos");
        HBox.setHgrow(nameField, Priority.ALWAYS);
        HBox rowName = new HBox(10, lName, nameField);
        rowName.setAlignment(Pos.CENTER_LEFT);

        // --- imagen ---
        Label lImage = new Label("Imagen JPG:");
        lImage.setMinWidth(110);
        Button pickFileBtn = new Button("Elegir archivo...");
        Label fileNameLabel = new Label("(ninguno)");
        fileNameLabel.setStyle("-fx-text-fill: #6B7280;");
        // referencia compartida con la lambda del listener
        final File[] selected = new File[1];
        pickFileBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setInitialDirectory(new File("."));
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                    "Imagenes", "*.jpg", "*.jpeg", "*.png"));
            File f = fc.showOpenDialog(dialog.getDialogPane().getScene().getWindow());
            if (f != null) {
                selected[0] = f;
                fileNameLabel.setText(f.getName());
                fileNameLabel.setStyle("-fx-text-fill: #111111;");
            }
        });
        HBox rowImage = new HBox(10, lImage, pickFileBtn, fileNameLabel);
        rowImage.setAlignment(Pos.CENTER_LEFT);

        // --- coords (dos filas de dos) ---
        TextField latMin = coordField("Lat. min");
        TextField latMax = coordField("Lat. max");
        TextField lonMin = coordField("Lon. min");
        TextField lonMax = coordField("Lon. max");
        HBox rowCoords1 = new HBox(10, labeledField("Lat. min", latMin), labeledField("Lat. max", latMax));
        HBox rowCoords2 = new HBox(10, labeledField("Lon. min", lonMin), labeledField("Lon. max", lonMax));

        Label errorMsg = new Label();
        errorMsg.setStyle("-fx-text-fill: #ed1616;");
        errorMsg.setWrapText(true);
        errorMsg.managedProperty().bind(errorMsg.visibleProperty());
        errorMsg.setVisible(false);

        VBox content = new VBox(12, header, rowName, rowImage, rowCoords1, rowCoords2, errorMsg);
        content.setPadding(new Insets(15));
        content.setMinSize(520, 320);
        content.setPrefSize(520, 320);
        dialog.getDialogPane().setContent(content);

        dialog.getDialogPane().setMinSize(540, 360);
        dialog.getDialogPane().setPrefSize(540, 360);

        dialog.setOnShown(ev -> {
            Stage st = (Stage) dialog.getDialogPane().getScene().getWindow();
            st.setMinWidth(540);
            st.setMinHeight(360);
            st.setWidth(540);
            st.setHeight(360);
            st.centerOnScreen();
            st.toFront();
            nameField.requestFocus();
        });

        // Filtramos el cierre con OK para validar antes de cerrar el dialogo.
        // Si la validacion falla, consumimos el evento y mostramos el error.
        Button okNode = (Button) dialog.getDialogPane().lookupButton(okBtn);
        okNode.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            String name = nameField.getText() == null ? "" : nameField.getText().trim();
            if (name.isEmpty()) { showErr(errorMsg, "Falta el nombre"); ev.consume(); return; }
            if (selected[0] == null) { showErr(errorMsg, "Selecciona una imagen"); ev.consume(); return; }
            double latMinV, latMaxV, lonMinV, lonMaxV;
            try {
                latMinV = Double.parseDouble(latMin.getText().trim().replace(',', '.'));
                latMaxV = Double.parseDouble(latMax.getText().trim().replace(',', '.'));
                lonMinV = Double.parseDouble(lonMin.getText().trim().replace(',', '.'));
                lonMaxV = Double.parseDouble(lonMax.getText().trim().replace(',', '.'));
            } catch (NumberFormatException nfe) {
                showErr(errorMsg, "Las 4 coordenadas deben ser numeros (ej: 39.85)");
                ev.consume();
                return;
            }
            if (latMinV >= latMaxV || lonMinV >= lonMaxV) {
                showErr(errorMsg, "lat_min debe ser menor que lat_max, igual con lon");
                ev.consume();
                return;
            }
            // todo OK -> intentamos persistir
            MapRegion mr = SportActivityApp.getInstance()
                    .addMapRegion(name, selected[0], latMinV, latMaxV, lonMinV, lonMaxV);
            if (mr == null) {
                showErr(errorMsg, "No se pudo guardar el mapa (¿nombre repetido?)");
                ev.consume();
            }
            // si todo bien, dejamos que el evento siga -> el dialogo se cierra
        });

        Optional<ButtonType> res = dialog.showAndWait();
        return res.isPresent() && res.get() == okBtn;
    }

    // helper para construir un campo de coordenada con su etiqueta encima
    private static VBox labeledField(String label, TextField field) {
        Label l = new Label(label);
        l.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 11;");
        VBox v = new VBox(3, l, field);
        HBox.setHgrow(v, Priority.ALWAYS);
        return v;
    }

    private static TextField coordField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setPrefHeight(32);
        return tf;
    }

    private static void showErr(Label label, String msg) {
        label.setText(msg);
        label.setVisible(true);
    }

    // Alert sencillo "no hay mapa para esta region" (lo usamos cuando una actividad
    // entra sin mapa para informar al usuario).
    public static void infoSinMapa(Window owner) {
        Alert a = new Alert(Alert.AlertType.INFORMATION,
                "Esta actividad no tiene mapa asociado.\n"
                + "Pulsa el boton para anadir uno con su bounding box.");
        if (owner != null) a.initOwner(owner);
        a.setHeaderText(null);
        a.showAndWait();
    }
}
