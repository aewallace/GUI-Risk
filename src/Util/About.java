package Util;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicBoolean;

import LogPlayer.LogPlayer;
import Master.FXUIGameMaster;
import Player.FXUIPlayer;
import Util.WindowResizeHandler;
import Util.FXUIAudioAC;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.Glow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Handles the "about" and "more" dialog windows used in 
 * the {@link FXUIGameMaster} class.
 * ...May provide more functionality as time goes on.
 * 02 May 2016, 21:02
 *
 * @author A.E. Wallace
 * 
*/
public class About {

    private static boolean firstLaunch = true;
    private static final Color globalTextColor = Color.WHITE;

    public About() {
    }

    /**
     * Show friendly About info.
     * @param owner The Window of the Stage calling this method
     * @param autoExit whether this window should automatically close after
     * a set amount of time (around 5 seconds)
     * @param flatten whether should display in its own window (false) or return
     * a VBox for display elsewhere (true)
     */
    public VBox showFriendlyInfo(Window owner, boolean autoExit, Boolean flatten) {
        final Stage dialog = new Stage();
        
        dialog.setTitle("Hi, friend. :D");
        if(owner != null && !flatten){
	        dialog.initOwner(owner);
			//dialog.initStyle(StageStyle.UTILITY);
	        //dialog.initModality(Modality.WINDOW_MODAL);
	        dialog.setX(owner.getX());
	        dialog.setY(owner.getY() + 100);
        }

        final Text info1 = new Text();
        info1.setText(":::\n\nRISK!\n");
        info1.setTextAlignment(TextAlignment.CENTER);
        info1.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        info1.setFill(About.globalTextColor);
        info1.setEffect(new Glow(0.7));
        final Text info2 = new Text();
        info2.setText("or\nconquest of\nthe modern Mercator"
        		+ "\n\nJava + JavaFX\n\nWallace, Denney\n\n2015, 2016\n\n:D\n\n:::::::");
        info2.setTextAlignment(TextAlignment.CENTER);
        info2.setFont(Font.font("Arial", FontWeight.THIN, 14));
        info2.setFill(About.globalTextColor);
        info2.setStrokeWidth(0.2);
        if (About.firstLaunch) {
            dialog.setTitle("about(basic)");
            info1.setText("\\(^.^\")/\n\nRISK!\n");
            info2.setText("an open source way to\nSTEAL THE WORLD\nor something like that"
            		+ "\n\nJava + JavaFX\n\nWallace, Denney\n\n2015, 2016\n\n<3\n\n:::::::");
        }

        final Hyperlink hlink = new Hyperlink(":::");
        hlink.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                try {
                    Desktop.getDesktop().browse(new URI("http://xkcd.com/977/"));
                } catch (IOException e1) {
                    e1.printStackTrace();
                } catch (URISyntaxException e1) {
                    e1.printStackTrace();
                }
            }
        });

        final Button closeButton = new Button("OK");
        closeButton.setDefaultButton(true);
        closeButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                dialog.close();
            }
        });
        if(flatten){
        	closeButton.setVisible(false);
        }

        final VBox layout = new VBox(10);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(45, 15, 45, 15));
        layout.getChildren().setAll(
        		/*iconIn,*/
                info1, hlink, info2,
                closeButton
        );
        final AtomicBoolean delayAutoClose = new AtomicBoolean(false);
        layout.setOnMouseEntered(new EventHandler<MouseEvent>(){
			@Override
			public void handle(MouseEvent event) {
				// TODO Auto-generated method stub
				delayAutoClose.set(true);
			}
        	
        });
        
        ScrollPane internalScrollPane = new ScrollPane(layout);

        if(!flatten){
        	layout.setBackground(new Background(new BackgroundFill(Color.CORNFLOWERBLUE, null, null)));
	        dialog.setScene(new Scene(internalScrollPane));
	        dialog.show();
	        About.firstLaunch = false;
	        if (autoExit) {
	            FXUIGameMaster.autoCloseDialogs(dialog, null, delayAutoClose);
	        }
        }
        else{
        	return layout;
        }
        return null;
    }

    /**
     * Show advanced version information (more than the friendly about screen)
     * @param owner The Window of the Stage calling this method
     * @param flatten "true" returns a VBox for display elsewhere, "false" puts
     * content in its own window
     */
    public VBox showAdvancedVerInfo(Window owner, Boolean flatten) {
        final Stage dialog = new Stage();
        
        if(!flatten){
	        dialog.setTitle("more.");
	        dialog.initOwner(owner);
			//dialog.initStyle(StageStyle.UTILITY);
	        //dialog.initModality(Modality.WINDOW_MODAL);
	        dialog.setX(owner.getX());
	        dialog.setY(owner.getY() + FXUIGameMaster.DEFAULT_DIALOG_OFFSET);
        }

        final Hyperlink hlinkD = new Hyperlink("denney");
        hlinkD.setFont(Font.font("Arial", FontWeight.NORMAL, 16));
        hlinkD.setTextFill(About.globalTextColor);
        hlinkD.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                try {
                    Desktop.getDesktop().browse(new URI("http://github.com/sethau"));
                } catch (IOException e1) {
                    e1.printStackTrace();
                } catch (URISyntaxException e1) {
                    e1.printStackTrace();
                }
            }
        });

        final Hyperlink hlinkW = new Hyperlink("wallace");
        hlinkW.setFont(Font.font("Arial", FontWeight.NORMAL, 16));
        hlinkW.setTextFill(About.globalTextColor);
        hlinkW.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                try {
                    Desktop.getDesktop().browse(new URI("http://github.com/aewallace"));
                } catch (IOException e1) {
                    e1.printStackTrace();
                } catch (URISyntaxException e1) {
                    e1.printStackTrace();
                }
            }
        });

        final Text bridge2 = new Text("\n\n:::::::\n2015\n:::::::\n\n");
        bridge2.setTextAlignment(TextAlignment.CENTER);
        bridge2.setFont(Font.font("Arial", FontWeight.THIN, 16));
        bridge2.setFill(About.globalTextColor);

        final Text deepVersionInfo = new Text(FXUIGameMaster.VERSION_INFO
                + "\n\n");
        deepVersionInfo.setTextAlignment(TextAlignment.CENTER);
        deepVersionInfo.setFont(Font.font("Arial", FontWeight.THIN, 12));
        deepVersionInfo.setFill(About.globalTextColor);

        final Text subVersionInfo = new Text("\n" + FXUIPlayer.versionInfo
                + "\n\n" + LogPlayer.versionInfo + "\n\n" 
				+ WindowResizeHandler.versionInfo + "\n\n"
				+ FXUIAudioAC.shortVersion + "\n" + FXUIAudioAC.audioFileOrigSrc + "\n\n");
        subVersionInfo.setTextAlignment(TextAlignment.CENTER);
        subVersionInfo.setFont(Font.font("Arial", FontWeight.THIN, 11));
        subVersionInfo.setFill(About.globalTextColor);

        final Button closeButton = new Button("OK");
        closeButton.setDefaultButton(true);
        closeButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                dialog.close();
            }
        });
        if(flatten){
        	closeButton.setVisible(false);
        }
        //textField.setMinHeight(TextField.USE_PREF_SIZE);

        final VBox layout = new VBox(10);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(30, 10, 30, 10));
        layout.getChildren().setAll(
                deepVersionInfo,
                subVersionInfo,
                hlinkD, bridge2, hlinkW,
                closeButton
        );

        if(!flatten){
        	layout.setBackground(new Background(new BackgroundFill(Color.CORNFLOWERBLUE, null, null)));
	        dialog.setScene(new Scene(layout));
	        dialog.show();
        }
        else{
        	return layout;
        }
        return null;
    }
}
