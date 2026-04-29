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
import mapademo.App;

/**
 * FXML Controller class
 *
 * @author alexi
 */
public class LoginPageController implements Initializable {

    @FXML
    private Button enterbutton;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
    }
    
    public void enterApp() {
        try{
            App.getInstance().switchToMain();
        }
        catch (Exception e){}
    }
    
}
