/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */
package mapademo.login;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import mapademo.App;

/**
 * FXML Controller class
 *
 * @author alexi
 */
public class LoginPageController implements Initializable {

    @FXML
    private TextField emailField;
    @FXML
    private Label emailErrorMsg;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label PasswordErrorMsg;
    @FXML
    private Button logInButton;
    @FXML
    private Button logInButton1;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
    }    
    
    // alternar entre login / singin.
    @FXML
    public void switchToSingin(){
        try{
            App.getInstance().switchToSingin();
        }
        catch(Exception e){System.out.println(e);}
    }



}
