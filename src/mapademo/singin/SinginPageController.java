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
        // TODO
    }

    // efecto hover: bajamos la opacidad del boton al pasar el raton por encima
    @FXML
    private void hoverEnter(MouseEvent e){
        ((Node) e.getSource()).setOpacity(0.85);
    }

    @FXML
    private void hoverExit(MouseEvent e){
        ((Node) e.getSource()).setOpacity(1.0);
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
        boolean ageOk = User.isOlderThan(birth, 16);
        if(!ageOk){
            dateErrorMsg.setText("Tienes que tener mas de 16");
            dateErrorMsg.setVisible(true);
            return;
        }
        
        // Registra el usuario y asignamos el usuario actual
        SportActivityApp.getInstance().registerUser(nickName, email, password, birth, selectedAvatarPath);
        SportActivityApp.getInstance().login(nickName, password);
        
        // Cambia a la aplicacion
        try{
            App.getInstance().switchToHome();
        }catch(Exception e){}
    }

}
