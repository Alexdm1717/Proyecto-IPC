/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */
package mapademo.singin;

import java.net.URL;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
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
    private DatePicker birthDateField;
    @FXML
    private Label dateErrorMsg;
    @FXML
    private Button avatarButton;
    @FXML
    private Label avatarErrorMsg;
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
        LocalDate birth = birthDateField.getValue();
        // Anadir la selecion de foto
        String avatarPath = "";
        
        boolean nickNameOk = User.checkNickName(nickName);
        if (!nickNameOk){
            nameErrorMsg.setText("NickName incorrecto");
            nameErrorMsg.setVisible(true);
            return;
        }
        boolean emailOk = User.checkEmail(email);
        if(!emailOk){
            emailErrorMsg.setText("Email incorrecto");
            emailErrorMsg.setVisible(true);
            return;
        }
        boolean passwordOk = User.checkPassword(password);
        if (!passwordOk){
            PasswordErrorMsg.setText("Password Incorrecto");
            PasswordErrorMsg.setVisible(true);
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
            App.getInstance().switchToMain();
        }catch(Exception e){}
    }
}
