package mapademo.home;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
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
    @FXML private VBox recentSessionsBox;

    // formato para la fecha que se ve en las cards de las sesiones
    private DateTimeFormatter fechaCorta = DateTimeFormatter.ofPattern("dd MMM yyyy");


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

        // ordenamos por fecha de mas reciente a mas antigua (las 3 primeras seran las que mostremos)
        activities.sort(Comparator.comparing(Activity::getStartTime).reversed());

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

        // ---- ultimas 3 sesiones ----
        recentSessionsBox.getChildren().clear();
        if (activities.isEmpty()) {
            // mensajito por defecto cuando el usuario aun no ha registrado nada
            Label vacio = new Label("Aun no has registrado ninguna sesion");
            vacio.setTextFill(Color.web("#6B7280"));
            vacio.setFont(Font.font("SansSerif", 14));
            recentSessionsBox.getChildren().add(vacio);
        } else {
            // como max 3 cards (si hay menos sesiones pues menos)
            int max = Math.min(3, activities.size());
            for (int i = 0; i < max; i++) {
                recentSessionsBox.getChildren().add(buildSessionCard(activities.get(i)));
            }
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
