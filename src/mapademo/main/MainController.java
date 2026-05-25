package mapademo.main;


import mapademo.App;
import mapademo.util.MapDialogs;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polyline;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import upv.ipc.sportlib.Activity;
import upv.ipc.sportlib.Annotation;
import upv.ipc.sportlib.AnnotationType;
import upv.ipc.sportlib.GeoPoint;
import upv.ipc.sportlib.MapProjection;
import upv.ipc.sportlib.MapRegion;
import upv.ipc.sportlib.SportActivityApp;
import upv.ipc.sportlib.TrackPoint;
 


public class MainController implements Initializable {

    // Controles del mapa
    @FXML private Slider zoom_slider;
    @FXML private ScrollPane map_scrollpane;
    @FXML private Pane map_pane;
    @FXML private Button switch_map_button;
    @FXML private Button add_map_button;
    
    
    // Listas laterales
    @FXML private ListView<Activity> activities_listview;
    @FXML private ListView<Annotation> annotations_listview;
    
    
    // Estado interno
    private Group zoomGroup;
    private Activity currentActivity;
    private MapProjection currentProjection;
    
    private final SportActivityApp app = SportActivityApp.getInstance();
    
    
    // Maquina de estados para anotaciones de 2 puntos (LINE / CIRCLE)
    private AnnotationType pendingType = null;
    private String pendingText = "";
    private String pendingColor = "#E63946";
    private final List<GeoPoint> pendingPoints = new ArrayList<>();
    
    
    // Pan con arrastre
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
        zoom_slider.valueProperty().addListener(
                (obs, o, n) -> zoom(n.doubleValue())
        );

        // de inicio no hay actividad cargada, asi que los dos botones de mapa
        // arrancan deshabilitados hasta que el usuario suba un GPX
        refreshMapButtons();
        
        // CellFactory: Actividades
        activities_listview.setCellFactory(lv -> new ListCell<Activity>(){
            @Override
            protected void updateItem(Activity a, boolean empty){
                super.updateItem(a, empty);
                if(empty || a == null){
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(a.getName() + "\n" + a.getStartTime().toLocalDate());
                    setOnMouseClicked(e -> {
                        if(e.getButton() == MouseButton.PRIMARY) showActivity(a);
                    });
                }
            }
        });
        
        
        // CellFactory: Anotaciones
        annotations_listview.setCellFactory(lv -> new ListCell<Annotation>(){
            @Override
            protected void updateItem(Annotation ann, boolean empty){
                super.updateItem(ann, empty);
                if(empty || ann == null){
                    setText(null);
                    setGraphic(null);
                } else {
                    String tipo = "";
                    AnnotationType type = ann.getType();
                    
                    if(type == AnnotationType.POINT){
                        tipo = "Punto ";
                    } else if (type == AnnotationType.TEXT){
                        tipo = "Texto ";
                    } else if (type == AnnotationType.LINE){
                        tipo = "Linea ";
                    } else if (type == AnnotationType.CIRCLE){
                     tipo = "Circulo ";
                    }
                    String texto = ann.getText().isBlank() ? "(sin texto)" : ann.getText();
                    setText("[" + tipo + "] " + texto);
                }
            }
        });
    }    
    
    
    // -----------------------------------------------------------------
    // Zoom
    // -----------------------------------------------------------------
    
    @FXML void zoomIn(ActionEvent e){zoom_slider.setValue(zoom_slider.getValue() + 0.1);}
    @FXML void zoomOut(ActionEvent e){zoom_slider.setValue(zoom_slider.getValue() - 0.1);}
    
    private void zoom(double scale){
        if(zoomGroup == null) return;
        
        double h = map_scrollpane.getHvalue();
        double v = map_scrollpane.getVvalue();
        
        zoomGroup.setScaleX(scale);
        zoomGroup.setScaleY(scale);
        
        map_scrollpane.setHvalue(h);
        map_scrollpane.setVvalue(v);
    }
    
    
    // -----------------------------------------------------------------
    // Carga fichero GPX
    // -----------------------------------------------------------------
    //
    // Actividad "pendiente": la lib persiste en BBDD al hacer importActivity,
    // asi que no hay forma de tener una Activity sin guardarla. La marcamos
    // como pendiente y la borramos si el usuario abandona sin pulsar Guardar.
    private Activity pendingActivity = null;

    @FXML
    private void loadFile(ActionEvent event){
        FileChooser fc = new FileChooser();
        fc.setInitialDirectory(new File("."));
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Rutas GPX", "*.gpx")
        );

        File f = fc.showOpenDialog(zoom_slider.getScene().getWindow());
        if(f == null) return;

        // si habia otra actividad pendiente (importada y aun no guardada),
        // la quitamos de la BBDD antes de meter la nueva
        descartarPendiente();

        Activity activity = app.importActivity(f);
        if(activity == null) return;

        pendingActivity = activity;

        if(!activities_listview.getItems().contains(activity)){
            activities_listview.getItems().add(activity);
        }

        showActivity(activity);
    }

    // Borra la actividad pendiente de BBDD (si la hay) y la quita de la lista.
    // Se llama cuando el usuario abandona la pantalla sin guardar, o importa otra.
    private void descartarPendiente(){
        if(pendingActivity != null){
            app.removeActivity(pendingActivity);
            activities_listview.getItems().remove(pendingActivity);
            pendingActivity = null;
        }
    }

    // Botón "Guardar actividad": confirma que la pendiente se queda en el historial
    @FXML
    private void guardarActividad(){
        if(pendingActivity == null){
            // no hay nada que guardar
            new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.WARNING,
                "No hay ninguna actividad cargada para guardar"
            ).showAndWait();
            return;
        }
        // simplemente la "promovemos" a guardada: ya estaba en BBDD, ahora la dejamos ahi
        pendingActivity = null;

        // limpiamos la pantalla para que se pueda cargar otra actividad
        resetMainUI();

        // confirmacion al usuario
        new javafx.scene.control.Alert(
            javafx.scene.control.Alert.AlertType.INFORMATION,
            "Actividad guardada en el historial"
        ).showAndWait();
    }

    // Deja la pantalla del main como recien abierta (sin actividad cargada)
    private void resetMainUI(){
        currentActivity = null;
        currentProjection = null;
        cancelPending();
        activities_listview.getItems().clear();
        annotations_listview.getItems().clear();
        if(map_pane != null) map_pane.getChildren().clear();
        map_scrollpane.setContent(null);
        refreshMapButtons();
    }
    
    // -----------------------------------------------------------------
    // Mostrar una actividad: ruta + marcadores inicio/fin + anotaciones previas
    // -----------------------------------------------------------------
    
    private void showActivity(Activity activity){
        currentActivity = activity;
        cancelPending();
        refreshMapButtons();
        
        MapRegion region = activity.getSuggestedMap();
        
        // Carga sincrona para que getWidth/getHeight no pete
        Image img = new Image(new File(region.getImagePath()).toURI().toString(), false);
        
        buildMap(new File(region.getImagePath()));
        
        currentProjection = new MapProjection(region, img.getWidth(), img.getHeight());
        
        // Ruta + bounding box para centrar la vista despues
        Polyline route = new Polyline();
        route.setStroke(Color.web("#E63946"));
        route.setStrokeWidth(2.5);
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (TrackPoint tp: activity.getTrackPoints()){
            Point2D p = currentProjection.project(tp);
            route.getPoints().addAll(p.getX(),p.getY());
            if (p.getX() < minX) minX = p.getX();
            if (p.getY() < minY) minY = p.getY();
            if (p.getX() > maxX) maxX = p.getX();
            if (p.getY() > maxY) maxY = p.getY();
        }

        // Marcador de inicio (verde)
        Point2D pS = currentProjection.project(activity.getStartPoint());
        Circle startCircle = makeMarker(pS, Color.LIMEGREEN, Color.DARKGREEN);
        installTooltip(startCircle, "Inicio");


        // Marcador de fin (rojo)
        Point2D pE = currentProjection.project(activity.getEndPoint());
        Circle endCircle = makeMarker(pE, Color.TOMATO, Color.DARKRED);
        installTooltip(endCircle, "Fin");

        map_pane.getChildren().addAll(route, startCircle, endCircle);

        // Centramos el scroll sobre el centro de la ruta (despues de que se haga el layout).
        // El contenido del scroll esta escalado por el zoom, asi que multiplicamos.
        if (!activity.getTrackPoints().isEmpty()) {
            final double cx = (minX + maxX) / 2;
            final double cy = (minY + maxY) / 2;
            final double mapW = img.getWidth();
            final double mapH = img.getHeight();
            Platform.runLater(() -> centerScrollOnRoute(mapW, mapH, cx, cy));
        }
        
        
        // Anotaciones persistidas de esta actividad
        annotations_listview.getItems().clear();
        for(Annotation ann: activity.getAnnotations()){
            drawAnnotation(ann);
            annotations_listview.getItems().add(ann);
        }
    }
    
    
    // Mueve el scrollpane para que el centro de la ruta quede en el centro del viewport.
    // Como el zoomGroup escala el contenido, hay que multiplicar las coords por el zoom.
    private void centerScrollOnRoute(double mapW, double mapH, double cx, double cy){
        if (map_scrollpane.getViewportBounds() == null) return;
        if (zoomGroup == null) return;
        double scale = zoomGroup.getScaleX();
        double viewW = map_scrollpane.getViewportBounds().getWidth();
        double viewH = map_scrollpane.getViewportBounds().getHeight();
        double scaledW = mapW * scale;
        double scaledH = mapH * scale;
        if (scaledW > viewW){
            double h = (cx*scale - viewW/2) / (scaledW - viewW);
            map_scrollpane.setHvalue(Math.max(0, Math.min(1, h)));
        }
        if (scaledH > viewH){
            double v = (cy*scale - viewH/2) / (scaledH - viewH);
            map_scrollpane.setVvalue(Math.max(0, Math.min(1, v)));
        }
    }

    // -----------------------------------------------------------------
    // builMap: construye el Pane del mapa con todos sus handlers de raton
    // -----------------------------------------------------------------
    private void buildMap(File imgFile){
        if (!imgFile.exists()){
            map_scrollpane.setContent(new Label("Imagen no encontrada"));
            return;
        }
        
        Image img = new Image(imgFile.toURI().toString(), false);
        double W = img.getWidth(), H = img.getHeight();
        
        map_pane = new Pane();
        map_pane.setPrefSize(W,H);
        map_pane.setMinSize(W,H);
        map_pane.setMaxSize(W,H);
        
        ImageView iv = new ImageView(img);
        iv.setFitWidth(W);
        iv.setFitHeight(H);
        map_pane.getChildren().add(iv);
        
        // Clic: derecho = menu anotaciones / izquierdo = 2.o punto pendiente
        map_pane.setOnMouseClicked(e -> {
            if(e.getButton() == MouseButton.SECONDARY){
                if(pendingType == null){
                    if (currentActivity != null){
                        showAnnotationMenu(e.getX(), e.getY(),e.getScreenX(),e.getScreenY());
                    }
                } else {
                    // Clic derecho cancela la seleccion en curso
                    cancelPending();
                }
            } else if(e.getButton() == MouseButton.PRIMARY && pendingType != null){
                // Capturar 2.o punto para LINE / CIRCLE
                pendingPoints.add(currentProjection.unproject(e.getX(), e.getY()));
                if (pendingPoints.size() == 2){
                    saveAnnotation(pendingType, pendingText, pendingColor,
                            new ArrayList<>(pendingPoints));
                    cancelPending();
                }
            }
        });
        
        // Pan con arrastre izquierdo
        map_pane.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY && pendingType == null){
                dragStartX = e.getSceneX();
                dragStartY = e.getSceneY();
                scrollStartH = map_scrollpane.getHvalue();
                scrollStartV = map_scrollpane.getVvalue();
                map_pane.setStyle("-fx-cursor: closed-hand;");
            }
        });
        
        map_pane.setOnMouseReleased(e -> {
            if (e.getButton() == MouseButton.PRIMARY && pendingType == null){
                map_pane.setStyle("");
            }
        });
        
        map_pane.setOnMouseDragged(e -> {
            if(e.getButton() == MouseButton.PRIMARY && pendingType == null){
                double dX = e.getSceneX() - dragStartX;
                double dY = e.getSceneY() - dragStartY;
                double mapW = map_pane.getWidth() * zoomGroup.getScaleX();
                double mapH = map_pane.getHeight() * zoomGroup.getScaleY();
                double viewW = map_scrollpane.getViewportBounds().getWidth();
                double viewH = map_scrollpane.getViewportBounds().getHeight();
                
                map_scrollpane.setHvalue(
                        Math.max(0, (double) Math.min(1, scrollStartH - dX / (mapW - viewW)))
                );
                map_scrollpane.setVvalue(
                        Math.max(0, (double) Math.min(1,scrollStartV - dY / (mapH - viewH)))
                );
            }
        });
        
        // Jerarquia Group para el zoom
        zoomGroup = new Group();
        Group contentGroup = new Group();
        zoomGroup.getChildren().add(map_pane);
        contentGroup.getChildren().add(zoomGroup);
        zoomGroup.setScaleX(zoom_slider.getValue());
        zoomGroup.setScaleY(zoom_slider.getValue());
        map_scrollpane.setContent(contentGroup);
    }
    
    
    // -----------------------------------------------------------------
    // Menu contextual de tipos de anotacion
    // -----------------------------------------------------------------
    
    private void showAnnotationMenu(
            double mapX, double mapY, double screenX, double screenY){
        
        ContextMenu menu = new ContextMenu();
        
        MenuItem miPoint = new MenuItem("Punto");
        MenuItem miText = new MenuItem("Texto");
        MenuItem miLine = new MenuItem("Linea (selecciona 2 puntos)");
        MenuItem miCircle = new MenuItem("Circulo (selecciona 2 puntos)");
        
        miPoint.setOnAction(e -> startOnePointAnnotation(AnnotationType.POINT, mapX, mapY));
        
        miText.setOnAction(e -> startOnePointAnnotation(AnnotationType.TEXT, mapX, mapY));
        
        miLine.setOnAction(e -> startTwoPointAnnotation(AnnotationType.LINE, mapX, mapY));
        
        miCircle.setOnAction(e -> startTwoPointAnnotation(AnnotationType.CIRCLE, mapX, mapY));
        
        
        menu.getItems().addAll(miPoint, miText, miLine, miCircle);
        menu.show(map_pane.getScene().getWindow(), screenX, screenY);
    }
    
    
    // Anotacion de 1 punto: mostrar dialogo y guardar directamente.
    // Diferimos con runLater para que el ContextMenu termine de cerrarse antes de abrir
    // el dialogo (si no, en Linux el dialogo se renderiza detras de la ventana principal).
    private void startOnePointAnnotation(AnnotationType type, double mapX, double mapY){
        Platform.runLater(() -> {
            Optional<String[]> res = showAnnotationDialog(type);
            if(res.isEmpty()) return;

            GeoPoint geo = currentProjection.unproject(mapX, mapY);
            saveAnnotation(type, res.get()[0], res.get()[1], List.of(geo));
        });
    }


    // Anotacion de 2 puntos: dialogo -> esperar 2.o clic izquierdo
    private void startTwoPointAnnotation(AnnotationType type, double mapX, double mapY){
        Platform.runLater(() -> {
            Optional<String[]> res = showAnnotationDialog(type);
            if (res.isEmpty()) return;

            pendingText = res.get()[0];
            pendingColor = res.get()[1];
            pendingType = type;
            pendingPoints.clear();
            pendingPoints.add(currentProjection.unproject(mapX, mapY));
            map_pane.setStyle("-fx-cursor: crosshair;");
        });
    }
    
    private void cancelPending(){
        pendingType = null;
        pendingPoints.clear();
        if(map_pane != null) map_pane.setStyle("");
    }
    
    
    // Dialogo para texto y color de la anotacion
    // Retorna String[0] = texto, String[1] = Color CSS hex
    private Optional<String[]> showAnnotationDialog(AnnotationType type){
        String header = "";
        
        if(type == AnnotationType.POINT){
            header = "Punto - introduce los datos";
        } else if (type == AnnotationType.TEXT){
            header = " Texto - introduce los datos";
        } else if (type == AnnotationType.LINE){
            header = " Linea - haz cliz izquierdo en el mapa para el 2.o punto";
        } else if (type == AnnotationType.CIRCLE){
            header = "Circulo - haz clic izquierdo en el mapa para el borde";
        }
        
        
        Dialog<String[]> dialog = new Dialog<>();
        dialog.setTitle("Nueva anotacion");
        dialog.initOwner(map_pane.getScene().getWindow());
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.setResizable(true);

        ButtonType okBtn = new ButtonType("Aceptar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okBtn, ButtonType.CANCEL);

        // Header como Label dentro del contenido (no usamos setHeaderText porque a veces
        // colapsa el DialogPane en Linux y deja los campos enanos)
        Label headerLabel = new Label(header);
        headerLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        headerLabel.setWrapText(true);
        headerLabel.setMinWidth(380);

        // Fila Texto:
        Label tLabel = new Label("Texto:");
        tLabel.setMinWidth(60);
        TextField textField = new TextField();
        textField.setPromptText("Texto (opcional)");
        textField.setPrefWidth(300);
        HBox.setHgrow(textField, Priority.ALWAYS);
        HBox row1 = new HBox(10, tLabel, textField);
        row1.setAlignment(Pos.CENTER_LEFT);

        // Fila Color:
        Label cLabel = new Label("Color:");
        cLabel.setMinWidth(60);
        ColorPicker colorPicker = new ColorPicker(Color.web("#E63946"));
        colorPicker.setPrefWidth(Double.MAX_VALUE);
        HBox.setHgrow(colorPicker, Priority.ALWAYS);
        HBox row2 = new HBox(10, cLabel, colorPicker);
        row2.setAlignment(Pos.CENTER_LEFT);

        VBox content = new VBox(12, headerLabel, row1, row2);
        content.setPadding(new Insets(15));
        content.setMinSize(440, 160);
        content.setPrefSize(440, 160);
        dialog.getDialogPane().setContent(content);

        // Tamano del DialogPane y del Stage forzados a la vez (Linux a veces ignora uno u otro)
        dialog.getDialogPane().setMinSize(460, 220);
        dialog.getDialogPane().setPrefSize(460, 220);

        dialog.setOnShown(ev -> {
            javafx.stage.Stage st = (javafx.stage.Stage) dialog.getDialogPane().getScene().getWindow();
            st.setMinWidth(460);
            st.setMinHeight(220);
            st.setWidth(460);
            st.setHeight(220);
            st.centerOnScreen();
            st.toFront();
            textField.requestFocus();
        });

        dialog.setResultConverter(btn -> {
            if (btn != okBtn) return null;
            Color c = colorPicker.getValue();
            String hex = String.format("#%02X%02X%02X",
                    (int)(c.getRed()   * 255),
                    (int)(c.getGreen() * 255),
                    (int)(c.getBlue()  * 255));
            return new String[]{ textField.getText().trim(), hex };
        });

        return dialog.showAndWait();
    }
    
    
    // Persistir anotacion y dibujarla
    private void saveAnnotation(AnnotationType type, String text, String color, List<GeoPoint> points){
        Annotation ann = new Annotation(type, text, color, 2.5, points);
        Annotation saved = app.addAnnotation(currentActivity, ann);
        if (saved != null){
            drawAnnotation(saved);
            annotations_listview.getItems().add(saved);
        }
    }
    
    
    // Dibujar una anotacion sobre el mapa
    // El Tooltip solo aparece al pasar el cursor encima (no permanente)
    private void drawAnnotation(Annotation ann){
        List<GeoPoint> geos = ann.getGeoPoints();
        Color color = safeColor(ann.getColor());
        String tip = buildTooltipText(ann);
        
            if(ann.getType() == AnnotationType.POINT) {
                Point2D p = currentProjection.project(geos.get(0));
                Circle c = new Circle(9, color);
                c.setCenterX(p.getX());
                c.setCenterY(p.getY());
                c.setStroke(color.darker());
                c.setStrokeWidth(2);
                installTooltip(c, tip);
                map_pane.getChildren().add(c);
            }
            
            if(ann.getType() == AnnotationType.TEXT) {
                Point2D p = currentProjection.project(geos.get(0));
                Text t = new Text(ann.getText().isBlank() ? "?" : ann.getText());
                t.setX(p.getX());
                t.setY(p.getY());
                t.setFill(color);
                t.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
                installTooltip(t, tip);
                map_pane.getChildren().add(t);
            }
            
            if(ann.getType() == AnnotationType.LINE) {
                Point2D p1 = currentProjection.project(geos.get(0));
                Point2D p2 = currentProjection.project(geos.get(1));
                Line l = new Line(p1.getX(), p1.getY(), p2.getX(), p2.getY());
                l.setStroke(color);
                l.setStrokeWidth(ann.getStrokeWidth());
                installTooltip(l, tip);
                map_pane.getChildren().add(l);
            }
 
            if(ann.getType() == AnnotationType.CIRCLE) {
                Point2D center = currentProjection.project(geos.get(0));
                Point2D edge   = currentProjection.project(geos.get(1));
                double  radius = Math.hypot(
                        edge.getX() - center.getX(),
                        edge.getY() - center.getY());
                Circle c = new Circle(radius);
                c.setCenterX(center.getX());
                c.setCenterY(center.getY());
                c.setFill(Color.TRANSPARENT);
                c.setStroke(color);
                c.setStrokeWidth(ann.getStrokeWidth());
                installTooltip(c, tip);
                map_pane.getChildren().add(c);
            }
        
    }
    
    // ────────────────────────────────────────────────────────────────────────
    // Helpers
    // ────────────────────────────────────────────────────────────────────────
    
    private Circle makeMarker(Point2D p, Color fill, Color stroke){
        Circle c = new Circle(9, fill);
        c.setCenterX(p.getX());
        c.setCenterY(p.getY());
        c.setStroke(stroke);
        c.setStrokeWidth(2);
        return c;
    }
    
    /** Tooltip que aparece solo al pasar el cursor, no permanentemente. */
    private void installTooltip(Node node, String text) {
        Tooltip tip = new Tooltip(text);
        tip.setShowDelay(Duration.millis(150));
        tip.setHideDelay(Duration.millis(100));
        tip.setShowDuration(Duration.seconds(8));
        Tooltip.install(node, tip);
    }
    
    
    private String buildTooltipText(Annotation ann) {
        String tipo = "";
        AnnotationType type = ann.getType();
        
        if(type == AnnotationType.POINT){
            tipo = "Punto ";
        } else if (type == AnnotationType.TEXT){
            tipo = "Texto ";
        } else if (type == AnnotationType.LINE){
            tipo = "Linea ";
        } else if (type == AnnotationType.CIRCLE){
            tipo = "Circulo ";
        }
        
        return ann.getText().isBlank() ? tipo : tipo + ": " + ann.getText();
    }
    
    private Color safeColor(String hex) {
        try   { return Color.web(hex); }
        catch (Exception e) { return Color.RED; }
    }
    
    
    // ─────────────────────────────────────────────────────────────────────────
    // Agregar / Cambiar mapa
    // ─────────────────────────────────────────────────────────────────────────
    //
    // "Agregar mapa": para cuando la actividad cargada no tiene mapa todavia.
    // Solo esta habilitado en ese caso.
    @FXML
    private void agregarMapa(ActionEvent event){
        boolean added = MapDialogs.showAddMapDialog(map_scrollpane.getScene().getWindow());
        if (added && currentActivity != null){
            // recargamos la actividad para que la libreria asocie el nuevo MapRegion
            // (getSuggestedMap selecciona el mapa cuyo bounding box contiene los puntos)
            showActivity(currentActivity);
        }
    }

    // "Cambiar mapa": el usuario quiere cambiar el mapa actual por otro distinto.
    // Pide los mismos datos (imagen + bounding box).
    @FXML
    private void cambiarMapa(ActionEvent event){
        boolean added = MapDialogs.showAddMapDialog(map_scrollpane.getScene().getWindow());
        if (added && currentActivity != null){
            showActivity(currentActivity);
        }
    }

    // Activa/desactiva los botones de mapa segun la actividad cargada.
    // Si la actividad tiene mapa -> Cambiar habilitado, Agregar deshabilitado.
    // Si la actividad NO tiene mapa -> Agregar habilitado, Cambiar deshabilitado.
    // Si no hay actividad cargada -> los dos deshabilitados.
    //
    // Ojo: la libreria puede devolver un MapRegion para una actividad aunque
    // el JPG no exista en disco (ej: pirineos esta registrado pero el archivo
    // puede no haberse generado todavia con el script python). Por eso comprobamos
    // tambien si la imagen existe fisicamente.
    private void refreshMapButtons(){
        if (switch_map_button == null || add_map_button == null) return;
        boolean hayActividad = currentActivity != null;
        boolean tieneMapa = false;
        if (hayActividad){
            MapRegion mr = currentActivity.getSuggestedMap();
            tieneMapa = mr != null && new File(mr.getImagePath()).exists();
        }
        switch_map_button.setDisable(!tieneMapa);
        add_map_button.setDisable(!hayActividad || tieneMapa);
    }
    @FXML
    private void VolverHome(){
        // si se va sin pulsar Guardar, descartamos la actividad pendiente
        descartarPendiente();
        try{
            App.getInstance().switchToHome();
        }catch(Exception e){e.printStackTrace();}

    }

    // hover: verde claro -> verde mas oscuro, y gris claro -> gris mas oscuro
    @FXML
    private void hoverEnter(MouseEvent e){
        Node n = (Node) e.getSource();
        n.setStyle(n.getStyle()
                .replace("#C6FF3B", "#A1E000")
                .replace("#dfdfdf", "#cfcfcf"));
    }

    @FXML
    private void hoverExit(MouseEvent e){
        Node n = (Node) e.getSource();
        n.setStyle(n.getStyle()
                .replace("#A1E000", "#C6FF3B")
                .replace("#cfcfcf", "#dfdfdf"));
    }
}
