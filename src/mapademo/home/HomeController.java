package mapademo.home;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import mapademo.App;
import upv.ipc.sportlib.SportActivityApp;
import upv.ipc.sportlib.User;

public class HomeController implements Initializable {

    @FXML private ImageView profileImage;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        User user = SportActivityApp.getInstance().getCurrentUser();
        if (user == null) return;
        try {
            Image img = user.getAvatar();
            if (img != null) {
                profileImage.setImage(img);
                double w = img.getWidth();
                double h = img.getHeight();
                if (w > 0 && h > 0) {
                    double size = Math.min(w, h);
                    double x = (w - size) / 2;
                    double y = (h - size) / 2;
                    profileImage.setViewport(new Rectangle2D(x, y, size, size));
                }
            }
        } catch (Exception ignored) {}
    }

 
    @FXML
    private void startNewSession() {
        try {
            App.getInstance().switchToRunTracker();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

       //esto está por hacer
    @FXML
    private void openHistorial() {
        new Alert(Alert.AlertType.INFORMATION, "Historial: próximamente").showAndWait();
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
