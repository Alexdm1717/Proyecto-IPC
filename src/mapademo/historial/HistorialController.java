package mapademo.historial;

import java.io.File;
import java.lang.reflect.Array;
import java.net.URL;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import java.util.Optional;
import javafx.event.ActionEvent;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
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
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polyline;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import mapademo.App;
import mapademo.util.MapDialogs;
import upv.ipc.sportlib.Activity;
import upv.ipc.sportlib.Annotation;
import upv.ipc.sportlib.AnnotationType;
import upv.ipc.sportlib.GeoPoint;
import upv.ipc.sportlib.MapProjection;
import upv.ipc.sportlib.MapRegion;
import upv.ipc.sportlib.SportActivityApp;
import upv.ipc.sportlib.TrackPoint;


public class HistorialController implements Initializable {

    // ----- elementos del fxml -----
    @FXML private ListView<Activity> sessionsList;  // lista de sesiones (izquierda)
    @FXML private ScrollPane mapScroll;        // scroll que envuelve el mapa
    @FXML private Pane mapPane;                // donde dibujamos el mapa + ruta
    @FXML private LineChart<Number, Number> elevationChart;  // grafica del perfil de desnivel
    @FXML private NumberAxis xAxis;            // eje X de la grafica (distancia en km)
    @FXML private NumberAxis yAxis;            // eje Y de la grafica (altitud en m)

    // lista de anotaciones de la actividad activa (columna derecha, debajo de stats)
    @FXML private ListView<Annotation> annotationsList;

    // labels de las estadisticas (columna derecha)
    @FXML private Label statNombre;
    @FXML private Label statFecha;
    @FXML private Label statDistancia;
    @FXML private Label statDuracion;
    @FXML private Label statVelocidad;
    @FXML private Label statRitmo;
    @FXML private Label statDesnivel;
    @FXML private Label statAltitud;
    @FXML private Label statAnotaciones;

    // formato para la fecha+hora de la sesion en las stats
    private DateTimeFormatter fechaLarga = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    // ----- estado de la actividad que se esta viendo ahora mismo -----
    private MapProjection currentProjection;   // sirve para pasar de lat/lon a coords del mapa
    private List<TrackPoint> currentTrackPoints;
    // distancia acumulada en METROS hasta cada trackpoint.
    // La precalculamos en buildElevationChart y la usamos al hacer hover para localizar
    // que trackpoint corresponde a la posicion del raton sobre la grafica.
    private double[] cumDistancesM;
    // circulito azul que se mueve por el mapa siguiendo al raton sobre la grafica
    private Circle mapCursor;

    // ----- estado para crear anotaciones (copiado de MainController) -----
    // Maquina de estados para anotaciones de 2 puntos (LINE / CIRCLE)
    private AnnotationType pendingType = null;
    private String pendingText = "";
    private String pendingColor = "#E63946";
    private final List<GeoPoint> pendingPoints = new ArrayList<>();
    // actividad actualmente cargada (para asociar las anotaciones)
    private Activity currentActivity;
    private final SportActivityApp app = SportActivityApp.getInstance();


    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // ---- configuracion del ListView ----

        // placeholder: lo que se ve cuando la lista esta vacia
        Label vacio = new Label("No has importado ninguna sesion todavia");
        vacio.setTextFill(Color.web("#6B7280"));
        vacio.setFont(Font.font("SansSerif", 13));
        vacio.setWrapText(true);
        sessionsList.setPlaceholder(vacio);

        // cellFactory: como se pinta cada fila (una card visual con nombre/fecha/km)
        sessionsList.setCellFactory(lv -> {
            ListCell<Activity> cell = new ListCell<Activity>(){
                @Override
                protected void updateItem(Activity a, boolean empty){
                    super.updateItem(a, empty);
                    if (empty || a == null){
                        setText(null);
                        setGraphic(null);
                    } else {
                        setText(null);
                        setGraphic(buildSessionCard(a));
                    }
                }
            };
            // resaltado verde de la fila seleccionada (en vez del azul por defecto)
            cell.selectedProperty().addListener((obs, antes, ahora) -> {
                if (ahora) cell.setStyle("-fx-background-color: #C6FF3B;");
                else cell.setStyle("");
            });
            return cell;
        });

        // al cambiar la seleccion cargamos esa actividad en mapa+grafica+stats
        sessionsList.getSelectionModel().selectedItemProperty().addListener((obs, antes, ahora) -> {
            if (ahora != null) showActivity(ahora);
        });

        // cellFactory de la lista de anotaciones: pintamos "[Tipo] texto"
        annotationsList.setCellFactory(lv -> new ListCell<Annotation>(){
            @Override
            protected void updateItem(Annotation ann, boolean empty){
                super.updateItem(ann, empty);
                if (empty || ann == null){
                    setText(null);
                } else {
                    String tipo;
                    AnnotationType t = ann.getType();
                    if (t == AnnotationType.POINT) tipo = "Punto";
                    else if (t == AnnotationType.TEXT) tipo = "Texto";
                    else if (t == AnnotationType.LINE) tipo = "Linea";
                    else if (t == AnnotationType.CIRCLE) tipo = "Circulo";
                    else tipo = "?";
                    String texto = (ann.getText() == null || ann.getText().isBlank())
                            ? "(sin texto)" : ann.getText();
                    setText("[" + tipo + "] " + texto);
                }
            }
        });

        // pintamos las sesiones y seleccionamos la mas reciente por defecto
        refreshSessionsList();
        if (!sessionsList.getItems().isEmpty()){
            sessionsList.getSelectionModel().selectFirst();
        }
    }


    // =================================================================
    // LISTA DE SESIONES (columna izquierda)
    // =================================================================

    // Refresca los items del ListView con las sesiones del usuario actual
    private void refreshSessionsList() {
        sessionsList.getItems().setAll(sortedUserActivities());
    }

    // Sesiones del usuario actual, mas recientes primero.
    // Si dos tienen la misma fecha, desempata por id (la mas recien anadida
    // tiene id mas alto en BBDD), tambien descendente.
    private List<Activity> sortedUserActivities() {
        List<Activity> list = new ArrayList<>();
        List<Activity> fromApp = SportActivityApp.getInstance().getUserActivities();
        if (fromApp != null) list.addAll(fromApp);
        // copiamos a ArrayList para evitar que la lista venga inmutable y .sort pete
        list.sort(
            Comparator.comparing(Activity::getStartTime)
                .thenComparingLong(Activity::getId)
                .reversed()
        );
        return list;
    }

    // Card de una sesion: nombre+fecha a la izquierda, km a la derecha.
    // Al clicar se carga esa sesion en el mapa+grafica+stats.
    private HBox buildSessionCard(Activity a) {
        HBox card = new HBox(12);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color: #F7F8F5; -fx-background-radius: 10; -fx-cursor: hand;");
        card.setPadding(new Insets(10, 12, 10, 12));

        VBox left = new VBox(2);
        Label nombre = new Label(a.getName() != null ? a.getName() : "Sesion");
        nombre.setTextFill(Color.web("#111111"));
        nombre.setFont(Font.font("SansSerif", FontWeight.BOLD, 14));

        String fechaText = "—";
        if (a.getStartTime() != null) {
            fechaText = a.getStartTime().toLocalDate().toString();
        }
        Label fecha = new Label(fechaText);
        fecha.setTextFill(Color.web("#6B7280"));
        fecha.setFont(Font.font("SansSerif", 11));
        left.getChildren().addAll(nombre, fecha);

        // spacer para que los km se vayan al lado derecho del HBox
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Label km = new Label(String.format("%.1f km", a.getTotalDistance() / 1000.0));
        km.setTextFill(Color.web("#111111"));
        km.setFont(Font.font("SansSerif", FontWeight.BOLD, 13));

        card.getChildren().addAll(left, sp, km);
        return card;
    }


    // =================================================================
    // BOTON IMPORTAR GPX (encima de la lista)
    // =================================================================
    @FXML
    private void importarSesion() {
        FileChooser fc = new FileChooser();
        fc.setInitialDirectory(new File("."));
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Rutas GPX", "*.gpx"));
        File f = fc.showOpenDialog(sessionsList.getScene().getWindow());
        if (f == null) return;  // ha cancelado el dialogo

        // la libreria parsea el GPX y crea la Activity en la BBDD
        Activity a = SportActivityApp.getInstance().importActivity(f);
        if (a == null) {
            new Alert(Alert.AlertType.ERROR, "No se pudo importar el GPX").showAndWait();
            return;
        }
        // refrescamos la lista y dejamos seleccionada la sesion recien importada
        // (el listener de seleccion se encarga de cargarla en mapa/grafica/stats)
        refreshSessionsList();
        sessionsList.getSelectionModel().select(a);
    }


    // =================================================================
    // CARGAR UNA SESION: mapa + grafica + estadisticas
    // =================================================================

    // Punto de entrada cuando el usuario selecciona una sesion
    private void showActivity(Activity a) {
        currentActivity = a;
        cancelPending();
        currentTrackPoints = a.getTrackPoints();
        if (currentTrackPoints == null) currentTrackPoints = new ArrayList<>();

        buildMap(a);
        buildElevationChart();
        updateStats(a);
    }


    // -----------------------------------------------------------------
    // Dibuja el mapa: imagen de fondo + ruta + marcadores inicio/fin
    // -----------------------------------------------------------------
    private void buildMap(Activity a) {
        mapPane.getChildren().clear();

        // La libreria puede devolver un MapRegion aunque la imagen no exista en disco
        // (ej: pirineos esta registrado por defecto pero el JPG solo aparece despues
        // de correr el script python). En ambos casos -> placeholder "no hay mapa".
        MapRegion region = a.getSuggestedMap();
        if (region == null || !new File(region.getImagePath()).exists()) {
            mostrarPlaceholderSinMapa();
            return;
        }
        File imgFile = new File(region.getImagePath());

        // carga sincrona (el 'false') para que getWidth/getHeight devuelvan ya el tamano real
        Image img = new Image(imgFile.toURI().toString(), false);
        double W = img.getWidth();
        double H = img.getHeight();

        // fijamos el tamano del Pane al de la imagen para que el scroll funcione bien
        mapPane.setPrefSize(W, H);
        mapPane.setMinSize(W, H);
        mapPane.setMaxSize(W, H);

        ImageView iv = new ImageView(img);
        iv.setFitWidth(W);
        iv.setFitHeight(H);
        mapPane.getChildren().add(iv);

        // proyeccion que convierte (lat,lon) -> (x,y) en el plano de la imagen
        currentProjection = new MapProjection(region, W, H);

        // dibujamos la ruta como polyline.
        // De paso vamos calculando el bounding box de la ruta para luego centrar el scroll.
        Polyline route = new Polyline();
        route.setStroke(Color.web("#E63946"));
        route.setStrokeWidth(2.5);
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (TrackPoint tp : currentTrackPoints) {
            Point2D p = currentProjection.project(tp);
            route.getPoints().addAll(p.getX(), p.getY());
            if (p.getX() < minX) minX = p.getX();
            if (p.getY() < minY) minY = p.getY();
            if (p.getX() > maxX) maxX = p.getX();
            if (p.getY() > maxY) maxY = p.getY();
        }
        mapPane.getChildren().add(route);

        // centramos el scroll en el medio de la ruta.
        // Hace falta esperar al layout (runLater) porque el viewport todavia no tiene tamano.
        if (!currentTrackPoints.isEmpty()) {
            final double cx = (minX + maxX) / 2;
            final double cy = (minY + maxY) / 2;
            Platform.runLater(() -> centerMapOn(W, H, cx, cy));
        }

        // marcadores: verde donde empieza, rojo donde acaba
        if (a.getStartPoint() != null) {
            Point2D pS = currentProjection.project(a.getStartPoint());
            mapPane.getChildren().add(makeMarker(pS, Color.LIMEGREEN, Color.DARKGREEN));
        }
        if (a.getEndPoint() != null) {
            Point2D pE = currentProjection.project(a.getEndPoint());
            mapPane.getChildren().add(makeMarker(pE, Color.TOMATO, Color.DARKRED));
        }
        
        drawAnnotations(a);
        
        // creamos el cursor azul que se ira moviendo por encima del mapa cuando
        // el usuario pase el raton sobre la grafica del perfil de desnivel.
        // Lo dejamos invisible hasta que se haga hover sobre la grafica.
        mapCursor = new Circle(7, Color.web("#0AADFF"));
        mapCursor.setStroke(Color.WHITE);
        mapCursor.setStrokeWidth(2);
        mapCursor.setVisible(false);
        mapPane.getChildren().add(mapCursor);

        // Handlers para crear anotaciones (clic derecho menu, clic izquierdo 2.o punto).
        // Copiado del MainController, solo que aqui no hay drag/pan ni zoom.
        mapPane.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                if (pendingType == null) {
                    if (currentActivity != null) {
                        showAnnotationMenu(e.getX(), e.getY(), e.getScreenX(), e.getScreenY());
                    }
                } else {
                    cancelPending();
                }
            } else if (e.getButton() == MouseButton.PRIMARY && pendingType != null) {
                pendingPoints.add(currentProjection.unproject(e.getX(), e.getY()));
                if (pendingPoints.size() == 2) {
                    saveAnnotation(pendingType, pendingText, pendingColor,
                            new ArrayList<>(pendingPoints));
                    cancelPending();
                }
            }
        });
    }

    // Cuando la actividad no tiene mapa asociado pintamos un texto y un boton
    // para que el usuario pueda anadir uno con su bounding box.
    private void mostrarPlaceholderSinMapa(){
        // ajustamos mapPane al tamano del viewport del scroll para que se vea centrado
        double w = Math.max(400, mapScroll.getViewportBounds() != null
                ? mapScroll.getViewportBounds().getWidth() : 400);
        double h = Math.max(300, mapScroll.getViewportBounds() != null
                ? mapScroll.getViewportBounds().getHeight() : 300);
        mapPane.setPrefSize(w, h);
        mapPane.setMinSize(w, h);
        mapPane.setMaxSize(w, h);

        Label texto = new Label("No hay mapa para esta actividad");
        texto.setTextFill(Color.web("#6B7280"));
        texto.setFont(Font.font("SansSerif", FontWeight.BOLD, 16));

        Button addBtn = new Button("Añadir mapa");
        addBtn.setStyle("-fx-background-color: #C6FF3B; -fx-background-radius: 10; -fx-cursor: hand;");
        addBtn.setPrefHeight(40);
        addBtn.setPrefWidth(180);
        addBtn.setOnAction(e -> anadirMapaParaActividad());
        // mismo hover que el resto: verde claro -> verde oscuro
        addBtn.setOnMouseEntered(e -> addBtn.setStyle(addBtn.getStyle().replace("#C6FF3B", "#A1E000")));
        addBtn.setOnMouseExited(e -> addBtn.setStyle(addBtn.getStyle().replace("#A1E000", "#C6FF3B")));

        VBox box = new VBox(15, texto, addBtn);
        box.setAlignment(Pos.CENTER);
        box.setPrefSize(w, h);
        mapPane.getChildren().add(box);
    }

    // Llama al dialogo de anadir mapa. Si se anade correctamente, recargamos la
    // actividad para que la libreria reasocie el mapa via getSuggestedMap().
    private void anadirMapaParaActividad(){
        Activity sel = sessionsList.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        boolean added = MapDialogs.showAddMapDialog(mapPane.getScene().getWindow());
        if (added) showActivity(sel);
    }

    // Mueve el scroll para que el punto (cx,cy) del mapa quede en el centro del viewport.
    // Las posiciones del scroll van de 0 a 1, por eso hay que normalizar.
    private void centerMapOn(double mapW, double mapH, double cx, double cy) {
        if (mapScroll.getViewportBounds() == null) return;
        double viewW = mapScroll.getViewportBounds().getWidth();
        double viewH = mapScroll.getViewportBounds().getHeight();
        // si el mapa entero cabe en el viewport no hay que mover nada
        if (mapW > viewW) {
            double h = (cx - viewW / 2) / (mapW - viewW);
            mapScroll.setHvalue(Math.max(0, Math.min(1, h)));
        }
        if (mapH > viewH) {
            double v = (cy - viewH / 2) / (mapH - viewH);
            mapScroll.setVvalue(Math.max(0, Math.min(1, v)));
        }
    }

    // Marcador circular pequeno para inicio/fin
    private Circle makeMarker(Point2D p, Color fill, Color stroke) {
        Circle c = new Circle(8, fill);
        c.setCenterX(p.getX());
        c.setCenterY(p.getY());
        c.setStroke(stroke);
        c.setStrokeWidth(2);
        return c;
    }


    // -----------------------------------------------------------------
    // Grafica de perfil de desnivel (distancia acumulada vs altitud)
    // -----------------------------------------------------------------
    private void buildElevationChart() {
        elevationChart.getData().clear();

        // con menos de 2 trackpoints no se puede dibujar nada
        if (currentTrackPoints.size() < 2) {
            cumDistancesM = new double[0];
            return;
        }

        // Precalculamos la distancia acumulada (en metros) hasta cada trackpoint.
        // Esto nos sirve para dos cosas:
        //  1) usar el valor como X en la grafica
        //  2) cuando hacemos hover, busqueda binaria sobre este array para encontrar
        //     que trackpoint corresponde a la posicion del raton.
        cumDistancesM = new double[currentTrackPoints.size()];
        cumDistancesM[0] = 0;
        for (int i = 1; i < currentTrackPoints.size(); i++) {
            cumDistancesM[i] = cumDistancesM[i - 1]
                    + currentTrackPoints.get(i - 1).distanceTo(currentTrackPoints.get(i));
        }

        // construimos la serie de puntos (km, altura) y la pintamos
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        for (int i = 0; i < currentTrackPoints.size(); i++) {
            double km = cumDistancesM[i] / 1000.0;
            double ele = currentTrackPoints.get(i).getElevation();
            series.getData().add(new XYChart.Data<>(km, ele));
        }
        elevationChart.getData().add(series);

        // estilamos la linea para que sea azul y un poco mas gruesa
        if (series.getNode() != null) {
            series.getNode().setStyle("-fx-stroke: #0AADFF; -fx-stroke-width: 2;");
        }

        // listeners para la interaccion hover -> cursor sobre mapa
        elevationChart.setOnMouseMoved(e -> updateCursorFromChart(e.getSceneX()));
        elevationChart.setOnMouseExited(e -> {
            if (mapCursor != null) mapCursor.setVisible(false);
        });
    }


    // -----------------------------------------------------------------
    // Hover: convertir posicion del raton sobre la grafica
    //        en un trackpoint y mover el cursor azul del mapa ahi
    // -----------------------------------------------------------------
    private void updateCursorFromChart(double sceneX) {
        // proteccion por si todavia no hay nada cargado
        if (cumDistancesM == null || cumDistancesM.length == 0) return;
        if (currentProjection == null || mapCursor == null) return;

        // pasamos la X del raton (en coords de la escena) a coords del eje X de la grafica,
        // y luego a valor del eje (que esta en km porque asi pintamos los datos)
        Point2D localOnAxis = xAxis.sceneToLocal(sceneX, 0);
        double kmHover = xAxis.getValueForDisplay(localOnAxis.getX()).doubleValue();

        // si el raton esta fuera del rango del eje (sobre la leyenda por ejemplo)
        // ocultamos el cursor del mapa
        if (kmHover < xAxis.getLowerBound() || kmHover > xAxis.getUpperBound()) {
            mapCursor.setVisible(false);
            return;
        }

        // pasamos a metros porque cumDistancesM esta en metros
        double metersHover = kmHover * 1000.0;
        int idx = findClosestIndex(metersHover);
        TrackPoint tp = currentTrackPoints.get(idx);

        // proyectamos el trackpoint a coords del mapa y movemos el circulo ahi
        Point2D p = currentProjection.project(tp);
        mapCursor.setCenterX(p.getX());
        mapCursor.setCenterY(p.getY());
        mapCursor.setVisible(true);
    }

    // Busqueda binaria sobre cumDistancesM (esta ordenado por construccion).
    // Devuelve el indice del trackpoint cuya distancia acumulada esta mas cerca
    // de la distancia que recibimos como parametro.
    private int findClosestIndex(double meters) {
        int lo = 0, hi = cumDistancesM.length - 1;
        while (lo < hi) {
            int mid = (lo + hi) / 2;
            if (cumDistancesM[mid] < meters) lo = mid + 1;
            else hi = mid;
        }
        // la busqueda binaria nos da el primer indice cuya distancia >= meters.
        // El mas cercano puede ser ese o el anterior, asi que comparamos.
        if (lo > 0 && Math.abs(cumDistancesM[lo - 1] - meters) < Math.abs(cumDistancesM[lo] - meters)) {
            return lo - 1;
        }
        return lo;
    }


    // =================================================================
    // ESTADISTICAS (columna derecha)
    // =================================================================
    private void updateStats(Activity a) {
        statNombre.setText(a.getName() != null ? a.getName() : "—");

        statFecha.setText(a.getStartTime() != null ? a.getStartTime().format(fechaLarga) : "—");

        // distancia: getTotalDistance() es metros -> km
        statDistancia.setText(String.format("%.2f km", a.getTotalDistance() / 1000.0));

        statDuracion.setText(formatDuration(a.getDuration()));

        // getAverageSpeed ya devuelve km/h, no hace falta convertir
        double kmh = a.getAverageSpeed();
        statVelocidad.setText(String.format("%.2f km/h", kmh));

        // ritmo medio = minutos por kilometro. Lo calculamos a partir de la velocidad
        // porque no sabemos en que unidad devuelve getAveragePace() la libreria.
        if (kmh > 0) {
            double minPorKm = 60.0 / kmh;
            int m = (int) minPorKm;
            int s = (int) ((minPorKm - m) * 60);
            statRitmo.setText(String.format("%d:%02d min/km", m, s));
        } else {
            statRitmo.setText("—");
        }

        // ascenso y descenso acumulado (en metros)
        statDesnivel.setText(String.format("+%.0f m / -%.0f m",
                a.getElevationGain(), a.getElevationLoss()));

        statAltitud.setText(String.format("%.0f m / %.0f m",
                a.getMinElevation(), a.getMaxElevation()));

        // numero de anotaciones que el usuario haya hecho en esta sesion
        List<Annotation> anns = a.getAnnotations();
        int numAnotaciones = (anns != null) ? anns.size() : 0;
        statAnotaciones.setText(String.valueOf(numAnotaciones));

        // rellenamos tambien la lista de anotaciones de la columna derecha
        annotationsList.getItems().clear();
        if (anns != null) annotationsList.getItems().addAll(anns);
    }

    // Formatea un Duration tipo "1h 23m 45s" o "23m 45s" si no llega a la hora
    private String formatDuration(Duration d) {
        if (d == null) return "—";
        long total = d.getSeconds();
        long h = total / 3600;
        long m = (total % 3600) / 60;
        long s = total % 60;
        if (h > 0) return String.format("%dh %02dm %02ds", h, m, s);
        return String.format("%dm %02ds", m, s);
    }
    
    
    private void drawAnnotations(Activity a){
        List<Annotation> list = a.getAnnotations();
        for(Annotation ann: list){
            drawAnnotation(ann);
        }
    }
    
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
                mapPane.getChildren().add(c);
            }
            
            if(ann.getType() == AnnotationType.TEXT) {
                Point2D p = currentProjection.project(geos.get(0));
                Text t = new Text(ann.getText().isBlank() ? "?" : ann.getText());
                t.setX(p.getX());
                t.setY(p.getY());
                t.setFill(color);
                t.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
                installTooltip(t, tip);
                mapPane.getChildren().add(t);
            }
            
            if(ann.getType() == AnnotationType.LINE) {
                Point2D p1 = currentProjection.project(geos.get(0));
                Point2D p2 = currentProjection.project(geos.get(1));
                Line l = new Line(p1.getX(), p1.getY(), p2.getX(), p2.getY());
                l.setStroke(color);
                l.setStrokeWidth(ann.getStrokeWidth());
                installTooltip(l, tip);
                mapPane.getChildren().add(l);
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
                mapPane.getChildren().add(c);
            }
        
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
    
    /** Tooltip que aparece solo al pasar el cursor, no permanentemente. */
    private void installTooltip(Node node, String text) {
        Tooltip tip = new Tooltip(text);
        tip.setShowDelay(javafx.util.Duration.millis(150));
        tip.setHideDelay(javafx.util.Duration.millis(100));
        tip.setShowDuration(javafx.util.Duration.seconds(8));
        Tooltip.install(node, tip);
    }
    
    private Color safeColor(String hex) {
        try   { return Color.web(hex); }
        catch (Exception e) { return Color.RED; }
    }


    // =================================================================
    // NAVEGACION
    // =================================================================
    @FXML
    private void volverHome() {
        try {
            App.getInstance().switchToHome();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // =================================================================
    // HOVER de los botones (cambian de verde claro a verde oscuro)
    // =================================================================
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


    // =================================================================
    // CREACION DE ANOTACIONES (copiado del MainController)
    // =================================================================

    // Menu contextual con los 4 tipos de anotacion
    private void showAnnotationMenu(double mapX, double mapY, double screenX, double screenY){
        ContextMenu menu = new ContextMenu();
        MenuItem miPoint  = new MenuItem("Punto");
        MenuItem miText   = new MenuItem("Texto");
        MenuItem miLine   = new MenuItem("Linea (selecciona 2 puntos)");
        MenuItem miCircle = new MenuItem("Circulo (selecciona 2 puntos)");

        miPoint.setOnAction(e -> startOnePointAnnotation(AnnotationType.POINT, mapX, mapY));
        miText.setOnAction(e -> startOnePointAnnotation(AnnotationType.TEXT, mapX, mapY));
        miLine.setOnAction(e -> startTwoPointAnnotation(AnnotationType.LINE, mapX, mapY));
        miCircle.setOnAction(e -> startTwoPointAnnotation(AnnotationType.CIRCLE, mapX, mapY));

        menu.getItems().addAll(miPoint, miText, miLine, miCircle);
        menu.show(mapPane.getScene().getWindow(), screenX, screenY);
    }

    // Anotacion de 1 punto: dialogo y guardar directo (diferido para que el menu se cierre)
    private void startOnePointAnnotation(AnnotationType type, double mapX, double mapY){
        Platform.runLater(() -> {
            Optional<String[]> res = showAnnotationDialog(type);
            if (res.isEmpty()) return;
            GeoPoint geo = currentProjection.unproject(mapX, mapY);
            saveAnnotation(type, res.get()[0], res.get()[1], List.of(geo));
        });
    }

    // Anotacion de 2 puntos: dialogo y esperar 2.o clic izquierdo
    private void startTwoPointAnnotation(AnnotationType type, double mapX, double mapY){
        Platform.runLater(() -> {
            Optional<String[]> res = showAnnotationDialog(type);
            if (res.isEmpty()) return;
            pendingText = res.get()[0];
            pendingColor = res.get()[1];
            pendingType = type;
            pendingPoints.clear();
            pendingPoints.add(currentProjection.unproject(mapX, mapY));
            mapPane.setStyle("-fx-cursor: crosshair;");
        });
    }

    private void cancelPending(){
        pendingType = null;
        pendingPoints.clear();
        if (mapPane != null) mapPane.setStyle("");
    }

    // Dialogo para texto y color de la anotacion. Mismo workaround que en Main
    // (initOwner, modality, forzar tamano del Stage, foco en el TextField) para
    // que no salga cortado ni detras en Linux.
    private Optional<String[]> showAnnotationDialog(AnnotationType type){
        String header = "";
        if (type == AnnotationType.POINT)       header = "Punto - introduce los datos";
        else if (type == AnnotationType.TEXT)   header = "Texto - introduce los datos";
        else if (type == AnnotationType.LINE)   header = "Linea - haz clic izquierdo en el mapa para el 2.o punto";
        else if (type == AnnotationType.CIRCLE) header = "Circulo - haz clic izquierdo en el mapa para el borde";

        Dialog<String[]> dialog = new Dialog<>();
        dialog.setTitle("Nueva anotacion");
        dialog.initOwner(mapPane.getScene().getWindow());
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setResizable(true);

        ButtonType okBtn = new ButtonType("Aceptar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okBtn, ButtonType.CANCEL);

        Label headerLabel = new Label(header);
        headerLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        headerLabel.setWrapText(true);
        headerLabel.setMinWidth(380);

        Label tLabel = new Label("Texto:");
        tLabel.setMinWidth(60);
        TextField textField = new TextField();
        textField.setPromptText("Texto (opcional)");
        textField.setPrefWidth(300);
        HBox.setHgrow(textField, Priority.ALWAYS);
        HBox row1 = new HBox(10, tLabel, textField);
        row1.setAlignment(Pos.CENTER_LEFT);

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
        dialog.getDialogPane().setMinSize(460, 220);
        dialog.getDialogPane().setPrefSize(460, 220);

        dialog.setOnShown(ev -> {
            Stage st = (Stage) dialog.getDialogPane().getScene().getWindow();
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

    // Persistir la anotacion (la lib la guarda en BBDD) y dibujarla sobre el mapa
    private void saveAnnotation(AnnotationType type, String text, String color, List<GeoPoint> points){
        if (currentActivity == null) return;
        Annotation ann = new Annotation(type, text, color, 2.5, points);
        Annotation saved = app.addAnnotation(currentActivity, ann);
        if (saved != null){
            drawAnnotation(saved);
            annotationsList.getItems().add(saved);
        }
    }
}
