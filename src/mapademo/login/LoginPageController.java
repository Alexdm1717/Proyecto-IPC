/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */
package mapademo.login;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import mapademo.App;
import upv.ipc.sportlib.SportActivityApp;

/**
 * FXML Controller class
 *
 * @author alexi
 */
public class LoginPageController implements Initializable {

    @FXML
    private PasswordField passwordField;
    @FXML
    private Label PasswordErrorMsg;
    @FXML
    private Button logInButton;
    @FXML
    private Button logInButton1;
    @FXML
    private TextField nameField;
    @FXML
    private Label nameErrorMsg;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
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

    // alternar entre login / singin.
    @FXML
    public void switchToSingin(){
        try{
            App.getInstance().switchToSingin();
        }
        catch(Exception e){System.out.println(e);}
    }

    @FXML
    private void handleLogIn(MouseEvent event) {
        String nickName = nameField.getText();
        String password = passwordField.getText();
        
        boolean ok = SportActivityApp.getInstance().login(nickName, password);
        
        if(ok){
            try{App.getInstance().switchToHome();}catch(Exception e){}
        }
        else{
            String error = "NickName o contrasena incorrectos";
            nameErrorMsg.setText(error);
            nameErrorMsg.setVisible(true);
            PasswordErrorMsg.setText(error);
            PasswordErrorMsg.setVisible(true);
        }
    }
    
    



}
