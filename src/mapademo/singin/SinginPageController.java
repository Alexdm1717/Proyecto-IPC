/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */
package mapademo.singin;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.DatePicker; 
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
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

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
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
        
        // Anadir la selecion de foto
        String avatarPath = "";
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
        SportActivityApp.getInstance().registerUser(nickName, email, password, birth, avatarPath);
        SportActivityApp.getInstance().login(nickName, password);
        
        // Cambia a la aplicacion
        try{
            App.getInstance().switchToHome();
        }catch(Exception e){}
    }

}
