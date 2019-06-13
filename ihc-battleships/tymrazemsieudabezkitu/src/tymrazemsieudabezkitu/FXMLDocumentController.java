/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tymrazemsieudabezkitu;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.stage.PopupWindow;

import static jdk.nashorn.internal.objects.NativeArray.map;

/**
 *
 * @author Admin
 */
public class FXMLDocumentController implements Initializable {

    private String ship_size;
    private int ship_size_number;
    private int mousex;
    private int mousey;
    private int mouse_enemy_x;
    private int mouse_enemy_y;
    private boolean ships_on = false;
    private boolean ship_can_be_placed_verticaly = false;
    private boolean ship_can_be_placed_horizontaly = false;
    private boolean ship_collision_verticaly = false;
    private boolean ship_collision_horizontaly = false;
    private int map[][];
    private int enemy_map[][];
    private String enemy_map_string;
    private Pane shippane;
    private Pane hitshippane;
    private Pane misspane;
    private JSch jsch;
    private Session session;
    private ChannelSftp sftpChannel;
    private int player_number;
    private Scene current_scene;
    private boolean enemy_is_ready = false;
    private String turn;
    private int player_win = 0;
    
    
    @FXML
    private Button readybutton;

    @FXML
    private Label label;

    @FXML
    private AnchorPane mainanchor;

    @FXML
    private GridPane mapagrid;

    @FXML
    private GridPane mapagridunder;

    @FXML
    private AnchorPane my_map_anchor;

    @FXML
    private AnchorPane enemy_anchor;

    @FXML
    private GridPane mapaenemyover;

    @FXML
    private GridPane mapaenemy;

    @FXML
    private GridPane mapaenemyunder;

    @FXML
    private ChoiceBox shipchose;

    @FXML
    private Label collision;

    @FXML
    private Label horizontaly;

    @FXML
    private Label verticaly;

    @FXML
    private Button appclose;

    @FXML
    private Label player2waitlabbel;

    @FXML
    private Label playerwinlabel;
    
    @FXML
    private void button_close_app(ActionEvent event) throws SftpException {
        trycatch_disconnect_from_server();
        Platform.exit();
    }

    @FXML
    private void ready_button_click_handler(ActionEvent event) throws SftpException {
        shipsset();
        if (ships_on) {
            label.setText("We can start the game.");
            trycatch_put_map_on_server();
            mainanchor.getChildren().remove(readybutton);
            trycatch_check_if_second_player_is_ready(sftpChannel);
            my_map_anchor.getChildren().remove(mapagrid);
            enemy_anchor.getChildren().remove(mapaenemyover);
            mainanchor.getChildren().remove(shipchose);
            mainanchor.getChildren().remove(horizontaly);
            mainanchor.getChildren().remove(verticaly);
            mainanchor.getChildren().remove(collision);
            set_turn_number(sftpChannel);
        } else {
            label.setText("Place all of your ships first.");
        }
        showmap(map);
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        mapgenerate();
        jsch = new JSch();
        connect_to_server();
        turn = "start";
        trycatch_set_player_number();
        setChoseBox();
        mousshipplace();

    }

    private void set_turn_number(ChannelSftp chsftp) throws SftpException {
        String currentDirectory = chsftp.pwd();
        String turnbytes = "player1";
        if (turn.equals("start")) {
            turn = "player1";
            InputStream stream = new ByteArrayInputStream(turnbytes.getBytes());
            sftpChannel.put(stream, currentDirectory + "/turn.txt");
        }

        update_my_map(chsftp);
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                if (map[i][j] == 2) {
                    hitshippane = new Pane();
                    hitshippane.setStyle("-fx-background-color: #FF0000");
                    mapagridunder.add(hitshippane, j, i);
                }
                if (map[i][j] == 3) {
                    misspane = new Pane();
                    misspane.setStyle("-fx-background-color: #0000FF");
                    mapagridunder.add(misspane, j, i);
                }
            }
        }

        OutputStream turnstream = new ByteArrayOutputStream();
        sftpChannel.get(currentDirectory + "/turn.txt", turnstream);
        turn = turnstream.toString();

 
        try {
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException sleepex) {
            System.out.println("Delay error");
        }

        mapaenemy.setOnMousePressed(new EventHandler<javafx.scene.input.MouseEvent>() {
            @Override
            public void handle(javafx.scene.input.MouseEvent event) {

                // pobierz mape a na dole zupdatuj
                if (turn.equals("player1") && player_number == 1) {
                    if (enemy_map[mouse_enemy_x][mouse_enemy_y] == 1 && event.getButton() == MouseButton.PRIMARY) {
                        hitshippane = new Pane();
                        hitshippane.setStyle("-fx-background-color: #FF0000");
                        mapaenemyunder.add(hitshippane, mouse_enemy_y, mouse_enemy_x);
                        enemy_map[mouse_enemy_x][mouse_enemy_y] = 2;                        
                        check_if_win();
                        System.out.println(player_win);
                        if(player_win!=0)
                        {
                            player_number = 0;
                            try {
                            set_turn_number(chsftp);
                        } catch (SftpException ex) {
                            Logger.getLogger(FXMLDocumentController.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        }
                        try_catch_update_enemy_map(chsftp);
                        System.out.println("You shoot enemy ship!");
                    } else if (enemy_map[mouse_enemy_x][mouse_enemy_y] == 0 && event.getButton() == MouseButton.PRIMARY) {
                        misspane = new Pane();
                        misspane.setStyle("-fx-background-color: #0000FF");
                        mapaenemyunder.add(misspane, mouse_enemy_y, mouse_enemy_x);
                        enemy_map[mouse_enemy_x][mouse_enemy_y] = 3;
                        try_catch_update_enemy_map(chsftp);
                        System.out.println("You missed");
                        String player = "player2";
                        InputStream stream = new ByteArrayInputStream(player.getBytes());
                        try {
                            sftpChannel.put(stream, currentDirectory + "/turn.txt");
                        } catch (SftpException ex) {
                            Logger.getLogger(FXMLDocumentController.class.getName()).log(Level.SEVERE, null, ex);
                        }

                        try {
                            set_turn_number(chsftp);
                        } catch (SftpException ex) {
                            Logger.getLogger(FXMLDocumentController.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    } else if (enemy_map[mouse_enemy_x][mouse_enemy_y] == 2 || enemy_map[mouse_enemy_x][mouse_enemy_y] == 3 && event.getButton() == MouseButton.PRIMARY) {
                        System.out.println("You already shot here");
                    }
                }

                if (turn.equals("player2") && player_number == 2) {
                    if (enemy_map[mouse_enemy_x][mouse_enemy_y] == 1 && event.getButton() == MouseButton.PRIMARY) {
                        hitshippane = new Pane();
                        hitshippane.setStyle("-fx-background-color: #FF0000");
                        mapaenemyunder.add(hitshippane, mouse_enemy_y, mouse_enemy_x);
                        enemy_map[mouse_enemy_x][mouse_enemy_y] = 2;
                        check_if_win();
                        System.out.println(player_win);
                        if(player_win!=0)
                        {
                            player_number = 0;
                            try {
                            set_turn_number(chsftp);
                        } catch (SftpException ex) {
                            Logger.getLogger(FXMLDocumentController.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        }
                        System.out.println("You shoot enemy ship!");
                    } else if (enemy_map[mouse_enemy_x][mouse_enemy_y] == 0 && event.getButton() == MouseButton.PRIMARY) {
                        misspane = new Pane();
                        misspane.setStyle("-fx-background-color: #0000FF");
                        mapaenemyunder.add(misspane, mouse_enemy_y, mouse_enemy_x);
                        enemy_map[mouse_enemy_x][mouse_enemy_y] = 3;
                        try_catch_update_enemy_map(chsftp);
                        System.out.println("You missed");                        
                        String player = "player1";
                        InputStream stream = new ByteArrayInputStream(player.getBytes());
                        try {
                            sftpChannel.put(stream, currentDirectory + "/turn.txt");
                        } catch (SftpException ex) {
                            Logger.getLogger(FXMLDocumentController.class.getName()).log(Level.SEVERE, null, ex);
                        }

                        try {
                            set_turn_number(chsftp);
                        } catch (SftpException ex) {
                            Logger.getLogger(FXMLDocumentController.class.getName()).log(Level.SEVERE, null, ex);
                        }

                    } else if (enemy_map[mouse_enemy_x][mouse_enemy_y] == 2 || enemy_map[mouse_enemy_x][mouse_enemy_y] == 3) {
                        System.out.println("You already shot here");
                    }
                }
            }
        });

        
        
        if(player_win!=0)
        {
            playerwinlabel.setText("Player "+Integer.toString(player_win)+" wins");
        }
        else{
        showmap(map);
        if ((turn.equals("player1") && player_number == 2) || (turn.equals("player2") && player_number == 1)) {
            try {
                set_turn_number(chsftp);
            } catch (SftpException ex) {
                Logger.getLogger(FXMLDocumentController.class.getName()).log(Level.SEVERE, null, ex);
            }

            // zamiana numeru tury
        }
    }

    }
    private void check_if_win()
    {
        int one_counter = 0;
        for(int i=0; i<10;i++)
        {
            for(int j=0;j<10;j++)
            {
                if(enemy_map[i][j] == 1)
                {
                    one_counter = one_counter + 1;
                }
            }
        }
        
        if(one_counter == 0)
        {
            if(player_number==1)
            {
                player_win = player_number;
            }
            if(player_number==2)
            {
                player_win = player_number;
            }
        }        
    }
    
    private void trycatch_check_if_second_player_is_ready(ChannelSftp chsftp) throws SftpException {
        try {
            check_if_second_player_is_ready(chsftp);
        } catch (SftpException ex) {
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException sleepex) {
                System.out.println("Delay error");
            }
            trycatch_check_if_second_player_is_ready(chsftp);
        }
    }

    private void check_if_second_player_is_ready(ChannelSftp chsftp) throws SftpException {
        String currentDirectory = chsftp.pwd();

        if (player_number == 1) {
            SftpATTRS attrs_player2_map = null;
            attrs_player2_map = chsftp.stat(currentDirectory + "/player_2_map.txt");
            if (attrs_player2_map == null) {
                check_if_second_player_is_ready(chsftp);
            }
            if (attrs_player2_map != null) {
                enemy_is_ready = true;
                get_map_from_server(chsftp);
            }
        }

        if (player_number == 2) {
            SftpATTRS attrs_player2_map = null;
            attrs_player2_map = chsftp.stat(currentDirectory + "/player_1_map.txt");
            if (attrs_player2_map == null) {
                check_if_second_player_is_ready(chsftp);
            }
            if (attrs_player2_map != null) {
                enemy_is_ready = true;
                get_map_from_server(chsftp);
            }
        }
        if (enemy_is_ready == true) {

        }
    }

    private void trycatch_disconnect_from_server() throws SftpException {
        try {
            disconnect_from_server(session, sftpChannel);
        } catch (SftpException ex) {
            Logger.getLogger(FXMLDocumentController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void trycatch_set_player_number() {
        try {
            set_player_number(sftpChannel);
        } catch (SftpException ex) {
            Logger.getLogger(FXMLDocumentController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void trycatch_put_map_on_server() {
        try {
            put_map_on_server(sftpChannel);
            //check_if_second_player_is_ready();
            // zrobic tak ze jak macham myszka po kliknieciu guziku to sprawdza czy plik jest czy nie
        } catch (SftpException ex) {
            Logger.getLogger(FXMLDocumentController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void set_player_number(ChannelSftp chsftp) throws SftpException {

        String currentDirectory = chsftp.pwd();
        String dir = "player1.txt";
        String dir2 = "player2.txt";
        SftpATTRS attrs = null;
        SftpATTRS attrs2 = null;
        chsftp.put("turn.txt");
        try {
            attrs = chsftp.stat(currentDirectory + "/" + dir);
            attrs2 = chsftp.stat(currentDirectory + "/" + dir2);

        } catch (SftpException e) {
        }

        if (player_number != 1 && player_number != 2 && attrs2 != null && attrs == null) {
            System.out.println("checking if it works for 1020123192 time :<");
        }

        if (attrs != null && attrs2 != null) {

            chsftp.rm(dir);
            chsftp.rm(dir2);
            player_number = 1;
            chsftp.put(dir);
            //chsftp.put("player_1_map.txt");
        }

        if (attrs == null && attrs2 == null) {
            player_number = 1;
            chsftp.put(dir);
            // chsftp.put("player_1_map.txt");
        } else if (attrs != null && attrs2 == null) {
            player_number = 2;
            chsftp.put(dir2);
            // chsftp.put("player_2_map.txt");
        }

    }

    private void update_my_map(ChannelSftp chsftp) throws SftpException {
        String currentDirectory = chsftp.pwd();
        if (player_number == 1) {
            OutputStream stream = new ByteArrayOutputStream();
            sftpChannel.get(currentDirectory + "/player_1_map.txt", stream);
            String map_string = stream.toString();
            String map_without_nextlines = "";

            int nextline_counter = 0;
            char tempchar;

            for (int i = 0; i < map_string.length(); i++) {
                if (map_string.charAt(i) != '\n') {
                    map_without_nextlines += map_string.charAt(i);
                }
            }
            System.out.println(map_without_nextlines.length());

            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    tempchar = map_without_nextlines.charAt(nextline_counter);
                    map[i][j] = Character.getNumericValue(tempchar);
                    nextline_counter = nextline_counter + 1;
                }
            }
        }

        if (player_number == 2) {
            OutputStream stream = new ByteArrayOutputStream();
            sftpChannel.get(currentDirectory + "/player_2_map.txt", stream);
            String map_string = stream.toString();
            String map_without_nextlines = "";

            int nextline_counter = 0;
            char tempchar;

            for (int i = 0; i < map_string.length(); i++) {
                if (map_string.charAt(i) != '\n') {
                    map_without_nextlines += map_string.charAt(i);
                }
            }
            System.out.println(map_without_nextlines.length());

            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    tempchar = map_without_nextlines.charAt(nextline_counter);
                    map[i][j] = Character.getNumericValue(tempchar);
                    nextline_counter = nextline_counter + 1;
                }
            }
        }

    }

    private void get_map_from_server(ChannelSftp chsftp) throws SftpException {
        String currentDirectory = chsftp.pwd();
        if (player_number == 1) {
            OutputStream stream = new ByteArrayOutputStream();
            sftpChannel.get(currentDirectory + "/player_2_map.txt", stream);
            String enemy_map_string = stream.toString();
            String enemy_map_without_nextlines = "";

            int nextline_counter = 0;
            char tempchar;

            for (int i = 0; i < enemy_map_string.length(); i++) {
                if (enemy_map_string.charAt(i) != '\n') {
                    enemy_map_without_nextlines += enemy_map_string.charAt(i);
                }
            }
            System.out.println(enemy_map_without_nextlines.length());

            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    tempchar = enemy_map_without_nextlines.charAt(nextline_counter);
                    enemy_map[i][j] = Character.getNumericValue(tempchar);
                    nextline_counter = nextline_counter + 1;
                }
            }
        }

        if (player_number == 2) {
            OutputStream stream = new ByteArrayOutputStream();
            sftpChannel.get(currentDirectory + "/player_1_map.txt", stream);
            String enemy_map_string = stream.toString();
            String enemy_map_without_nextlines = "";

            int nextline_counter = 0;
            char tempchar;

            for (int i = 0; i < enemy_map_string.length(); i++) {
                if (enemy_map_string.charAt(i) != '\n') {
                    enemy_map_without_nextlines += enemy_map_string.charAt(i);
                }
            }
            System.out.println(enemy_map_without_nextlines.length());

            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    tempchar = enemy_map_without_nextlines.charAt(nextline_counter);
                    enemy_map[i][j] = Character.getNumericValue(tempchar);
                    nextline_counter = nextline_counter + 1;
                }
            }

        }

    }

    private void try_catch_update_enemy_map(ChannelSftp chsftp) {
        try {
            update_enemy_map(sftpChannel);
        } catch (SftpException ex) {
            Logger.getLogger(FXMLDocumentController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void update_enemy_map(ChannelSftp chsftp) throws SftpException {
        String currentDirectory = chsftp.pwd();
        if (player_number == 1) {
            int rowcoutner = 0;
            String content = new String();
            for (int i = 0; i < 10; i++) {
                if (rowcoutner != i) {
                    content = content + '\n';
                    rowcoutner++;
                }
                for (int j = 0; j < 10; j++) {
                    content = content + enemy_map[i][j];
                }
            }

            InputStream stream = new ByteArrayInputStream(content.getBytes());
            sftpChannel.put(stream, currentDirectory + "/player_2_map.txt");

        }
        if (player_number == 2) {
            int rowcoutner = 0;
            String content = new String();
            for (int i = 0; i < 10; i++) {
                if (rowcoutner != i) {
                    content = content + '\n';
                    rowcoutner++;
                }
                for (int j = 0; j < 10; j++) {
                    content = content + enemy_map[i][j];
                }
            }

            InputStream stream = new ByteArrayInputStream(content.getBytes());
            sftpChannel.put(stream, currentDirectory + "/player_1_map.txt");

        }
    }

    private void put_map_on_server(ChannelSftp chsftp) throws SftpException {
        String currentDirectory = chsftp.pwd();
        if (player_number == 1) {
            int rowcoutner = 0;
            String content = new String();
            for (int i = 0; i < 10; i++) {
                if (rowcoutner != i) {
                    content = content + '\n';
                    rowcoutner++;
                }
                for (int j = 0; j < 10; j++) {
                    content = content + map[i][j];
                }
            }

            InputStream stream = new ByteArrayInputStream(content.getBytes());
            sftpChannel.put(stream, currentDirectory + "/player_1_map.txt");
            readybutton.disableProperty();
        }
        if (player_number == 2) {
            int rowcoutner = 0;
            String content = new String();
            for (int i = 0; i < 10; i++) {
                if (rowcoutner != i) {
                    content = content + '\n';
                    rowcoutner++;
                }
                for (int j = 0; j < 10; j++) {
                    content = content + map[i][j];
                }
            }

            InputStream stream = new ByteArrayInputStream(content.getBytes());
            sftpChannel.put(stream, currentDirectory + "/player_2_map.txt");
            readybutton.disableProperty();
        }
    }

    private void mapgenerate() {
        map = new int[10][10];
        enemy_map = new int[10][10];
        int numCols = 10;
        int numRows = 10;

        for (int i = 0; i < numCols; i++) {
            for (int j = 0; j < numRows; j++) {
                addPane(i, j);
                addPane_on_enemy_grid(i, j);
            }
        }
    }

    private void mousshipplace() {
        mapagrid.setOnMousePressed(new EventHandler<javafx.scene.input.MouseEvent>() {
            @Override
            public void handle(javafx.scene.input.MouseEvent event) {

                if (ship_can_be_placed_verticaly == true && ship_collision_verticaly == false) {
                    if (ship_size_number == 5) {
                        if (event.getButton() == MouseButton.PRIMARY) {
                            System.out.println("SHIP PLACED on: " + mousex + " " + mousey);
                            shipchose.getItems().remove("Ship size 5");
                            collision.setText("Yes");
                            addShipVerticaly(mousey, mousex, 5);
                            ship_size_number = 0;
                        }

                    }
                    if (ship_size_number == 4) {
                        if (event.getButton() == MouseButton.PRIMARY) {
                            System.out.println("SHIP PLACED on: " + mousex + " " + mousey);
                            shipchose.getItems().remove("Ship size 4");
                            addShipVerticaly(mousey, mousex, 4);
                            ship_size_number = 0;
                        }
                    }
                    if (ship_size_number == 3) {
                        if (event.getButton() == MouseButton.PRIMARY) {
                            System.out.println("SHIP PLACED on: " + mousex + " " + mousey);
                            shipchose.getItems().remove("Ship size 3");
                            addShipVerticaly(mousey, mousex, 3);
                            ship_size_number = 0;
                        }

                    }
                    if (ship_size_number == 2) {
                        if (event.getButton() == MouseButton.PRIMARY) {
                            System.out.println("SHIP PLACED on: " + mousex + " " + mousey);
                            shipchose.getItems().remove("Ship size 2");
                            addShipVerticaly(mousey, mousex, 2);
                            ship_size_number = 0;
                        }

                    }
                    if (ship_size_number == 1) {
                        if (event.getButton() == MouseButton.PRIMARY) {
                            System.out.println("SHIP PLACED on: " + mousex + " " + mousey);
                            shipchose.getItems().remove("Ship size 1");
                            addShipVerticaly(mousey, mousex, 1);
                            ship_size_number = 0;
                        }

                    }
                }

                if (ship_can_be_placed_horizontaly == true && ship_collision_horizontaly == false) {
                    if (ship_size_number == 5) {
                        if (event.getButton() == MouseButton.SECONDARY) {
                            System.out.println("SHIP PLACED on: " + mousex + " " + mousey);
                            shipchose.getItems().remove("Ship size 5");
                            collision.setText("Yes");
                            addShipHorizontary(mousey, mousex, 5);
                            ship_size_number = 0;
                        }

                    }
                    if (ship_size_number == 4) {
                        if (event.getButton() == MouseButton.SECONDARY) {
                            System.out.println("SHIP PLACED on: " + mousex + " " + mousey);
                            shipchose.getItems().remove("Ship size 4");
                            addShipHorizontary(mousey, mousex, 4);
                            ship_size_number = 0;
                        }

                    }
                    if (ship_size_number == 3) {
                        if (event.getButton() == MouseButton.SECONDARY) {
                            System.out.println("SHIP PLACED on: " + mousex + " " + mousey);
                            shipchose.getItems().remove("Ship size 3");
                            addShipHorizontary(mousey, mousex, 3);
                            ship_size_number = 0;
                        }

                    }
                    if (ship_size_number == 2) {
                        if (event.getButton() == MouseButton.SECONDARY) {
                            System.out.println("SHIP PLACED on: " + mousex + " " + mousey);
                            shipchose.getItems().remove("Ship size 2");
                            addShipHorizontary(mousey, mousex, 2);
                            ship_size_number = 0;
                        }

                    }
                    if (ship_size_number == 1) {
                        if (event.getButton() == MouseButton.SECONDARY) {
                            System.out.println("SHIP PLACED on: " + mousex + " " + mousey);
                            shipchose.getItems().remove("Ship size 1");
                            addShipHorizontary(mousey, mousex, 1);
                            ship_size_number = 0;
                        }
                    }
                }
            }
        });
    }

    private void setChoseBox() {
        shipchose.setTooltip(new Tooltip("Select Ship Size"));
        shipchose.getItems().add("Ship size 5");
        shipchose.getItems().add("Ship size 4");
        shipchose.getItems().add("Ship size 3");
        shipchose.getItems().add("Ship size 2");
        shipchose.getItems().add("Ship size 1");

        shipchose.getSelectionModel().selectedItemProperty().addListener((v, oldValue, newValue) -> {
            try {
                ship_size = newValue.toString();

                switch (ship_size) {
                    case ("Ship size 5"):
                        ship_size_number = 5;
                        break;
                    case ("Ship size 4"):
                        ship_size_number = 4;
                        break;
                    case ("Ship size 3"):
                        ship_size_number = 3;
                        break;
                    case ("Ship size 2"):
                        ship_size_number = 2;
                        break;
                    case ("Ship size 1"):
                        ship_size_number = 1;
                        break;
                }
                System.out.println(ship_size_number);
            } catch (NullPointerException x) {
                System.out.println("no ships left");
            }
        });
    }

    private void showmap(int map[][]) {
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                System.out.print(map[i][j] + " ");
            }
            System.out.println("");
        }
    }

    private void connect_to_server() {
        try {
            session = jsch.getSession("a42677", "unix.ubi.pt", 22);
            session.setPassword("haslo123");
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect();
            sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect();
            sftpChannel.cd("/users/engenharia/eninf/a42677/public_html/ihcgame");
            player_number = 0;

        } catch (JSchException ex) {
            Logger.getLogger(FXMLDocumentController.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SftpException ex) {
            Logger.getLogger(FXMLDocumentController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void disconnect_from_server(Session s, ChannelSftp chsftp) throws SftpException {
        if (player_number == 1) {
            chsftp.rm("player1.txt");

            try {
                chsftp.rm("player_1_map.txt");
            } catch (SftpException x) {
                System.out.println("player left before placing ships");
            }
        }
        if (player_number == 2) {
            chsftp.rm("player2.txt");

            try {
                chsftp.rm("player_2_map.txt");
            } catch (SftpException x) {
                System.out.println("player left before placing ships");
            }
        }

        chsftp.rm("turn.txt");
        chsftp.disconnect();
        s.disconnect();
    }

    private void shipsset() {
        int check_map = 0;
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                check_map = check_map + map[i][j];
            }
        }
        if (check_map == 15) {
            ships_on = true;
            System.out.printf("all ships set captain :D");
        }
    }

    private void addShipVerticaly(int colIndex, int rowIndex, int shipsize) {
        for (int i = 0; i < shipsize; i++) {
            shippane = new Pane();
            shippane.setStyle("-fx-background-color: #00FF00");
            mapagridunder.add(shippane, colIndex, rowIndex - i);
            map[rowIndex - i][colIndex] = 1;
        }

    }

    private void addShipHorizontary(int colIndex, int rowIndex, int shipsize) {

        for (int i = 0; i < shipsize; i++) {
            shippane = new Pane();
            shippane.setStyle("-fx-background-color: #00FF00");
            mapagridunder.add(shippane, colIndex - i, rowIndex);
            map[rowIndex][colIndex - i] = 1;
        }
    }

    private void collisioncheckverticaly(int colIndex, int rowIndex, int shipsize) {
        int counter = 0;
        if (rowIndex - shipsize >= 0) {
            for (int i = 0; i < shipsize; i++) {

                if (map[rowIndex - i][colIndex] == 1) {
                    counter = counter + 1;
                }

            }
            if (counter != 0) {
                ship_collision_verticaly = true;
            }
            if (counter == 0) {
                ship_collision_verticaly = false;
            }
        }
    }

    private void collisioncheckhorizontaly(int colIndex, int rowIndex, int shipsize) {
        int counter = 0;
        if (colIndex - shipsize >= 0) {
            for (int i = 0; i < shipsize; i++) {
                if (map[rowIndex][colIndex - i] == 1) {
                    counter = counter + 1;
                }
            }
            if (counter == 0) {
                ship_collision_horizontaly = false;
            }
            if (counter > 0) {
                ship_collision_horizontaly = true;
            }

        }
    }

    private void collisioninfo() {
        if (ship_collision_horizontaly == true || ship_collision_verticaly == true) {
            collision.setText("Collision: Yes");
        } else {
            collision.setText("Collision: No");
        }
    }

    private void addPane(int colIndex, int rowIndex) {
        int currentcolindex;
        int currentrowindex;
        Pane pane = new Pane();

        pane.setOnMouseEntered(e -> {
            //System.out.printf("Mouse enetered cell [%d, %d]%n", colIndex, rowIndex);
            System.out.println("Map = " + map[rowIndex][colIndex]);
            // System.out.println(ship_size_number);
            //pane.setStyle("-fx-background-color: #F0FF33");
            if (ship_size_number > 0) {
                switch (ship_size_number) {
                    case (5):

                        if (rowIndex - 4 >= 0) {
                            verticaly.setText("Verticaly: Ok");
                            ship_can_be_placed_verticaly = true;
                            //  System.out.println("Can place ship verticaly here");
                            //SPRAWDZANIE KOLIZJI TU
                            collisioncheckverticaly(colIndex, rowIndex, 4);
                            collisioncheckhorizontaly(colIndex, rowIndex, 4);
                            collisioninfo();
                        } else {
                            verticaly.setText("Verticaly: Not Ok");
                            ship_can_be_placed_verticaly = false;
                            //  System.out.println("Can not place ship verticaly here");
                        }
                        if (colIndex - 4 >= 0) {
                            horizontaly.setText("Horizontaly: Ok");
                            ship_can_be_placed_horizontaly = true;
                            //   System.out.println("Can place ship horizontaly here");
                            collisioncheckverticaly(colIndex, rowIndex, 4);
                            collisioncheckhorizontaly(colIndex, rowIndex, 4);
                            collisioninfo();
                        } else {
                            horizontaly.setText("Horizontaly: Not Ok");
                            ship_can_be_placed_horizontaly = false;
                            //    System.out.println("Can not place ship horizontaly here");
                        }
                        break;
                    case (4):
                        if (rowIndex - 3 >= 0) {
                            verticaly.setText("Verticaly: Ok");
                            ship_can_be_placed_verticaly = true;
                            //    System.out.println("Can place ship verticaly here");
                            collisioncheckverticaly(colIndex, rowIndex, 4);
                            collisioncheckhorizontaly(colIndex, rowIndex, 4);
                            collisioninfo();
                        } else {
                            verticaly.setText("Verticaly: Not Ok");
                            ship_can_be_placed_verticaly = false;
                            //    System.out.println("Can not place ship verticaly here");
                        }
                        if (colIndex - 3 >= 0) {
                            horizontaly.setText("Horizontaly: Ok");
                            ship_can_be_placed_horizontaly = true;
                            //    System.out.println("Can place ship horizontaly here");
                            collisioncheckverticaly(colIndex, rowIndex, 4);
                            collisioncheckhorizontaly(colIndex, rowIndex, 4);
                            collisioninfo();
                        } else {
                            horizontaly.setText("Horizontaly: Not Ok");
                            ship_can_be_placed_horizontaly = false;
                            //    System.out.println("Can not place ship horizontaly here");
                        }
                        break;
                    case (3):
                        if (rowIndex - 2 >= 0) {
                            verticaly.setText("Verticaly: Ok");
                            ship_can_be_placed_verticaly = true;
                            //    System.out.println("Can place ship verticaly here");
                            collisioncheckverticaly(colIndex, rowIndex, 3);
                            collisioncheckhorizontaly(colIndex, rowIndex, 3);
                            collisioninfo();
                        } else {
                            verticaly.setText("Verticaly: Not Ok");
                            ship_can_be_placed_verticaly = false;
                            //    System.out.println("Can not place ship verticaly here");
                        }
                        if (colIndex - 2 >= 0) {
                            horizontaly.setText("Horizontaly: Ok");
                            ship_can_be_placed_horizontaly = true;
                            //   System.out.println("Can place ship horizontaly here");
                            collisioncheckverticaly(colIndex, rowIndex, 3);
                            collisioncheckhorizontaly(colIndex, rowIndex, 3);
                            collisioninfo();
                        } else {
                            horizontaly.setText("Horizontaly: Not Ok");
                            ship_can_be_placed_horizontaly = false;
                            //    System.out.println("Can not place ship horizontaly here");
                        }
                        break;
                    case (2):
                        if (rowIndex - 1 >= 0) {
                            verticaly.setText("Verticaly: Ok");
                            ship_can_be_placed_verticaly = true;
                            //   System.out.println("Can place ship verticaly here");
                            collisioncheckverticaly(colIndex, rowIndex, 2);
                            collisioncheckhorizontaly(colIndex, rowIndex, 2);
                            collisioninfo();
                        } else {
                            verticaly.setText("Verticaly: Not Ok");
                            ship_can_be_placed_verticaly = false;
                            //    System.out.println("Can not place ship verticaly here");
                        }
                        if (colIndex - 1 >= 0) {
                            horizontaly.setText("Horizontaly: Ok");
                            ship_can_be_placed_horizontaly = true;
                            //    System.out.println("Can place ship horizontaly here");
                            collisioncheckverticaly(colIndex, rowIndex, 2);
                            collisioncheckhorizontaly(colIndex, rowIndex, 2);
                            collisioninfo();
                        } else {
                            horizontaly.setText("Horizontaly: Not Ok");
                            ship_can_be_placed_horizontaly = false;
                            //    System.out.println("Can not place ship horizontaly here");
                        }
                        break;
                    case (1):
                        if (rowIndex >= 0) {
                            verticaly.setText("Verticaly: Ok");
                            ship_can_be_placed_verticaly = true;
                            //    System.out.println("Can place ship verticaly here");
                            collisioncheckverticaly(colIndex, rowIndex, 1);
                            collisioncheckhorizontaly(colIndex, rowIndex, 1);
                            collisioninfo();
                        } else {
                            verticaly.setText("Verticaly: Not Ok");
                            ship_can_be_placed_verticaly = false;
                            //    System.out.println("Can not place ship verticaly here");
                        }
                        if (colIndex >= 0) {
                            horizontaly.setText("Horizontaly: Ok");
                            ship_can_be_placed_horizontaly = true;
                            //    System.out.println("Can place ship horizontaly here");
                            collisioncheckverticaly(colIndex, rowIndex, 1);
                            collisioncheckhorizontaly(colIndex, rowIndex, 1);
                            collisioninfo();
                        } else {
                            horizontaly.setText("Horizontaly: Not Ok");
                            ship_can_be_placed_horizontaly = false;
                            //   System.out.println("Can not place ship horizontaly here");
                        }
                        break;
                }
                mousex = rowIndex;
                mousey = colIndex;
            }
        });

        mapagrid.add(pane, colIndex, rowIndex);

    }

    private void addPane_on_enemy_grid(int colIndex, int rowIndex) {

        Pane pane = new Pane();

        pane.setOnMouseEntered(e -> {

            System.out.println("Map = " + enemy_map[rowIndex][colIndex]);

            mouse_enemy_x = rowIndex;
            mouse_enemy_y = colIndex;

        });

        mapaenemy.add(pane, colIndex, rowIndex);
    }

}
