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

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
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
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
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
	public static final String versionInfo = "FXUI-RISK-Master\nVersion 00x16h\nStamp 2015.04.05, 21:50\nType:Modifiable/MNT(00)";
	private static final int DEFAULT_APP_WIDTH = 1600;
	private static final int DEFAULT_APP_HEIGHT = 1062;
	private static final int IDLE_MODE = 0, NEW_GAME_MODE = 1, LOADED_GAME_MODE = 2;
	private static int workingMode = IDLE_MODE;
	protected static final String LOGFILE = "LOG.txt";
	protected static final String STATSFILE = "STATS.txt";
	protected static final String EVENT_DELIM = "...";
	protected static final boolean LOGGING_OFF = false;
	protected static final boolean LOGGING_ON = true;
	protected static boolean loggingEnabled;
	private static FXUI_Crossbar crossbar = new FXUI_Crossbar();
	protected RiskMap map;
	protected Deque<Card> deck;
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
	private static Text errorTextElement;
	private static Text currentPlayStatus;
	private String errorTextInitialContents;
	private boolean errorHasOccurred;
	private ArrayList<Text> playerDisplayCache = null;
	private HashMap<String, Text> textNodeMap;
	private Map<String, Color> playerColorMap;
	private int numGames = 1;
	private static boolean proceedWithExit = false;
	private static boolean fullAppExit = false;
	
	//to handle recovering a prior session or help with launching a new game session
	private SavePoint savePoint = new SavePoint();
	private SavePoint loadedSaveIn = null;
	private HashMap<String, Country> stringCountryRepresentation = new HashMap<String, Country>();
	ArrayList<Button> buttonCache = new ArrayList<Button>();
	private static boolean initiationGood = false;
	private static boolean endGame = false;
	private boolean loadSucceeded = true;
	private boolean finishedLoading = false;
	private static Player currentPlayer = null;
	private boolean updateUI = false;
	private static boolean exitDialogIsShowing = true;
	private static boolean skipExitConfirmation = false;
	
	/**
	 * Allows easy access to the Thread.sleep() function without continuous trying/catching.
	 * @param mullisecs the length of time, in milliseconds, to sleep the thread. Similar to Thread.sleep(), value is a long.
	 */
	private void sleep(long millisecs){
		try {
			Thread.sleep(millisecs);
		} catch (InterruptedException e) {
			//We never care about what caused the interruption so long as we're using this method.
		}
	}
	
	/**
	* If the app detects a call from the system to exit the program, 
	* and it's from a dialog window, handle the call by...asking if we really want to exit.
	*/
	public static int doYouWantToMakeAnExit(boolean shutAppDownOnAccept, int currentAttempts){
		exitDialogIsShowing = true;
		
		if(!FXUIGameMaster.skipExitConfirmation && Platform.isFxApplicationThread()){ //if this is the FX thread, make it all happen, and use showAndWait
			exitDialogHelper(shutAppDownOnAccept, true);
		}
		
		else if(!FXUIGameMaster.skipExitConfirmation && !Platform.isFxApplicationThread()){ //if this isn't the FX thread, we can pause logic with a call to sleep()
			Platform.runLater(new Runnable(){
				@Override public void run(){
					exitDialogHelper(shutAppDownOnAccept, false);
				}
			});
			do{
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			while(exitDialogIsShowing); 
		}
		
		else{
			exitDialogIsShowing = false;
		}
		
		if(FXUIGameMaster.proceedWithExit || FXUIGameMaster.skipExitConfirmation)
		{
			FXUIGameMaster.proceedWithExit = false;
			return RiskConstants.MAX_ATTEMPTS;
		}
		else{
			return currentAttempts - 1;
		}
	}
	
	private static void exitDialogHelper(boolean shutAppDownOnAccept, boolean fxThread)
	{
		Window owner = pane.getScene().getWindow();
		try{
			final Button yeah = new Button("Yes");
			final Button nah = new Button("No");
			final Stage dialog = new Stage();
			dialog.setTitle("bye bye?");
			dialog.initOwner(owner);
			dialog.setX(owner.getX());
			dialog.setY(owner.getY());
			
			final Text queryText = new Text("     Did you want to end the game?     \n[If enabled, your most recent\ncheckpoint will be saved]");
			queryText.setTextAlignment(TextAlignment.CENTER);
			
			final Text querySymbol = new Text("end game?");
			querySymbol.setTextAlignment(TextAlignment.CENTER);
			querySymbol.setFont(Font.font("Arial", FontWeight.BOLD, 24));
			
			yeah.setOnAction(new EventHandler<ActionEvent>() {
				@Override public void handle(ActionEvent t) {
					crossbar.signalHumanEndingGame();
					FXUIGameMaster.proceedWithExit = true;
					FXUIGameMaster.skipExitConfirmation = shutAppDownOnAccept;
					if(!shutAppDownOnAccept)
					{
						exitDialogIsShowing = false;
						FXUIGameMaster.endGame = true;
						currentPlayStatus.setText("I D L E");
					}
					crossbar.tryCloseCurrentPlayerDialog();
					dialog.close();
				}
			});
			
			nah.setDefaultButton(true);
			nah.setOnAction(new EventHandler<ActionEvent>() {
				@Override public void handle(ActionEvent t) {
					exitDialogIsShowing = false;
					FXUIGameMaster.proceedWithExit = false;
					dialog.close();
					yeah.setDisable(true);
				}
			});
			
			if(shutAppDownOnAccept)
			{
				queryText.setText("     Application fully exiting;     \nShall we go?");
				querySymbol.setText("exiting!");
				nah.setDisable(true);
			}
			Text spaceBuffer = new Text("-+-+-+-+-");
			
			final VBox layout = new VBox(10);
			layout.setAlignment(Pos.CENTER);
			//layout.setStyle("-fx-padding: 20;");
			layout.setStyle("-fx-background-color: pink");
			layout.getChildren().setAll(
			querySymbol, queryText, nah, yeah, spaceBuffer
			);
			
			dialog.setScene(new Scene(layout));
			if(fxThread){
				dialog.showAndWait();
				}
			else{
				dialog.show();
			}
		}
		catch(Exception e){System.out.println("attempted exit failed:: " + e);}
	}
	
	/**
	* Prepares save info. Use at the beginning of each new round, as a checkpoint,
	*   to avoid any one player getting an advantage over any other player upon later resume.
	*   Doesn't guarantee perfect state save, but helps.
	*/
	private void prepareSave(){
		disableSaveButton();
		if(loadedSaveIn != null){ //updating an old save
			savePoint.updateSaveIdentificationInfo(loadedSaveIn.getOriginalSaveDate(), new Date(), this.round);
		}
		else{ //completely new save
			savePoint.updateSaveIdentificationInfo(new Date(), new Date(), this.round);
		}
		savePoint.prepAllCountryDetails(map);
		savePoint.prepAllPlayerDetails((HashMap<String, Player>) playerMap, allPlayers);
		savePoint.prepRoundsCompleted(round);
		savePoint.prepLogCache(internalLogCache);
		for (String player : players){
			savePoint.prepCardsForGivenPlayer(player, createCardSetCopy(player));
		}
		System.out.println("Checkpoint reached.");
		enableSaveButton();
	}
	
	/**
	* Performs actual write to disc using most recent checkpoint available.
	* Checkpoints are acquired with prepareSave(), automatically performed after initial player allocation.
	* Write to secondary storage is triggered either automatically at each new round,
	* or manually with the "Save" button  (with no discernible difference between the two).
	* 
	* @return returns true on successful save, or false when a show-stopping exception was thrown.
	*/
	private boolean performSave(){
		disableSaveButton();
		boolean succeeded = false;
		try{
			OutputStream file = new FileOutputStream("fxuigm_save.ser");
			OutputStream buffer = new BufferedOutputStream(file);
			ObjectOutput output = new ObjectOutputStream(buffer);
			output.writeObject(savePoint);
			output.close();
			succeeded = true;
		}
		catch(Exception e){
			System.out.println("Save failed. ::: " + e);
			
		}
		enableSaveButton();
		if(!succeeded)
		{
			setErrorStatus("Save failed");
		}
		return succeeded;
	}
	
	/**
	* Starts the process of loading a game from a previous save, as the method you should directly use.
	* Ensures the loading process occurs on the JavaFX thread, and attempts to prevent the game logic
	* from unintentionally running before the environment is reconstructed.
	* @return returns true if the load succeeded, or false if a show-stopping exception was encountered
	*/
	private boolean loadFromSave(){
		/*if(!Platform.isFxApplicationThread()){
			Platform.runLater(new Runnable(){
				@Override public void run(){
					System.out.println("running xternally");
					runLoadingScript();
				}
			});
			while(!finishedLoading)
				{ 
				sleep(1000);
				System.out.println("waiting 4eva");
				}
		}
		else{*/
			runLoadingScript();
			
		/*}*/
		
		return loadSucceeded;
	}
	
	/**
	 * Acts as the heavy lifter when it comes to the loading process. An integral
	 * part of the loadFromSave() method.
	 * Separated into its own method to avoid redundant code due to alternative ways we
	 * must ensure this portion of code runs on the JavaFX thread.
	 * 
	 * Reconstructs old game as best as possible, using slightly more specific info than a new game.
	 * Also stores the SavePoint object within the class for future reference.
	 * Sets the flag to indicate the loading success or failure. Also sets a flag
	 * which may be used by the caller to ensure (assuming a success loading process!)
	 * that the program does not continue until complete.
	 */
	private void runLoadingScript()
	{
		try{
			loadSucceeded = false;
			finishedLoading = false;
			InputStream file = new FileInputStream("fxuigm_save.ser");
			InputStream buffer = new BufferedInputStream(file);
			ObjectInput input = new ObjectInputStream(buffer);
			SavePoint loadedSave = (SavePoint)input.readObject();
			loadedSaveIn = loadedSave;
			loadSucceeded = loadSucceeded || loadPlayersFromSave(loadedSave);
			if(!loadSucceeded){System.out.println("load failure P0");} // TODO allow better diagnostics and recovery in the future
			loadSucceeded = loadSucceeded && resetCountryInfo(loadedSave);
			if(!loadSucceeded){System.out.println("load failure P1");}
			loadSucceeded = loadSucceeded && restorePreviousLogInfo(loadedSave);
			if(!loadSucceeded){System.out.println("load failure P2");}
			representPlayersOnUI();
			refreshUIElements(true);
			input.close();
			buffer.close();
			file.close();
			finishedLoading = true;
		}
		catch(Exception e){
			System.out.println("Load failed. ::: " + e);
			e.printStackTrace();
			loadSucceeded = false;
		}
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
					log.close();
				}
				this.log = new FileWriter(LOGFILE);
				
				if(this.stats == null){
					this.stats = new FileWriter(STATSFILE);
				}
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
		this.playerMap.clear();
		this.allPlayers.clear();
		this.players.clear();
		
		this.playerMap = new HashMap<String, Player>();
		this.allPlayers = new ArrayList<String>();
		this.players = new ArrayList<String>();
		
		final String FXP = FXUIPlayer.class.toString();
		final String EDP = EasyDefaultPlayer.class.toString();
		final String HDP = HardDefaultPlayer.class.toString();
		final String NDP = NormalDefaultPlayer.class.toString();
		final String S_P = Seth.class.toString();
		System.out.print("loadPlayersFromSave entered...");
		System.out.println(loadedSave.getPlayerIsEliminatedMap().entrySet().size() + " players to load today!!");
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
					System.out.println("Failed to cast/load " + playerIn.getKey() + " as a valid player.");
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
		if (buttonCache.size() < 4){
			return;
		}
		Platform.runLater(new Runnable()
		{
			@Override public void run(){
				if(workingMode == IDLE_MODE)
				{
					buttonCache.get(0).setDisable(false); //we can start a new game
					buttonCache.get(1).setDisable(false); //we can load a previous game
					buttonCache.get(3).setDisable(true); //we cannot highlight the countries owner by a given player...
					disableSaveButton(); //we cannot use the save button
					setPlayStatus("I D L E"); //set the status to "IDLE"
				}
				else {
					buttonCache.get(0).setDisable(true); //we cannot start a new game...at this point.
					buttonCache.get(1).setDisable(true); //we cannot load a previous game...at this point.
					buttonCache.get(3).setDisable(false); //we CAN highlight the countries owner by a given player.
					//save button is set dynamically while the game is in play, so do not concern yourself with it
					setPlayStatus("in play."); //set the status to "in play"; will be overwritten with an error if need be
				}
			}
		});
	}
	
	/**
	 * Companion method to disable the save button; ensures actions happen on FX thread
	 */
	public void disableSaveButton(){
		Platform.runLater(new Runnable()
		{
			@Override public void run(){
				buttonCache.get(2).setDisable(true); //disable the save button
			}
		});
	}
	
	/**
	 * Companion method to enable the save button; ensures actions happen on FX thread
	 */
	public void enableSaveButton(){
		Platform.runLater(new Runnable()
		{
			@Override public void run(){
				buttonCache.get(2).setDisable(false); //enable the save button
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
			currentPlayStatus.setText(status);
			return true;
		}
		else //place in event queue for running on the FX thread
		{
			Platform.runLater(new Runnable()
			{
				@Override public void run(){
					currentPlayStatus.setText(status);
				}
			});
			return false;
		}
	}
	
	/**
	 * Used to set the text of the "errorTextElement" UI element, even from unsafe threads (unsafe: "not the FX thread")
	 * TODO add the ability to push these messages to diagnostics dialog & secondary storage
	 * @param error text to be displayed  (or default/alternative text, if no error occurred)
	 * @return "true" if called from the JavaFX thread, "false" otherwise
	 */
	private boolean setErrorStatus(String status){
		if(Platform.isFxApplicationThread()) //if already on FX thread, can directly set
		{
			errorTextElement.setText(status);
			return true;
		}
		else //place in event queue for running on the FX thread
		{
			Platform.runLater(new Runnable()
			{
				@Override public void run(){
					errorTextElement.setText(status);
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
		//This is only here temporarily, until I can figure out the control flow of this application.
		FXUIPlayer.setOwnerWindow(FXUIGameMaster.pane.getScene().getWindow()); //applies to all human player(s), so now made static.
		
		
		if (workingMode == NEW_GAME_MODE){
			initiationGood = initializeForces();
			if(!initiationGood){
				setPlayStatus("creation of new game failed");
			}
		}
		else if (workingMode == LOADED_GAME_MODE){
			initiationGood = loadFromSave();
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
				this.updateUI = currentPlayer.getClass().toString().equals(FXUIPlayer.class.toString());
				writeLogLn(true, currentPlayer.getName() + " is starting their turn.");
				writeStatsLn();
				this.turnCount++;
				try {
					refreshUIElements(this.updateUI);
					
					reinforce(currentPlayer, true);
					if(FXUIGameMaster.endGame = crossbar.isHumanEndingGame() || FXUIGameMaster.endGame){
						break;
					}
					else{
						refreshUIElements(this.updateUI);
					}
					
					attack(currentPlayer);
					if(FXUIGameMaster.endGame = crossbar.isHumanEndingGame() || FXUIGameMaster.endGame){
						break;
					}
					else{
						refreshUIElements(this.updateUI);
					}
					
					fortify(currentPlayer);
					if(FXUIGameMaster.endGame = crossbar.isHumanEndingGame() || FXUIGameMaster.endGame){
						break;
					}
					else{
						refreshUIElements(this.updateUI);
					}
					
					turn = (this.players.indexOf(currentPlayer.getName()) + 1) % this.players.size();
					if(FXUIGameMaster.endGame = crossbar.isHumanEndingGame() || FXUIGameMaster.endGame){
						break;
					}
					else{
						refreshUIElements(this.updateUI);
					}
					if (currentPlayer.getClass().toString().equals(FXUIPlayer.class.toString()) && !FXUIGameMaster.endGame)
					{
						prepareSave();
						performSave();
					}
				}
				catch (PlayerEliminatedException e) {
					//If an elimination exception is thrown up to this level,
					//then it was currentPlayer who was eliminated.
					turn %= this.players.size();
				}
			}
			if(!FXUIGameMaster.fullAppExit && !FXUIGameMaster.endGame && this.players.size() > 0){
				writeStatsLn();
				System.out.println(this.players.get(0) + " is the victor!");
				writeLogLn(true, this.players.get(0) + " is the victor!");
			}
			else
			{
				System.out.println("Game forced to exit by UI player; sorry 'bout it!");
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
		
		refreshUIElements(true);
		currentPlayer = null;
		workingMode = IDLE_MODE;
		setButtonAvailability();
		boolean didEndGame = FXUIGameMaster.endGame;
		FXUIGameMaster.endGame = false;
		crossbar.resetEndGameSignal();
		if(this.players.size() > 0 && !didEndGame)
		{
			return this.players.get(0);
		}
		else
		{
			return null;
		}
	}
	
	/**
	* Triggers each Player type to invoke initial troop allocation on countries assigned to each player.
	* Used only for new games, as similar information for each round
	* is cached in SavePoint objects as the game goes on.
	* @return returns true if at least one player succeeded, false otherwise
	*/
	protected boolean initializeForces() {
		boolean valid;
		int attempts;
		int playerIndex = 0;
		//get initial troop allocation
		while (playerIndex < this.players.size()) {
			Player player = this.playerMap.get(this.players.get(playerIndex));
			writeLogLn(true, "Getting initial troop allocation from " + player.getName() + "...");
			int reinforcements;
			valid = false;
			attempts = 0;
			while (!valid && attempts < RiskConstants.MAX_ATTEMPTS  && !FXUIGameMaster.fullAppExit) {
				attempts++;
				reinforcements = RiskConstants.INIT_ARMIES / this.players.size();
				ReinforcementResponse rsp = tryInitialAllocation(player, reinforcements);
				if (valid = ReinforcementResponse.isValidResponse(rsp, this.map, player.getName(), reinforcements)
						&& validateInitialAllocation(rsp.getAllocation(), player.getName(), reinforcements)) {
					allocateArmies(player.getName(), rsp.getAllocation(), reinforcements);
					playerIndex++;
					writeLogLn(true, "Troops successfully allocated for " + player.getName() + "...");
				}
			}
			
			if (!valid || crossbar.isHumanEndingGame()) {
				try {
					if(!valid){
						eliminate(player, null, "You failed to provide a valid initial army allocation.");
					}
					else{
						eliminate(player, null, "Player decided the game isn't for them right now. No worries.");
					}
				}
				catch (PlayerEliminatedException e) {
					playerIndex = 0;
				}
			}
		}
		
		return this.players.size() > 0;
	}
	
	protected void validatePlayerName(Player player) throws PlayerEliminatedException {
		if (!(this.playerMap.containsKey(player.getName()) && this.playerMap.get(player.getName()) == player)) {
			System.out.println(player.getName());
			System.out.println(this.playerMap.get(player.getName()));
			System.out.println(player);
			System.out.println(player.toString());
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
		// TODO turn off oppCards IN THIS METHOD; it's unused after you get extra reinforcements manually beforehand
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
			refreshUIElements(this.updateUI);
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
					refreshUIElements(this.updateUI);
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
		if (!valid) {
			eliminate(attacker, null, "You failed to provide a valid advance response.");
		}
		else if (crossbar.isHumanEndingGame()) // TODO you never reach this. try again?
		{
			eliminate(attacker, null, "The advancer decided to take a break. 'S OK. Get some cookies. Or hot cocoa.");
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
			System.out.println(e);
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
			return true;
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
					/*try{
					map.setCountryOwner(card.getCountry(), this.playerMap.get(this.players.get(allocationIdx % this.players.size())).getName());
					allocationIdx++;
					}
					catch(Exception e)
					{
						System.out.println("allocateMap:: " + e);
					}*/
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
			System.out.println("eliminate() :: this.playerMap does not contain so-called 'loser'");
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
				System.out.println("Error writing log: " + e);
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
				System.out.println("Error writing statistics: " + e);
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
			if(this.playerDisplayCache != null && this.playerDisplayCache.size() > 0){
				for (Text oldPlayer : this.playerDisplayCache){
					FXUIGameMaster.pane.getChildren().remove(oldPlayer);
				}
			}
			
			this.playerDisplayCache = new ArrayList<Text>();
			ArrayList<Color> colors = new ArrayList<Color>();
			colors.add(Color.WHITE);
			colors.add(Color.AQUA);
			colors.add(Color.RED);
			colors.add(Color.GREENYELLOW);
			colors.add(Color.CORAL);
			colors.add(Color.VIOLET);
			this.playerColorMap = new HashMap<String, Color>();
			int i = -1;
			
			for (String playerName : this.playerMap.keySet())
			{
				this.playerColorMap.put(playerName, colors.get(++i % colors.size()));
				Text txt = new Text(200 * (i) + 50, 20, "∎"+playerName.toLowerCase());
				txt.setFont(Font.font("Verdana", FontWeight.THIN, 20));
				txt.setFill(colors.get((i) % colors.size()));
				FXUIGameMaster.pane.getChildren().add(txt);
				this.playerDisplayCache.add(txt);
			}
		}
		catch (RuntimeException e) {
		}
	}
	
	
	public static void main(String[] args) throws IOException {
		launch(FXUIGameMaster.class, args);
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
			}
		};
		Thread gameThread = new Thread(gameCode);
		gameThread.setDaemon(true);
		gameThread.start();
		if (gameThread.isAlive()){
			return true;
		}
		else{
			return false;
		}
	}
	
	public void runGameAndDisplayVictor(){
		try {
			// TODO add support for selecting types of players
			HashMap<String, Integer> winLog = new HashMap<String, Integer>();
			RiskConstants.SEED = 1;
			for (int i = 0; i < this.numGames; i++) {
				RiskConstants.resetTurnIn();
				PlayerFactory.resetPlayerCounts();
				initializeFXGMClass("Countries.txt", RiskConstants.DEFAULT_PLAYERS + "," + PlayerFactory.FXUI, i == this.numGames - 1 ? LOGGING_ON : LOGGING_OFF);
				representPlayersOnUI();
				
				System.out.print((i + 1) + " - ");
				
				String victor = begin();
				
				System.out.println("Hi user!!! game execute was successfully!!!!"); //yes very successfully!!!!
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
					System.out.println(entry.getKey() + " had a win percentage of " + 100.0 * entry.getValue() / this.numGames + "%");
				}
			}
		}
		catch (RuntimeException e) {
			System.out.println("fake main() method runtime error:: " + e);
			e.printStackTrace();
		} catch (IOException e) {
			//TODO determine if this particular catch statement is even needed?
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
	public void initializeFXGMClass(String mapFile, String players, boolean logSwitch) throws IOException {
		for (Country country : Country.values()) {
			stringCountryRepresentation.put(country.getName(), country);
		}
		
		this.round = 0;
		this.turnCount = 0;
		if (rand == null) {
			rand = new Random(RiskConstants.SEED);
		}
		
		loggingEnabled = logSwitch;
		if (loggingEnabled == LOGGING_ON) {
			this.log = new FileWriter(LOGFILE);
			this.stats = new FileWriter(STATSFILE);
		}
		
		writeLogLn(true, "Loading map from " + mapFile + "...");
		if (starterMap == null) {
			starterMap = new RiskMap();
		}
		
		this.map = starterMap.getCopy();
		loadDeck();
		if (!loadPlayers(players)) {
			System.out.println("Invalid number of players. 2-6 Players allowed.");
		}
		
		allocateMap();
	}
	
	/**
	 * Populates the UI with the countries using predefined window locations.
	 * @param nodeFile
	 */
	private void loadTextNodesForUI(String nodeFile) {
		try {
			if (nodeFile != null) { // TODO make better way to package app with original TextNodes.txt file
				this.textNodeMap = new HashMap<String, Text>();
				File fileRepresentation = new File(nodeFile);
				getClass().getResourceAsStream(nodeFile);
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
		if (!guaranteeRefresh && this.round % this.players.size() != 0){ //just...just don't update too often
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
				sleep(timeToWaitBetweenElements);
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
	 * @param playerName the String representation of the player in question.
	 */
	private void flashPlayerCountries(String playerName){
		final Set<Country> myCountries = RiskUtils.getPlayerCountries(map, playerName);
		final long timeDeltaMS = 220;
		Runnable clockedBlinkTask = new Runnable()
		{
			@Override public void run()
			{
				for (int i = 0; i < 2; i++)
				{
					for (Country country : myCountries){
						sleep(timeDeltaMS);
						Platform.runLater(new Runnable(){
							@Override public void run(){
								textNodeMap.get(country.getName()).setOpacity(0.1d);
								textNodeMap.get(country.getName()).setStroke(Color.BISQUE);
							} 
						});
						sleep(timeDeltaMS);
						Platform.runLater(new Runnable(){
							@Override public void run(){
								textNodeMap.get(country.getName()).setOpacity(1.0d);
								textNodeMap.get(country.getName()).setStroke(Color.BISQUE);
							} 
						});
						sleep(timeDeltaMS);
						Platform.runLater(new Runnable(){
							@Override public void run(){
								textNodeMap.get(country.getName()).setOpacity(1.0d);
								textNodeMap.get(country.getName()).setStroke(null);
							} 
						});
						sleep(timeDeltaMS);
						Platform.runLater(new Runnable(){
							@Override public void run(){
								textNodeMap.get(country.getName()).setOpacity(0.1d);
								textNodeMap.get(country.getName()).setStroke(Color.BISQUE);
							} 
						});
						sleep(timeDeltaMS);
						Platform.runLater(new Runnable(){
							@Override public void run(){
								textNodeMap.get(country.getName()).setOpacity(1.0d);
								textNodeMap.get(country.getName()).setStroke(Color.DARKSALMON);
							} 
						});
						sleep(timeDeltaMS);
						Platform.runLater(new Runnable(){
							@Override public void run(){
								textNodeMap.get(country.getName()).setOpacity(1.0d);
								textNodeMap.get(country.getName()).setStroke(null);
							} 
						});
					}
					for (Country country : myCountries){
						Platform.runLater(new Runnable(){
							@Override public void run(){
								textNodeMap.get(country.getName()).setOpacity(0.5d);
								textNodeMap.get(country.getName()).setStroke(Color.DARKSALMON);
							} 
						});
					}
					sleep(timeDeltaMS);
					for (Country country : myCountries){
						Platform.runLater(new Runnable(){
							@Override public void run(){
								textNodeMap.get(country.getName()).setOpacity(1.0d);
								textNodeMap.get(country.getName()).setStroke(Color.BISQUE);
							} 
						});
					}
					sleep(timeDeltaMS);
					for (Country country : myCountries){
						Platform.runLater(new Runnable(){
							@Override public void run(){
								textNodeMap.get(country.getName()).setOpacity(1.0d);
								textNodeMap.get(country.getName()).setStroke(null);
							} 
						});
					}
					
				}
			}
		};
		Thread clockedBlinkThread = new Thread(clockedBlinkTask);
		clockedBlinkThread.setDaemon(true);
		clockedBlinkThread.start();
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
		System.out.println("Width first set: " + widthOfPriScreen + " :: Height first set: " + heightOfPriScreen);
		
		pane = new Pane();
		pane.setPrefSize(DEFAULT_APP_WIDTH, DEFAULT_APP_HEIGHT);
		pane.setStyle("-fx-background-color: blue");
		/*We set the image in the pane based on whether there was an error or not.
		* If there was an error, it'll be changed later.*/
		
		//Facilitate checking for errors...
		errorHasOccurred = false;
		errorTextInitialContents = "Status...";
		
		//pre-load the error background, just in case...
		Image imageE = new Image("RiskBoardAE.jpg",true);
		ImageView backgroundImg = new ImageView();
		backgroundImg.setImage(imageE);
		pane.getChildren().add(backgroundImg);
		
		//...which will happen here:
		//populate the countries and players, and find out if there was an error doing either activity
		//assumes a "default" activity of starting a new game
		initializeFXGMClass("Countries.txt", RiskConstants.DEFAULT_PLAYERS + "," + PlayerFactory.FXUI, LOGGING_ON);
		loadTextNodesForUI("TextNodes.txt");
		representPlayersOnUI();
		//now display elements -- status and buttons -- according to whether there was an error!
		errorTextElement = new Text(errorTextInitialContents);
		errorTextElement.setFont(Font.font("Verdana", FontWeight.THIN, 20));
		if(errorHasOccurred)
		{
			errorTextElement.setFill(Color.RED);
		}
		else
		{
			errorTextElement.setFill(Color.WHITE);
			Image imageOK = new Image("RiskBoard.jpg", true);
			backgroundImg.setImage(imageOK);
		}
		
		//The vertical box to contain the major buttons and status.
		VBox primaryStatusButtonPanel = new VBox(10);
		HBox lowerButtonPanel = new HBox(15);
		
		primaryStatusButtonPanel.setAlignment(Pos.CENTER_LEFT);
		primaryStatusButtonPanel.setLayoutX(29);
		primaryStatusButtonPanel.setLayoutY(525);
		primaryStatusButtonPanel.getChildren().add(errorTextElement);
		
		
		currentPlayStatus = new Text("H E L L O");
		currentPlayStatus.setFont(Font.font("Verdana", FontWeight.NORMAL, 24));
		currentPlayStatus.setFill(Color.WHITE);
		
		
		//End the current game, but don't close the program.
		Button stopGameBtn = new Button("Bow out.\n(End current game)");
		stopGameBtn.setOnAction(new EventHandler<ActionEvent>(){
			@Override public void handle(ActionEvent t){
				Platform.runLater(new Runnable()
				{
					@Override
					public void run() {
						crossbar.signalHumanEndingGame();
						crossbar.tryCloseCurrentPlayerDialog();
						FXUIGameMaster.endGame = true;
					}
				});
			}
		});
		
		//Button to initiate the game
		Button startBtn = new Button("Let's go!!\n(Start new game)");
		startBtn.setOnAction(new EventHandler<ActionEvent>(){
			@Override public void handle(ActionEvent t){
				if(workingMode == IDLE_MODE)
				{
					workingMode = FXUIGameMaster.NEW_GAME_MODE;
					createGameLogicThread();
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
		
		//testing saving functionality
		Button saveMe = new Button("save.");
		saveMe.setOnAction(new EventHandler<ActionEvent>(){
			@Override public void handle(ActionEvent t){
				performSave();
			}
		});
		saveMe.setDisable(true);
		
		Button restoreMe = new Button("load.");
		restoreMe.setOnAction(new EventHandler<ActionEvent>(){
			@Override public void handle(ActionEvent t){
				if(workingMode == IDLE_MODE)
				{
					workingMode = LOADED_GAME_MODE;
					createGameLogicThread();
				}
			}
		});
		
		lowerButtonPanel.getChildren().addAll(tellMe, tellMe2, saveMe, restoreMe);
		
		//Exit the application entirely
		Button exitApp = new Button("Lights out!\n(Exit to desktop)");
		exitApp.setOnAction(new EventHandler<ActionEvent>(){
			@Override public void handle(ActionEvent t){
				tryToExit(primaryStage);
			}
		});
		
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
		
		//tweaks to perform if there was an error...
		if(errorHasOccurred){
			currentPlayStatus.setText("------");
			startBtn.setDisable(true);
			stopGameBtn.setDisable(true);
		}
		else{
			pane.setOnKeyPressed(new EventHandler<KeyEvent>(){
				@Override public void handle(KeyEvent t){
					if(workingMode == IDLE_MODE)
					{
						workingMode = FXUIGameMaster.NEW_GAME_MODE;
						createGameLogicThread();
					}
				}
			});
		}
		
		primaryStatusButtonPanel.getChildren().addAll(currentPlayStatus,startBtn,
								stopGameBtn,exitApp,lowerButtonPanel);
		//****layout of text & buttons displayed upon launch ends here.***
		
		pane.getChildren().addAll(flashCurrCountries, primaryStatusButtonPanel);
		

		// DEFAULT_APP_WIDTH, DEFAULT_APP_HEIGHT);
		scene = new Scene(pane,widthOfPriScreen, heightOfPriScreen);
		
		
		//one more tweak to perform if there was -no- error
		scene.widthProperty().addListener(new ChangeListener<Number>() {
			@Override public void changed(ObservableValue<? extends Number> observableValue, Number oldSceneWidth, Number newSceneWidth) {
				System.out.println("Width: " + newSceneWidth);
				resize(null);
			}
		});
		scene.heightProperty().addListener(new ChangeListener<Number>() {
			@Override public void changed(ObservableValue<? extends Number> observableValue, Number oldSceneHeight, Number newSceneHeight) {
				System.out.println("Height: " + newSceneHeight);
				resize(null);
			}
		});
		
		//Add buttons to a secondary cache, to allow easy enable/disable depending on state.
		buttonCache.add(startBtn);
		buttonCache.add(restoreMe);
		buttonCache.add(saveMe);
		buttonCache.add(flashCurrCountries);
		
		//Get the primary window showin', already!
		resize(primaryStage);
		primaryStage.setTitle("RISK!");
		primaryStage.setScene(scene);
		primaryStage.show();
		
		
		//go ahead and launch the "About" window, and tell it to autohide -- time until autohide set via the "About" class.
		nAbout.launch(pane.getScene().getWindow(), true);
		
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
		
		final Text info2= new Text();
		info2.setText("\n\nJava + JavaFX\n\nDenney, Wallace\n\n2015\n\n:D\n\n:::::::");
		info2.setTextAlignment(TextAlignment.CENTER);
		info2.setFont(Font.font("Arial", FontWeight.THIN, 12));
		
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
		if(autoExit)
		{
			Runnable task = new Runnable() {
				@Override public void run() {
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					Platform.runLater(new Runnable()
					{
						@Override public void run(){
							dialog.close();
						} 
					});
				}
			};
			Thread th = new Thread(task);
			th.setDaemon(true);
			th.start();
		}
	}
	
	public void more(Window owner) {
		final Stage dialog = new Stage();

		dialog.setTitle("more.");
		dialog.initOwner(owner);
		//dialog.initStyle(StageStyle.UTILITY);
		//dialog.initModality(Modality.WINDOW_MODAL);
		dialog.setX(owner.getX());
		dialog.setY(owner.getY() + 300);

		
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
		
		final Text deepVersionInfo= new Text(FXUIGameMaster.versionInfo + "\n\n");
		deepVersionInfo.setTextAlignment(TextAlignment.CENTER);
		deepVersionInfo.setFont(Font.font("Arial", FontWeight.THIN, 12));
		
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
		hlinkD, bridge2, hlinkW,
		submitButton
		);

		dialog.setScene(new Scene(layout));
		dialog.showAndWait();
	}

}



