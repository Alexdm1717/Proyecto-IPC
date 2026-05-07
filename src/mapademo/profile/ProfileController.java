package mapademo.profile;

import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import mapademo.App;
import upv.ipc.sportlib.SportActivityApp;
import upv.ipc.sportlib.User;

public class ProfileController implements Initializable {

    @FXML private ImageView avatarImage;
    @FXML private TextField nickField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField birthDateField;
    @FXML private Label emailErrorMsg;
    @FXML private Label passwordErrorMsg;
    @FXML private Label dateErrorMsg;

    
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private String selectedAvatarPath;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        emailErrorMsg.managedProperty().bind(emailErrorMsg.visibleProperty());
        passwordErrorMsg.managedProperty().bind(passwordErrorMsg.visibleProperty());
        dateErrorMsg.managedProperty().bind(dateErrorMsg.visibleProperty());

        User user = SportActivityApp.getInstance().getCurrentUser();
        if (user == null) return;

        nickField.setText(user.getNickName());
        emailField.setText(user.getEmail());
        passwordField.setText(user.getPassword());
        if (user.getBirthDate() != null) {
            birthDateField.setText(user.getBirthDate().format(DATE_FORMAT));
        }
        selectedAvatarPath = user.getAvatarPath();

        try {
            Image img = user.getAvatar();
            if (img != null) setAvatarCentered(img);
        } catch (Exception ignored) {}
    }

    @FXML
    private void chooseAvatar() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Selecciona avatar");
        fc.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg")
        );
        File file = fc.showOpenDialog(avatarImage.getScene().getWindow());
        if (file != null) {
            selectedAvatarPath = file.getAbsolutePath();
            setAvatarCentered(new Image(file.toURI().toString()));
        }
    }

    private void setAvatarCentered(Image img) {
        avatarImage.setImage(img);
        double w = img.getWidth();
        double h = img.getHeight();
        if (w > 0 && h > 0) {
            double size = Math.min(w, h);
            double x = (w - size) / 2;
            double y = (h - size) / 2;
            avatarImage.setViewport(new Rectangle2D(x, y, size, size));
        }
    }

    @FXML
    private void applyChanges() {
        emailErrorMsg.setVisible(false);
        passwordErrorMsg.setVisible(false);
        dateErrorMsg.setVisible(false);

        String email = emailField.getText();
        String password = passwordField.getText();
        String birthText = birthDateField.getText();

        if (!User.checkEmail(email)) {
            emailErrorMsg.setText("Email incorrecto");
            emailErrorMsg.setVisible(true);
            return;
        }

        String passwordError = null;
        if (password.length() < 8 || password.length() > 20) {
            passwordError = "Debe tener entre 8 y 20 caracteres";
        } else if (!password.matches(".*[a-z].*")) {
            passwordError = "Falta una letra minúscula";
        } else if (!password.matches(".*[A-Z].*")) {
            passwordError = "Falta una letra mayúscula";
        } else if (!password.matches(".*\\d.*")) {
            passwordError = "Falta un dígito";
        } else if (!password.matches(".*[!@#$%&*()\\-+=].*")) {
            passwordError = "Falta un símbolo (!@#$%&*()-+=)";
        }
        if (passwordError != null) {
            passwordErrorMsg.setText(passwordError);
            passwordErrorMsg.setVisible(true);
            return;
        }

        LocalDate birth;
        try {
            birth = LocalDate.parse(birthText, DATE_FORMAT);
        } catch (DateTimeParseException e) {
            dateErrorMsg.setText("Formato debe ser dd/MM/yyyy");
            dateErrorMsg.setVisible(true);
            return;
        }
        if (!User.isOlderThan(birth, 16)) {
            dateErrorMsg.setText("Tienes que tener mas de 16");
            dateErrorMsg.setVisible(true);
            return;
        }

        boolean ok = SportActivityApp.getInstance()
            .updateCurrentUser(email, password, birth, selectedAvatarPath);

        if (ok) {
            new Alert(Alert.AlertType.INFORMATION, "Cambios guardados").showAndWait();
        } else {
            new Alert(Alert.AlertType.ERROR, "No se pudieron guardar los cambios").showAndWait();
        }
    }

    @FXML
    private void volverHome() {
        try {
            App.getInstance().switchToHome();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
