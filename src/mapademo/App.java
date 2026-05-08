/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mapademo;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import upv.ipc.sportlib.User;

/**
 *
 * @author jose
 */
public class App extends Application {
    // Referencias a los archivos
    String loginPath = "/mapademo/login/LoginPage.fxml";
    String singinPath = "/mapademo/singin/SinginPage.fxml";
    String homePath = "/mapademo/home/Home.fxml";
    String runTrackerPath = "/mapademo/main/Main.fxml";
    String profilePath = "/mapademo/profile/Profile.fxml";
    Stage stageRef;

    private static App instance;

    public App(){
        instance = this;
    }

    public static App getInstance(){
        return instance;
    }

    @Override
    public void start(Stage stage) throws Exception {
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/resources/logo.png")));
        stageRef = stage;
        switchToLogin();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

    // Si la primera vez no hay scene, la creamos. Si ya hay, solo cambiamos su root.
    // Asi no se cierra y abre la ventana cada vez que cambiamos de pantalla
    // y no se descuadra el tamaño.
    public void switchToLogin() throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource(loginPath));
        stageRef.setTitle("Login");
        if (stageRef.getScene() == null) {
            stageRef.setScene(new Scene(root, 1280, 840));
        } else {
            stageRef.getScene().setRoot(root);
        }
        stageRef.setWidth(1280);
        stageRef.setHeight(840);
        if (!stageRef.isShowing()) stageRef.show();
    }

    public void switchToSingin() throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource(singinPath));
        stageRef.setTitle("SingIn");
        if (stageRef.getScene() == null) {
            stageRef.setScene(new Scene(root, 1280, 840));
        } else {
            stageRef.getScene().setRoot(root);
        }
        stageRef.setWidth(1280);
        stageRef.setHeight(840);
        if (!stageRef.isShowing()) stageRef.show();
    }
    //ahora main es RunTracker en las llamadas y la pantalla "main" es Home porque entiendo que esa
    //pantalla que has hecho es la de una nueva sesion, que se accede desde un botón de la Home.
    public void switchToHome() throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource(homePath));
        stageRef.setTitle("Home");
        if (stageRef.getScene() == null) {
            stageRef.setScene(new Scene(root, 1280, 840));
        } else {
            stageRef.getScene().setRoot(root);
        }
        stageRef.setWidth(1280);
        stageRef.setHeight(840);
        if (!stageRef.isShowing()) stageRef.show();
    }

    public void switchToProfile() throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource(profilePath));
        stageRef.setTitle("Perfil");
        if (stageRef.getScene() == null) {
            stageRef.setScene(new Scene(root, 1280, 840));
        } else {
            stageRef.getScene().setRoot(root);
        }
        stageRef.setWidth(1280);
        stageRef.setHeight(840);
        if (!stageRef.isShowing()) stageRef.show();
    }

    public void switchToRunTracker() throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource(runTrackerPath));
        stageRef.setTitle("Run Tracker");
        if (stageRef.getScene() == null) {
            stageRef.setScene(new Scene(root, 1280, 840));
        } else {
            stageRef.getScene().setRoot(root);
        }
        stageRef.setWidth(1280);
        stageRef.setHeight(840);
        if (!stageRef.isShowing()) stageRef.show();
    }

    // =========================================================
    //  SECCION DE USUARIO
    // =========================================================

    private User usuario;

    public void setUser(User user){
        usuario = user;
    }

    public User getUser(){
        return usuario;
    }

}
