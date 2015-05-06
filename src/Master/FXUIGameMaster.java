/*FXUI GameMaster Class
*Albert Wallace, 2015. Version info now found in class definition.
*for Seth Denney's RISK, JavaFX UI-capable version
*
*Base build from original GameMaster class implementation, by Seth Denney, Feb 20 2015 
*/


package Master;

import java.awt.Desktop;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Labeled;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Scale;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import LogPlayer.LogPlayer;
import Map.Country;
import Map.RiskMap;
import Player.FXUIPlayer;
import Player.EasyDefaultPlayer;
import Player.HardDefaultPlayer;
import Player.NormalDefaultPlayer;
import Player.Player;
import Player.PlayerFactory;
import Player.Seth;
import Response.AdvanceResponse;
import Response.AttackResponse;
import Response.CardTurnInResponse;
import Response.DefendResponse;
import Response.FortifyResponse;
import Response.ReinforcementResponse;
import Util.Card;
import Util.DiceRoller;
import Util.FXUI_Crossbar;
import Util.OSExitException;
import Util.PlayerEliminatedException;
import Util.RiskConstants;
import Util.RiskUtils;
import Util.RollOutcome;
import Util.SavePoint;
import Util.TextNodes;

/**
* Represents the primary game controller for a game of Risk.
* 	Asks for necessary responses and makes necessary decisions,
* 	acting as a trigger for any Player objects added.
* 
* Compatible with original CPU opponents (Player), as well as
* 	a human player through FXUI Player (FXUIPlayer) type.
* 
* 
* UI elements are JavaFX, done with Java JDK 8. (By extension, elements were done under JavaFX 8)
* Compatibility with JDK 7 / JRE1.7 was retroactively restored.
* (source files with "Stamp" -- aka date/time stamp -- of Feb 21 2015, 6:00 PM -- aka Y2015.M02.D21.HM1800 -- & later apply).
* JDK 7/JRE 1.7 will be the target until further notified.
*
*/
public class FXUIGameMaster extends Application {
	public static final String versionInfo = "FXUI-RISK-Master\nVersion 01x06h\nStamp 2015.05.06, 18:22\nStability:Unstable(00)";
	public static final String ERROR = "(ERROR!!)", INFO = "(info:)", WARN = "(warning-)"; 
	private static final String DEFAULT_CHKPNT_FILE_NAME = "fxuigm_save.ser";
	private static String loadfrom_filename = DEFAULT_CHKPNT_FILE_NAME;
	private static String saveto_filename = DEFAULT_CHKPNT_FILE_NAME;
	private static final long AUTO_CLOSE_TIMEOUT = 4500;
	private static final int DEFAULT_APP_WIDTH = 1600;
	private static final int DEFAULT_APP_HEIGHT = 1062;
	public static final int DEFAULT_DIALOG_OFFSET = 300;
	private static final int IDLE_MODE = 0, NEW_GAME_MODE = 1, LOADED_GAME_MODE = 2;
	private static int workingMode = IDLE_MODE;
	protected static final String LOGFILE = "LOG.txt";
	protected static final String STATSFILE = "STATS.txt";
	protected static final String EVENT_DELIM = "...";
	protected static final boolean LOGGING_OFF = false, LOGGING_ON = true;
	protected static boolean forceEnableLogging = false, forceLoggingIsIndeterminate = true;
	protected static boolean loggingEnabled = true; //this is the one that has the final say as to whether the log file is created
	private static FXUI_Crossbar crossbar = new FXUI_Crossbar();
	protected RiskMap map;
	protected Deque<Card> deck;
	protected static String desiredPlayersForGame = null;
	protected List<String> players;
	protected Map<String, Player> playerMap;
	protected Map<String, Collection<Card>> playerCardMap;
	
	protected static RiskMap starterMap = null;
	protected static Random rand;
	protected static int allocationIdx = 0;
	
	protected FileWriter log, stats;
	protected ArrayList<String> internalLogCache = new ArrayList<String>();
	protected List<String> allPlayers;
	protected int round, turnCount;
	
	//private ScrollPane scrollPane;
	private Scene scene;
	private static Pane pane;
	private static Text subStatusTextElement;
	private static Text mainStatusTextElement;
	private String errorTextInitialContents;
	private HBox playerDisplay = null;
	private HashMap<String, Text> textNodeMap;
	private Map<String, Color> playerColorMap;
	private static boolean fullAppExit = false;
	
	//to handle recovering a prior session or help with launching a new game session
	private static SavePoint savePoint = new SavePoint();
	private static SavePoint loadedSaveIn = null;
	private static String loadFailureReason = "";
	private HashMap<String, Country> stringCountryRepresentation = new HashMap<String, Country>();
	private static ArrayList<Node> buttonCache = null;
	private enum ButtonIndex{
		BTN_START,
		BTN_SAVE,
		BTN_HIGHLIGHT,
		CKBX_LOGGING,
		BTN_LOG_PLAYBACK
	}
	private static boolean endGame = false;
	private static Player currentPlayer = null;
	private static boolean skipExitConfirmation = false;
	private static Thread priGameLogicThread = null;
	private static final boolean LOAD_ALT_SAVE = false, LOAD_DEFAULT_SAVE = true;
	
	
	/**
	* If the app detects a call from the system to exit the program, 
	* and it's from a dialog window, handle the call by...asking if we really want to exit.
	*/
	public static int doYouWantToMakeAnExit(boolean shutAppDownOnAccept, int currentAttempts){
		AtomicBoolean dialogIsShowing = new AtomicBoolean(true);
		AtomicBoolean allowAppToExit = new AtomicBoolean(false);
		if(!FXUIGameMaster.skipExitConfirmation && Platform.isFxApplicationThread()){ //if this is the FX thread, make it all happen, and use showAndWait
			exitDialogHelper(shutAppDownOnAccept || FXUIGameMaster.fullAppExit, true, allowAppToExit, dialogIsShowing);
		}
		
		else if(!FXUIGameMaster.skipExitConfirmation && !Platform.isFxApplicationThread()){ //if this isn't the FX thread, we can pause logic with a call to RiskUtils.sleep()
			Platform.runLater(new Runnable(){
				@Override public void run(){
					exitDialogHelper(shutAppDownOnAccept || FXUIGameMaster.fullAppExit, false, allowAppToExit, dialogIsShowing);
				}
			});
			if(!shutAppDownOnAccept){
				do{
					RiskUtils.sleep(100);
				}
				while(dialogIsShowing.get() && !FXUIGameMaster.fullAppExit);
				if(FXUIGameMaster.fullAppExit){
					allowAppToExit.set(true);
				}
			}
			else{
				do{
					RiskUtils.sleep(100);
				}
				while(dialogIsShowing.get());
			}
		}
		
		else{
			dialogIsShowing.set(false);
		}
		if(allowAppToExit.get() || FXUIGameMaster.skipExitConfirmation)
		{
			FXUIGameMaster.skipExitConfirmation = false;
			return RiskConstants.MAX_ATTEMPTS;
		}
		else{
			return currentAttempts - 1;
		}
	}
	
	
	private static void exitDialogHelper(final boolean shutAppDownOnAccept, final boolean fxThread, AtomicBoolean allowAppToExit, AtomicBoolean dialogIsShowing)
	{
		Window owner = pane.getScene().getWindow();
		try{
			final Stage dialog = new Stage();
			dialog.setTitle("bye bye?");
			dialog.initOwner(owner);
			dialog.setX(owner.getX());
			dialog.setY(owner.getY());
			
			final VBox layout = new VBox(10);
			layout.setAlignment(Pos.CENTER);
			layout.setStyle("-fx-background-color: pink; -fx-padding: 30");
			
			final Text queryText = new Text("     Did you want to end the game?     ");
			queryText.setTextAlignment(TextAlignment.CENTER);
			
			final Text querySymbol = new Text("(o.o ?)");
			querySymbol.setTextAlignment(TextAlignment.CENTER);
			querySymbol.setFont(Font.font("Arial", FontWeight.BOLD, 24));
			

			Text spaceBuffer = new Text("\nIf enabled, your most recent\ncheckpoint will auto-save,\nor you may manually save");
			querySymbol.setTextAlignment(TextAlignment.CENTER);
			querySymbol.setFont(Font.font("Arial", FontWeight.LIGHT, 16));
			

			final Button yeah = new Button("Yes");
			final Button nah = new Button("No");
			final Button saveMe = new Button("save last checkpoint to...");
			
			yeah.setOnAction(new EventHandler<ActionEvent>() {
				@Override public void handle(ActionEvent t) {
					crossbar.signalHumanEndingGame();
					allowAppToExit.set(true);
					dialogIsShowing.set(false);
					FXUIGameMaster.skipExitConfirmation = shutAppDownOnAccept;
					if(!shutAppDownOnAccept)
					{
						FXUIGameMaster.endGame = true;
						mainStatusTextElement.setText("I D L E");
					}
					crossbar.tryCloseCurrentPlayerDialog();
					dialog.close();
				}
			});
			
			nah.setDefaultButton(true);
			nah.setOnAction(new EventHandler<ActionEvent>() {
				@Override public void handle(ActionEvent t) {
					dialogIsShowing.set(false);
					allowAppToExit.set(false);
					dialog.close();
				}
			});
			
			saveMe.setTooltip(new Tooltip("Changes the location where your game is being auto-saved"
					+ "\nAND IMMEDIATELY saves to that new location!"));
			saveMe.setOnAction(new EventHandler<ActionEvent>(){
				@Override public void handle(ActionEvent t){
					if(performSave(true))
					{
						saveMe.getTooltip().setText("saved. autosave set to " + saveto_filename
								+ ".\n\nChanges the location where your game is being auto-saved"
								+ "\nAND IMMEDIATELY saves to that new location!");
						spaceBuffer.setText("checkpoint saved. autosaving there\nuntil app restart");
					}
					else{
						saveMe.getTooltip().setText("save failed; try again???"
								+ "\n\nChanges the location where your game is being auto-saved"
								+ "\nAND IMMEDIATELY saves to that new location!");
						spaceBuffer.setText("manual save failed.\n(checkpoint may be auto-saved)");
					}
				}
			});
			
			if(!FXUIGameMaster.savePoint.getIsReady()){
				saveMe.setDisable(true);
				saveMe.setText("save checkpoint (N/A)");
			}
			
			if(shutAppDownOnAccept)
			{
				yeah.setText("[continue]");
				layout.setStyle("-fx-background-color: black; -fx-padding: 30");
				queryText.setText("thanks for playing!\n\n[this window will auto-close]");
				queryText.setFill(Color.WHEAT);
				querySymbol.setText("zZz (u_u?) zZz");
				querySymbol.setFill(Color.WHEAT);
				spaceBuffer.setFill(Color.WHEAT);
				spaceBuffer.setText("+\n+\n+");
				nah.setVisible(false);
				saveMe.setVisible(false);
				deathKnell(dialog, spaceBuffer, dialogIsShowing);
			}
			
			layout.getChildren().setAll(
					querySymbol, queryText, saveMe, nah, yeah, spaceBuffer
			);
			
			dialog.setOnCloseRequest(new EventHandler<WindowEvent>(){
				@Override public void handle(WindowEvent t){
					dialogIsShowing.set(false);
					allowAppToExit.set(false);
				}
			});
			
			dialog.setScene(new Scene(layout));
			if(fxThread){
				dialog.showAndWait();
				}
			else{
				dialog.show();
			}
		}
		catch(Exception e){System.out.println(ERROR+"attempted exit failed:: " + e);}
	}
	
	
	/**
	 * Allow the player to decide if they want to start a new game, or launch an old game.
	 * MUST BE RUN ON JAVAFX THREAD.
	 * @param shutAppDownOnAccept
	 * @param fxThread
	 */
	private int displayGameSelector()
	{
		if (!Platform.isFxApplicationThread() || FXUIGameMaster.workingMode != IDLE_MODE){
			return -1;
		}
		Window owner = pane.getScene().getWindow();
		final Stage dialog = new Stage();
		dialog.setTitle("new? restore?");
		dialog.initOwner(owner);
		dialog.setX(owner.getX());
		dialog.setY(owner.getY()+300);
		
		final Pane miniPane = new Pane();
		
		final VBox layout = new VBox(10);
		layout.setAlignment(Pos.CENTER);
		layout.setStyle("-fx-background-color: lightcyan; -fx-padding: 50");
		
		final Text spaceBuffer = new Text("");
		
		final Text queryText = new Text("     Greetings, Human!     \nWhat would you like to do?\n-\n");
		queryText.setTextAlignment(TextAlignment.CENTER);
		
		final Text querySymbol = new Text("\\(OxO ?)");
		querySymbol.setTextAlignment(TextAlignment.CENTER);
		querySymbol.setFont(Font.font("Arial", FontWeight.BOLD, 24));

		final Button newGameBtn = new Button("Launch a NEW game.");
		final Text newGameText = new Text("\nStart a brand new game.");
		newGameText.setTextAlignment(TextAlignment.CENTER);
		newGameBtn.setTooltip(new Tooltip("Launch a brand new game, with the potential to overwrite\nprevious game saves."));
		
		final Button loadGameBtn = new Button("LOAD previous save.");
		final Text loadGameText = new Text();
		loadGameText.setTextAlignment(TextAlignment.CENTER);
		final Text loadGameSubText = new Text();
		loadGameSubText.setTextAlignment(TextAlignment.CENTER);
		loadGameSubText.setOpacity(0.5d);
		final String startingTooltipContents = "Load from the default save file!\nCurrently set to load from " + loadfrom_filename;
		final Tooltip ldToolTip = new Tooltip(startingTooltipContents);
		loadGameBtn.setTooltip(ldToolTip);
		
		final Button cnclBtn = new Button("[cancel]");
		cnclBtn.setTooltip(new Tooltip("Return to the main menu screen without launching a game of either type."));
		
		
		/*
		 * Different event handlers, to be used depending on states/keys&buttons to make available.
		 */
		EventHandler<ActionEvent> defaultLdBtnHandler = new EventHandler<ActionEvent>() {
			@Override public void handle(ActionEvent t) {
				FXUIGameMaster.workingMode = LOADED_GAME_MODE;
				dialog.close();
			}
		};
		
		EventHandler<ActionEvent> loadAltFileHandler = new EventHandler<ActionEvent> (){
			@Override public void handle(ActionEvent t) {
				final FileChooser fileChooser = new FileChooser();
				File file = fileChooser.showOpenDialog(new Stage());
	            if (file != null) {
	            	//active_checkpoint_file_name = file.getPath();
	            	if(loadFromSave(true, file.getAbsolutePath())){
		            	loadfrom_filename = file.getAbsolutePath();
						FXUIGameMaster.workingMode = LOADED_GAME_MODE;
		            	dialog.close();
	            	}
	            	else{
	            		loadGameText.setText("load failed.");
	            		ldToolTip.setText(
            				(FXUIGameMaster.loadFailureReason.contains("FileNotFound") 
								? //If true, set text to...
								"Can't load: "
								+ "OS dependent path error;\nplace the file in the same dir as the app,"
								+ "\nrename as " + DEFAULT_CHKPNT_FILE_NAME + "& relaunch app"
								: //Or if false, set text to...
								"Can't load save file!\nReason:"
        					)
        					+ loadFailureReason 
        					+ "\n\nCLICK to load alt save file"
        				);
	            	}
	            }
			}
		};
		
		
		AtomicBoolean loadButtonState = new AtomicBoolean(false);
		EventHandler<KeyEvent> altKeyEventHandler = new EventHandler<KeyEvent>(){
			@Override
			public void handle(KeyEvent event) {
				if(event.getEventType() == KeyEvent.KEY_PRESSED){
					if(event.getCode() == KeyCode.ALT || event.getCode() == KeyCode.SHIFT){
						loadButtonState.set(!loadButtonState.get());
						if(loadButtonState.get() == LOAD_DEFAULT_SAVE){
							loadGameBtn.setOnAction(loadAltFileHandler);
							loadGameBtn.setText("SELECT OTHER save file...");
							ldToolTip.setText("Select a different checkpoint/save file!\n(Opens \"Locate File...\" dialog)");
						}
						else if (loadButtonState.get() == LOAD_ALT_SAVE){
							loadGameBtn.setOnAction(defaultLdBtnHandler);
							loadGameBtn.setText("LOAD from known checkpoint...");
							ldToolTip.setText(startingTooltipContents);
						}
					}
				}
			}
		};
		
		newGameBtn.setOnAction(new EventHandler<ActionEvent>() {
			@Override public void handle(ActionEvent t) {
				FXUIGameMaster.workingMode = NEW_GAME_MODE;
				dialog.close();
			}
		});
		newGameBtn.setDefaultButton(true);
		
		loadGameBtn.setOnAction(defaultLdBtnHandler);
		
		cnclBtn.setOnAction(new EventHandler<ActionEvent> (){
			@Override public void handle(ActionEvent t) {
				dialog.close();
			}
		});
		
		
		if(!loadFromSave(true, DEFAULT_CHKPNT_FILE_NAME)){
			loadGameBtn.setOpacity(0.5d);
			loadGameText.setOpacity(0.5d);
			loadGameBtn.setOnAction(loadAltFileHandler);
			loadGameBtn.setFocusTraversable(false);
			queryText.setText("     Make new game?     \n\n\n");
			ldToolTip.setText("Tried to load default save file...\n" +
					(FXUIGameMaster.loadFailureReason.contains("FileNotFound") ?
								"No checkpoint/save file found!\n" : "Couldn't load it!\nReason:")
					+ loadFailureReason 
					+ "\n\nCLICK to load alt save file");
			loadGameText.setText("Load alternate save file");
                        loadGameBtn.setText("FIND previous save file...");
		}
		else{
			loadGameText.setText("Load game from save file!");
			loadGameSubText.setText("(ALT/SHIFT switches loading types.)\n\n(hover over buttons to see more info.)");
			miniPane.setOnKeyPressed(altKeyEventHandler);
		}
		
		layout.getChildren().setAll(
				querySymbol, queryText, newGameText, newGameBtn, loadGameText, loadGameBtn, loadGameSubText, spaceBuffer, cnclBtn
		);
		miniPane.getChildren().add(layout);
		
		dialog.setScene(new Scene(miniPane));
		
		FXUIGameMaster.crossbar.setCurrentPlayerDialog(dialog);
		dialog.showAndWait();
		FXUIGameMaster.crossbar.setCurrentPlayerDialog(null);
		
		return FXUIGameMaster.workingMode;
	}
	
	/**
	 * Used to auto-close the final dialog when exiting the app. Also gives a sort of
	 * ...ASCII animation to indicate closing process.
	 * @param dialog the dialog window to be closed (which permits the logic to end)
	 */
	public static void deathKnell(Stage dialog, Text animatedRegion, AtomicBoolean callerIsVisible){
		if(dialog == null){return;}
		else if(animatedRegion == null){
			new Thread(new Runnable()
			{
				@Override public void run(){
					RiskUtils.sleep(AUTO_CLOSE_TIMEOUT);
					Platform.runLater(new Runnable(){
						@Override public void run(){
							dialog.close();
						}
					});
				}
			}).start();
			return;
		}
		else if (callerIsVisible != null){
			final String originalAnimState = "G O O D B Y E       : D";
			final int discreteAnimSteps = 10;
			final int origStrLen = originalAnimState.length();
			final int singleChunkLength = (int)Math.floor(origStrLen/discreteAnimSteps); 
			new Thread(new Runnable()
			{
				@Override public void run(){
					for (int i = discreteAnimSteps; i > 0 && callerIsVisible.get(); --i){
						final int iO = i;
						Platform.runLater(new Runnable(){
							@Override public void run(){
								String output = originalAnimState.substring(0, iO*singleChunkLength);
								animatedRegion.setText(output);
							}
						});
						RiskUtils.sleep((long)AUTO_CLOSE_TIMEOUT/discreteAnimSteps);
					}
					Platform.runLater(new Runnable(){
						@Override public void run(){
							dialog.close();
						}
					});
				}
			}).start();
		}
	}
	
	/**
	* Prepares save info. Use at the beginning of each new round, as a checkpoint,
	*   to avoid any one player getting an advantage over any other player upon later resume.
	*   Doesn't guarantee perfect state save, but helps.
	*/
	private boolean prepareSave(){
		if(savePoint == null){savePoint = new SavePoint();}
		disableSaveButton();
		boolean saveIsReady = false;
		HashMap<String, Collection<Card>> playerCardsetMap = new HashMap<>();
		for (String player : this.players){
			playerCardsetMap.put(player, createCardSetCopy(player));
		}
		/*
		 * If loadedSaveIn != null, we're working with an old save, so transplant the original save date
		 * from that old game save. Else, we're working with a new game, and a new save date for the game.
		 * Successive saves of a new game should only receive an updated latestSaveDate while 
		 * keeping the original originalSaveDate.
		 */
		Date originalSaveDate = (loadedSaveIn != null && loadedSaveIn.getOriginalSaveDate() != null) 
				? loadedSaveIn.getOriginalSaveDate()
				: savePoint.getOriginalSaveDate();
		saveIsReady = savePoint.prepareOverallSave
			(
				originalSaveDate, new Date(), this.round,
				this.map,
				(HashMap<String, Player>) playerMap, this.allPlayers,
				internalLogCache,
				this.players,
				playerCardsetMap
			);
		System.out.println(INFO+"Checkpoint reached.");
		enableSaveButton();
		return saveIsReady;
	}
	
	/**
	* Performs actual write to disc using most recent checkpoint available.
	* Checkpoints are acquired with prepareSave(), automatically performed after initial player allocation.
	* Write to secondary storage is triggered either automatically at each new round,
	* or manually with the "Save" button  (with no discernible difference between the two).
	* 
	* @return returns true on successful save, or false when a show-stopping exception was thrown.
	*/
	private static boolean performSave(boolean customSave){
		disableSaveButton();
		// TODO add informative error messages
		boolean succeeded = false;
		try{
			OutputStream fileOutStream = null;
			
			if(customSave){
				FileChooser fileChooser = new FileChooser();
				File fileOut = fileChooser.showSaveDialog(new Stage());
				fileChooser.setTitle("Set your save location & save now:");
				if(fileOut == null){
					customSave = false;
				}
				else{
					fileOutStream = new FileOutputStream(fileOut);
					saveto_filename = fileOut.getAbsolutePath();
				}
			}
			if(!customSave){
				fileOutStream = new FileOutputStream(saveto_filename);
			}
			OutputStream buffer = new BufferedOutputStream(fileOutStream);
			ObjectOutput output = new ObjectOutputStream(buffer);
			output.writeObject(savePoint);
			output.close();
			succeeded = true;
			System.out.println(INFO+"Checkpoint saved to " + saveto_filename + ".");
			setErrorStatus("checkpoint saved");
		}
		catch(Exception e){
			System.out.println(ERROR+"Save failed. ::: " + e);
			
		}
		enableSaveButton();
		if(!succeeded)
		{
			setErrorStatus("save failed");
		}
		return succeeded;
	}
	
	/**
	* Restores a prior game session. Loads the contents of a serialized SavePoint object 
	* into memory & rebuilds the game session from that limited info.
	* (Relies on some basic initialization elsewhere).
	* Loading should occur on the game logic thread (whichever thread prompts for responses).
	* Should *not* be run on threads which may delay reading/processing (can cause concurrency issues,
	* as it does not force a lock of any necessary resources).
	* @param testing To test the ability to read the save file & skip actually loading data, "true".
	* 	To do a full load of previous save data, "false".
	* @param potentialLocation location of the save file. Ignored unless testing = true.
	* @return returns true if the load succeeded, or false if a show-stopping exception was encountered
	*/
	private boolean loadFromSave(boolean testing, String potentialLocation)
	{
		boolean loadSucceeded = true;
		try{
			
			InputStream file = new FileInputStream(testing ? potentialLocation : FXUIGameMaster.loadfrom_filename);
			InputStream buffer = new BufferedInputStream(file);
			ObjectInput input = new ObjectInputStream(buffer);
			SavePoint loadedSave = (SavePoint)input.readObject();
			loadedSaveIn = loadedSave;
			if(!testing){
				if(!loadPlayersFromSave(loadedSave)){
					loadFailureReason.concat("\n!LF::: " + 
						"Couldn't load prior player information from the given save file!");
					loadSucceeded = false;
				}
				else{
					loadSucceeded &= true;
				}
				if(!resetCountryInfo(loadedSave)){
					loadFailureReason.concat("\n!LF::: " +
						"Couldn't reset status information for all Countries.");
					loadSucceeded = false;
				}
				else{
					loadSucceeded &= true;
				}
				
				if(!restorePreviousLogInfo(loadedSave)){
					loadFailureReason.concat("\n!LF::: " + 
						"Couldn't restore the log for the prior game session!");
					loadSucceeded = false;
				}
				else{
					loadSucceeded &= true;
				}
				representPlayersOnUI();
				refreshUIElements(true);
			}
			input.close();
			buffer.close();
			file.close();
			loadFailureReason ="";
		}
		catch(Exception e){
			if(!testing){
				System.out.println(ERROR+"Load failed. ::: " + e);
				e.printStackTrace();
			}
			loadFailureReason ="\n!LF::: " + 
					"exception occurred: " + e;
			loadSucceeded = false;
		}
		return loadSucceeded;
	}
	
	
	/**
	* Loads the log from the prior game -- up to the checkpoint -- so we can update the actual physical log file properly.
	* @return "False" if no log info could be found in the previous save, or "True" otherwise.
	*/
	private boolean restorePreviousLogInfo(SavePoint loadedSave)
	{
		internalLogCache = loadedSaveIn.getLogCache();
		
		if (loggingEnabled == LOGGING_ON) {
			try {
				if (this.log != null && this.stats != null) {
					this.log.close();
					this.stats.close();
				}
				this.log = new FileWriter(LOGFILE);
				this.stats = new FileWriter(STATSFILE);
			}
			catch (IOException e) {
			}
		}
		
		for (String cacheLine : internalLogCache)
		{
			writeLogLn(false, cacheLine);
		}
		return loadedSave != null && loadedSave.getLogCache() != null;
	}
	
	/**
	* Pulls the players and their cards from a given SavePoint object.
	* @param loadedSave the save from which we get player info
	* @return returns false if the amount of *active* players is outside the bounds of the Risk rules, or true otherwise
	*/
	protected boolean loadPlayersFromSave(SavePoint loadedSave)
	{
		boolean success = true;
		//clear the player list...just in case.
		writeLogLn(true, "Loading players...");
		try{
			this.playerMap.clear();
			this.allPlayers.clear();
			this.players.clear();
		}
		catch(Exception e){
			this.playerMap = new HashMap<String, Player>();
			this.allPlayers = new ArrayList<String>();
			this.players = new ArrayList<String>();
		}
		
		
		final String FXP = FXUIPlayer.class.toString();
		final String EDP = EasyDefaultPlayer.class.toString();
		final String HDP = HardDefaultPlayer.class.toString();
		final String NDP = NormalDefaultPlayer.class.toString();
		final String S_P = Seth.class.toString();
		for (Entry<String, Boolean> playerIn : loadedSave.getPlayerIsEliminatedMap().entrySet())
		{
			this.allPlayers.add(playerIn.getKey());
			Player playerObjectToCast = null;
			
			if (playerIn.getValue() == false)//player isn't eliminated
			{
				String switcher = loadedSave.getActivePlayersAndTheirTypes().get(playerIn.getKey());
				if (switcher.equals(FXP)){
					playerObjectToCast = new FXUIPlayer(playerIn.getKey());
					FXUIPlayer.setCrossbar(FXUIGameMaster.crossbar);
				}
				else if (switcher.equals(EDP)){
					playerObjectToCast = new EasyDefaultPlayer(playerIn.getKey());
				}
				else if (switcher.equals(HDP)){
					playerObjectToCast = new HardDefaultPlayer(playerIn.getKey());
				}
				else if (switcher.equals(NDP)){
					playerObjectToCast = new NormalDefaultPlayer(playerIn.getKey());
				}
				else if (switcher.equals(S_P)){
					playerObjectToCast = new Seth(playerIn.getKey());
				}
				
				if(playerObjectToCast == null){
					System.out.println(ERROR + "Failed to cast/load " + playerIn.getKey() +
							" as a valid player. (Attempted type: " + loadedSave.getActivePlayersAndTheirTypes().get(playerIn.getKey()) + "...Is the Player type know to the GameMaster?)");
					success = false;
				}
				else
				{
					this.playerMap.put(playerIn.getKey(), playerObjectToCast);
					this.players.add(playerIn.getKey());
				}
			}
		}
		
		this.playerCardMap = new HashMap<String, Collection<Card>>();
		for (Player playerM : this.playerMap.values()) {
			ArrayList<Card> newCards = new ArrayList<Card>();
			if (loadedSave.getPlayersAndTheirCards().get(playerM.getName()) != null)
			{
				for (String cardRepresentation : loadedSave.getPlayersAndTheirCards().get(playerM.getName() ) ){
					if(cardRepresentation.contains(RiskConstants.WILD_CARD)){
						newCards.add(new Card(RiskConstants.WILD_CARD, null));
					}
					else{
						String[] ssmm = cardRepresentation.split(",");
						//System.out.println(ssmm[0] + ssmm[1]);
						Card cdOut = new Card(ssmm[0], stringCountryRepresentation.get(ssmm[1]));
						newCards.add(cdOut);
					}
				}
			}
			this.playerCardMap.put(playerM.getName(), newCards);
		}
		
		
		if (this.players.size() < RiskConstants.MIN_PLAYERS || this.players.size() > RiskConstants.MAX_PLAYERS || !success) {
			return false;
		}
		else {
			writeLogLn(true, "Players:");
			for (String playerName : this.players) {
				writeLogLn(true, playerName);
			}
			writeLogLn(true, EVENT_DELIM);
			return true;
		}
	}
	
	/**
	* Takes country info (including owners + army count) from a SavePoint object,
	* and updates the internal data of the map with said info. Refreshing the map is done elsewhere.
	* @param loadedSave the SavePoint object from which we source our data
	*/
	public boolean resetCountryInfo(SavePoint loadedSave)
	{
		for (Entry<String, Integer> entryOutArmy : loadedSave.getCountriesAndArmyCount().entrySet()){
			map.setCountryArmies(stringCountryRepresentation.get(entryOutArmy.getKey()), entryOutArmy.getValue());
		}
		for (Entry<String, String> entryOutOwner : loadedSave.getCountriesAndOwners().entrySet()){
			map.setCountryOwner(stringCountryRepresentation.get(entryOutOwner.getKey()), entryOutOwner.getValue());
		}
		
		this.round = loadedSave.getRoundsPlayed();
		
		return loadedSave.getCountriesAndArmyCount().entrySet().size() == loadedSave.getCountriesAndOwners().entrySet().size()
		&& loadedSave.getCountriesAndOwners().entrySet().size() > 0;
	}
	
	/**
	* This method is used as a way to have the 
	* 	new game, load game and save game buttons disabled at
	* 	select critical points/when the option is not pertinent,
	* 	and re-enable them when the game is not in any critical sections.
	* 	Does not handle all fine-tuned disables/enables; only initial startup and game play.
	* 	Based on states that might otherwise be easily compromised.
	*/
	public void setButtonAvailability(){
		if (buttonCache.size() != ButtonIndex.values().length){
			System.out.println(WARN+"I can't determine if I'm able to access some of" + 
							" the UI buttons to disable/enable them. Weird, huh?");
			return;
		}
		Platform.runLater(new Runnable()
		{
			@Override public void run(){
				if(workingMode == IDLE_MODE)
				{
					buttonCache.get(ButtonIndex.BTN_START.ordinal()).setDisable(false); //we can start a new game
					disableSaveButton(); //we cannot use the save button
					buttonCache.get(ButtonIndex.BTN_HIGHLIGHT.ordinal()).setDisable(true); //we cannot highlight the countries owner by a given player...
					buttonCache.get(ButtonIndex.CKBX_LOGGING.ordinal()).setDisable(false); //we are allowed to enable/disable logging
					buttonCache.get(ButtonIndex.BTN_LOG_PLAYBACK.ordinal()).setDisable(false); //disable the log playback button
					//the checkbox shouldn't have changed during play, but we update its text
					((CheckBox)buttonCache.get(ButtonIndex.CKBX_LOGGING.ordinal())).setText("Enable logging?\n(State: " + (FXUIGameMaster.loggingEnabled==LOGGING_ON ? "Yes" : "No") + ")");
					setPlayStatus("I D L E"); //set the status to "IDLE"
				}
				else {
					buttonCache.get(ButtonIndex.BTN_START.ordinal()).setDisable(true); //we cannot start a new game...at this point.
					//save button is set dynamically while the game is in play, so do not concern yourself with it
					buttonCache.get(ButtonIndex.BTN_HIGHLIGHT.ordinal()).setDisable(false); //we CAN highlight the countries owner by a given player.
					buttonCache.get(ButtonIndex.CKBX_LOGGING.ordinal()).setDisable(true); //we are not allowed to enable/disable logging
					buttonCache.get(ButtonIndex.BTN_LOG_PLAYBACK.ordinal()).setDisable(true); //disable the log playback button
					((CheckBox)buttonCache.get(ButtonIndex.CKBX_LOGGING.ordinal())).setIndeterminate(false);
					((CheckBox)buttonCache.get(ButtonIndex.CKBX_LOGGING.ordinal())).setSelected(FXUIGameMaster.loggingEnabled);
					((CheckBox)buttonCache.get(ButtonIndex.CKBX_LOGGING.ordinal())).setText("Enable logging?\n(State: " + (FXUIGameMaster.loggingEnabled==LOGGING_ON ? "Yes" : "No") + " -- Locked during play)");
					setPlayStatus("in play."); //set the status to "in play"; will be overwritten with an error if need be
				}
			}
		});
	}
	
	/**
	 * Companion method to disable the save button; ensures actions happen on FX thread
	 */
	public static void disableSaveButton(){
		Platform.runLater(new Runnable()
		{
			@Override public void run(){
				buttonCache.get(ButtonIndex.BTN_SAVE.ordinal()).setDisable(true); //disable the save button
			}
		});
	}
	
	/**
	 * Companion method to enable the save button; ensures actions happen on FX thread
	 */
	public static void enableSaveButton(){
		Platform.runLater(new Runnable()
		{
			@Override public void run(){
				buttonCache.get(ButtonIndex.BTN_SAVE.ordinal()).setDisable(false); //enable the save button
			}
		});
	}
	
	/**
	 * Used to set the text of the "currentPlayStatus" UI element, even from unsafe threads (unsafe: "not the FX thread")
	 *
	 * @param status text to be displayed
	 * @return "true" if called from the JavaFX thread, "false" otherwise
	 */
	private boolean setPlayStatus(String status){
		if(Platform.isFxApplicationThread()) //if already on FX thread, can directly set
		{
			mainStatusTextElement.setText(status);
			return true;
		}
		else //place in event queue for running on the FX thread
		{
			Platform.runLater(new Runnable()
			{
				@Override public void run(){
					mainStatusTextElement.setText(status);
				}
			});
			return false;
		}
	}
	
	/**
	 * Used to set the text of the "subStatusTextElement" UI element, even from unsafe threads (unsafe: "not the FX thread").
	 * As implied, intended use is to indicate some sort of error -- without replacing the main status.
	 * TODO add the ability to push these messages to diagnostics dialog & secondary storage
	 * @param error text to be displayed  (or default/alternative text, if no error occurred)
	 * @return "true" if called from the JavaFX thread, "false" otherwise
	 */
	private static boolean setErrorStatus(String status){
		if(Platform.isFxApplicationThread()) //if already on FX thread, can directly set
		{
			subStatusTextElement.setText(status);
			return true;
		}
		else //place in event queue for running on the FX thread
		{
			Platform.runLater(new Runnable()
			{
				@Override public void run(){
					subStatusTextElement.setText(status);
				}
			});
			return false;
		}
	}
	
	
	/**
	* Once button logic has been handled, and it is verified that no other game has been started,
	* this method is run to trigger the various states of attack/defense/etc, trigger checkpoints, and
	* check for any calls to exit the game prematurely that were not caught by internal exception handlers.
	* @return name of the winner if the game has an ideal termination, null otherwise.
	*/
	private String begin() {
		boolean initiationGood = false;
		if (workingMode == NEW_GAME_MODE){
			if (!(initiationGood = loadPlayers(FXUIGameMaster.desiredPlayersForGame))) {
				System.out.println(ERROR+"Invalid number of players. 2-6 Players allowed.");
				setPlayStatus("creation of new game failed; 2-6 Players allowed");
			}
			else{
				representPlayersOnUI();
				allocateMap();
				initiationGood = initializeForces() && initiationGood;
				if(!initiationGood){
					setPlayStatus("creation of new game failed");
				}
				else{
					prepareSave();
				}
			}
			
		}
		else if (workingMode == LOADED_GAME_MODE){
			initiationGood = loadFromSave(false, null);
			if(!initiationGood){
				setPlayStatus("load failed!!");
			}
		}
		if (initiationGood) {
			crossbar.resetEndGameSignal();
			//play round-robin until there is only one player left
			int turn = 0;
			while (this.players.size() > 1 && !FXUIGameMaster.endGame) {
				if (turn == 0) {
					this.round++;
					writeLogLn(true, "Beginning Round " + round + "!");
					if (this.round > RiskConstants.MAX_ROUNDS) {
						return "Stalemate!";
					}
				}
				currentPlayer = this.playerMap.get(this.players.get(turn));
				boolean canUpdateUIAndSave = currentPlayer.getClass().toString().equals(FXUIPlayer.class.toString());
				writeLogLn(true, currentPlayer.getName() + " is starting their turn.");
				writeStatsLn();
				this.turnCount++;
				try {
					for(int gameStep = 4; gameStep > -1 && !(FXUIGameMaster.endGame = crossbar.isHumanEndingGame() || FXUIGameMaster.endGame); gameStep--){
						refreshUIElements(canUpdateUIAndSave);
						switch(gameStep){
						case 4:
							reinforce(currentPlayer, true);
							break;
						case 3:
							attack(currentPlayer);
							break;
						case 2:
							fortify(currentPlayer);
							break;
						case 1:
							turn = (this.players.indexOf(currentPlayer.getName()) + 1) % this.players.size();
							break;
						case 0:
							if (canUpdateUIAndSave){
								prepareSave();
								performSave(false);
							}
							break;
						default:
							break;
						}
					}
					
				}
				catch (PlayerEliminatedException e) {
					//If an elimination exception is thrown up to this level,
					//then it was currentPlayer who was eliminated.
					turn %= this.players.size();
				}
			}
			FXUIGameMaster.currentPlayer = null;
			if(!FXUIGameMaster.fullAppExit && !FXUIGameMaster.endGame && this.players.size() > 0){
				refreshUIElements(true);
				writeStatsLn();
				System.out.println(this.players.get(0) + " is the victor!");
				writeLogLn(true, this.players.get(0) + " is the victor!");
				flashPlayerCountries(this.players.get(0));
			}
			else
			{
				System.out.println(WARN+"Game forced to exit by UI player; sorry 'bout it!");
			}
		}
		
		try {
			if (this.log != null && this.stats != null) {
				log.close();
				stats.close();
			}
		}
		catch (IOException e) {
		}
		workingMode = IDLE_MODE;
		setButtonAvailability();
		crossbar.resetEndGameSignal();
		if(this.players.size() > 0 && !FXUIGameMaster.endGame)
		{
			return this.players.get(0);
		}
		else
		{
			FXUIGameMaster.endGame = false;
			return null;
		}
	}
	
	/**
	* Triggers each Player type to invoke initial troop allocation on countries assigned to each player.
	* Used only for new games, as saved games can restore their state fairly well.
	* @return returns true if at least one player succeeded, false otherwise
	*/
	protected boolean initializeForces() {
		boolean valid;
		int attempts;
		int playerIndex = 0;
		//get initial troop allocation
		while (playerIndex < this.players.size() && !crossbar.isHumanEndingGame()) {
			Player player = this.playerMap.get(this.players.get(playerIndex));
			writeLogLn(true, "Getting initial troop allocation from " + player.getName() + "...");
			int reinforcements;
			valid = false;
			attempts = 0;
			
			/*
			 * If we reach a point where there is only one player remaining,
			 * given that we are just doing the initial player setup, the game
			 * is no longer valid
			 **/
			if(this.players.size() < RiskConstants.MIN_PLAYERS){
				try {
					eliminate(player, null, "Player count: " + this.players.size() + "; required players: " + RiskConstants.MIN_PLAYERS);
				}
				catch (PlayerEliminatedException e) {
					writeLogLn(false, "WARNING: Invalid game initialization. Invalid game state reached!");
				}
			}
			else{
				while (!valid && attempts < RiskConstants.MAX_ATTEMPTS  && !FXUIGameMaster.fullAppExit && !crossbar.isHumanEndingGame()) {
					attempts++;
					reinforcements = RiskConstants.INIT_ARMIES / this.players.size();
					ReinforcementResponse rsp = tryInitialAllocation(player, reinforcements);
					if (crossbar.isHumanEndingGame()){
						break;
					}
					if (valid = ReinforcementResponse.isValidResponse(rsp, this.map, player.getName(), reinforcements)
							&& validateInitialAllocation(rsp.getAllocation(), player.getName(), reinforcements)) {
						allocateArmies(player.getName(), rsp.getAllocation(), reinforcements);
						playerIndex++;
						writeLogLn(true, "Troops successfully allocated for " + player.getName() + "...");
					}
				}
				
				if (!valid || crossbar.isHumanEndingGame()) {
					try {
						if(crossbar.isHumanEndingGame()){
							eliminate(player, null, "Ending game prematurely; ejecting player from game!");
						}
						else{
							eliminate(player, null, "You failed to provide a valid initial army allocation.");
						}
					}
					catch (PlayerEliminatedException e) {
						playerIndex = 0;
					}
				}
			}
		}
		return this.players.size() > 0;
	}
	
	protected void validatePlayerName(Player player) throws PlayerEliminatedException {
		if (!(this.playerMap.containsKey(player.getName()) && this.playerMap.get(player.getName()) == player)) {
			eliminate(player, null, "Players who hide their true identity are not welcome here. BEGONE!");
		}
	}
	
	protected void reinforce(Player currentPlayer, boolean withCountryBonus) throws PlayerEliminatedException {
		int reinforcements = 0;
		int attempts = 0;
		boolean valid = false;
		reinforcements += getCardTurnIn(currentPlayer, getPlayerCardCounts());
		if(FXUIGameMaster.endGame = crossbar.isHumanEndingGame() || FXUIGameMaster.endGame){
			return;
		}
		Map<String, Integer> oppCards = getPlayerCardCounts();
		if (withCountryBonus) {
			reinforcements += RiskUtils.calculateReinforcements(this.map, currentPlayer.getName());
		}
		writeLogLn(true, currentPlayer.getName() + " reinforcing with " + reinforcements + " armies.");
		while (!valid && attempts < RiskConstants.MAX_ATTEMPTS  && !FXUIGameMaster.fullAppExit) {
			attempts++;
			ReinforcementResponse rsp = tryReinforce(currentPlayer, oppCards, reinforcements);
			if(FXUIGameMaster.endGame = crossbar.isHumanEndingGame() || FXUIGameMaster.endGame){
				return;
			}
			if (valid = ReinforcementResponse.isValidResponse(rsp, this.map, currentPlayer.getName(), reinforcements)) {
				for (Map.Entry<Country, Integer> entry : rsp.getAllocation().entrySet()) {
					this.map.addCountryArmies(entry.getKey(), entry.getValue());
					writeLogLn(true, entry.getValue() + " " + entry.getKey().getName());
				}
			}
		}
		if (!valid) {
			eliminate(currentPlayer, null, "You failed to provide a valid reinforcement allocation.");
		}
		else if(crossbar.isHumanEndingGame()) {
			eliminate(currentPlayer, null, "Player decided to leave. Come back any time, friend!");
		}
		writeLogLn(true, EVENT_DELIM);
	}
	
	protected void attack(Player currentPlayer) throws PlayerEliminatedException {
		int attempts = 0;
		boolean resetTurn;
		boolean hasGottenCard = false;
		while (attempts < RiskConstants.MAX_ATTEMPTS && !FXUIGameMaster.fullAppExit) {
			attempts++;
			resetTurn = false;
			AttackResponse atkRsp = tryAttack(currentPlayer, createCardSetCopy(currentPlayer.getName()), getPlayerCardCounts());
			if (atkRsp != null) {
				if (AttackResponse.isValidResponse(atkRsp, this.map, currentPlayer.getName())) {
					writeLogLn(true, currentPlayer.getName() + " is attacking "
					+ atkRsp.getDfdCountry() + "(" + this.map.getCountryArmies(atkRsp.getDfdCountry())
					+ ") from " + atkRsp.getAtkCountry() + "(" + this.map.getCountryArmies(atkRsp.getAtkCountry()) + ")!");
					System.out.println(currentPlayer.getName() + " is attacking "
					+ atkRsp.getDfdCountry() + "(" + this.map.getCountryArmies(atkRsp.getDfdCountry())
					+ ") from " + atkRsp.getAtkCountry() + "(" + this.map.getCountryArmies(atkRsp.getAtkCountry()) + ")!");
					attempts = 0;
					Player defender = getOwnerObject(atkRsp.getDfdCountry());
					DefendResponse dfdRsp = null;
					try {
						//this is guaranteed to either be valid or throw a PlayerEliminatedException
						dfdRsp = defend(defender, getPlayerCardCounts(), atkRsp);
					}
					catch (PlayerEliminatedException e) {
						//defender messed up and was auto-eliminated
						resetTurn = true;
					}
					if (!resetTurn) {
						carryOutAttack(atkRsp, dfdRsp);
						hasGottenCard = checkForTakeover(currentPlayer, atkRsp, hasGottenCard) || hasGottenCard;
					}
				}
			}
			else {
				//because an attack is not required, a null response is taken to mean that the player declines the opportunity
				attempts = RiskConstants.MAX_ATTEMPTS;
			}
		}
	}
	
	protected DefendResponse defend(Player defender, Map<String, Integer> oppCards, AttackResponse atkRsp) throws PlayerEliminatedException {
		int attempts = 0;
		boolean valid = false;
		DefendResponse rsp = null;
		while (!valid && attempts < RiskConstants.MAX_ATTEMPTS && !FXUIGameMaster.fullAppExit) {
			attempts++;
			rsp = tryDefend(defender, createCardSetCopy(defender.getName()), oppCards, new AttackResponse(atkRsp));
			valid = DefendResponse.isValidResponse(rsp, this.map, atkRsp.getDfdCountry());
		}
		if (!valid) {
			eliminate(defender, null, "You failed to provide a valid defense response.");
		}
		else if(crossbar.isHumanEndingGame())
		{
			eliminate(defender, null, "This defender wants a break. Go ahead, friend. You deserve it.");
		}
		return rsp;
	}
	
	protected void carryOutAttack(AttackResponse atk, DefendResponse dfd) {
		RollOutcome result = DiceRoller.roll(atk.getNumDice(), dfd.getNumDice());
		this.map.addCountryArmies(atk.getAtkCountry(), -1 * result.getAtkLosses());
		this.map.addCountryArmies(atk.getDfdCountry(), -1 * result.getDfdLosses());
		writeLogLn(true, "\tAttacker lost: " + result.getAtkLosses() + "; Defender lost: " + result.getDfdLosses());
	}
	
	protected boolean checkForTakeover(Player attacker, AttackResponse atkRsp, boolean hasGottenCard) throws PlayerEliminatedException {
		if (this.map.getCountryArmies(atkRsp.getDfdCountry()) == 0) {
			String loserName = this.map.getCountryOwner(atkRsp.getDfdCountry());
			writeLogLn(true, attacker.getName() + " has taken " + atkRsp.getDfdCountry() + " from " + loserName + "!");
			this.map.setCountryOwner(atkRsp.getDfdCountry(), attacker.getName());
			if (!hasGottenCard) {
				awardCard(attacker.getName());
			}
			boolean allowReinforce = false;
			try {
				advanceArmies(attacker, atkRsp);
				allowReinforce = true;
			}
			catch (PlayerEliminatedException attackerException) {
				throw attackerException;
			}
			finally {
				checkForElimination(attacker, loserName, atkRsp.getDfdCountry(), allowReinforce);
			}
			return true;
		}
		else {
			return false;
		}
	}
	
	protected void advanceArmies(Player attacker, AttackResponse atkRsp) throws PlayerEliminatedException {
		int attempts = 0;
		boolean valid = false;
		while (!valid && attempts < RiskConstants.MAX_ATTEMPTS && !FXUIGameMaster.fullAppExit) {
			attempts++;
			AdvanceResponse advRsp = tryAdvance(attacker, createCardSetCopy(attacker.getName()), getPlayerCardCounts(), atkRsp);
			if (valid = AdvanceResponse.isValidResponse(advRsp, atkRsp, this.map)) {
				writeLogLn(true, attacker.getName() + " advanced " + advRsp.getNumArmies() + " into " + atkRsp.getDfdCountry() + " from " + atkRsp.getAtkCountry() + ".");
				this.map.addCountryArmies(atkRsp.getAtkCountry(), -1 * advRsp.getNumArmies());
				this.map.addCountryArmies(atkRsp.getDfdCountry(), advRsp.getNumArmies());
			}
		}
		if (crossbar.isHumanEndingGame())
		{
			eliminate(attacker, null, "The advancer decided to take a break. 'S OK. Get some cookies. Or hot cocoa.");
		}
		else if (!valid) {
			eliminate(attacker, null, "You failed to provide a valid advance response.");
		}
	}
	
	protected void awardCard(String playerName) {
		writeLogLn(true, "Awarding " + playerName + " one card.");
		if (this.deck.size() > 0) {
			this.playerCardMap.get(playerName).add(this.deck.removeFirst());
		}
	}
	
	protected void checkForElimination(Player attacker, String loserName, Country takenCountry, boolean allowReinforce) throws PlayerEliminatedException {
		try {
			if (RiskUtils.getPlayerCountries(this.map, loserName).size() == 0) {
				eliminate(getPlayerObject(loserName), attacker, "You were eliminated by " + attacker.getName() + " at " + takenCountry.getName() + ".");
			}
		}
		catch (PlayerEliminatedException defenderException) {
			//this ensures that attacker will not be allowed to reinforce if (s)he was auto-eliminated during the advanceArmies() call or the game ended.
			//also, player can only reinforce after eliminating another player if (s)he is forced to turn in cards
			if (allowReinforce && this.playerCardMap.get(attacker.getName()).size() >= RiskConstants.FORCE_TURN_IN && this.players.size() > 1) {
				reinforce(attacker, false);//note that if the current player fails to reinforce, the player can be eliminated here and an exception thrown back up to begin()
			}
		}
	}
	
	protected void fortify(Player currentPlayer) {
		int attempts = 0;
		boolean valid = false;
		while (!valid && attempts < RiskConstants.MAX_ATTEMPTS && !FXUIGameMaster.fullAppExit) {
			attempts++;
			FortifyResponse rsp = tryFortify(currentPlayer, createCardSetCopy(currentPlayer.getName()), getPlayerCardCounts());
			if (rsp != null) {
				if (valid = FortifyResponse.isValidResponse(rsp, this.map, currentPlayer.getName())) {
					writeLogLn(true, currentPlayer.getName() + " is transferring " + rsp.getNumArmies() + " from " + rsp.getFromCountry() + " to " + rsp.getToCountry() + ".");
					this.map.addCountryArmies(rsp.getFromCountry(), -1 * rsp.getNumArmies());
					this.map.addCountryArmies(rsp.getToCountry(), rsp.getNumArmies());
				}
			}
			else {
				//because fortification is not required, a null response is taken to mean that the player declines the opportunity
				attempts = RiskConstants.MAX_ATTEMPTS;
			}
		}
	}
	
	protected ReinforcementResponse tryInitialAllocation(Player player, int reinforcements){
		try {
			ReinforcementResponse rsp = player.getInitialAllocation(this.map.getReadOnlyCopy(), reinforcements);
			validatePlayerName(player);
			return rsp;
		}
		catch (RuntimeException|PlayerEliminatedException e) {
			//e.printStackTrace();
			return null;
		}
	}
	
	protected CardTurnInResponse tryTurnIn(Player player, Collection<Card> cardSet, Map<String, Integer> oppCards, boolean turnInRequired){
		try {
			CardTurnInResponse rsp = player.proposeTurnIn(this.map.getReadOnlyCopy(), cardSet, oppCards, turnInRequired);
			validatePlayerName(player);
			return rsp;
		}
		catch (RuntimeException|PlayerEliminatedException e) {
			//e.printStackTrace();
			return null;
		}
	}
	
	protected ReinforcementResponse tryReinforce(Player player, Map<String, Integer> oppCards, int reinforcements){
		try {
			ReinforcementResponse rsp = player.reinforce(this.map.getReadOnlyCopy(), createCardSetCopy(player.getName()), oppCards, reinforcements);
			validatePlayerName(player);
			return rsp;
		}
		catch (RuntimeException|PlayerEliminatedException e) {
			//e.printStackTrace();
			return null;
		}
	}
	
	protected AttackResponse tryAttack(Player player, Collection<Card> cardSet, Map<String, Integer> oppCards){
		try {
			AttackResponse rsp = player.attack(this.map.getReadOnlyCopy(), createCardSetCopy(player.getName()), oppCards);
			validatePlayerName(player);
			return rsp;
		}
		catch (RuntimeException|PlayerEliminatedException e) {
			e.printStackTrace();
			System.out.println(ERROR+"tryAttack "+e);
			return null;
		}
	}
	
	protected DefendResponse tryDefend(Player player, Collection<Card> cardSet, Map<String, Integer> oppCards, AttackResponse atkRsp) {
		try {
			DefendResponse rsp = player.defend(this.map.getReadOnlyCopy(), createCardSetCopy(player.getName()), oppCards, atkRsp.getAtkCountry(), atkRsp.getDfdCountry(), atkRsp.getNumDice());
			validatePlayerName(player);
			return rsp;
		}
		catch (RuntimeException|PlayerEliminatedException e) {
			//e.printStackTrace();
			return null;
		}
	}
	
	protected AdvanceResponse tryAdvance(Player player, Collection<Card> cardSet, Map<String, Integer> oppCards, AttackResponse atkRsp){
		try {
			AdvanceResponse rsp = player.advance(this.map.getReadOnlyCopy(), createCardSetCopy(player.getName()), oppCards, atkRsp.getAtkCountry(), atkRsp.getDfdCountry(), atkRsp.getNumDice());
			validatePlayerName(player);
			return rsp;
		}
		catch (RuntimeException|PlayerEliminatedException e) {
			//e.printStackTrace();
			return null;
		}
	}
	
	protected FortifyResponse tryFortify(Player player, Collection<Card> cardSet, Map<String, Integer> oppCards){
		try {
			FortifyResponse rsp = player.fortify(this.map.getReadOnlyCopy(), createCardSetCopy(player.getName()), oppCards);
			validatePlayerName(player);
			return rsp;
		}
		catch (RuntimeException|PlayerEliminatedException e) {
			//e.printStackTrace();
			return null;
		}
	}
	
	protected boolean validateInitialAllocation(Map<Country, Integer> allocation, String playerName, int armies) {
		Map<Country, Boolean> allocatedCheck = new HashMap<Country, Boolean>();
		for (Country country : RiskUtils.getPlayerCountries(this.map, playerName)) {
			allocatedCheck.put(country, false);
		}
		for (Country country : allocation.keySet()) {
			if (!allocatedCheck.containsKey(country)) {
				return false;
			}
			else if (allocation.get(country) < 1) {
				return false;
			}
			else {
				allocatedCheck.put(country, true);
				armies -= allocation.get(country);
			}
		}
		for (Boolean check : allocatedCheck.values()) {
			if (!check) {
				return false;
			}
		}
		return armies == 0;
	}
	
	protected void allocateArmies(String playerName, Map<Country, Integer> allocation, int reinforcements) {
		writeLogLn(true, playerName + " reinforcing with " + reinforcements + " armies.");
		for (Map.Entry<Country, Integer> entry : allocation.entrySet()) {
			this.map.setCountryArmies(entry.getKey(), entry.getValue());
			writeLogLn(true, entry.getValue() + " " + entry.getKey().getName());
		}
		writeLogLn(true, EVENT_DELIM);
	}
	
	protected int getCardTurnIn(Player currentPlayer, Map<String, Integer> oppCards) throws PlayerEliminatedException {
		int cardBonus = 0;
		int attempts = 0;
		boolean valid = false;
		boolean turnInRequired = oppCards.get(currentPlayer.getName()) >= RiskConstants.FORCE_TURN_IN;
		while (!valid && attempts < RiskConstants.MAX_ATTEMPTS && !FXUIGameMaster.fullAppExit) {
			CardTurnInResponse rsp = tryTurnIn(currentPlayer, createCardSetCopy(currentPlayer.getName()), oppCards, turnInRequired);
			if (rsp != null) {
				if (valid = CardTurnInResponse.isValidResponse(rsp, this.playerCardMap.get(currentPlayer.getName()))) {
					cardBonus = RiskConstants.advanceTurnIn();
					writeLogLn(true, currentPlayer.getName() + " turned in cards for " + cardBonus + " additional reinforcements!");
					if (rsp.getBonusCountry() != null) {
						if (this.map.getCountryOwner(rsp.getBonusCountry()).equals(currentPlayer.getName())) {
							this.map.addCountryArmies(rsp.getBonusCountry(), RiskConstants.BONUS_COUNTRY_ARMIES);
						}
					}
					for (Card card : rsp.getCards()) {
						this.playerCardMap.get(currentPlayer.getName()).remove(card);
						this.deck.addLast(card);
					}
				}
			}
			else {
				//if a turn-in is not required, a null response is taken as the player declining
				valid = !turnInRequired;
			}
			attempts++;
		}
		if (!valid && turnInRequired) {
			eliminate(currentPlayer, null, "You were required to turn in cards this turn, and you failed to do so.");
		}
		else if(crossbar.isHumanEndingGame()){
			eliminate(currentPlayer, null, "The player is opting out of the game altogether. Have a good day, buddy.");
		}
		return cardBonus;
	}
	
	protected Collection<Card> createCardSetCopy(String playerName) {
		Collection<Card> copy = new ArrayList<Card>();
		for (Card card : this.playerCardMap.get(playerName)) {
			copy.add(new Card(card.getType(), card.getCountry()));
		}
		return copy;
	}
	
	protected Player getOwnerObject(Country country) {
		String playerName = this.map.getCountryOwner(country);
		return getPlayerObject(playerName);
	}
	
	protected Player getPlayerObject(String playerName) {
		for (Player player : this.playerMap.values()) {
			if (player.getName().equals(playerName)) {
				return player;
			}
		}
		return null;
	}
	
	protected Map<String, Integer> getPlayerCardCounts() {
		Map<String, Integer> playerCardCounts = new HashMap<String, Integer>();
		for (String playerName : this.playerMap.keySet()) {
			playerCardCounts.put(playerName, this.playerCardMap.get(playerName).size());
		}
		return playerCardCounts;
	}
	
	protected void loadDeck() {
		writeLogLn(true, "Building deck...");
		List<Card> newDeck = new ArrayList<Card>();
		int i = 0;
		for (Country country : Country.values()) {
			newDeck.add(new Card(RiskConstants.REG_CARD_TYPES[i % RiskConstants.REG_CARD_TYPES.length], country));
			i++;
		}
		for (i = 0; i < RiskConstants.NUM_WILD_CARDS; i++) {
			newDeck.add(new Card(RiskConstants.WILD_CARD, null));
		}
		shuffleCards(newDeck);
		this.deck = new LinkedList<Card>(newDeck);
	}
	
	protected void shuffleCards(List<Card> cardList) {
		int j;
		Card temp;
		for (int i = 0; i < 2 * cardList.size(); i++) {
			j = rand.nextInt(cardList.size());
			temp = cardList.get(i % cardList.size());
			cardList.set(i % cardList.size(), cardList.get(j));
			cardList.set(j, temp);
		}
	}
	
	protected boolean loadPlayers(String players) {
		if(workingMode == LOADED_GAME_MODE){
			return false;
		}
		writeLogLn(true, "Loading players...");
		this.playerMap = new HashMap<String, Player>();
		if (players == null) {
			players = RiskConstants.DEFAULT_PLAYERS;
		}
		
		List<Player> playerList = PlayerFactory.getPlayersFromString(players);

		FXUIPlayer.setCrossbar(FXUIGameMaster.crossbar);
		
		for (Player player : playerList) {
			this.playerMap.put(player.getName(), player);
		}
		
		this.players = new ArrayList<String>(this.playerMap.keySet());
		this.allPlayers = new ArrayList<String>(this.playerMap.keySet());
		
		shufflePlayers(this.players);//choose a random turn order
		
		this.playerCardMap = new HashMap<String, Collection<Card>>();
		
		for (Player player : this.playerMap.values()) {
			this.playerCardMap.put(player.getName(), new ArrayList<Card>());
		}
		
		if (this.players.size() < RiskConstants.MIN_PLAYERS || this.players.size() > RiskConstants.MAX_PLAYERS) {
			return false;
		}
		else {
			writeLogLn(true, "Players:");
			for (String playerName : this.players) {
				writeLogLn(true, playerName);
			}
			writeLogLn(true, EVENT_DELIM);
			return true;
		}
	}
	
	protected void shufflePlayers(List<String> playerList) {
		int j;
		String temp;
		for (int i = 0; i < 2 * playerList.size(); i++) {
			j = rand.nextInt(playerList.size());
			temp = playerList.get(i % playerList.size());
			playerList.set(i % playerList.size(), playerList.get(j));
			playerList.set(j, temp);
		}
	}
	
	//only allocates unowned countries
	protected void allocateUnownedCountries() {
		if (this.players.size() > 0) {
			writeLogLn(true, "Re-allocating eliminated player's countries...");
			for (Country country : Country.values()) {
				if (map.getCountryOwner(country) == null) {
					map.setCountryOwner(country, this.playerMap.get(this.players.get(allocationIdx % this.players.size())).getName());
					if (this.round > 0) {
						//If these countries are being eliminated during a game,
						//it is due to a player being eliminated by the Master,
						//and so the re-allocated countries must be occupied.
						map.setCountryArmies(country, 1);
					}
					allocationIdx++;
				}
			}
		}
	}
	
	//allocates ALL countries on map
	protected void allocateMap() {
		if (this.players.size() > 0) {
			writeLogLn(true, "Allocating countries...");
			for (Card card : this.deck) {
				if (!card.getType().equals(RiskConstants.WILD_CARD)) {
					map.setCountryOwner(card.getCountry(), this.playerMap.get(this.players.get(allocationIdx % this.players.size())).getName());
					allocationIdx++;
				}
			}
		}
	}
	
	protected void eliminate(Player loser, Player eliminator, String reason) throws PlayerEliminatedException {
		if (this.playerMap.containsKey(loser.getName())) {
			writeLogLn(true, loser.getName() + " Eliminated! " + reason);
			System.out.println(loser.getName() + " Eliminated! " + reason);
			for (Country country : Country.values()) {
				if (map.getCountryOwner(country).equals(loser.getName())) {
					if (eliminator != null) {
						map.setCountryOwner(country, eliminator.getName());
					}
					else {
						map.setCountryOwner(country, null);
					}
				}
			}
			if (eliminator != null) {
				for (Card card : this.playerCardMap.get(loser.getName())) {
					this.playerCardMap.get(eliminator.getName()).add(card);
				}
				this.playerCardMap.get(loser.getName()).clear();
			}
			this.players.remove(loser.getName());
			this.playerMap.remove(loser.getName());
			allocateUnownedCountries();
			throw new PlayerEliminatedException(loser.getName() + " Eliminated! " + reason);
		}
		else{
			System.out.println(WARN+"eliminate() :: this.playerMap does not contain so-called 'loser'");
		}
	}
	
	protected void writeLogLn(boolean mirrorToInternalCache, String line) {
		if(mirrorToInternalCache){
			internalLogCache.add(line);
		}
		if (this.log != null) {
			try {
				this.log.write(line + "\r\n");
				this.log.flush();
			}
			catch (IOException e) {
				System.out.println(ERROR+"Error writing log: " + e);
			}
		}
	}
	
	protected void writeStatsLn() {
		if (this.stats != null) {
			try {
				stats.write(this.turnCount + " " + this.round + " ");
				for (String playerName : this.players) {
					//count player's countries
					stats.write(RiskUtils.getPlayerCountries(this.map, playerName).size() + " ");
					//count player's armies
					stats.write(RiskUtils.countPlayerArmies(this.map, playerName, null) + " ");
				}
				stats.write("\r\n");
				this.stats.flush();
			}
			catch (IOException e) {
				System.out.println(ERROR+"Error writing statistics: " + e);
			}
		}
	}
	
	/**
	 * Used to show list of players at top edge of UI.
	 * Due to threading requirements, currently wraps up a secondary method to accomplish this task.
	 */
	private void representPlayersOnUI() {
		Platform.runLater(new Runnable()
		{
			@Override public void run(){
				representPlayersSubroutine();
			}
		});
	}
	
	/**
	 * Helper method for representPlayersOnUI.
	 */
	private void representPlayersSubroutine(){
		try {
			//clears the old display of players
			if(this.playerDisplay != null){
				FXUIGameMaster.pane.getChildren().remove(this.playerDisplay);
			}
			
			ArrayList<Color> colors = new ArrayList<Color>();
			colors.add(Color.WHITE);
			colors.add(Color.AQUA);
			colors.add(Color.RED);
			colors.add(Color.GREENYELLOW);
			colors.add(Color.CORAL);
			colors.add(Color.VIOLET);
			this.playerColorMap = new HashMap<String, Color>();
			int i = -1;
			
			HBox namesOfPlayers = new HBox(40);
			namesOfPlayers.setLayoutX(50);
			namesOfPlayers.setLayoutY(5);
			
			if (this.playerMap == null || this.playerMap.size() == 0){
				System.out.println(ERROR+"Player map not populated; please fix logic!");
			}
			for (String playerName : this.playerMap.keySet())
			{
				this.playerColorMap.put(playerName, colors.get(++i % colors.size()));
				Text txt = new Text("#"+playerName.toLowerCase());
				txt.setFont(Font.font("Verdana", FontWeight.THIN, 20));
				txt.setFill(colors.get((i) % colors.size()));
                                txt.setOnMouseClicked(new EventHandler<MouseEvent>(){
                                        @Override public void handle(MouseEvent t){
                                                if(currentPlayer!=null){
                                                        flashPlayerCountries(playerName);
                                                }
                                        }
                                });
				namesOfPlayers.getChildren().add(txt);
			}
			FXUIGameMaster.pane.getChildren().add(namesOfPlayers);
			this.playerDisplay = namesOfPlayers;
		}
		catch (RuntimeException e) {
		}
	}
	
	
	public static void main(String[] args) throws IOException {
		launch(FXUIGameMaster.class, args);
		if(FXUIGameMaster.priGameLogicThread != null){
			try {
				System.out.println("Full exit?");
				FXUIGameMaster.priGameLogicThread.join();
				System.out.println("Full exit.");
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
	/**
	* Creates a secondary thread to run game logic (to allow blocking without blocking the UI.
	* Relies on the caller to detect if a game is already in progress.
	* @return returns "true" so long as the game thread was started successfully; "false" otherwise.
	*/
	public boolean createGameLogicThread(){
		
		setButtonAvailability();
		Runnable gameCode = new Runnable()
		{
			@Override public void run(){
				runGameAndDisplayVictor();
				priGameLogicThread = null;
			}
		};
		Thread gameThread = new Thread(gameCode);
		gameThread.setDaemon(true);
		gameThread.start();
		if (gameThread.isAlive()){
			FXUIGameMaster.priGameLogicThread = gameThread;
			return true;
		}
		else{
			return false;
		}
	}
	
	public void runGameAndDisplayVictor(){
		try {
			// TODO add support for selecting types of players, which is set during call to initializeFXGMClass
			HashMap<String, Integer> winLog = new HashMap<String, Integer>();
			RiskConstants.SEED = 1;
			int numGames = 1;
			for (int i = 0; i < numGames; i++) {
				RiskConstants.resetTurnIn();
				PlayerFactory.resetPlayerCounts();
				boolean doWeLog = (!forceLoggingIsIndeterminate && forceEnableLogging) || (forceLoggingIsIndeterminate && i == numGames - 1 ? LOGGING_ON : LOGGING_OFF);
				initializeFXGMClass("Countries.txt", RiskConstants.DEFAULT_PLAYERS + "," + PlayerFactory.FXUIAsk, doWeLog);
				
				System.out.print((i + 1) + " - ");
				
				String victor = begin();
				
				System.out.println(INFO+"Hi user!!! game execute was successfully!!!!"); //yes very successfully!!!!
				if (victor != null)
				{
					setPlayStatus("Victor: " + victor);
					if (!winLog.containsKey(victor)) {
						winLog.put(victor, 0);
					}
					winLog.put(victor, winLog.get(victor) + 1);
				}
			}
			if(!FXUIGameMaster.fullAppExit){
				for (Map.Entry<String, Integer> entry : winLog.entrySet()) {
					System.out.println(entry.getKey() + " had a win percentage of " + 100.0 * entry.getValue() / numGames + "%");
				}
			}
		}
		catch (RuntimeException e) {
			System.out.println(ERROR+"low-level game runtime error:: " + e);
			e.printStackTrace();
		}	
	}
	
	/**
	* Does a tiny bit of initialization on the map's internal structures, without setting
	* up/displaying players or other user-facing info.
	* Replaces GameMaster() from the original GameMaster class, since JavaFX would otherwise leave it unused.
	* @param mapFile
	* @param playerFile
	* @param logSwitch
	* @throws IOException
	*/
	public void initializeFXGMClass(String mapFile, String players, boolean logSwitch){
		for (Country country : Country.values()) {
			stringCountryRepresentation.put(country.getName(), country);
		}
		
		this.round = 0;
		this.turnCount = 0;
		if (rand == null) {
			rand = new Random(RiskConstants.SEED);
		}
		
		FXUIGameMaster.savePoint = new SavePoint();
		
		FXUIGameMaster.loggingEnabled = logSwitch;
		if (FXUIGameMaster.loggingEnabled == LOGGING_ON) {
			System.out.println(INFO+"Trying to enable logging...");
			try{
				this.stats = new FileWriter(STATSFILE);
				this.log = new FileWriter(LOGFILE);
				System.out.println(INFO+"Logging enabled!");
			}
			catch(IOException e){
				FXUIGameMaster.loggingEnabled = LOGGING_OFF;
				setErrorStatus("log disabled (I/O error)");
				System.out.println(WARN+"Logging DISABLED!");
			}
		}
		else{
			System.out.println(INFO+"Logging disabled.");
			this.log = null;
			this.stats = null;
		}
		
		writeLogLn(true, "Loading map from " + mapFile + "...");
		if (starterMap == null) {
			starterMap = new RiskMap();
		}
		
		this.map = starterMap.getCopy();
		loadDeck();
		FXUIGameMaster.desiredPlayersForGame = players;
	}
	
	/**
	 * Populates the UI with the countries using predefined window locations.
	 * @param nodeFile
	 */
	private void loadTextNodesForUI(String nodeFile) {
		try {
			if (nodeFile != null) {
				this.textNodeMap = new HashMap<String, Text>();
				File fileRepresentation = new File(nodeFile);
				//basic check for existence of country list file
				if (!fileRepresentation.exists()){
					Scanner reader = new Scanner(TextNodes.nodes);
					while (reader.hasNext()) {
						int nextX = reader.nextInt();
						int nextY = reader.nextInt();
						String nextCountry = reader.nextLine().trim();
						Text txt = new Text(nextX, nextY, nextCountry + "\n0");
						txt.setFont(Font.font("Verdana", FontWeight.BOLD, 20));
						this.textNodeMap.put(nextCountry, txt);
						FXUIGameMaster.pane.getChildren().add(txt);
					}
					reader.close();
				}
				else{
					Scanner reader = new Scanner(fileRepresentation);
					while (reader.hasNext()) {
						int nextX = reader.nextInt();
						int nextY = reader.nextInt();
						String nextCountry = reader.nextLine().trim();
						Text txt = new Text(nextX, nextY, nextCountry + "\n0");
						txt.setFont(Font.font("Verdana", FontWeight.BOLD, 20));
						this.textNodeMap.put(nextCountry, txt);
						FXUIGameMaster.pane.getChildren().add(txt);
					}
					reader.close();
				}
			}
		}
		catch (RuntimeException e) {
			setErrorStatus(e.getMessage());
		} catch (FileNotFoundException e) {
			setErrorStatus(e.getMessage());
			e.printStackTrace();
		}
	}
	
	//and so begins the complex setup to create a stepping refresh cycle that updates elements one by one
	//it's to be on a clock, so an exterior thread is to be used, and you must then have that thread refer back to the JavaFX thread
	// with code in the appropriate places in a loop...
	// so sorry 'bout it.
	
	/**
	* Main entry method to prompt a refresh of the countries and their counts in the main FXUI window.
	* Has no bearing on the secondary dialogs.
	* If the game is being "exited", or the current player is not a "human", the refresh is as instantaneous as possible.
	* Else, internal decision logic will request a slower refresh, to provide for more easily detected changes.
	* @param guaranteeRefresh if "true", will refresh (with a dynamic decision on if it's instant or staggered updating). If "false",
	* 		will skip updating altogether for the majority of the time.
	*/
	public void refreshUIElements(boolean guaranteeRefresh)
	{	
		if (this.players == null || this.players.size() == 0){
			System.out.println(INFO+"FXUIGM - Player count mismatch; Delaying country refresh...");
			return;
		}
		if(this.playerColorMap == null || this.playerColorMap.size() == 0){
			System.out.println(INFO+"FXUIGM - PlayerColorMap size mismatch; Delaying country refresh...");
			return;
		}
		if (!guaranteeRefresh && this.round % this.players.size() != 0) //just...just don't update too often. Skip this once.
		{
			return;
		}
		
		//else, decide dynamically...
		else if (FXUIGameMaster.endGame || FXUIGameMaster.currentPlayer == null
				|| !FXUIGameMaster.currentPlayer.getClass().toString().equals(FXUIPlayer.class.toString()))
		{
			createClockedRefreshCycle(0);
		}
		else
		{
			Runnable clockedUIRefreshTask = new Runnable()
			{
				@Override public void run(){ createClockedRefreshCycle(30); }
			};
			Thread clockedUIRefreshThread = new Thread(clockedUIRefreshTask);
			clockedUIRefreshThread.setDaemon(true);
			clockedUIRefreshThread.start();
		}
	}
	
	/**
	* Creates a clocked refresh cycle -- that is, automatically updates each country's status in the main window
	* one after the other, separated by a pause of some time (passed in as a parameter to the method).
	* @param timeToWaitBetweenElements the time -- in ns -- to pause between the update of any two elements. (only effective > 0)
	*/
	private void createClockedRefreshCycle(int timeToWaitBetweenElements)
	{
		for (Country country : Country.values())
		{
			//the setup to actually update the status of the current country for the current player
			Platform.runLater(new Runnable()
			{
				@Override public void run(){
					performStepOfRefreshProcess(country);
				} 
			});
			//and the sleep to create the pause between updates...
			if(timeToWaitBetweenElements > 0){
				RiskUtils.sleep(timeToWaitBetweenElements);
			}
			if(FXUIGameMaster.endGame){
				break;
			}
		}
	}
	
	/**
	* As part of the clocked refresh cycle, updates the visual state of a singular country, passed in as a param.
	* @param country the country whose data is to be refreshed on the main map.
	*/
	private void performStepOfRefreshProcess(Country country){
		this.textNodeMap.get(country.getName()).setFill(this.playerColorMap.get(this.map.getCountryOwner(country)));
		this.textNodeMap.get(country.getName()).setText(country.getName() + "\n" + this.map.getCountryArmies(country));
	}
	
	/**
	 * Used to help highlight a given player's countries on the main UI.
	 * Does each country individually, then all countries at once. Repeats ??? times.
	 * @param playerName the String representation of the player in question.
	 */
	private void flashPlayerCountries(String playerName){
		final Set<Country> myCountries = RiskUtils.getPlayerCountries(map, playerName);
		final long timeDeltaMS = 220;
		final int totalCycles = 2;
		Runnable clockedBlinkTask = new Runnable()
		{
			@Override public void run()
			{
				for (int i = 0; i < totalCycles && !FXUIGameMaster.endGame; i++)
				{
					/*
					 * Do all countries at once. Relatively speaking, anyway.
					 */
					for(int kLoop = 0; kLoop < totalCycles; kLoop++){ //all countries at once
						singleBlinkTypeB(0.25d, timeDeltaMS, myCountries);
						if(FXUIGameMaster.endGame){break;}
						singleBlinkTypeB(0.5d, timeDeltaMS, myCountries);
						if(FXUIGameMaster.endGame){break;}
						singleBlinkTypeB(0.75d, timeDeltaMS, myCountries);
						if(FXUIGameMaster.endGame){break;}
						singleBlinkTypeB(1.0d, timeDeltaMS, myCountries);
						if(FXUIGameMaster.endGame){break;}
						singleBlinkTypeB(0.75d, timeDeltaMS, myCountries);
					}
					
					/*
					 * If we should stop the animation: reset the display, then skip the second half of the animation...
					 */
					if(FXUIGameMaster.endGame){
						singleBlinkTypeB(1.0d, timeDeltaMS, myCountries);
						break;
					}
					
					/*
					 * Then do each country individually...
					 */
					for (Country country : myCountries){ //each country separately
						for(int nLoop = 0; nLoop < totalCycles-1 && !FXUIGameMaster.endGame; nLoop++){
							singleBlinkTypeA(0.1d, timeDeltaMS, country, Color.BISQUE);
							singleBlinkTypeA(0.5d, timeDeltaMS, country, Color.DARKSALMON);
							singleBlinkTypeA(1.0d, timeDeltaMS, country, null);
						}
						
					}
					
				}
			}
		};
		Thread clockedBlinkThread = new Thread(clockedBlinkTask);
		clockedBlinkThread.setDaemon(true);
		clockedBlinkThread.start();
	}
	
	/**
	 * Sets the opacity and outline color for one (1) given country 
	 * to the given input values (1 opacity setting, one outline color) after a sleep of some milliseconds.
	 * @param opacitySetting target opacity setting, as a double. Range: from 0.0 to 1.0.
	 * @param timeDeltaMS time to wait before switching opacity and outline values, in milliseconds.
	 * @param country single country whose opacity and outline color is to be changed.
	 * @param colorValue target color value for the single country
	 */
	private void singleBlinkTypeA(double opacitySetting, long timeDeltaMS, Country country, Color colorValue){
		RiskUtils.sleep(timeDeltaMS);
		Platform.runLater(new Runnable(){
			@Override public void run(){
				textNodeMap.get(country.getName()).setOpacity(opacitySetting);
				textNodeMap.get(country.getName()).setStroke(colorValue);
			} 
		});
	}
	
	/**
	 * Sets the opacity value for multiple (>=1) countries to one (1) input value after a wait/sleep of some milliseconds.
	 * @param opacitySetting target opacity setting for the countries' labels, as a double. Range: from 0.0 to 1.0
	 * @param timeDeltaMS time to wait before switching opacity values, in milliseconds
	 * @param myCountries countries on the UI to update
	 */
	private void singleBlinkTypeB(double opacitySetting, long timeDeltaMS, Set<Country> myCountries){
		RiskUtils.sleep(timeDeltaMS);
		for (Country country : myCountries){
			if(FXUIGameMaster.endGame){
				break;
			}
			Platform.runLater(new Runnable(){
				@Override public void run(){
					textNodeMap.get(country.getName()).setOpacity(opacitySetting);
				} 
			});
		}
	}
	
	
	/**
	* Get your life in the form of a game!
	* This method is the point at which the JavaFX items are populated on the main map screen.
	* 
	* Extra dialogs are further prompted elsewhere, depending on their needs/uses.
	* (In this file is the method to present an exit confirmation dialog, as is the class representing the About dialog.)
	*/
	@Override
	public void start(final Stage primaryStage) throws Exception {
		final About nAbout = new About();
		
		double widthOfPriScreen = Screen.getPrimary().getVisualBounds().getWidth() - 5;
		double heightOfPriScreen = Screen.getPrimary().getVisualBounds().getHeight() - 25;
		System.out.println("(info)Width first set: " + widthOfPriScreen + " :: Height first set: " + heightOfPriScreen);
		
		pane = new Pane();
		pane.setPrefSize(DEFAULT_APP_WIDTH, DEFAULT_APP_HEIGHT);
		pane.setStyle("-fx-background-color: blue");
		/*We set the image in the pane based on whether there was an error or not.
		* If there was an error, it'll be changed later.*/
		
		//Facilitate checking for errors...
		errorTextInitialContents = "currently";
		
		ImageView backgroundImg = new ImageView();

		Image imageOK = new Image("RiskBoard.jpg", true);
		backgroundImg.setImage(imageOK);
		pane.getChildren().add(backgroundImg);
		
		//populate the countries
		loadTextNodesForUI("TextNodes.txt");
		
		//now display elements -- status and buttons!
		subStatusTextElement = new Text(errorTextInitialContents);
		subStatusTextElement.setFill(Color.WHITE);
		subStatusTextElement.setFont(Font.font("Verdana", FontWeight.THIN, 20));
		
		//The vertical box to contain the major buttons and status.
		VBox primaryStatusButtonPanel = new VBox(10);
		HBox lowerButtonPanel = new HBox(15);
		
		primaryStatusButtonPanel.setAlignment(Pos.CENTER_LEFT);
		primaryStatusButtonPanel.setLayoutX(29);
		primaryStatusButtonPanel.setLayoutY(525);
		primaryStatusButtonPanel.getChildren().add(subStatusTextElement);
		
		
		mainStatusTextElement = new Text("ready to play!");
		mainStatusTextElement.setFont(Font.font("Verdana", FontWeight.NORMAL, 24));
		mainStatusTextElement.setFill(Color.WHITE);
		
		
		//End the current game, but don't close the program.
		Button stopGameBtn = new Button("Bow out.\n(End current game)");
		stopGameBtn.setOnAction(new EventHandler<ActionEvent>(){
			@Override public void handle(ActionEvent t){
				Platform.runLater(new Runnable()
				{
					@Override
					public void run() {
						//crossbar.signalHumanEndingGame();
						crossbar.tryCloseCurrentPlayerDialog();
						//FXUIGameMaster.endGame = true;
					}
				});
			}
		});
		
		//Button to initiate the game
		Button startBtn = new Button("Let's go!!\n(Start/Load game)");
		startBtn.setOnAction(new EventHandler<ActionEvent>(){
			@Override public void handle(ActionEvent t){
				if(workingMode == IDLE_MODE)
				{
					int stateOut = displayGameSelector();
					if(stateOut != IDLE_MODE){
						createGameLogicThread();
					}
				}
			}
			
		});
		
		//your standard About buttons...
		Button tellMe = new Button("About");
		tellMe.setOnAction(new EventHandler<ActionEvent>(){
			@Override public void handle(ActionEvent t){
				nAbout.launch(pane.getScene().getWindow(), false);
			}
		});
		
		//...I said "About buttons". Plural. Yep.	
		Button tellMe2 = new Button("more.");
		tellMe2.setOnAction(new EventHandler<ActionEvent>(){
			@Override public void handle(ActionEvent t){
				nAbout.more(pane.getScene().getWindow());
			}
		});
		
		//Attempt to force a manual game save, when allowed
		Button saveMe = new Button("save game as...");
		saveMe.setTooltip(new Tooltip("Changes the location where your game is being auto-saved"
				+ "\nAND IMMEDIATELY saves to that new location!"));
		saveMe.setOnAction(new EventHandler<ActionEvent>(){
			@Override public void handle(ActionEvent t){
				if(performSave(true))
				{
					saveMe.getTooltip().setText("saved. autosave set to " + saveto_filename
							+ ".\n\nChanges the location where your game is being auto-saved"
							+ "\nAND IMMEDIATELY saves to that new location!");
				}
				else{
					saveMe.getTooltip().setText("save failed; try again???"
							+ "\n\nChanges the location where your game is being auto-saved"
							+ "\nAND IMMEDIATELY saves to that new location!");
				}
			}
		});
		saveMe.setDisable(true);
		
		
		Button logPlayback = new Button("open log player.");
		logPlayback.setOnAction(new EventHandler<ActionEvent>(){
			@Override public void handle(ActionEvent t){
				Platform.runLater(new Runnable(){
					@Override public void run(){
						LogPlayer.setAsLaunchedFromFXUIGM();
						new LogPlayer().start(new Stage());
					} 
				});
			}
		});
		
		
		
		//Exit the application entirely
		Button exitApp = new Button("Lights out!\n(Exit to desktop)");
		exitApp.setOnAction(new EventHandler<ActionEvent>(){
			@Override public void handle(ActionEvent t){
				tryToExit(primaryStage);
			}
		});
		
		CheckBox doLogging = new CheckBox("Enable logging?\nauto (yes)");
		doLogging.setTooltip(new Tooltip("YES: Always log (each game overwrites the log of the last game for normal games)\n"
				+ "NO: Never log (whatever log file exists will remain untouched)\n"
				+ "INDETERMINATE/AUTO: Effectively YES, unless redefined elsewhere.\n"
				+ "For game simulations (when available): enabling (setting to YES) may result in a flood of logs!"));
		doLogging.setTextFill(Color.ANTIQUEWHITE);
		doLogging.setOnAction(new EventHandler<ActionEvent>(){
			@Override public void handle(ActionEvent t){
				if(doLogging.isIndeterminate()){
					//tell it to do whatever it wants by default
					forceEnableLogging = false;
					forceLoggingIsIndeterminate = true;
					doLogging.setText("Enable logging?\nauto (yes)");
				}
				else if(doLogging.isSelected()){
					//tell it to enable logging
					forceEnableLogging = true;
					forceLoggingIsIndeterminate = false;
					doLogging.setText("Enable logging?\n" + (FXUIGameMaster.loggingEnabled ? "Yes" : "No"));
				}
				else if(!doLogging.isSelected()){
					//tell it to forcefully disable logging
					forceEnableLogging = false;
					forceLoggingIsIndeterminate = false;
					doLogging.setText("Enable logging?\n" + (FXUIGameMaster.loggingEnabled ? "Yes" : "No"));
				}
			}
		});
		doLogging.setIndeterminate(true);
		
		Button flashCurrCountries = new Button("HIGHLIGHT current player's countries");
		flashCurrCountries.setDisable(true);
		flashCurrCountries.setLayoutX(25);
		flashCurrCountries.setLayoutY(35);
		flashCurrCountries.setOnAction(new EventHandler<ActionEvent>(){
			@Override public void handle(ActionEvent t){
				if(currentPlayer!=null){
					flashPlayerCountries(currentPlayer.getName());
				}
			}
		});
		
		pane.setOnKeyPressed(new EventHandler<KeyEvent>(){
			@Override public void handle(KeyEvent t){
				if(workingMode == IDLE_MODE)
				{
					workingMode = FXUIGameMaster.NEW_GAME_MODE;
					createGameLogicThread();
				}
			}
		});
		
		lowerButtonPanel.getChildren().addAll(tellMe, tellMe2);
		primaryStatusButtonPanel.getChildren().addAll(mainStatusTextElement,startBtn,
								stopGameBtn,exitApp,lowerButtonPanel, saveMe, doLogging, logPlayback);
		//****layout of text & buttons displayed upon launch ends here.***
		
		pane.getChildren().addAll(flashCurrCountries, primaryStatusButtonPanel);
		

		// DEFAULT_APP_WIDTH, DEFAULT_APP_HEIGHT);
		scene = new Scene(pane,widthOfPriScreen, heightOfPriScreen);
		
		
		//one more tweak to perform if there was -no- error
		scene.widthProperty().addListener(new ChangeListener<Number>() {
			@Override public void changed(ObservableValue<? extends Number> observableValue, Number oldSceneWidth, Number newSceneWidth) {
				//System.out.println("Width: " + newSceneWidth);
				resize(null);
			}
		});
		scene.heightProperty().addListener(new ChangeListener<Number>() {
			@Override public void changed(ObservableValue<? extends Number> observableValue, Number oldSceneHeight, Number newSceneHeight) {
				//System.out.println("Height: " + newSceneHeight);
				resize(null);
			}
		});
		
		//Add buttons to an array, to allow easy enable/disable depending on state.
		//Use the ENUM table "ButtonIndex" to access elements in the array -- and set the targeted capacity.***
		buttonCache = new ArrayList<Node>(ButtonIndex.values().length);
		for (int loopIdx = 0; loopIdx < ButtonIndex.values().length; ++loopIdx){
			buttonCache.add(null); //necessary to actually create the slots in the array
		}
		//this presentation preferred to indicate importance of enumeration in ButtonIndex
		//in alternative setups, you could merely do "buttonCache.add(element)" for each individual object.
		buttonCache.set(ButtonIndex.BTN_START.ordinal(), startBtn);
		buttonCache.set(ButtonIndex.BTN_SAVE.ordinal(), saveMe);
		buttonCache.set(ButtonIndex.BTN_HIGHLIGHT.ordinal(), flashCurrCountries);
		buttonCache.set(ButtonIndex.CKBX_LOGGING.ordinal(), doLogging);
		buttonCache.set(ButtonIndex.BTN_LOG_PLAYBACK.ordinal(), logPlayback);
		
		//Get the primary window showin', already!
		resize(primaryStage);
		primaryStage.setTitle("RISK!");
		primaryStage.setScene(scene);
		primaryStage.show();
		
		
		//go ahead and launch the "About" window, and tell it to autohide -- time until autohide set via the "About" class.
		nAbout.launch(pane.getScene().getWindow(), true);
		
		//In case FXUIPlayer dialogs need help with positioning, tell them where to get it.
		FXUIPlayer.setOwnerWindow(pane.getScene().getWindow()); //applies to all human player(s), so now made static.
		
		//Help control what happens when the user tries to exit by telling the app...what...to do.
		//In this case, we're telling it "Yes, we're trying to exit from the main window, so display the appropriate dialog.
		//That's what this single boolean does.
		scene.getWindow().setOnCloseRequest(new EventHandler<WindowEvent>(){
			@Override
			public void handle(WindowEvent t)
			{
				tryToExit(primaryStage);
			}
		});
	}
	
	/**
	 * Used to assist in resizing of the primary window at launch as well as after launch.
	 * Attempts to keep the proper aspect ratio of the contents of the main window.
	 * Has no effect on secondary dialogs.
	 * (Note: use of this method after launch does not snap the OS window bounds to the contents
	 * of the window when the aspect ratio is different).
	 * @param stageIn Stage object which represents the contents of the window to be resized.
	 */
	private void resize(Stage stageIn)
	{
		double currLiveRatio = scene.getWidth()/scene.getHeight();
		double targetRatio = (double)(DEFAULT_APP_WIDTH)/(double)(DEFAULT_APP_HEIGHT);
		double newWidth = 0;
		double newHeight = 0;
		
		
		if (currLiveRatio <= targetRatio) //wider than high; limit by height
		{
			newWidth = scene.getWidth();
			newHeight = scene.getWidth() / targetRatio;
			Scale scale = new Scale(newWidth/(DEFAULT_APP_WIDTH), newWidth/(DEFAULT_APP_WIDTH));
			scale.setPivotX(0);
			scale.setPivotY(0);
			scene.getRoot().getTransforms().setAll(scale);
			if(stageIn != null){
				stageIn.setHeight(newHeight);
			}

		}
		else //higher than wide; limit by width
		{
			newHeight = scene.getHeight();
			newWidth = scene.getHeight() * targetRatio;
			Scale scale = new Scale(newHeight/(DEFAULT_APP_HEIGHT), newHeight/(DEFAULT_APP_HEIGHT));
			scale.setPivotX(0);
			scale.setPivotY(0);
			scene.getRoot().getTransforms().setAll(scale);
			if(stageIn != null){
				stageIn.setWidth(newWidth);
				
			}
			
		}
	}
	
	/**
	 * Used to attempt a clean exit of the application.
	 * As of right now, we *must* show the confirmation dialog,
	 * as it triggers a cleanup of active windows and such to allow
	 * the game logic thread to end gracefully (i.e., not interrupt them).
	 */
	private void tryToExit(Stage primaryStage){
		FXUIGameMaster.fullAppExit = true;
		crossbar.signalHumanEndingGame();
		crossbar.tryCloseCurrentPlayerDialog();
		FXUIGameMaster.endGame = true;
		RiskUtils.sleep(500); //Singular use on the FX thread.
		primaryStage.hide();
		doYouWantToMakeAnExit(true, 0);
		primaryStage.close();
	}
	
} //end of main FXUIGameMaster class

/**
* Handles the "about" and "more" dialog windows
* @author wallace162x11
*
*/
class About {
	private static boolean firstLaunch = true;
	About(){
	}
	
	public void launch(Window owner, boolean autoExit) {
		final Stage dialog = new Stage();

		dialog.setTitle("Hi, friend. :D");
		dialog.initOwner(owner);
		//dialog.initStyle(StageStyle.UTILITY);
		//dialog.initModality(Modality.WINDOW_MODAL);
		dialog.setX(owner.getX());
		dialog.setY(owner.getY() + 300);

		final Text info1= new Text();
		info1.setText(":::\n\nRISK!\nor\nconquest of the modern Mercator");
		info1.setTextAlignment(TextAlignment.CENTER);
		info1.setFont(Font.font("Arial", FontWeight.THIN, 16));
		final Text info2= new Text();
		info2.setText("\n\nJava + JavaFX\n\nDenney, Wallace\n\n2015\n\n:D\n\n:::::::");
		info2.setTextAlignment(TextAlignment.CENTER);
		info2.setFont(Font.font("Arial", FontWeight.THIN, 12));
		if(About.firstLaunch){
			dialog.setTitle("about(basic)");
			info1.setText("\\(^.^\")/\n\nRISK!\nan open source way to\n\"risk\" it all. HA. (sorry.)");
			info2.setText("\n\nJava + JavaFX\n\nDenney, Wallace\n\n2015\n\n<3\n\n:::::::");
		}
		
		final Hyperlink hlink = new Hyperlink(":::");
		hlink.setOnAction(new EventHandler<ActionEvent>() {
			@Override public void handle(ActionEvent t){
				try {
					Desktop.getDesktop().browse(new URI("http://xkcd.com/977/"));
				} catch (IOException e1) {
					e1.printStackTrace();
				} catch (URISyntaxException e1) {
					e1.printStackTrace();
				}
			}
		});
		
		
		final Button submitButton = new Button("OK");
		submitButton.setDefaultButton(true);
		submitButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override public void handle(ActionEvent t) {
				dialog.close();
			}
		});
		//textField.setMinHeight(TextField.USE_PREF_SIZE);

		final VBox layout = new VBox();
		layout.setAlignment(Pos.CENTER);
		layout.setStyle("-fx-padding: 50;");
		//old::: 	      layout.setStyle("-fx-background-color: azure; -fx-padding: 10;");
		layout.getChildren().setAll(
		info1, hlink, info2,
		submitButton
		);

		dialog.setScene(new Scene(layout));
		dialog.show();
		About.firstLaunch = false;
		if(autoExit)
		{
			FXUIGameMaster.deathKnell(dialog,null,null);
		}
	}
	
	public void more(Window owner) {
		final Stage dialog = new Stage();

		dialog.setTitle("more.");
		dialog.initOwner(owner);
		//dialog.initStyle(StageStyle.UTILITY);
		//dialog.initModality(Modality.WINDOW_MODAL);
		dialog.setX(owner.getX());
		dialog.setY(owner.getY() + FXUIGameMaster.DEFAULT_DIALOG_OFFSET);

		
		final Hyperlink hlinkD = new Hyperlink("denney");
		hlinkD.setFont(Font.font("Arial", FontWeight.NORMAL, 16));
		hlinkD.setOnAction(new EventHandler<ActionEvent>() {
			@Override public void handle(ActionEvent t){
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
		hlinkW.setOnAction(new EventHandler<ActionEvent>() {
			@Override public void handle(ActionEvent t){
				try {
					Desktop.getDesktop().browse(new URI("http://github.com/aewallace"));
				} catch (IOException e1) {
					e1.printStackTrace();
				} catch (URISyntaxException e1) {
					e1.printStackTrace();
				}
			}
		});
		
		final Text bridge2= new Text("\n\n:::::::\n2015\n:::::::\n\n");
		bridge2.setTextAlignment(TextAlignment.CENTER);
		bridge2.setFont(Font.font("Arial", FontWeight.THIN, 16));
		
		final Text deepVersionInfo= new Text(FXUIGameMaster.versionInfo + 
				"\n\n");
		deepVersionInfo.setTextAlignment(TextAlignment.CENTER);
		deepVersionInfo.setFont(Font.font("Arial", FontWeight.THIN, 12));
		
		final Text subVersionInfo = new Text("\n" + FXUIPlayer.versionInfo +
				"\n\n" + LogPlayer.versionInfo + "\n\n");
		subVersionInfo.setTextAlignment(TextAlignment.CENTER);
		subVersionInfo.setFont(Font.font("Arial", FontWeight.THIN, 11));
		
		
		final Button submitButton = new Button("OK");
		submitButton.setDefaultButton(true);
		submitButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override public void handle(ActionEvent t) {
				dialog.close();
			}
		});
		//textField.setMinHeight(TextField.USE_PREF_SIZE);

		final VBox layout = new VBox(10);
		layout.setAlignment(Pos.CENTER);
		layout.setStyle("-fx-padding: 50;");
		//old::: 	      layout.setStyle("-fx-background-color: azure; -fx-padding: 10;");
		layout.getChildren().setAll(
		deepVersionInfo,
		subVersionInfo,
		hlinkD, bridge2, hlinkW,
		submitButton
		);

		dialog.setScene(new Scene(layout));
		dialog.showAndWait();
	}

}



