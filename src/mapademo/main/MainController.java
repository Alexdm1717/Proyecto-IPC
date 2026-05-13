/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */
package mapademo.main;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polyline;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import upv.ipc.sportlib.Activity;
import upv.ipc.sportlib.MapProjection;
import upv.ipc.sportlib.MapRegion;
import upv.ipc.sportlib.SportActivityApp;
import upv.ipc.sportlib.TrackPoint;

/**
 * FXML Controller class
 *
 * @author alexi
 */
public class MainController implements Initializable {

    @FXML
    private ImageView map;
    @FXML
    private Button switch_map_button;
    @FXML
    private Slider zoom_slider;
    @FXML
    private Button zoom_up_button;
    @FXML
    private Button zoom_down_button;
    @FXML
    private ScrollPane map_scrollpane;
    @FXML
    private Pane map_pane;

    private Group zoomGroup;

    /** Lista lateral que muestra todos los POIs añadidos al mapa. */
    @FXML
    private ListView<Poi> map_listview;

    @FXML
    private Button load_track_button;

    // ── Variables para el pan con arrastre (clic izquierdo) ───────────────
    private double dragStartX, dragStartY;
    private double scrollStartH, scrollStartV;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        zoom_slider.setMin(0.5);
        zoom_slider.setMax(1.5);
        zoom_slider.setValue(1.0);

        // Listener que invoca zoom() cada vez que el slider cambia de valor.
        zoom_slider.valueProperty().addListener(
                (observable, oldVal, newVal) -> zoom((Double) newVal)
        );

        map_listview.setCellFactory(listView -> new ListCell<Poi>() {
            @Override
            protected void updateItem(Poi poi, boolean empty) {
                super.updateItem(poi, empty);

                if (empty || poi == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(poi.getCode() + " - " + poi.getPosition());
                }
            }
        });
    }

    @FXML
    void zoomIn(ActionEvent event) {
        zoom_slider.setValue(zoom_slider.getValue() + 0.1);
    }

    @FXML
    void zoomOut(ActionEvent event) {
        zoom_slider.setValue(zoom_slider.getValue() - 0.1);
    }

    private void zoom(double scaleValue) {
        // Guardamos la posición del scroll antes de escalar
        double scrollH = map_scrollpane.getHvalue();
        double scrollV = map_scrollpane.getVvalue();

        // Aplicamos el zoom escalando el Group en ambos ejes
        zoomGroup.setScaleX(scaleValue);
        zoomGroup.setScaleY(scaleValue);

        // Restauramos la posición del scroll para que el centro visual
        // permanezca estable durante el zoom
        map_scrollpane.setHvalue(scrollH);
        map_scrollpane.setVvalue(scrollV);
    }

    @FXML
    private void cambiarMapa(ActionEvent event) throws IOException {
        FileChooser fc = new FileChooser();
        fc.setInitialDirectory(new File("."));

        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Imagenes", "*.png", "*.jpg", "*.jpeg")
        );

        File ogFile = fc.showOpenDialog(zoom_slider.getScene().getWindow());

        if (ogFile == null) { return; }

        guardarImagen(ogFile);
        buildMap(ogFile);
    }

    /**
     * Guarda una copia del archivo dentro del proyecto
     * y devuelve la ruta local para la base de datos.
     */
    private String guardarImagen(File archivoOriginal) {
        File directorioDestino = new File("maps");
        if (!directorioDestino.exists()) {
            directorioDestino.mkdirs();
        }

        File archivoDestino = new File(directorioDestino, archivoOriginal.getName());
        String rutaRelativa = "maps/" + archivoOriginal.getName();

        try {
            if (!archivoDestino.exists()) {
                Files.copy(archivoOriginal.toPath(), archivoDestino.toPath());
                System.out.println("Imagen copiada con exito");
            } else {
                System.out.println("La imagen ya existe");
            }
            return rutaRelativa;
        } catch (IOException e) {
            System.out.println(e);
            return null;
        }
    }

    private void buildMap(File imgFile) {
        // Comprobacion defensiva
        if (!imgFile.exists()) {
            map_scrollpane.setContent(new Label("Imagen no encontrada"));
            System.out.println("Imagen no encontrada");
            return;
        }

        // Carga SÍNCRONA (false) para garantizar que getWidth()/getHeight()
        // devuelvan el valor real y no 0 (carga asíncrona por defecto).
        Image img = new Image(imgFile.toURI().toString(), false);

        double W = img.getWidth();
        double H = img.getHeight();

        // ── mapPane: lienzo del mapa ───────────────────────────────────
        map_pane = new Pane();
        map_pane.setPrefSize(W, H);
        map_pane.setMinSize(W, H);
        map_pane.setMaxSize(W, H);

        // Añadimos la imagen como fondo del Pane
        ImageView iv = new ImageView(img);
        iv.setFitWidth(W);
        iv.setFitHeight(H);
        map_pane.getChildren().add(iv);

        // ── CLIC DERECHO → abre el diálogo de POI directamente ────────
        map_pane.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                addPoi(e.getX(), e.getY());
            }
        });

        // ── CLIC IZQUIERDO presionado → guarda el punto de inicio ─────
        map_pane.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                dragStartX   = e.getSceneX();
                dragStartY   = e.getSceneY();
                scrollStartH = map_scrollpane.getHvalue();
                scrollStartV = map_scrollpane.getVvalue();
                map_pane.setStyle("-fx-cursor: closed-hand;");
            }
        });

        // ── CLIC IZQUIERDO soltado → restaura el cursor ────────────────
        map_pane.setOnMouseReleased(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                map_pane.setStyle("");
            }
        });

        // ── ARRASTRE con clic izquierdo → pan del mapa ────────────────
        map_pane.setOnMouseDragged(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                double deltaX = e.getSceneX() - dragStartX;
                double deltaY = e.getSceneY() - dragStartY;

                double mapW  = map_pane.getWidth()  * zoomGroup.getScaleX();
                double mapH  = map_pane.getHeight() * zoomGroup.getScaleY();
                double viewW = map_scrollpane.getViewportBounds().getWidth();
                double viewH = map_scrollpane.getViewportBounds().getHeight();

                // Normalizamos el desplazamiento al rango [0, 1] del ScrollPane
                double newH = scrollStartH - deltaX / (mapW - viewW);
                double newV = scrollStartV - deltaY / (mapH - viewH);

                map_scrollpane.setHvalue(Math.max(0, Math.min(1, newH)));
                map_scrollpane.setVvalue(Math.max(0, Math.min(1, newV)));
            }
        });

        // ── Jerarquía de Groups para el zoom ──────────────────────────
        // Anidar zoomGroup dentro de contentGroup evita que el ScrollPane
        // reajuste su contenido durante el escalado.
        zoomGroup = new Group();
        Group contentGroup = new Group();
        zoomGroup.getChildren().add(map_pane);
        contentGroup.getChildren().add(zoomGroup);

        // Aplicamos el zoom actual (valor del slider)
        double zoom = zoom_slider.getValue();
        zoomGroup.setScaleX(zoom);
        zoomGroup.setScaleY(zoom);

        // Asignamos el contentGroup como contenido del ScrollPane
        map_scrollpane.setContent(contentGroup);
    }

    private void addPoi(double x, double y) {
        // ── Construcción del diálogo personalizado ────────────────────
        Dialog<Poi> poiDialog = new Dialog<>();
        poiDialog.setTitle("Nuevo POI");
        poiDialog.setHeaderText("Introduce un nuevo POI");

        // Personalizamos el icono de la ventana del diálogo
        Stage dialogStage = (Stage) poiDialog.getDialogPane().getScene().getWindow();
        dialogStage.getIcons().add(
                new Image(getClass().getResourceAsStream("/resources/logo.png"))
        );

        // Botones del diálogo: Aceptar y Cancelar
        ButtonType okButton = new ButtonType("Aceptar", ButtonBar.ButtonData.OK_DONE);
        poiDialog.getDialogPane().getButtonTypes().addAll(okButton, ButtonType.CANCEL);

        // Campo de texto para el nombre del POI
        TextField nameField = new TextField();
        nameField.setPromptText("Nombre del POI");

        // Layout del contenido del diálogo
        VBox vbox = new VBox(10, new Label("Nombre:"), nameField);
        poiDialog.getDialogPane().setContent(vbox);

        // ResultConverter: transforma la selección del botón en un objeto Poi
        poiDialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButton) {
                return new Poi(nameField.getText().trim(), x, y);
            }
            return null;
        });

        // Mostramos el diálogo y esperamos la respuesta del usuario
        Optional<Poi> result = poiDialog.showAndWait();

        if (result.isPresent()) {
            Poi poi = result.get();
            poi.setPosition(new Point2D(x, y));

            // Añadimos el POI al ListView
            map_listview.getItems().add(poi);

            // Dibujamos la etiqueta sobre el mapa en las coordenadas exactas del clic
            Text text = new Text(poi.getCode());
            text.setX(x);
            text.setY(y);
            map_pane.getChildren().add(text);
        }
    }

    private void addCircle(double x, double y) {
        Circle circle = new Circle(10, Color.RED);
        circle.setCenterX(x);
        circle.setCenterY(y);
        map_pane.getChildren().add(circle);
    }

    @FXML
    void listClicked(MouseEvent event) {
        Poi itemSelected = map_listview.getSelectionModel().getSelectedItem();
        if (itemSelected == null) return;

        // ── Dimensiones del mapa con el zoom actual aplicado ──────────
        double mapWidth  = map_pane.getWidth()  * zoomGroup.getScaleX();
        double mapHeight = map_pane.getHeight() * zoomGroup.getScaleY();

        // ── Posición del POI escalada ──────────────────────────────────
        double poiX = itemSelected.getPosition().getX() * zoomGroup.getScaleX();
        double poiY = itemSelected.getPosition().getY() * zoomGroup.getScaleY();

        // ── Tamaño visible del ScrollPane (viewport) ───────────────────
        double viewW = map_scrollpane.getViewportBounds().getWidth();
        double viewH = map_scrollpane.getViewportBounds().getHeight();

        // ── Cálculo del scroll normalizado [0, 1] ─────────────────────
        double scrollH = (poiX - viewW / 2) / (mapWidth  - viewW);
        double scrollV = (poiY - viewH / 2) / (mapHeight - viewH);

        scrollH = Math.max(0, Math.min(1, scrollH));
        scrollV = Math.max(0, Math.min(1, scrollV));

        // ── Animación suave con Timeline ──────────────────────────────
        final Timeline timeline = new Timeline();
        final KeyValue kv1 = new KeyValue(map_scrollpane.hvalueProperty(), scrollH);
        final KeyValue kv2 = new KeyValue(map_scrollpane.vvalueProperty(), scrollV);
        final KeyFrame kf  = new KeyFrame(Duration.millis(500), kv1, kv2);
        timeline.getKeyFrames().add(kf);
        timeline.play();
    }

    private void buildActivity(Activity activity) {
        MapRegion region = activity.getSuggestedMap();

        // Carga SÍNCRONA (false) para que getWidth()/getHeight() no devuelvan 0.
        // Por defecto JavaFX carga imágenes en un hilo aparte, lo que haría
        // que las dimensiones fueran 0 en el momento de crear la proyección.
        Image img = new Image(
                new File(region.getImagePath()).toURI().toString(), false
        );

        // Construimos el mapa con la imagen del recorrido
        buildMap(new File(region.getImagePath()));

        // Creamos la proyección con las dimensiones reales de la imagen
        MapProjection proj = new MapProjection(region, img.getWidth(), img.getHeight());

        // Construimos la polilínea con todos los puntos del recorrido
        Polyline route = new Polyline();
        route.setStroke(Color.RED);
        route.setStrokeWidth(2);

        for (TrackPoint tp : activity.getTrackPoints()) {
            Point2D p = proj.project(tp);
            route.getPoints().addAll(p.getX(), p.getY());
        }

        // Añadimos la ruta sobre el mapa (encima de la imagen de fondo)
        map_pane.getChildren().add(route);
    }

    @FXML
    private void loadFile(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.setInitialDirectory(new File("."));

        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Rutas", "*.gpx")
        );

        File ogFile = fc.showOpenDialog(zoom_slider.getScene().getWindow());

        if (ogFile == null) { return; }

        SportActivityApp app = SportActivityApp.getInstance();
        Activity activity = app.importActivity(ogFile);
        buildActivity(activity);
    }
}
