/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */
package mapademo.singin;

import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import mapademo.App;
import upv.ipc.sportlib.SportActivityApp;
import upv.ipc.sportlib.User;

/**
 * FXML Controller class
 *
 * @author alexi
 */
public class SinginPageController implements Initializable {

    @FXML
    private TextField nameField;
    @FXML
    private Label nameErrorMsg;
    @FXML
    private TextField emailField;
    @FXML
    private Label emailErrorMsg;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label PasswordErrorMsg;
    @FXML
    private TextField birthDateField;
    @FXML
    private Label dateErrorMsg;
    @FXML
    private Button SingInButton;
    @FXML
    private Button logInButton;
    @FXML
    private ImageView avatarImage;

    // ruta de la foto que ha elegido el usuario, vacia si no ha elegido nada
    private String selectedAvatarPath = "";

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Bindeamos managed a visible para que cuando un label de error se haga visible
        // tambien ocupe espacio en el layout (los teniamos con managed=false
        // para que no ocuparan al estar ocultos, pero eso hacia que al ponerlos
        // visibles no se vieran porque el padre no les reservaba sitio).
        nameErrorMsg.managedProperty().bind(nameErrorMsg.visibleProperty());
        emailErrorMsg.managedProperty().bind(emailErrorMsg.visibleProperty());
        PasswordErrorMsg.managedProperty().bind(PasswordErrorMsg.visibleProperty());
        dateErrorMsg.managedProperty().bind(dateErrorMsg.visibleProperty());
    }

    // efecto hover: el verde claro pasa a verde mas oscuro,
    // y el gris claro pasa a gris algo mas oscuro
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

    // abre un FileChooser para que el usuario elija su foto de perfil
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
            Image img = new Image(file.toURI().toString());
            avatarImage.setImage(img);
            // recortamos un cuadrado del centro para que se vea bien dentro del circulo
            double w = img.getWidth();
            double h = img.getHeight();
            if (w > 0 && h > 0) {
                double size = Math.min(w, h);
                double x = (w - size) / 2;
                double y = (h - size) / 2;
                avatarImage.setViewport(new Rectangle2D(x, y, size, size));
            }
        }
    }

    
    // alternar entre login / singin.
    @FXML
    public void switchToLogin(){
        try{
            App.getInstance().switchToLogin();
        }catch(Exception e){}
    }

    @FXML
    private void handleForm(MouseEvent event) {
        String nickName = nameField.getText();
        String email = emailField.getText();
        String password = passwordField.getText();
        String birthText = birthDateField.getText();
        LocalDate birth;

        //al clicar pone por defecto los mensajes de error en oculto,
        //si siguen estando mal se vuelven a activar después
        nameErrorMsg.setVisible(false);                                                                                                                                                       
        emailErrorMsg.setVisible(false);                                                                                                                                                      
        PasswordErrorMsg.setVisible(false);     
        dateErrorMsg.setVisible(false);
        
        boolean nickNameOk = User.checkNickName(nickName);
        if (!nickNameOk){
            nameErrorMsg.setText("6-15 caracteres. Solo letras, números, _ y -");
            nameErrorMsg.setVisible(true);
            return;
        }
        boolean emailOk = User.checkEmail(email);
        if(!emailOk){
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
            PasswordErrorMsg.setText(passwordError);                                                                                                                                          
            PasswordErrorMsg.setVisible(true);      
            return;                                                                                                                                                                           
        }
        
        //he cambiado esto a .ofPattern para que parsee correctamente, antes los dates parseaban en otro formato de fecha rara
        try {
            birth = LocalDate.parse(birthText, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (DateTimeParseException e) {
            dateErrorMsg.setText("Formato debe ser dd/MM/yyyy");
            dateErrorMsg.setVisible(true);
            return;
        }
        boolean ageOk = User.isOlderThan(birth, 12);
        if(!ageOk){
            dateErrorMsg.setText("Tienes que tener mas de 12");
            dateErrorMsg.setVisible(true);
            return;
        }

        // Intenta registrar. Si devuelve false significa que el nick ya existe o
        // algo ha fallado al guardar en BBDD. Antes lo ignorabamos y seguia adelante
        // intentando hacer login con un usuario que no existia, sin avisar al usuario.
        boolean ok = SportActivityApp.getInstance().registerUser(
                nickName, email, password, birth, selectedAvatarPath);
        if(!ok){
            nameErrorMsg.setText("Ese nombre de usuario ya existe");
            nameErrorMsg.setVisible(true);
            return;
        }

        // Registrado correctamente: mostramos dialogo de bienvenida con los datos
        // y entramos directamente al home
        Alert ok_dialog = new Alert(Alert.AlertType.INFORMATION,
                "Bienvenido a Running La Safor\n"
                + "Nombre de usuario: " + nickName + "\n"
                + "Contraseña: " + password);
        ok_dialog.setHeaderText(null);
        ok_dialog.setTitle("Registro completado");
        ok_dialog.showAndWait();

        // hacemos login para que el currentUser quede asignado y entramos al home
        SportActivityApp.getInstance().login(nickName, password);
        try{
            App.getInstance().switchToHome();
        }catch(Exception e){}
    }

}
