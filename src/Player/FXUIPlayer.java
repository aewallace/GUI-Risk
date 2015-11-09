/*FXUI Player Class
*Albert Wallace, 2015. Version number now found in Player class definition.
*for Seth Denney's RISK, JavaFX UI-capable version
*
*Base build from original "player" interface, 
*incorporating elements of nothing but http://stackoverflow.com/questions/16823644/java-fx-waiting-for-user-input
*so thanks stackoverflow!
**/

package Player;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import Map.Country;
import Map.RiskMap;
import Master.FXUIGameMaster;
import Response.AdvanceResponse;
import Response.AttackResponse;
import Response.CardTurnInResponse;
import Response.DefendResponse;
import Response.FortifyResponse;
import Response.ReinforcementResponse;
import Util.Card;
import Util.FXUI_Crossbar;
import Util.RiskConstants;
import Util.RiskUtils;

// TODO revise handling of "system exits". Consider updating the crossbar, so FXUIGameMaster can respond appropriately with a well-placed check?


/**
* Encapsulates the UI elements a human may use
* 	to respond to the GameMaster as a valid Player.
* 
* Requires FXUI GameMaster. Not compatible with original GameMaster;
* 	implemented UI elements require triggering from active JavaFX application.
* 
* UI elements are JavaFX, done with Java JDK 8.
*
*/
public class FXUIPlayer implements Player {
	public static final String versionInfo = "FXUI-RISK-Player\nVersion 01x10h\nStamp 2015.11.08, 18:41\nStability: Alpha(01)";

	private static boolean instanceAlreadyCreated = false;
	private static FXUI_Crossbar crossbar = new FXUI_Crossbar();
	private static Window owner = null;
	private double windowXCoord = 0;
	private double windowYCoord = 0;

	public static void setOwnerWindow(Window ownerIn) {
		FXUIPlayer.owner = ownerIn;
	}

	/**
	* Getter for the FXUI GameMaster-Player Crossbar
	* @return crossbar, the desired crossbar, static across all instances.
	*/
	public static FXUI_Crossbar getCrossbar() {
		return crossbar;
	}

	/**
	* Setter for the FXUI GameMaster-Player Crossbar
	* @param crossbar, the crossbar to use, static across all instances.
	*/
	public static void setCrossbar(FXUI_Crossbar crossbar) {
		FXUIPlayer.crossbar = crossbar;
	}
	
	private String name;
	private static final int MAX_NAME_LENGTH = 22;
	private int reinforcementsApplied = 0;
	private int maxAtkDiceAvailable = 0;
	private boolean passTurn = false;
	private final String blankText = "-----";
	private String attackTarget = blankText, attackSource = blankText;
	private boolean keepRunning = false;
	private final ExitStateSubHelper exitDecider = new ExitStateSubHelper();
	
	private boolean lastCoordIsKnown = false;
	

	
	/**
	* Note: there is an artificial limitation (imposed by this class) where only one user may be a human player.
	* If/when provisions are made to allow naming of characters, the limitation can be safely removed with no loss of functionality...
	* ...except for where the crossbar is concerned. It treats any/all human players as one.
	*/
	public FXUIPlayer() {
		if (instanceAlreadyCreated)
		{
			throw new UnsupportedOperationException("One instance of FXUIPlayer allowed at a time!");
		}
		else
		{
			this.name = "Human Player";
		}
	}
	
	public FXUIPlayer(String nameIn) {
		if (instanceAlreadyCreated){
			throw new UnsupportedOperationException("One instance of FXUIPlayer allowed at a time!");
		}
		else{
			this.name = nameIn;
		}
	}
	
	public FXUIPlayer(boolean askForName, Collection<String> unavailableNames){
		if (instanceAlreadyCreated)
		{
			throw new UnsupportedOperationException("One instance of FXUIPlayer allowed at a time!");
		}
		else
		{
			String desiredName = null;
			if(askForName && null != (desiredName = askForDesiredName(unavailableNames))){
				this.name = desiredName;
			}
			else{
				this.name = "Human " + this.hashCode();
			}
		}
	}
	
	/**
	 * Given a valid dialog, extracts the X and Y coordinates of this window as located on screen.
	 * Saves the coordinates to a couple of class instance variables.
	 * @param dialog the window from which we will be gathering the coordinates
	 */
	private void saveLastKnownWindowLocation(Stage dialog){
		this.windowXCoord = dialog.getX();
		this.windowYCoord = dialog.getY();
		this.lastCoordIsKnown = true;
	}
	
	/**Given a valid dialog, places the dialog at coordinates previously recorded from a prior dialog.
	 * If no dialog's coordinates had been set, uses whatever position is set in the associated vars.
	 * @param dialog dialog to be placed at the last remembered dialog coords.
	 */
	private void putWindowAtLastKnownLocation(Stage dialog){
		final int single_screen = 1;
		if(!this.lastCoordIsKnown && FXUIPlayer.owner != null){
			dialog.setX(FXUIPlayer.owner.getX());
			dialog.setY(FXUIPlayer.owner.getY() + FXUIGameMaster.DEFAULT_DIALOG_OFFSET);
		}
		else{
			if(Screen.getScreens().size() == single_screen){
				double widthOfPriScreen = Screen.getPrimary().getVisualBounds().getWidth() - 5;
				double heightOfPriScreen = Screen.getPrimary().getVisualBounds().getHeight() - 25;
				dialog.setX(this.windowXCoord < 0 || this.windowXCoord > widthOfPriScreen ? 0 : this.windowXCoord);
				dialog.setY(this.windowYCoord < 0 || this.windowXCoord > heightOfPriScreen ? 0 : this.windowYCoord);
			}
			else{
				dialog.setX(this.windowXCoord);
				dialog.setY(this.windowYCoord);
			}
		}
	}
	
	/**
	 * Waits for distinct player dialogs to close. Requires dialogs to be registered with the local Crossbar.
	 * Else, if no dialog is registered with the local crossbar, immediately returns.
	 * If used with the incorrect dialog, will stall indefinitely until the correct, associated dialog is closed.
	 * Will be interrupted (and return) if an attempt to end the game is registered by the local Crossbar.
	 */
	private void waitForDialogToClose(FXUI_Crossbar xbar){
		RiskUtils.sleep(1000);
		do{
			RiskUtils.sleep(100);
		}
		while(xbar.getCurrentPlayerDialog() != null && xbar.getCurrentPlayerDialog().isShowing() && !xbar.isHumanEndingGame());
	}
	
	/**
	 * Decides if the prior closing of a dialog was intended as part of the game's progression, or if the user
	 * closed the dialog methods for other reasons.
	 * If the dialog was closed through other means, prompts the user to confirm whether the window was closed
	 * in error, assuming the proper flag was left set/unset.
	 * The local ExitDecider contains the flag to determine whether the close was expected or unexpected.
	 * The local Crossbar contains the flag to determine if we have already indicated that the app *is* being closed.
	 */
	private void checkIfCloseMeansMore(ExitStateSubHelper exitHelper, FXUI_Crossbar xbar){
		if(exitHelper.isSystemExit() && !xbar.isHumanEndingGame()){
			//ask if the user actually wants the game to end
			this.keepRunning = FXUIGameMaster.doYouWantToMakeAnExit(false,0) <= 0;
		}
	}
	
	/**
	 * Check to see if a potential name is valid.
	 * @param potentialName name to check
	 * @param unavailableNames 
	 * @return "true" if acceptable, "false" otherwise
	 */
	private boolean validateName(String potentialName, Collection<String> unavailableNames){
		FXUIGameMaster.diagnosticPrintln("("+potentialName+")");
		potentialName = potentialName.trim();
		if(potentialName == null || potentialName.length() < 1){
			return false;
		}
		if(unavailableNames != null && unavailableNames.contains(potentialName)){
			return false;
		}
		//String desiredCharSet = "[a-zA-Z0-9]{1-21}\\s[a-zA-Z0-9]{1-21}|[a-zA-Z0-9]{1,21}"; //chars with one space inbetween
		String desiredCharSet = "[a-zA-Z0-9]{1,"+MAX_NAME_LENGTH+"}((\\s)?[a-zA-Z0-9]{0,"+MAX_NAME_LENGTH+"})?"; //chars with one space in between
		Pattern patternToFind = Pattern.compile(desiredCharSet); 
		// TODO optimize the above line; maybe relocate it to the functions which
		// would likely call this method. That way, it's only compiled once.
		Matcher matchContainer = patternToFind.matcher(potentialName);
		return matchContainer.matches() && potentialName.length() < MAX_NAME_LENGTH+1;
	}
	
	/**
	* Getter for the player name. Ideally, the name is only set once, so no separate "setter" exists.
	* @return name
	*/
	public String getName() {
		return this.name;
	}
	
	/**
	 * Presents a dialog to ask a user for the name they want
	 * Designed to run on a main game thread that is NOT the JavaFX thread.
	 * @param unavailableNames names that are already taken
	 * @return
	 */
	private String askForDesiredName(Collection<String> unavailableNames) {
		final int maxTimes = 5;
		final AtomicInteger timesLeft = new AtomicInteger(maxTimes);
		System.out.println("Getting name of human...");
		
		TextField potentialName = new TextField();
		//Make the window and keep displaying until the user has confirmed selection
		do{
			this.keepRunning = false;
			final VBox layout = new VBox(10);
			final Text guideText = new Text(); //generic prompt info
			final Text guideText2 = new Text(); //in-deoth prompt info
			final Text statusText = new Text(); //status: acceptable or unacceptable
			timesLeft.set(maxTimes);
			Platform.runLater(new Runnable(){
				@Override public void run(){
					
					/***********
					* Begin mandatory processing on FX thread. (Required for Stage objects.)
					*/
					
					final Stage dialog = new Stage();
					FXUIPlayer.crossbar.setCurrentPlayerDialog(dialog);
					potentialName.setDisable(false);
					
					//now let us continue with window/element setup
					dialog.setTitle("Set Player Name.");
					if(FXUIPlayer.owner != null){
						dialog.initOwner(FXUIPlayer.owner);
					}
					
					layout.setAlignment(Pos.CENTER);
					layout.setStyle("-fx-padding: 20;");
					
					guideText.setText("Please give us a name for your player, Human.");
					guideText.setTextAlignment(TextAlignment.CENTER);
					guideText.setFont(Font.font("System", 16));
					
					guideText2.setText("\n"+MAX_NAME_LENGTH+" character limit,\nalphanumeric chacters allowed,\n1 space allowed,\nleading or trailing space ignored");
					guideText2.setTextAlignment(TextAlignment.CENTER);
					guideText2.setFont(Font.font("System", 13));
					
					statusText.setText("--------");
					
					potentialName.setPromptText("[enter name here]");
					
					Button checkName = new Button("check name");
					Button acceptIt = new Button("accept/ok");
					Button autoSet = new Button("skip(auto-set)");
					
					potentialName.setOnKeyTyped(new EventHandler<KeyEvent>(){
						@Override public void handle(KeyEvent t){
							//if(validateName(t.getCharacter())){
								//put stuff here if you want to ignore any invalid input off-the-bat
								//e.g., if they enter a single invalid character, 
								//we can catch it when they enter it
							//}
							timesLeft.set(maxTimes);
							if(validateName(potentialName.getText()+t.getCharacter(), unavailableNames))
							{
								statusText.setText("name OK!!");
								statusText.setFill(Color.BLACK);
							}
							else{
								statusText.setText("invalid!!");
								statusText.setFill(Color.BLACK);
							}
						}
					});
					checkName.setOnAction(new EventHandler<ActionEvent>(){
						@Override public void handle(ActionEvent t){
							potentialName.setDisable(true);
							if(validateName(potentialName.getText(), unavailableNames))
							{
								statusText.setText("name OK!!");
								statusText.setFill(Color.BLUE);
							}
							else{
								statusText.setText("invalid!!");
								statusText.setFill(Color.RED);
							}
							potentialName.setDisable(false);
						}
					});
					acceptIt.setOnAction(new EventHandler<ActionEvent>(){
						@Override public void handle(ActionEvent t){
							potentialName.setDisable(true);
							if(validateName(potentialName.getText(), unavailableNames))
							{
								exitDecider.setAsNonSystemClose();
								saveLastKnownWindowLocation(dialog);
								dialog.close();
							}
							else{
								statusText.setText("invalid!!");
								statusText.setFill(Color.RED);
								potentialName.setDisable(false);
							}
						}
					});
					autoSet.setOnAction(new EventHandler<ActionEvent>(){
						@Override public void handle(ActionEvent t){
							if(validateName(potentialName.getText(), unavailableNames))
							{
								exitDecider.setAsNonSystemClose();
								saveLastKnownWindowLocation(dialog);
								dialog.close();
							}
							else{
								timesLeft.decrementAndGet();
								statusText.setText("press " + timesLeft + "x to auto-set");
								statusText.setFill(Color.BLACK);
								if(timesLeft.get() == 0){
									potentialName.setText(null);
									exitDecider.setAsNonSystemClose();
									saveLastKnownWindowLocation(dialog);
									dialog.close();
								}
							}
						}
					});
					
					layout.getChildren().addAll(guideText, guideText2,statusText, potentialName, checkName, acceptIt, autoSet);
					//formally add linear layout to scene, and display the dialog
					dialog.setScene(new Scene(layout));
					dialog.show();
				}
			});
			
			/**
			* End mandatory FX thread processing.
			* Immediately following this, pause to wait for FX dialog to be closed!
			*/
			waitForDialogToClose(FXUIPlayer.crossbar);
			checkIfCloseMeansMore(exitDecider, FXUIPlayer.crossbar);
			reinforcementsApplied = 0;
			FXUIPlayer.crossbar.setCurrentPlayerDialog(null);
		}
		while(this.keepRunning);
		return potentialName.getText();
	}
	
	/**
		* Specify an allocation of the player's initial reinforcements.
		* RESPONSE REQUIRED
		* @param map
		* @param reinforcements
		* @return initial allocation
		*/
	public ReinforcementResponse getInitialAllocation(RiskMap map, int reinforcements) {
		//if the player asked to end the game, don't even display the dialog
		if(crossbar.isHumanEndingGame()){
			return null;
		}

		//else...make the window and keep displaying until the user has confirmed selection
		final ReinforcementResponse rsp = new ReinforcementResponse();
		do{
			this.keepRunning = false;
			
			final Set<Country> myCountries = RiskUtils.getPlayerCountries(map, this.name);
			final HashMap<String, Integer> countryUsedReinforcementCount = new HashMap<String, Integer>();
			final HashMap<String, Text> countryTextCache = new HashMap<String, Text>();
			
			final VBox layout = new VBox(10);
			final Text guideText = new Text(); //generic instructions for initial allocation
			final Text statusText = new Text(); //status: total reinforcements available, reinf used, reinf available.
			Platform.runLater(new Runnable(){
				@Override public void run(){
					/***********
					* Begin mandatory processing on FX thread. (Required for Stage objects.)
					*/
					
					final Stage dialog = new Stage();
					
					//now let us continue with window/element setup
					dialog.setTitle("Initial Troop Allocation!");
					if(FXUIPlayer.owner != null){
						dialog.initOwner(FXUIPlayer.owner);
					}
					putWindowAtLastKnownLocation(dialog);
					
					layout.setAlignment(Pos.CENTER);
					layout.setStyle("-fx-padding: 20;");
					
					guideText.setText("You have been assigned starting countries (seen below)\nand " + reinforcements + " initial troops;"
					+ "\nplease allocate those troops now.\nOne troop per country minimum;\nMust use all available troops.");
					guideText.setTextAlignment(TextAlignment.CENTER);
					layout.getChildren().add(guideText);
					
					statusText.setText("Total: " + reinforcements + "\nUsed: " + reinforcementsApplied + "\nAvailable: " + (reinforcements - reinforcementsApplied));
					
					
					/*
					* Text to indicate country + count being sent to that country for each country you own,
					* plus the buttons to increment/decrement said count. (Target minimum count for each country is 1).
					* Text + buttons are immediately added to the target vertical layout one row/country at a time.
					*/
					for (final Country ctIn : myCountries)
					{
						final HBox singleCountryDisp = new HBox(4);
						singleCountryDisp.setAlignment(Pos.CENTER);
						map.getCountryArmies(ctIn);
						countryUsedReinforcementCount.put(ctIn.getName(), 1);
						reinforcementsApplied++;
						countryTextCache.put(ctIn.getName(), new Text(ctIn.getName() + " + 1"));
						singleCountryDisp.getChildren().add(countryTextCache.get(ctIn.getName()));
						
						//button to increment reinforcement count for selected country
						Button plus = new Button ("+");
						plus.setOnAction(new EventHandler<ActionEvent>() {
							@Override
							public void handle(ActionEvent event){
								final String countryAffected = ctIn.getName();
								if (reinforcementsApplied + 1 <= reinforcements){
									reinforcementsApplied++;
									countryUsedReinforcementCount.put(countryAffected, countryUsedReinforcementCount.get(countryAffected)+1);
								}
								refreshReinforcementDisplay(false,countryTextCache,countryUsedReinforcementCount, statusText, reinforcements);
							}
						});
						//button to decrement reinforcement count for selected country
						Button minus = new Button ("-");
						minus.setOnAction(new EventHandler<ActionEvent>(){
							@Override public void handle(ActionEvent t){
								final String countryAffected = ctIn.getName();
								if (reinforcementsApplied - 1 >= 0 && countryUsedReinforcementCount.get(countryAffected) - 1 >= 1){
									reinforcementsApplied--;
									countryUsedReinforcementCount.put(countryAffected, countryUsedReinforcementCount.get(countryAffected)-1);
								}
								refreshReinforcementDisplay(false,countryTextCache,countryUsedReinforcementCount, statusText, reinforcements);
							}
						});
						singleCountryDisp.getChildren().addAll(plus, minus);
						layout.getChildren().add(singleCountryDisp);
					}
					
					refreshReinforcementDisplay(false,countryTextCache,countryUsedReinforcementCount, statusText, reinforcements);
					
					//button to attempt to accept final reinforcement allocation
					Button acceptIt = new Button ("Accept/OK");
					acceptIt.setOnAction(new EventHandler<ActionEvent>(){
						@Override public void handle(ActionEvent t){
							if (reinforcementsApplied == reinforcements)
							{
								for (Country country : myCountries){
									rsp.reinforce(country, countryUsedReinforcementCount.get(country.getName()));
								}
								exitDecider.setAsNonSystemClose();
								saveLastKnownWindowLocation(dialog);
								dialog.close();
							}
							else{
								refreshReinforcementDisplay(true,countryTextCache,countryUsedReinforcementCount, statusText, reinforcements);
							}
						}
					});
					
					layout.getChildren().addAll(statusText, acceptIt);
					//formally add linear layout to scene, and display the dialog
					dialog.setScene(new Scene(layout));
					FXUIPlayer.crossbar.setCurrentHumanName(getName());
					FXUIPlayer.crossbar.setCurrentPlayerDialog(dialog);
					dialog.show();
				}
			});
			
			/**
			* End mandatory FX thread processing.
			* Immediately following this, pause to wait for FX dialog to be closed!
			*/
			waitForDialogToClose(FXUIPlayer.crossbar);
			checkIfCloseMeansMore(exitDecider, FXUIPlayer.crossbar);
			reinforcementsApplied = 0;
			FXUIPlayer.crossbar.setCurrentPlayerDialog(null);
		}
		while(this.keepRunning);
		return rsp;
	}
	
	/**
	* Propose a subset of the player's cards that can be redeemed for additional reinforcements.
	* RESPONSE REQUIRED WHEN PLAYER HOLDS AT LEAST 5 CARDS, OTHERWISE OPTIONAL
	* Dummy method for this class.
	* @param map
	* @param myCards
	* @param playerCards
	* @param turnInRequired
	* @return subset of the player's cards
	*/
	@Override
	public CardTurnInResponse proposeTurnIn(RiskMap map, Collection<Card> myCards, Map<String, Integer> playerCards, boolean turnInRequired) {
		//if the player asked to end the game, don't even display the dialog
		if(crossbar.isHumanEndingGame()){
			return null;
		}
		
		//else...make the window and keep displaying until the user has confirmed selection
		final CardTurnInResponse rsp = new CardTurnInResponse();
		final HashMap<Integer, Card> cardsToTurnIn = new HashMap<>();
		do{
			this.passTurn = true;
			this.keepRunning = false;
			final HashMap<Integer, Text> cardStatusMapping = new HashMap<>();
			Platform.runLater(new Runnable(){
				@Override public void run(){
					
					/***********
					* Begin mandatory processing on FX thread. (Required for Stage objects.)
					*/
					final Stage dialog = new Stage();
					final String selected = "*SELECTED*";
					final String deselected = "not selected";
					
					Text guideText = new Text(); //guide text: generic instructions for turning in cards
					final String guideTextIfRequired = "As you have " + myCards.size() + " cards,\nplease turn in a selection of 3 cards:\n3x same type\nOR\n3x different type\nOR\nWild+Any combo\n"
					+ "[This action is required for this round]";
					final String guideTextIfOptional = "Turn In Cards?\nIf you can form a set of cards with...\n3x same type\nOR\n3x different type\nOR\nWild+Any two\n"
					+ "...You are allowed to do so at this point.\nOtherwise, you may review your cards for later use.";
					
					final VBox layout = new VBox(10);
					
					final HBox cardArrayDisplayRowA = new HBox(4);
					final HBox cardArrayDisplayRowB = new HBox(4);
					
					final Text statusText = new Text("--\n--"); //status text: used to indicate if an error occurred upon attempted submission
					Button acceptIt = new Button ("Accept/OK");
					Button skipIt = new Button ("Skip Action");
					
					
					//now...begin handling the layout details and such.
					dialog.setTitle(turnInRequired ? "Please Turn In Cards (required)" : "Turn In Cards? (optional)");
					if(FXUIPlayer.owner != null){
						dialog.initOwner(FXUIPlayer.owner);
					}
					
					putWindowAtLastKnownLocation(dialog);
					
					layout.setAlignment(Pos.CENTER);
					layout.setStyle("-fx-padding: 20;");
					
					//further set up the guide text, depending on whether you must turn in cards or not
					guideText.setText(turnInRequired ? guideTextIfRequired : guideTextIfOptional);
					guideText.setTextAlignment(TextAlignment.CENTER);
					
					//set up the player's Cards for display, if any Cards are available. (each Card is represented as a button)
					cardArrayDisplayRowA.setAlignment(Pos.CENTER);
					cardArrayDisplayRowB.setAlignment(Pos.CENTER);
					int indexInCards = 0;
					
					for (final Card cdIn : myCards){
						final int indexInCardsUM = indexInCards;
						final VBox cardWithStatus = new VBox(4);
						Text subText = new Text(deselected);
						
						cardStatusMapping.put(indexInCards, subText);
						String ctySrced = cdIn.getType().equals(RiskConstants.WILD_CARD) ? "wild" : cdIn.getCountry().getName();
						Button card = new Button ("******\n[type]\n" + cdIn.getType().toLowerCase() + "\n*****\n" + ctySrced.toLowerCase() + "\n[country]\n******");
						card.setAlignment(Pos.CENTER);
						card.setTextAlignment(TextAlignment.CENTER);
						card.setOnAction(new EventHandler<ActionEvent>(){
							@Override public void handle(ActionEvent t){
								final Integer cardAffected = (Integer)indexInCardsUM;
								if (cardsToTurnIn.containsKey(cardAffected)) {
									cardsToTurnIn.remove(cardAffected);
									cardStatusMapping.get(cardAffected).setText(deselected);
									cardStatusMapping.get(cardAffected).setFont(Font.getDefault());
								}
								else {
									cardsToTurnIn.put(cardAffected, cdIn);
									cardStatusMapping.get(cardAffected).setText(selected);
									cardStatusMapping.get(cardAffected).setFont(Font.font(null, FontWeight.BOLD, -1));
								}
							}
						});
						indexInCards++;
						cardWithStatus.getChildren().addAll(card, subText);
						if(indexInCards > 2){cardArrayDisplayRowB.getChildren().add(cardWithStatus);}
						else {cardArrayDisplayRowA.getChildren().add(cardWithStatus);}
					}
					
					acceptIt.setOnAction(new EventHandler<ActionEvent>(){
						@Override public void handle(ActionEvent t){
							if (cardsToTurnIn.size() == RiskConstants.NUM_CARD_TURN_IN){
								for (Card cdOut : cardsToTurnIn.values()){
									rsp.addCard(cdOut);
								}
								if (CardTurnInResponse.isValidResponse(rsp, myCards)){
									passTurn = false;
									exitDecider.setAsNonSystemClose();
									saveLastKnownWindowLocation(dialog);
									dialog.close();
								}
								else{
									statusText.setText("invalid selection.\n(cards not a valid set)");
									rsp.resetCards();
								}
							}
							else if(!turnInRequired){
								passTurn = true;
								exitDecider.setAsNonSystemClose();
								saveLastKnownWindowLocation(dialog);
								dialog.close();
							}
							else{
								statusText.setText("invalid selection.\n(invalid card count. " + RiskConstants.NUM_CARD_TURN_IN +" required)");
							}
						}
					});
					
					skipIt.setOnAction(new EventHandler<ActionEvent>(){
						@Override public void handle(ActionEvent t){
							passTurn = true;
							exitDecider.setAsNonSystemClose();
							saveLastKnownWindowLocation(dialog);
							dialog.close();
						}
					});
					
					if(turnInRequired){
						skipIt.setDisable(true);
						skipIt.setText("Skip [unavailable]");
					}
					
					//add status and buttons to layout
					layout.getChildren().addAll(guideText, cardArrayDisplayRowA, cardArrayDisplayRowB, statusText, acceptIt,skipIt);
					
					//formally add linear layout to scene, and display the dialog
					dialog.setScene(new Scene(layout));
					FXUIPlayer.crossbar.setCurrentHumanName(getName());
					FXUIPlayer.crossbar.setCurrentPlayerDialog(dialog);
					dialog.show();
				}
			});
			
			/**
			* End mandatory FX thread processing.
			* Immediately after this, pause the non-UI thread (which you should be back on) and wait for the dialog to close!
			*/
			waitForDialogToClose(FXUIPlayer.crossbar);
			checkIfCloseMeansMore(exitDecider, FXUIPlayer.crossbar);
			FXUIPlayer.crossbar.setCurrentPlayerDialog(null);
		}
		while(this.keepRunning);
		
		if(passTurn){
			passTurn = false;
			return null;
		}
		return rsp;
	}
	
	/**
	* Specify an allocation of the player's reinforcements.
	* RESPONSE REQUIRED
	* @param map
	* @param myCards
	* @param playerCards
	* @param reinforcements
	* @return reinforcement allocation
	*/
	public ReinforcementResponse reinforce(RiskMap map, Collection<Card> myCards, Map<String, Integer> playerCards, int reinforcements){
		//if the player asked to end the game, don't even display the dialog
		if(crossbar.isHumanEndingGame()){
			return null;
		}
		
		//else...make the window and keep displaying until the user has confirmed selection
		final ReinforcementResponse rsp = new ReinforcementResponse();
		do{
			this.keepRunning = false;
			final Set<Country> myCountries = RiskUtils.getPlayerCountries(map, this.name);
			final HashMap<String, Integer> countryUsedReinforcementCount = new HashMap<String, Integer>();
			final HashMap<String, Text> countryTextCache = new HashMap<String, Text>();
			Platform.runLater(new Runnable(){
				@Override public void run(){
					/***********
					* Begin mandatory processing on FX thread. (Required for Stage objects.)
					*/
					final Stage dialog = new Stage();
					final VBox layout = new VBox(10);
					ScrollPane spane = new ScrollPane();
					Text guideText = new Text();
					final Text statusText = new Text();
					Button acceptIt = new Button ("Accept/OK");
					
					//updating the elements with their contents &/or styles...
					dialog.setTitle("Reinforcement with new troops!");
					if(FXUIPlayer.owner != null){
						dialog.initOwner(FXUIPlayer.owner);
					}
					putWindowAtLastKnownLocation(dialog);
					layout.setAlignment(Pos.CENTER);
					layout.setStyle("-fx-padding: 20;");
					
					//Generic instructions for reinforcement
					guideText.setText("Please place extra reinforcements\nin the countries you own.");
					guideText.setTextAlignment(TextAlignment.CENTER);
					layout.getChildren().add(guideText);
					
					//status text: total reinforcements available, reinf used, reinf available.
					statusText.setText("Total: " + reinforcements + "\nUsed: " + reinforcementsApplied + "\nAvailable: " + (reinforcements - reinforcementsApplied));
					
					//Meat and potatoes of the dialog, generates the display for each of the countries (along with their current count),
					//  as well as creates the buttons to increment/decrement the troop count for each associated country.
					//Each country (& its controls) is given its own row, and is added to the layout as it is generated here.
					//This button/text generation only happens once and is updated in-place.
					for (final Country ctIn : myCountries)
					{
						final HBox singleCountryDisp = new HBox(4);
						Button plus = new Button ("+");
						Button minus = new Button ("-");
						singleCountryDisp.setAlignment(Pos.CENTER);
						map.getCountryArmies(ctIn);
						countryUsedReinforcementCount.put(ctIn.getName(), 0);
						countryTextCache.put(ctIn.getName(), new Text(ctIn.getName() + " + 0")); //"place" the country and its current reinf count
						singleCountryDisp.getChildren().add(countryTextCache.get(ctIn.getName()));
						
						//set what "plus" button does (increment reinforcement count for selected country)
						plus.setOnAction(new EventHandler<ActionEvent>(){
							@Override public void handle(ActionEvent t){
								final String countryAffected = ctIn.getName();
								if (reinforcementsApplied + 1 <= reinforcements){
									reinforcementsApplied++;
									countryUsedReinforcementCount.put(countryAffected, countryUsedReinforcementCount.get(countryAffected)+1);
								}
								refreshReinforcementDisplay(false,countryTextCache,countryUsedReinforcementCount, statusText, reinforcements);
							}
						});
						
						//set what "minus" button does (decrement reinforcement count for selected country)
						minus.setOnAction(new EventHandler<ActionEvent>(){
							@Override public void handle(ActionEvent t){
								final String countryAffected = ctIn.getName();
								if (reinforcementsApplied - 1 >= 0 && countryUsedReinforcementCount.get(countryAffected) - 1 >= 0){
									reinforcementsApplied--;
									countryUsedReinforcementCount.put(countryAffected, countryUsedReinforcementCount.get(countryAffected)-1);
								}
								refreshReinforcementDisplay(false,countryTextCache,countryUsedReinforcementCount, statusText, reinforcements);
							}
						});
						singleCountryDisp.getChildren().addAll(plus, minus);
						layout.getChildren().add(singleCountryDisp);
					}
					
					//button to attempt to accept final reinforcement allocation
					acceptIt.setOnAction(new EventHandler<ActionEvent>() {
						@Override
						public void handle(ActionEvent event){
							if (reinforcementsApplied == reinforcements)
							{
								for (Country country : myCountries){
									rsp.reinforce(country, countryUsedReinforcementCount.get(country.getName()));
								}
								exitDecider.setAsNonSystemClose();
								saveLastKnownWindowLocation(dialog);
								dialog.close();
							}
							else{
								refreshReinforcementDisplay(true,countryTextCache,countryUsedReinforcementCount, statusText, reinforcements);
							}
						}
					});//end eventhandler actionevent
					
					//add status info Text and acceptance Button to layout. (Note: this method does not ever allow the player to "skip")
					layout.getChildren().addAll(statusText, acceptIt);
					
					//formally add linear layout to scene, and display the dialog
					spane.setContent(layout);
					dialog.setScene(new Scene(spane));
					FXUIPlayer.crossbar.setCurrentPlayerDialog(dialog);
					FXUIPlayer.crossbar.setCurrentHumanName(getName());
					refreshReinforcementDisplay(false,countryTextCache,countryUsedReinforcementCount, statusText, reinforcements);
					dialog.show();
				}
			});
			
			/**
			* End mandatory FX thread processing.
			* Immediately after this, pause the non-UI thread (which you should be back on) and wait for the dialog to close!
			*/
			waitForDialogToClose(FXUIPlayer.crossbar);
			checkIfCloseMeansMore(exitDecider, FXUIPlayer.crossbar);
			FXUIPlayer.crossbar.setCurrentPlayerDialog(null);
			reinforcementsApplied = 0;
		}
		while(this.keepRunning);
		return rsp;
	}
	
	
	/**
	* Helper method to refresh the display of the "Reinforce" dialog.
	*  Task includes simple indication of an error state upon improper reinforcement allocation.
	*
	* @param isError set to "True" if you want to indicate that an error state has been triggered, false otherwise
	* @param textElements Primary window content to be updated. Updated in-place. Updates only the affected country.
	* @param dataSource the integers representing the current troop counts, as mapped to each country
	* @param statusText Secondary window content to be updated; the so-called status text created in the "Reinforce" method. Updated in-place.
	* @param reinforcements the total number of reinforcements available to the user during this turn/during this singular call to "Reinforce"
	*/
	private void refreshReinforcementDisplay(boolean isError, HashMap<String, Text> textElements, HashMap<String, Integer> dataSource, Text statusText, int reinforcements) {
		statusText.setText("Total: " + reinforcements + "\nUsed: " + reinforcementsApplied + "\nAvailable: " + (reinforcements - reinforcementsApplied));
		for(String countryToUpdate : textElements.keySet()){
			textElements.get(countryToUpdate).setText(countryToUpdate + " ::: " + dataSource.get(countryToUpdate));
			if (isError){
				textElements.get(countryToUpdate).setFill(Color.RED);
				statusText.setFill(Color.RED);
			}
			else{
				textElements.get(countryToUpdate).setFill(Color.BLACK);
				statusText.setFill(Color.BLACK);
			}
		}
	}
	
	
	/**
	* Specify how and where an attack should be mounted.
	* RESPONSE OPTIONAL
	* @param map
	* @param myCards
	* @param playerCards
	* @return attack choice
	*/
	public AttackResponse attack(RiskMap map, Collection<Card> myCards, Map<String, Integer> playerCards) {
		//if the player asked to end the game, don't even display the dialog
		if(crossbar.isHumanEndingGame()){
			return null;
		}
		
		//else...make the window and keep displaying until the user has confirmed selection
		final AttackResponse rsp = new AttackResponse();
		do{
			this.keepRunning = false;
			Collection<Country> sources = RiskUtils.getPossibleSourceCountries(map, RiskUtils.getPlayerCountries(map, this.getName()));
			
			Platform.runLater(new Runnable(){
				@Override public void run(){
					
					/***********
					* Begin mandatory processing on FX thread. (Required for Stage objects.)
					*/
					
					ScrollPane spane = new ScrollPane();
					final Stage dialog = new Stage();
					final VBox layout = new VBox(10);
					
					Text guideText = new Text();
					final Text statusText = new Text();
					
					final VBox sourceCountriesVBox = new VBox(10);
					final VBox targetCountriesVBox = new VBox(10);
					
					Text diceCountStatus = new Text("Dice Count:\n- - -");
					final Button diceCountDec = new Button ("Dice--");
					final Button diceCountInc = new Button ("Dice++");
					final HBox diceDisplay = new HBox(10);
					
					ScrollPane spaneLeft = new ScrollPane();
					ScrollPane spaneRight = new ScrollPane();
					final HBox bothCountryGroups = new HBox(10);
					
					Button acceptIt = new Button ("Accept/OK");
					Button skipIt = new Button ("[skip/pass]");
					
					HBox acceptanceBtns = new HBox(10);
					Text buttonDivider = new Text("***********");
					
					//now that things have been placed in memory, let's set it all up...
					dialog.setTitle("Attack? [optional]");
					if(FXUIPlayer.owner != null){
						dialog.initOwner(FXUIPlayer.owner);
					}
					
					
					layout.setAlignment(Pos.CENTER);
					layout.setStyle("-fx-padding: 20;");
					putWindowAtLastKnownLocation(dialog);
					
					//Generic instructions for attacking (the act of which is always optional, technically)
					guideText.setText("Select the country from which you want to attack [left],\nthen select the target of your attack [right].\n[attacking is optional; you may pass]");
					guideText.setTextAlignment(TextAlignment.CENTER);
					
					//status text: the target of the attack (name of country, when set), and the source of the attacks (name of country, when set)
					statusText.setText("Current selection: Attacking\n[no selection???]\nfrom\n[no selection???].");
					statusText.setTextAlignment(TextAlignment.CENTER);
					
					sourceCountriesVBox.setAlignment(Pos.CENTER);
					sourceCountriesVBox.setFillWidth(true);
					targetCountriesVBox.setAlignment(Pos.CENTER);
					targetCountriesVBox.setFillWidth(true);
					sourceCountriesVBox.getChildren().add(new Text("Source:"));
					
					//pre-setup for dice selection -- position in the dialog box, and disable buttons (you can't immediately change the dice count)
					diceCountStatus.setTextAlignment(TextAlignment.CENTER);
					diceCountInc.setDisable(true);
					diceCountDec.setDisable(true);
					diceDisplay.getChildren().addAll(diceCountDec, diceCountStatus, diceCountInc);
					diceDisplay.setAlignment(Pos.CENTER);
					//the actions for the increment and decrement buttons, when buttons are available
					diceCountInc.setOnAction(new EventHandler<ActionEvent>(){
						@Override public void handle(ActionEvent t){
							if (rsp.getNumDice() < maxAtkDiceAvailable)
							{
								rsp.setNumDice(rsp.getNumDice()+1);
								updateDiceDisplay(diceCountStatus, rsp.getNumDice(), maxAtkDiceAvailable, diceCountDec, diceCountInc);
							}
						}
					});
					
					diceCountDec.setOnAction(new EventHandler<ActionEvent>(){
						@Override public void handle(ActionEvent t){
							if (rsp.getNumDice() > 1)
							{
								rsp.setNumDice(rsp.getNumDice()-1);
								updateDiceDisplay(diceCountStatus, rsp.getNumDice(), maxAtkDiceAvailable, diceCountDec, diceCountInc);
							}
						}
					});
					
					/* Buttons representing your countries (source countries) from which you can attack, which -- when pressed -- will unveil the destination
					* countries (targets countries) at which your attack can be focused
					* */
					for (Country source : sources)
					{
						final Button ctSrcBtn = new Button(source.getName());
						ctSrcBtn.setOnAction(new EventHandler<ActionEvent>(){
							@Override public void handle(ActionEvent t){
								rsp.setAtkCountry(source);
								maxAtkDiceAvailable = map.getCountryArmies(rsp.getAtkCountry()) > RiskConstants.MAX_ATK_DICE ? RiskConstants.MAX_ATK_DICE : map.getCountryArmies(rsp.getAtkCountry()) - 1;
								rsp.setNumDice(maxAtkDiceAvailable); //default to the max dice available for an attack
								updateDiceDisplay(diceCountStatus, rsp.getNumDice(), maxAtkDiceAvailable, diceCountDec, diceCountInc);
								attackSource = source.getName();
								attackTarget = blankText;
								statusText.setText("Current selection: Attacking\n" + attackTarget + "\nfrom\n" + attackSource + ".");
								statusText.setFill(Color.BLACK);
								targetCountriesVBox.getChildren().clear();
								targetCountriesVBox.getChildren().add(new Text("Target:"));
								for (Country target : source.getNeighbors())
								{
									if (!map.getCountryOwner(target).equals(getName())) {
										final Button ctTgtBtn = new Button(target.getName());
										ctTgtBtn.setOnAction(new EventHandler<ActionEvent>(){
											@Override public void handle(ActionEvent t){
												rsp.setDfdCountry(target);
												attackTarget = target.getName();
												statusText.setText("Current selection: Attacking\n" + attackTarget + "\nfrom\n" + attackSource + ".");
											}
										});
										targetCountriesVBox.getChildren().add(ctTgtBtn);
									}
								}
							}
						});
						//finally add this source button for this singular country
						sourceCountriesVBox.getChildren().add(ctSrcBtn);
					}
					
					//button to attempt to accept final reinforcement allocation
					acceptIt.setOnAction(new EventHandler<ActionEvent>(){
						@Override public void handle(ActionEvent t){
							if (rsp.getAtkCountry() != null && rsp.getDfdCountry() != null)
							{
								if(!AttackResponse.isValidResponse(rsp, map, getName()))
								{
									statusText.setText("Not a valid response; try another combo.");
									statusText.setFill(Color.RED);
								}
								else{
									passTurn = false;
									exitDecider.setAsNonSystemClose();
									saveLastKnownWindowLocation(dialog);
									dialog.close();
								}
							}
							else
							{
								statusText.setText("Not a valid response; \nmake sure you select a target and source!!");
								statusText.setFill(Color.RED);
							}
						}
					});
					
					//if you want to pass on this action for this turn...
					skipIt.setOnAction(new EventHandler<ActionEvent>(){
						@Override public void handle(ActionEvent t){
							passTurn = true;
							exitDecider.setAsNonSystemClose();
							saveLastKnownWindowLocation(dialog);
							dialog.close();
						}
					});
					
					//finish setting up rest of layout...
					//includes double ScrollPane -- one for leftmost (source) contents, one for for rightmost (destination) contents
					spaneLeft.setPrefHeight(400);
					spaneLeft.setPrefWidth(200);
					spaneRight.setPrefHeight(400);
					spaneRight.setPrefWidth(200);
					spaneLeft.setFitToHeight(true);
					spaneLeft.setFitToWidth(true);
					spaneRight.setFitToHeight(true);
					spaneRight.setFitToWidth(true);
					
					spaneLeft.setContent(sourceCountriesVBox);
					spaneRight.setContent(targetCountriesVBox);
					
					bothCountryGroups.getChildren().addAll(spaneLeft, spaneRight);
					bothCountryGroups.setAlignment(Pos.CENTER);
					
					acceptanceBtns.getChildren().addAll(acceptIt, skipIt);
					acceptanceBtns.setAlignment(Pos.CENTER);
					
					//add status and buttons to layout
					buttonDivider.setTextAlignment(TextAlignment.CENTER);
					layout.getChildren().addAll(guideText, statusText, bothCountryGroups, buttonDivider, diceDisplay, acceptanceBtns);
					layout.setAlignment(Pos.CENTER);
					
					//formally add linear layout to scene through the use of a scroll pane, and display the dialog
					spane.setContent(layout);
					dialog.setScene(new Scene(spane));
					FXUIPlayer.crossbar.setCurrentPlayerDialog(dialog);
					FXUIPlayer.crossbar.setCurrentHumanName(getName());
					dialog.show();
				}
			});
			
			/**
			* End mandatory FX thread processing.
			* Immediately after this, pause the non-UI thread (which you should be back on) and wait for the dialog to close!
			*/
			waitForDialogToClose(FXUIPlayer.crossbar);
			checkIfCloseMeansMore(exitDecider, FXUIPlayer.crossbar);
			//if we have completed all business within the dialog, cleanup and return as required.
			FXUIPlayer.crossbar.setCurrentPlayerDialog(null);
			attackSource = blankText;
			attackTarget = blankText;
		}
		while(this.keepRunning);
		if(passTurn){
			passTurn = false;
			return null;
		}
		return rsp;
	}
	
	
	/**
	* Used with the attack dialog to update the status of the dice -- count used, available buttons, etc
	* @param diceStatusDisplay the Text field indicating the status of the dice
	* @param currentDiceCount the current number of dice to be applied/rolled when the attack commences
	* @param maxDiceCount the max number of dice available to roll given available troops
	* @param decBtn the button to decrement the dice count (disabled under select circumstances, re-enabled otherwise)
	* @param incBtn the button to increment the dice count (disabled under select circumstances, re-enabled otherwise)
	*/
	private void updateDiceDisplay(Text diceStatusDisplay, int currentDiceCount, int maxDiceCount, Button decBtn, Button incBtn){
		final String dieOrDice = currentDiceCount == 1 ? "die" : "dice";
		diceStatusDisplay.setText("Rolling " + currentDiceCount + " " 
				+ dieOrDice + ".\n(" + maxDiceCount + " allowed)");
		decBtn.setDisable(false);
		if(maxDiceCount > currentDiceCount){
			incBtn.setDisable(false);
		}
		else{
			incBtn.setDisable(true);
		}
		if(currentDiceCount == 1){
			decBtn.setDisable(true);
		}
		else{
			decBtn.setDisable(false);
		}
	}
	
	/**
	* Specify the number of armies that should be advanced into a conquered territory.
	* RESPONSE REQUIRED
	* @param map
	* @param myCards
	* @param playerCards
	* @param fromCountry
	* @param toCountry
	* @param min
	* @return advance choice
	*/
	public AdvanceResponse advance(RiskMap map, Collection<Card> myCards, Map<String, Integer> playerCards, Country fromCountry, Country toCountry, int minAdv){
		//if the player asked to end the game, don't even display the dialog
		if(crossbar.isHumanEndingGame()){
			return null;
		}
		
		//else...make the window and keep displaying until the user has confirmed selection
		final AdvanceResponse rsp = new AdvanceResponse(minAdv);
		do{
			this.keepRunning = false;
			final int sourceArmies = map.getCountryArmies(fromCountry);
			Platform.runLater(new Runnable(){
				@Override public void run(){
					
					/***********
					* Begin mandatory processing on FX thread. (Required for Stage objects.)
					*/
					
					final Stage dialog = new Stage();
					final VBox layout = new VBox(10);
					
					final Text sourceCount = new Text();
					final Text destCount = new Text();
					HBox countryCounts = new HBox(4);
					
					final Button plusle = new Button("Add/+");
					final Button minun = new Button("Recall/-");
					final HBox allocationButtons = new HBox(4);
					
					final Button acceptance = new Button("Submit/OK");
					final Text acceptanceStatus = new Text("Minimum to advance: " + minAdv);
					
					dialog.setTitle("Advance armies into conquests");
					if(FXUIPlayer.owner != null){
						dialog.initOwner(FXUIPlayer.owner);
					}
					
					putWindowAtLastKnownLocation(dialog);
					sourceCount.setTextAlignment(TextAlignment.CENTER);
					destCount.setTextAlignment(TextAlignment.CENTER);
					acceptanceStatus.setTextAlignment(TextAlignment.CENTER);
					
					final class UpdateStatus{
						boolean doubleCheck = false;
						UpdateStatus(){
						}
						public void refreshStatus(){
							sourceCount.setText(fromCountry.getName() + "\n:::::\n" + (sourceArmies - rsp.getNumArmies()));
							destCount.setText(toCountry.getName() + "\n:::::\n" + rsp.getNumArmies());
							doubleCheck = false;
						}
						public void resetAcceptance()
						{
							acceptanceStatus.setText("Minimum to advance: " + minAdv);
							doubleCheck = false;
						}
						public boolean verifyAcceptance()
						{
							if (sourceArmies - rsp.getNumArmies() != 0 && rsp.getNumArmies() != 0)
							{
								return true;
							}
							else if (!doubleCheck){
								acceptanceStatus.setText("You cannot leave 0 army members in " + (rsp.getNumArmies() == 0 ? toCountry.getName():fromCountry.getName()) + "?");
								return false;
							}
							else{
								
								return true;
							}
						}
					}
					
					countryCounts.setAlignment(Pos.CENTER);
					countryCounts.getChildren().addAll(sourceCount, destCount);
					
					final UpdateStatus updater = new UpdateStatus();
					updater.refreshStatus();
					
					plusle.setOnAction(new EventHandler<ActionEvent>() {
						@Override
						public void handle(ActionEvent event){
							updater.resetAcceptance();
							if (rsp.getNumArmies() < sourceArmies - 1)
							{
								rsp.setNumArmies(rsp.getNumArmies() + 1);
								updater.resetAcceptance();
								updater.refreshStatus();
							}
						}
					});
					minun.setOnAction(new EventHandler<ActionEvent>() {
						@Override
						public void handle(ActionEvent event){
							updater.resetAcceptance();
							if (rsp.getNumArmies() > minAdv + 1 && sourceArmies - (rsp.getNumArmies() + 1) >= 0)
							{
								updater.resetAcceptance();
								rsp.setNumArmies(rsp.getNumArmies() - 1);
								updater.refreshStatus();
							}
						}
					});
					
					acceptance.setOnAction(new EventHandler<ActionEvent>() {
						@Override
						public void handle(ActionEvent event){
							//we can't verify it with the official function, 
							//but we can check if we've actually put our soldiers somewhere
							//and if we so decide, it's possible to just skip proper allocation (wherein either src or dst has 0 troops
							if(updater.verifyAcceptance())
							{
								exitDecider.setAsNonSystemClose();
								saveLastKnownWindowLocation(dialog);
								dialog.close();
							}
						}
					});
					
					allocationButtons.setAlignment(Pos.CENTER);
					allocationButtons.getChildren().addAll(minun,plusle);
					
					layout.setAlignment(Pos.CENTER);
					layout.setStyle("-fx-padding: 20;");
					layout.getChildren().setAll(
					countryCounts, allocationButtons,acceptanceStatus, acceptance
					);
					
					//finally place the layout into the new dialog window, and display the dialog.
					dialog.setScene(new Scene(layout));
					FXUIPlayer.crossbar.setCurrentPlayerDialog(dialog);
					FXUIPlayer.crossbar.setCurrentHumanName(getName());
					dialog.show();
				}
			});
			
			/**
		* End mandatory FX thread processing.
		* Immediately after this, pause the non-UI thread (which you should be back on) and wait for the dialog to close!
		*/
			waitForDialogToClose(FXUIPlayer.crossbar);
			checkIfCloseMeansMore(exitDecider, FXUIPlayer.crossbar);
			FXUIPlayer.crossbar.setCurrentPlayerDialog(null);
		}
		while(this.keepRunning);
		return rsp;
	}
	
	/**
	* Propose a fortification transfer.
	* RESPONSE OPTIONAL
	* @param map
	* @param myCards
	* @param playerCards
	* @return fortification choice
	*/
	public FortifyResponse fortify(RiskMap map, Collection<Card> myCards, Map<String, Integer> playerCards){
		//if the player asked to end the game, don't even display the dialog
		if(crossbar.isHumanEndingGame()){
			return null;
		}
		//else...make the window and keep displaying until the user has confirmed selection
		
		final FortifyResponse rsp = new FortifyResponse();
		
		do{
			this.keepRunning = false;
			Collection<Country> sources = RiskUtils.getPossibleSourceCountries(map, RiskUtils.getPlayerCountries(map, this.name));
			Collection<Set<Country>> allConnectedSets = RiskUtils.getAllConnectedCountrySets(map, this.name);
			Map<Country, Set<Country>> destMap = new HashMap<Country, Set<Country>>();
			
			//Create mapping of each possible fortification source to all possible destinations
			for (Country source : sources) {
				if (!destMap.containsKey(source)) {
					boolean found = false;
					for (Set<Country> connectedSet : allConnectedSets) {
						for (Country country : connectedSet) {
							if (source == country) {
								destMap.put(source, connectedSet);
								//destMap.get(source).remove(source);
								found = true;
								break;
							}
						}
						if (found) {
							break;
						}
					}
				}
			}
			
			
			Platform.runLater(new Runnable(){
				@Override public void run(){
					/***********
					 * Begin mandatory processing on FX thread. (Required for Stage objects.)
					 */
					
					ScrollPane spane = new ScrollPane();
					final Stage dialog = new Stage();
					dialog.setTitle("Fortify? [optional]");
					if(FXUIPlayer.owner != null){
						dialog.initOwner(FXUIPlayer.owner);
					}
					
					putWindowAtLastKnownLocation(dialog);
					
					final VBox layout = new VBox(10);
					layout.setAlignment(Pos.CENTER);
					layout.setStyle("-fx-padding: 20;");
					
					
					//Set up instructions for fortification & add it to the layout.
					Text guideText = new Text();
					guideText.setText("Select the country from which you want to fortify [left],\nthen select the destination for your troops [right].\n[fortification is optional; you can skip this]");
					guideText.setTextAlignment(TextAlignment.CENTER);
					layout.getChildren().add(guideText);
					
					//status text to be used for indicating the currently selected countries & the appropriate count
					//by default, shouldn't display any applicable country.
					//Set-up here doubles as a placeholder function to assist with proper automatic dialog sizing.
					final Text statusText = new Text();
					statusText.setText("Current selection: \n[No Selection]\n----\n----\n----");
					statusText.setTextAlignment(TextAlignment.CENTER);
					
					final VBox sourceCountriesVBox = new VBox(10);
					final VBox targetCountriesVBox = new VBox(10);
					sourceCountriesVBox.setAlignment(Pos.CENTER);
					targetCountriesVBox.setAlignment(Pos.CENTER);
					sourceCountriesVBox.setFillWidth(true);
					targetCountriesVBox.setFillWidth(true);
					sourceCountriesVBox.getChildren().add(new Text("Source:"));
					
					for (Country source : sources) {
						final Button ctSrcBtn = new Button(source.getName());
						//disable the buttons if there's no adjacent countries
						ctSrcBtn.setDisable(true);
						for (Country dest : destMap.get(source)) {
							if (dest != source) {
								ctSrcBtn.setDisable(false);
							}
						}
						//what to do when the buttons are pressed:
						//show the country/countries that can receive troops
						ctSrcBtn.setOnAction(new EventHandler<ActionEvent>() {
							@Override
							public void handle(ActionEvent event) {
								statusText.setText("Current selection:\nFortifying\n????\nusing ??? troops from\n" + source.getName() + ".");
								
								//reset the target country list and set it up when a given source button is pressed
								targetCountriesVBox.getChildren().clear();
								targetCountriesVBox.getChildren().add(new Text("Target:")); 
								rsp.setFromCountry(source);
								
								for (Country dest : destMap.get(source)) {
									if (dest != source) {
										final Button ctTgtBtn = new Button(dest.getName());
										ctTgtBtn.setOnAction(new EventHandler<ActionEvent>(){
											@Override public void handle(ActionEvent t){
												rsp.setToCountry(dest);
												statusText.setFill(Color.BLACK);
												statusText.setText("Current selection:\nFortifying\n" + dest.getName() + "\nusing ??? troops from\n" + source.getName() + ".");
											}//end of actionevent definition
										});
										targetCountriesVBox.getChildren().add(ctTgtBtn);
									}
								}
							}
						});
						//button to decrement reinforcement count for selected country
						sourceCountriesVBox.getChildren().add(ctSrcBtn);
					}
					
					
					//Leftmost ScrollPane is for the source countries, rightmost ScrollPane is for the destination/target countries.
					ScrollPane spaneLeft = new ScrollPane();
					spaneLeft.setPrefHeight(400);
					spaneLeft.setPrefWidth(200);
					ScrollPane spaneRight = new ScrollPane();
					spaneRight.setPrefHeight(400);
					spaneRight.setPrefWidth(200);
					spaneLeft.setFitToHeight(true);
					spaneLeft.setFitToWidth(true);
					spaneRight.setFitToHeight(true);
					spaneRight.setFitToWidth(true);
					
					spaneLeft.setContent(sourceCountriesVBox);
					spaneRight.setContent(targetCountriesVBox);
					
					
					
					//finally add both lists to the layout.
					final HBox bothCountryGroups = new HBox(10);
					bothCountryGroups.getChildren().addAll(spaneLeft, spaneRight);
					bothCountryGroups.setAlignment(Pos.CENTER);
					
					
					final Button plusle = new Button("Troops++");
					//plusle.setDefaultButton(true);
					plusle.setOnAction(new EventHandler<ActionEvent>() {
						@Override
						public void handle(ActionEvent event){
							int curArmies = rsp.getNumArmies();
							if (rsp.getToCountry() != null && curArmies < map.getCountryArmies(rsp.getFromCountry()) - 1)
							{
								rsp.setNumArmies(rsp.getNumArmies() + 1);
								statusText.setFill(Color.BLACK);
								statusText.setText("Current selection:\nFortifying\n" + rsp.getToCountry().getName() + "\nusing " + rsp.getNumArmies() + " troops from\n" + rsp.getFromCountry().getName() + ".");
							}
						}
					});
					
					final Button minun = new Button("Troops--");
					//minun.setDefaultButton(true);
					minun.setOnAction(new EventHandler<ActionEvent>() {
						@Override
						public void handle(ActionEvent event){
							int curArmies = rsp.getNumArmies();
							if (rsp.getToCountry() != null && curArmies > 0)
							{
								rsp.setNumArmies(rsp.getNumArmies() - 1);
								statusText.setFill(Color.BLACK);
								statusText.setText("Current selection:\nFortifying\n" + rsp.getToCountry().getName() + "\nusing " + rsp.getNumArmies() + " troops from\n" + rsp.getFromCountry().getName() + ".");
							}
						}
					});
					
					HBox plusMinusBtns = new HBox(4);
					plusMinusBtns.setAlignment(Pos.CENTER);
					plusMinusBtns.getChildren().addAll(minun,plusle);
					
					final String playaName = getName();
					//button to attempt to accept final reinforcement allocation
					Button acceptIt = new Button ("Accept/OK");
					acceptIt.setOnAction(new EventHandler<ActionEvent>() {
						@Override
						public void handle(ActionEvent event){
							if (rsp.getFromCountry() != null &&
								rsp.getToCountry() != null &&
								FortifyResponse.isValidResponse(rsp, map, playaName))
							{
								exitDecider.setAsNonSystemClose();
								passTurn = false;
								saveLastKnownWindowLocation(dialog);
								dialog.close();
							}
							else
							{
								statusText.setText("Not a valid response; \nmake sure you select a target and source!!");
								statusText.setFill(Color.RED);
							}
						}
						
					});
					
					Button skipIt = new Button ("[skip/pass]");
					skipIt.setOnAction(new EventHandler<ActionEvent>() {
						@Override
						public void handle(ActionEvent event){
							passTurn = true;
							exitDecider.setAsNonSystemClose();
							saveLastKnownWindowLocation(dialog);
							dialog.close();
						}
					});
					
					
					//add status and buttons to layout
					Text buttonDividerTop = new Text("***********");
					buttonDividerTop.setTextAlignment(TextAlignment.CENTER);
					Text buttonDividerBottom = new Text("***********");
					buttonDividerBottom.setTextAlignment(TextAlignment.CENTER);
					layout.getChildren().addAll(statusText, bothCountryGroups, buttonDividerTop, plusMinusBtns, buttonDividerBottom, acceptIt, skipIt);
					layout.setAlignment(Pos.CENTER);
					
					//formally add linear layout to scene through use of scrollpane, then display the dialog
					spane.setContent(layout);
					dialog.setScene(new Scene(spane));
					FXUIPlayer.crossbar.setCurrentHumanName(getName());
					FXUIPlayer.crossbar.setCurrentPlayerDialog(dialog);
					dialog.show();
				}
			});
			
			/**
		* End mandatory FX thread processing.
		* Immediately following this, pause to wait for FX dialog to be closed!
		*/
			waitForDialogToClose(FXUIPlayer.crossbar);
			checkIfCloseMeansMore(exitDecider, FXUIPlayer.crossbar);
			FXUIPlayer.crossbar.setCurrentPlayerDialog(null);
		}
		while(this.keepRunning);
		if(passTurn){
			passTurn = false;
			return null;
		}
		return rsp;
	}
	
	/**
	* Specify how a territory should be defended.
	* RESPONSE REQUIRED
	* @param map
	* @param myCards
	* @param playerCards
	* @param atkCountry
	* @param dfdCountry
	* @param numAtkDice
	* @return defense choice
	*/
	public DefendResponse defend(RiskMap map, Collection<Card> myCards, Map<String, Integer> playerCards, Country atkCountry, Country dfdCountry, int numAtkDice){
		//if the player asked to end the game, don't even display the dialog
		if(crossbar.isHumanEndingGame()){
			return null;
		}
		
		DefendResponse rsp = new DefendResponse();
		//else...make the window and keep displaying until the user has confirmed selection
		
		
		int numDice = map.getCountryArmies(dfdCountry);
		if (numDice > RiskConstants.MAX_DFD_DICE) {
			numDice = RiskConstants.MAX_DFD_DICE;
		}
		final int maxDfdDiceAvailable = numDice;
		rsp.setNumDice(maxDfdDiceAvailable);
		
		do{
			this.keepRunning = false;
			Platform.runLater(new Runnable(){
				@Override public void run(){
					
					/***********
					* Begin mandatory processing on FX thread. (Required for Stage objects.)
					*/
					
					ScrollPane spane = new ScrollPane();
					final Stage dialog = new Stage();
					final VBox layout = new VBox(10);
					
					Text guideText = new Text();
					final Text statusText = new Text();
					
					Text diceCountStatus = new Text("Dice Count: " + rsp.getNumDice() + "\n(" + maxDfdDiceAvailable + " allowed)");
					final Button diceCountDec = new Button ("Dice--");
					final Button diceCountInc = new Button ("Dice++");
					final HBox diceDisplay = new HBox(10);
					
					Button acceptIt = new Button ("Accept/OK");
					
					HBox acceptanceBtns = new HBox(10);
					Text buttonDivider = new Text("***********");
					
					//now that things have been placed in memory, let's set it all up...
					
					dialog.setTitle("Defend! (?)");
					if(FXUIPlayer.owner != null){
						dialog.initOwner(FXUIPlayer.owner);
					}
					
					
					layout.setAlignment(Pos.CENTER);
					layout.setStyle("-fx-padding: 20;");
					putWindowAtLastKnownLocation(dialog);
					
					//Generic instructions for attacking (the act of which is always optional, technically)
					final String guideTextContents = "You are being attacked!"
							+ "\nAn anemo--err, an enemy--"
							+ "\nhas chosen to attack you at"
							+ "\n" + dfdCountry.getName() + "!"
							+ "\nThe attacker is attacking from\n" 
							+ atkCountry.getName() + "."
							+ "\nYou must decide how to defend yourself!"
							+ "\n\nYou roll dice to defend. Roll the die"
							+ "\n(or two dice, if you own enough countries)"
							+ "\n(more dice = better chance of good defense)"
							+ "\n(more dice = more troops lost if you lose)"
							+ "\nYour attacker is rolling " + numAtkDice +"."
							+ "\nYOU can roll a maximum of " + maxDfdDiceAvailable + "."
							+ "\n...How many will you roll?";
					guideText.setText(guideTextContents);
					guideText.setTextAlignment(TextAlignment.CENTER);
					
					//status text: the target of the attack (name of country, when set), and the source of the attacks (name of country, when set)
					statusText.setText("~~~");
					statusText.setTextAlignment(TextAlignment.CENTER);
					
					
					//pre-setup for dice selection -- position in the dialog box, and disable buttons (you can't immediately change the dice count)
					diceCountStatus.setTextAlignment(TextAlignment.CENTER);
					diceDisplay.getChildren().addAll(diceCountDec, diceCountStatus, diceCountInc);
					diceDisplay.setAlignment(Pos.CENTER);
					//the actions for the increment and decrement buttons, when buttons are available
					diceCountInc.setOnAction(new EventHandler<ActionEvent>(){
						@Override public void handle(ActionEvent t){
							if (rsp.getNumDice() < maxDfdDiceAvailable)
							{
								rsp.setNumDice(rsp.getNumDice()+1);
								updateDiceDisplay(diceCountStatus, rsp.getNumDice(), maxDfdDiceAvailable, diceCountDec, diceCountInc);
							}
						}
					});
					
					diceCountDec.setOnAction(new EventHandler<ActionEvent>(){
						@Override public void handle(ActionEvent t){
							if (rsp.getNumDice() > 1)
							{
								rsp.setNumDice(rsp.getNumDice()-1);
								updateDiceDisplay(diceCountStatus, rsp.getNumDice(), maxDfdDiceAvailable, diceCountDec, diceCountInc);
							}
						}
					});
					
					//button to attempt to accept final reinforcement allocation
					acceptIt.setOnAction(new EventHandler<ActionEvent>(){
						@Override public void handle(ActionEvent t){
							passTurn = false;
							exitDecider.setAsNonSystemClose();
							saveLastKnownWindowLocation(dialog);
							dialog.close();
						}
					});
					
					acceptanceBtns.getChildren().addAll(acceptIt);
					acceptanceBtns.setAlignment(Pos.CENTER);
					
					//add status and buttons to layout
					buttonDivider.setTextAlignment(TextAlignment.CENTER);
					layout.getChildren().addAll(guideText, statusText, buttonDivider, diceDisplay, acceptanceBtns);
					layout.setAlignment(Pos.CENTER);
					
					//formally add linear layout to scene through the use of a scroll pane, and display the dialog
					spane.setContent(layout);
					dialog.setScene(new Scene(spane));
					FXUIPlayer.crossbar.setCurrentPlayerDialog(dialog);
					FXUIPlayer.crossbar.setCurrentHumanName(getName());
					dialog.show();
				}
			});
			
			/**
			* End mandatory FX thread processing.
			* Immediately after this, pause the non-UI thread (which you should be back on) and wait for the dialog to close!
			*/
			waitForDialogToClose(FXUIPlayer.crossbar);
			checkIfCloseMeansMore(exitDecider, FXUIPlayer.crossbar);
			//if we have completed all business within the dialog, cleanup and return as required.
			FXUIPlayer.crossbar.setCurrentPlayerDialog(null);
		}
		while(this.keepRunning);
		return rsp;
	}
	
	/**
	* to determine whether the user is still playing the game, or if the user initiated a normal program exit from the system
	*/
	class ExitStateSubHelper {

		private boolean systemExitUsed = true;

		/**
		 * Get whether the program should attempt to exit back to the OS, or if the app should continue running after "dialog.close()" is called
		 * @return "false" ONCE AND ONLY ONCE after "setAsNonSystemClose()" until the next time said method is called again,
		 * 		returns "true" otherwise. (So only ask if we are closing the application once, because it defaults to
		 * 		saying "yes, we want to close the entire application!" every subsequent time until you raise the flag again).
		 */
		public boolean isSystemExit(){
			final boolean cExit = systemExitUsed;
			systemExitUsed = true;
			return cExit;
		}

		/**
		 * Raises a flag to tell the program to not attempt to exit.
		 * 		Aka "the user is interacting with the program as per normal use, so a dialog closing is OK".
		 * Use every time you close a dialog to prevent the app from asking if you're trying to leave/exit.
		 * @return "false" to indicate that we have successfully told the app to not fully exit, "true" otherwise. (Should never have "true" as a return!)
		 */
		public boolean setAsNonSystemClose()
		{
			systemExitUsed = false;
			return systemExitUsed;
		}
	}
}