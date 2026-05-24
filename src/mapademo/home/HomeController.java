package mapademo.home;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import mapademo.App;
import upv.ipc.sportlib.Activity;
import upv.ipc.sportlib.SportActivityApp;
import upv.ipc.sportlib.User;


public class HomeController implements Initializable {

    // ----- elementos del fxml -----
    @FXML private ImageView profileImage;
    @FXML private Label nicknameLabel;
    @FXML private Label emailLabel;
    @FXML private Label sessionsCountLabel;
    @FXML private Label kmThisMonthLabel;
    // ListView<Object> porque mezclamos Activity (cards normales) con la string "..."
    // que indica que hay mas sesiones de las que caben
    @FXML private ListView<Object> recentSessionsList;

    // formato para la fecha que se ve en las cards de las sesiones
    private DateTimeFormatter fechaCorta = DateTimeFormatter.ofPattern("dd MMM yyyy");

    // sesiones del usuario ordenadas, se guardan para repoblar la lista
    // cada vez que cambia el alto del ListView
    private List<Activity> allActivities = new ArrayList<>();
    // alto fijo de cada celda (lo usamos para calcular cuantas caben)
    private static final double CELL_HEIGHT = 64;


    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // si por lo que sea no hay usuario logueado no hacemos nada
        // (no deberia pasar porque a Home solo se llega tras login/registro)
        User user = SportActivityApp.getInstance().getCurrentUser();
        if (user == null) return;

        // ---- columna izquierda: datos del usuario ----
        nicknameLabel.setText("@" + user.getNickName());
        emailLabel.setText(user.getEmail());

        // pintamos el avatar del usuario. Si no tiene se queda con la perfil.png por defecto
        // que ya viene puesta en el fxml
        try {
            Image img = user.getAvatar();
            if (img != null) {
                profileImage.setImage(img);
                // las fotos suelen ser rectangulares, asi que recortamos un cuadrado
                // del centro para que se vea bien dentro del circulo y no descuadrado
                double w = img.getWidth();
                double h = img.getHeight();
                if (w > 0 && h > 0) {
                    double size = Math.min(w, h);
                    double x = (w - size) / 2;
                    double y = (h - size) / 2;
                    profileImage.setViewport(new Rectangle2D(x, y, size, size));
                }
            }
        } catch (Exception e) {
            // si peta cargando la imagen seguimos con la default
        }

        // ---- columna central: estadisticas y lista ----

        // cogemos las actividades del usuario actual.
        // Como la lista puede venir inmutable la copiamos a un ArrayList para poder ordenarla.
        List<Activity> activities = new ArrayList<>();
        List<Activity> fromApp = SportActivityApp.getInstance().getUserActivities();
        if (fromApp != null) activities.addAll(fromApp);

        // ordenamos por fecha (descendente). Si hay empate de fecha, desempata por id
        // (las recien anadidas tienen id mas alto en la BBDD), tambien descendente.
        activities.sort(
            Comparator.comparing(Activity::getStartTime)
                .thenComparingLong(Activity::getId)
                .reversed()
        );

        // numero total de sesiones (todas las que tiene el usuario, sin filtrar por fecha)
        sessionsCountLabel.setText(String.valueOf(activities.size()));

        // kilometros recorridos en los ultimos 30 dias.
        // getTotalDistance() viene en METROS, asi que despues dividimos entre 1000
        LocalDateTime haceUnMes = LocalDateTime.now().minusDays(30);
        double metrosUltimoMes = 0;
        for (Activity a : activities) {
            if (a.getStartTime() != null && a.getStartTime().isAfter(haceUnMes)) {
                metrosUltimoMes += a.getTotalDistance();
            }
        }
        kmThisMonthLabel.setText(String.format("%.1f km", metrosUltimoMes / 1000.0));

        // ---- ultimas sesiones (en el ListView) ----
        allActivities = activities;

        // placeholder por si el usuario aun no tiene nada registrado
        Label vacio = new Label("Aun no has registrado ninguna sesion");
        vacio.setTextFill(Color.web("#6B7280"));
        vacio.setFont(Font.font("SansSerif", 14));
        vacio.setWrapText(true);
        recentSessionsList.setPlaceholder(vacio);

        // Altura fija por celda. La necesitamos para saber cuantas caben en el ListView.
        recentSessionsList.setFixedCellSize(CELL_HEIGHT);

        // cellFactory: si el item es una Activity pintamos su card,
        // si es la string "..." pintamos los puntos suspensivos centrados
        recentSessionsList.setCellFactory(lv -> {
            ListCell<Object> cell = new ListCell<Object>(){
                @Override
                protected void updateItem(Object item, boolean empty){
                    super.updateItem(item, empty);
                    if (empty || item == null){
                        setText(null);
                        setGraphic(null);
                    } else if (item instanceof Activity){
                        setText(null);
                        setGraphic(buildSessionCard((Activity) item));
                    } else {
                        // fila de "..." cuando hay mas sesiones de las que caben
                        Label dots = new Label("...");
                        dots.setTextFill(Color.web("#6B7280"));
                        dots.setFont(Font.font("SansSerif", FontWeight.BOLD, 22));
                        HBox box = new HBox(dots);
                        box.setAlignment(Pos.CENTER);
                        setText(null);
                        setGraphic(box);
                    }
                }
            };
            // resaltado verde al seleccionar (en vez del azul por defecto)
            cell.selectedProperty().addListener((obs, antes, ahora) -> {
                if (ahora) cell.setStyle("-fx-background-color: #C6FF3B;");
                else cell.setStyle("");
            });
            return cell;
        });

        // ocultamos las scrollbars (el listview no es navegable: solo muestra lo que cabe)
        recentSessionsList.skinProperty().addListener((obs, oldVal, newVal) -> {
            Platform.runLater(() -> {
                recentSessionsList.lookupAll(".scroll-bar").forEach(sb -> {
                    sb.setVisible(false);
                    sb.setManaged(false);
                });
            });
        });

        // cada vez que cambia el alto del ListView recalculamos cuantas caben
        recentSessionsList.heightProperty().addListener((obs, antes, ahora) -> populateRecent());
        populateRecent();
    }

    // Llena el ListView con tantas actividades como quepan visualmente.
    // Si sobran, sustituye la ultima fila por "..." para indicar que hay mas.
    private void populateRecent(){
        double height = recentSessionsList.getHeight();
        if (height <= 0) return;
        int caben = (int) Math.floor(height / CELL_HEIGHT);
        if (caben < 1) caben = 1;

        recentSessionsList.getItems().clear();
        if (allActivities.size() <= caben){
            // todas caben
            recentSessionsList.getItems().addAll(allActivities);
        } else {
            // mostramos (caben - 1) actividades y dejamos la ultima fila para los "..."
            int mostrar = Math.max(0, caben - 1);
            recentSessionsList.getItems().addAll(allActivities.subList(0, mostrar));
            recentSessionsList.getItems().add("...");
        }
    }


    // -----------------------------------------------------------------
    // Card de sesion: HBox con nombre+fecha a la izquierda y los km a la derecha
    // -----------------------------------------------------------------
    private HBox buildSessionCard(Activity a) {
        HBox card = new HBox(15);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color: #EEF1EB; -fx-background-radius: 12;");
        card.setPadding(new Insets(12, 16, 12, 16));

        // bloque izquierda: nombre arriba, fecha mas pequena debajo
        VBox left = new VBox(2);
        Label nombre = new Label(a.getName() != null ? a.getName() : "Sesion");
        nombre.setTextFill(Color.web("#111111"));
        nombre.setFont(Font.font("SansSerif", FontWeight.BOLD, 15));

        String fechaText = "—";
        if (a.getStartTime() != null) {
            fechaText = a.getStartTime().toLocalDate().format(fechaCorta);
        }
        Label fecha = new Label(fechaText);
        fecha.setTextFill(Color.web("#6B7280"));
        fecha.setFont(Font.font("SansSerif", 12));

        left.getChildren().addAll(nombre, fecha);

        // Region para empujar los km al lado derecho del HBox (spacer flexible)
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        double km = a.getTotalDistance() / 1000.0;
        Label dist = new Label(String.format("%.2f km", km));
        dist.setTextFill(Color.web("#111111"));
        dist.setFont(Font.font("SansSerif", FontWeight.BOLD, 15));

        card.getChildren().addAll(left, sp, dist);
        return card;
    }


    // -----------------------------------------------------------------
    // Hover de los botones: cambian de verde claro a verde oscuro
    // -----------------------------------------------------------------
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


    // -----------------------------------------------------------------
    // Navegacion a las otras pantallas
    // -----------------------------------------------------------------
    @FXML
    private void startNewSession() {
        try {
            App.getInstance().switchToRunTracker();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void openHistorial() {
        try {
            App.getInstance().switchToHistorial();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void openProfile() {
        try {
            App.getInstance().switchToProfile();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
