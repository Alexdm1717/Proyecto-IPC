package mapademo.historial;

import java.io.File;
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
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polyline;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import mapademo.App;
import upv.ipc.sportlib.Activity;
import upv.ipc.sportlib.MapProjection;
import upv.ipc.sportlib.MapRegion;
import upv.ipc.sportlib.SportActivityApp;
import upv.ipc.sportlib.TrackPoint;


public class HistorialController implements Initializable {

    // ----- elementos del fxml -----
    @FXML private VBox sessionsListBox;        // contenedor de las cards de sesiones (izquierda)
    @FXML private ScrollPane mapScroll;        // scroll que envuelve el mapa
    @FXML private Pane mapPane;                // donde dibujamos el mapa + ruta
    @FXML private LineChart<Number, Number> elevationChart;  // grafica del perfil de desnivel
    @FXML private NumberAxis xAxis;            // eje X de la grafica (distancia en km)
    @FXML private NumberAxis yAxis;            // eje Y de la grafica (altitud en m)

    // labels de las estadisticas (columna derecha)
    @FXML private Label statNombre;
    @FXML private Label statFecha;
    @FXML private Label statDistancia;
    @FXML private Label statDuracion;
    @FXML private Label statVelocidad;
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


    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // pintamos la lista de sesiones a la izquierda
        refreshSessionsList();

        // si hay alguna sesion, abrimos la mas reciente por defecto
        // (asi no se ve la pantalla con el mapa y la grafica vacios al entrar)
        List<Activity> acts = sortedUserActivities();
        if (!acts.isEmpty()) {
            showActivity(acts.get(0));
        }
    }


    // =================================================================
    // LISTA DE SESIONES (columna izquierda)
    // =================================================================

    // Llena el VBox de la izquierda con una card por cada sesion del usuario
    private void refreshSessionsList() {
        sessionsListBox.getChildren().clear();
        List<Activity> acts = sortedUserActivities();

        if (acts.isEmpty()) {
            // si no hay ninguna sesion ponemos un mensajito
            Label vacio = new Label("No has importado ninguna sesion todavia");
            vacio.setTextFill(Color.web("#6B7280"));
            vacio.setFont(Font.font("SansSerif", 13));
            vacio.setWrapText(true);
            sessionsListBox.getChildren().add(vacio);
            return;
        }

        for (Activity a : acts) {
            sessionsListBox.getChildren().add(buildSessionCard(a));
        }
    }

    // Sesiones del usuario actual, mas recientes primero
    private List<Activity> sortedUserActivities() {
        List<Activity> list = new ArrayList<>();
        List<Activity> fromApp = SportActivityApp.getInstance().getUserActivities();
        if (fromApp != null) list.addAll(fromApp);
        // copiamos a ArrayList para evitar que la lista venga inmutable y .sort pete
        list.sort(Comparator.comparing(Activity::getStartTime).reversed());
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

        // listener para que al clicar en la card se cargue esta actividad
        card.setOnMouseClicked(e -> showActivity(a));
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
        File f = fc.showOpenDialog(sessionsListBox.getScene().getWindow());
        if (f == null) return;  // ha cancelado el dialogo

        // la libreria parsea el GPX y crea la Activity en la BBDD
        Activity a = SportActivityApp.getInstance().importActivity(f);
        if (a == null) {
            new Alert(Alert.AlertType.ERROR, "No se pudo importar el GPX").showAndWait();
            return;
        }
        // refrescamos la lista y abrimos la sesion recien importada
        refreshSessionsList();
        showActivity(a);
    }


    // =================================================================
    // CARGAR UNA SESION: mapa + grafica + estadisticas
    // =================================================================

    // Punto de entrada cuando el usuario selecciona una sesion
    private void showActivity(Activity a) {
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

        // la propia libreria nos dice que region del mapa cubre la actividad
        MapRegion region = a.getSuggestedMap();
        if (region == null) return;

        File imgFile = new File(region.getImagePath());
        if (!imgFile.exists()) {
            // no deberia pasar pero por si acaso
            Label err = new Label("Imagen del mapa no encontrada");
            err.setTextFill(Color.web("#6B7280"));
            mapPane.getChildren().add(err);
            return;
        }

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

        // creamos el cursor azul que se ira moviendo por encima del mapa cuando
        // el usuario pase el raton sobre la grafica del perfil de desnivel.
        // Lo dejamos invisible hasta que se haga hover sobre la grafica.
        mapCursor = new Circle(7, Color.web("#0AADFF"));
        mapCursor.setStroke(Color.WHITE);
        mapCursor.setStrokeWidth(2);
        mapCursor.setVisible(false);
        mapPane.getChildren().add(mapCursor);
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

        // getAverageSpeed devuelve m/s, lo pasamos a km/h multiplicando por 3.6
        double kmh = a.getAverageSpeed() * 3.6;
        statVelocidad.setText(String.format("%.2f km/h", kmh));

        // ascenso y descenso acumulado (en metros)
        statDesnivel.setText(String.format("+%.0f m / -%.0f m",
                a.getElevationGain(), a.getElevationLoss()));

        statAltitud.setText(String.format("%.0f m / %.0f m",
                a.getMinElevation(), a.getMaxElevation()));

        // numero de anotaciones que el usuario haya hecho en esta sesion
        int numAnotaciones = (a.getAnnotations() != null) ? a.getAnnotations().size() : 0;
        statAnotaciones.setText(String.valueOf(numAnotaciones));
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
}
