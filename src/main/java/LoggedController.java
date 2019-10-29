import com.jfoenix.controls.JFXTextArea;
import com.jfoenix.controls.JFXTextField;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class LoggedController implements Initializable {

    private ConnectServer conn;
    private  Main main;

    @FXML
    private JFXTextField textMessage;

    @FXML
    private TextArea textArea;


    @FXML
    private Label userNameTop;

    @FXML
    private Button btnLogout;

    private boolean run;

    @Override
    public void initialize(URL arg0, ResourceBundle arg1) {
        listenFromServer();


    }

    private void listenFromServer() {
        new Thread(() -> {
            userNameTop.setText(conn.getUserName());

            String msg;
            while (true) {
                try {
                    msg = (String) conn.getInput().readObject();
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("disconnected from the server "+e.toString());
                    break;
                }
                appendMsg(msg);
            }
        }).start();

    }

    @FXML
    public void handleSend() {
        conn.sendMessage(new ChatMessage(ChatMessage.MESSAGE, textMessage.getText()));
        textMessage.clear();
    }

    @FXML
    public void handleWhoIs() {
        conn.sendMessage(new ChatMessage(ChatMessage.WHOISIN, "")); }

    public void handleLogout() throws IOException {
        conn.sendMessage(new ChatMessage(ChatMessage.LOGOUT, "Disconnected "));
        Stage stage = (Stage) btnLogout.getScene().getWindow();
        stage.close();
    }

    public void setConnection(ConnectServer conn) {
        this.conn = conn;
    }

    public void appendMsg(String string) {
        Platform.runLater(() -> textArea.appendText(string));
    }

}
