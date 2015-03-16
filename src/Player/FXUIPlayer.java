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


import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.stage.Window;
import Map.Country;
import Map.RiskMap;
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

// TODO revise handling of "system exits". Consider updating the crossbar, so FXUIGameMaster can response appropriately with a well-placed check?


/**
 * Encapsulates the UI elements a human may use
 * 	to respond to the GameMaster as a valid Player.
 * 
 * Requires FXUI GameMaster. Not compatible with original GameMaster;
 * 	implemented UI elements require triggering from active JavaFX application.
 * 
 * UI elements are JavaFX, done with Java JDK 8. (By extension, elements were done under JavaFX 8)
 * Compatibility with JDK 7 / JRE1.7 was retroactively restored. 
 * (source files with "Stamp" -- aka date/time stamp -- of Feb 21 2015, 6:00 PM -- aka Y2015.M02.D21.HM1800 -- & later apply).
 * JDK 7/JRE 1.7 will be the target until further notified.
 *
 */
public class FXUIPlayer implements Player {
	public static final String versionInfo = "FXUI-RISK-Player\nVersion 00x11h\nStamp 2015.03.15, 19:10\nType:Alpha(01)";

	private static boolean instanceAlreadyCreated = false;
	private static FXUI_Crossbar crossbar;
	
	private String name;
	private int reinforcementsApplied = 0;
	private int maxDiceAvailable = 0;
	private boolean passTurn = false;
	private final String blankText = "-----";
	private String attackTarget = blankText, attackSource = blankText;
	private static Window owner = null;

	
	/**
	 * to determine whether the user is still playing the game, or if the user initiated a normal program exit from the system
	 * @author wallace162x11
	 */
	class doWeExit{
		private boolean systemExitUsed = true;
		
		//get whether the program should attempt to exit back to the OS, or if the app should continue running after "dialog.close()" is called
		public boolean isSystemExit(){
			final boolean cExit = systemExitUsed;
			systemExitUsed = true;
			return cExit;
		}
		
		//tell the program to not attempt to exit; the user is interacting with the program as per normal use, so a dialog closing is OK
		public boolean setAsNonSystemClose()
		{
			systemExitUsed = false;
			return systemExitUsed;
		}
	}
	private final doWeExit exitDecider = new doWeExit();
	
	
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
	
	public static void setOwnerWindow(Window ownerIn) {
		FXUIPlayer.owner = ownerIn;
	}
	
	/**
	 * Getter for the player name. Ideally, the name is only set once, so no separate "setter" exists.
	 * @return name
	 */
	public String getName(){
		return this.name;
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
	
	/**
	 * Specify an allocation of the player's initial reinforcements.
	 * RESPONSE REQUIRED
	 * @param map
	 * @param reinforcements
	 * @return initial allocation
	 */
	public ReinforcementResponse getInitialAllocation(RiskMap map, int reinforcements) {
		//if the player asked to end the game, don't even display the dialog
		if(crossbar.isHumanEndingGame(this)){
	    	return null;
	    }
		
		//else...initial memory/variable allocation & early setup
		crossbar.setCurrentHumanName(getName());
		final ReinforcementResponse rsp = new ReinforcementResponse();
		final Set<Country> myCountries = RiskUtils.getPlayerCountries(map, this.name);
		final HashMap<String, Integer> countryUsedReinforcementCount = new HashMap<String, Integer>();
		final HashMap<String, Text> countryTextCache = new HashMap<String, Text>();
	    final Stage dialog = new Stage();
	    final VBox layout = new VBox(10);
	    Text guideText = new Text(); //generic instructions for initial allocation
	    final Text statusText = new Text(); //status: total reinforcements available, reinf used, reinf available.
	    
	    
	    //and now let us continue with window/element setup
	    dialog.setTitle("Initial Troop Allocation!");
	    dialog.initOwner(FXUIPlayer.owner);
	    //dialog.setX(owner.getX());
	    //dialog.setY(owner.getY());
	    
	    layout.setAlignment(Pos.CENTER);
	    layout.setStyle("-fx-padding: 20;");
	    
	    guideText.setText("You have been assigned initial countries\nand " + reinforcements + " initial troops;"
	  		+ "\nplease allocate those troops now.\nOne troop per country minimum;\nMust use all available troops.");
	    guideText.setTextAlignment(TextAlignment.CENTER);
	    layout.getChildren().add(guideText);
	    
	    statusText.setText("Total: " + reinforcements + "\nUsed:" + reinforcementsApplied + "\nAvailable: " + (reinforcements - reinforcementsApplied));
	    
	    
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
			  		if (reinforcementsApplied - 1 >= 0 && countryUsedReinforcementCount.get(countryAffected) - 1 >= 1/* TODO use variable here*/){
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
	  			dialog.close();
	  		}
	  		else{
	  			refreshReinforcementDisplay(true,countryTextCache,countryUsedReinforcementCount, statusText, reinforcements);
	  		}
	  	  }
	    });
	    
	    layout.getChildren().addAll(statusText, acceptIt);
	    
	    //formally add linear layout to scene, and wait for the user to be done (click the OK button)
	    dialog.setScene(new Scene(layout));
	    FXUIPlayer.crossbar.setCurrentPlayerDialog(dialog);
	    dialog.showAndWait();
	    if (exitDecider.isSystemExit())
	    {
	    	//throw new OSExitException("User pressed an OS-provided button to close a window or exit the program!");
	    } 
	    reinforcementsApplied = 0;
		FXUIPlayer.crossbar.setCurrentPlayerDialog(null);
		//return result;
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
		if(crossbar.isHumanEndingGame(this)){
	    	return null;
	    }
		
		crossbar.setCurrentHumanName(getName());
		final CardTurnInResponse rsp = new CardTurnInResponse();  
		final HashMap<Integer, Card> cardsToTurnIn = new HashMap<Integer, Card>();
		final HashMap<Integer, Text> cardStatusMapping = new HashMap<Integer, Text>();
		
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
	    dialog.initOwner(FXUIPlayer.owner);
	    
	    //dialog.setX(owner.getX());
	    //dialog.setY(owner.getY());
	    
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
			  		passTurn = false;
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
		  				exitDecider.setAsNonSystemClose();
		  				dialog.close();
		  			}
		  			else{
		  				statusText.setText("invalid selection.\n(cards not a valid set)");
		  			}
		  		}
		  		else if(!turnInRequired){
		  			exitDecider.setAsNonSystemClose();
		  			dialog.close();
		  			passTurn = true;
		  		}
		  		else{
		  			statusText.setText("invalid selection.\n(invalid card count. " + RiskConstants.NUM_CARD_TURN_IN +" required)");
		  		}
	    	}
	    });
	    
	    skipIt.setOnAction(new EventHandler<ActionEvent>(){
	  	  @Override public void handle(ActionEvent t){
		  		exitDecider.setAsNonSystemClose();
		  		dialog.close();
		  		passTurn = true;
	  	  }
	    });
	    
	    if(turnInRequired){
	  	  skipIt.setDisable(true);
	  	  skipIt.setText("Skip [unavailable]");
	    }
	    
	    //add status and buttons to layout
	    layout.getChildren().addAll(guideText, cardArrayDisplayRowA, cardArrayDisplayRowB, statusText, acceptIt);
	    
	    //formally add linear layout to scene, and wait for the user to be done (click the OK button)
	    dialog.setScene(new Scene(layout));
	    FXUIPlayer.crossbar.setCurrentPlayerDialog(dialog);
	    dialog.showAndWait();
	    if (exitDecider.isSystemExit())
	    {
	    	//throw new OSExitException("User pressed an OS-provided button to close a window or exit the program!");
	    }
	    
	    //final cleanup and return if everything completed successfully
		FXUIPlayer.crossbar.setCurrentPlayerDialog(null);
		if(passTurn){
			passTurn = !passTurn;
			return null;
		}
		else{
			for (Card card : cardsToTurnIn.values()) {
				rsp.addCard(card);
			}
			return rsp;
		}
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
		if(crossbar.isHumanEndingGame(this)){
	    	return null;
	    }
		
		//else, continue setting up the dialog...
		//some helpful data structures.
		final ReinforcementResponse rsp = new ReinforcementResponse();
		final Set<Country> myCountries = RiskUtils.getPlayerCountries(map, this.name);
		final HashMap<String, Integer> countryUsedReinforcementCount = new HashMap<String, Integer>();
		final HashMap<String, Text> countryTextCache = new HashMap<String, Text>();
	    final Stage dialog = new Stage();
	    final VBox layout = new VBox(10);
	    ScrollPane spane = new ScrollPane();
	    Text guideText = new Text();
	    final Text statusText = new Text();
	    Button acceptIt = new Button ("Accept/OK");
	    
	    //updating the elements with their contents &/or styles...
	    dialog.setTitle("Reinforcement with new troops!");
	    dialog.initOwner(FXUIPlayer.owner);
	    //dialog.setX(owner.getX());
	    //dialog.setY(owner.getY());
	    layout.setAlignment(Pos.CENTER);
	    layout.setStyle("-fx-padding: 20;");
	    
	    
	    //Generic instructions for reinforcement
	    guideText.setText("Please place your reinforcements\nin the countries you own.");
	    guideText.setTextAlignment(TextAlignment.CENTER);
	    layout.getChildren().add(guideText);
	    
	    //status text: total reinforcements available, reinf used, reinf available.
	    statusText.setText("Total: " + reinforcements + "\nUsed:" + reinforcementsApplied + "\nAvailable: " + (reinforcements - reinforcementsApplied));
	    
	    
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
			  		if (reinforcementsApplied - 1 >= 0 && countryUsedReinforcementCount.get(countryAffected) - 1 >= 0 /* TODO use variable here*/){
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
					dialog.close();
				}
				else{
					refreshReinforcementDisplay(true,countryTextCache,countryUsedReinforcementCount, statusText, reinforcements);
				}
	    	}
		});//end eventhandler actionevent
	    
	    //add status info Text and acceptance Button to layout. (Note: this method does not ever allow the player to "skip")
	    layout.getChildren().addAll(statusText, acceptIt);
	    
	    //formally add linear layout to scene, and wait for the user to be done (click the OK button)
	    spane.setContent(layout);
	    dialog.setScene(new Scene(spane));
	    FXUIPlayer.crossbar.setCurrentPlayerDialog(dialog);
	    FXUIPlayer.crossbar.setCurrentHumanName(getName());
	    refreshReinforcementDisplay(false,countryTextCache,countryUsedReinforcementCount, statusText, reinforcements);
	    dialog.showAndWait();
	
	    if (exitDecider.isSystemExit())
	    {
	    	//throw new OSExitException("User pressed an OS-provided button to close a window or exit the program!");
	    }
		reinforcementsApplied = 0;
		FXUIPlayer.crossbar.setCurrentPlayerDialog(null);
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
	private void refreshReinforcementDisplay(boolean isError, HashMap<String, Text> textElements, HashMap<String, Integer> dataSource, Text statusText, int reinforcements){
		statusText.setText("Total: " + reinforcements + "\nUsed:" + reinforcementsApplied + "\nAvailable: " + (reinforcements - reinforcementsApplied));
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
	public AttackResponse attack(RiskMap map, Collection<Card> myCards, Map<String, Integer> playerCards){
		//if the player asked to end the game, don't even display the dialog
		if(crossbar.isHumanEndingGame(this)){
	    	return null;
	    }
		
		final AttackResponse rsp = new AttackResponse();
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
	    dialog.initOwner(FXUIPlayer.owner);
	    
	    
	    layout.setAlignment(Pos.CENTER);
	    layout.setStyle("-fx-padding: 20;");
	    //dialog.setX(owner.getX());
	    //dialog.setY(owner.getY());
	    
	    //Generic instructions for attacking (the act of which is always optional, technically)
	    guideText.setText("Select the country from which you want to attack [left],\nthen select the target of your attack [right].\n[attacking is optional; you may pass]");
	    guideText.setTextAlignment(TextAlignment.CENTER);
	    
	    //status text: the target of the attack (name of country, when set), and the source of the attacks (name of country, when set)
	    statusText.setText("Current selection: Attacking\n[no selection???]\nfrom\n[no selection???].");
	    statusText.setTextAlignment(TextAlignment.CENTER);
	    
	    sourceCountriesVBox.setAlignment(Pos.CENTER);
	    targetCountriesVBox.setAlignment(Pos.CENTER);
	    sourceCountriesVBox.getChildren().add(new Text("Source:"));

		Collection<Country> sources = RiskUtils.getPossibleSourceCountries(map, RiskUtils.getPlayerCountries(map, this.name));
		
		//pre-setup for dice selection -- position in the dialog box, and disable buttons (you can't immediately change the dice count)
	    diceCountStatus.setTextAlignment(TextAlignment.CENTER);
	    diceCountInc.setDisable(true);
	    diceCountDec.setDisable(true);
	    diceDisplay.getChildren().addAll(diceCountDec, diceCountStatus, diceCountInc);
	    diceDisplay.setAlignment(Pos.CENTER);
	    //the actions for the increment and decrement buttons, when buttons are available
	    diceCountInc.setOnAction(new EventHandler<ActionEvent>(){
			@Override public void handle(ActionEvent t){
		  		if (rsp.getNumDice() < maxDiceAvailable)
		  		{
		  			rsp.setNumDice(rsp.getNumDice()+1);
		  			updateDiceDisplay(diceCountStatus, rsp.getNumDice(), maxDiceAvailable, diceCountDec, diceCountInc);
		  		}
			}
		});
		    
		diceCountDec.setOnAction(new EventHandler<ActionEvent>(){
			@Override public void handle(ActionEvent t){
		  		if (rsp.getNumDice() > 1)
		  		{
		  			rsp.setNumDice(rsp.getNumDice()-1);
		  			updateDiceDisplay(diceCountStatus, rsp.getNumDice(), maxDiceAvailable, diceCountDec, diceCountInc);
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
					int maxDiceAvailable = map.getCountryArmies(rsp.getAtkCountry()) > RiskConstants.MAX_ATK_DICE ? RiskConstants.MAX_ATK_DICE : map.getCountryArmies(rsp.getAtkCountry()) - 1;
				    rsp.setNumDice(maxDiceAvailable); //default to the max dice available for an attack
				    updateDiceDisplay(diceCountStatus, rsp.getNumDice(), maxDiceAvailable, diceCountDec, diceCountInc);
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
	  			//rsp.setNumDice(maxDiceAvailable);
  				if(!AttackResponse.isValidResponse(rsp, map, getName()))
  				{
  					statusText.setText("Not a valid response; try another combo.");
  					statusText.setFill(Color.RED);
  				}
  				else{
  					passTurn = false;
  					exitDecider.setAsNonSystemClose();
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
	  		dialog.close();
	  	  }
	    });
	    
	    //finish setting up rest of layout...
	    //includes double ScrollPane -- one for leftmost (source) contents, one for for rightmost (destination) contents
  		spaneLeft.setPrefHeight(400);
  		spaneLeft.setPrefWidth(200);
  		spaneRight.setPrefHeight(400);
  		spaneRight.setPrefWidth(200);
  		
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
	    
	    //formally add linear layout to scene, and wait for the user to be done (click the OK button)
	    spane.setContent(layout);
	    dialog.setScene(new Scene(spane));
	    FXUIPlayer.crossbar.setCurrentPlayerDialog(dialog);
	    FXUIPlayer.crossbar.setCurrentHumanName(getName());
	    dialog.showAndWait();
	    
	    if (exitDecider.isSystemExit())
	    {
	    	//throw new OSExitException("User pressed an OS-provided button to close a window or exit the program!");
	    }
		
	    //if we have completed all business within the dialog, cleanup and return as required.
		attackSource = blankText;
		attackTarget = blankText;
		FXUIPlayer.crossbar.setCurrentPlayerDialog(null);
		if(passTurn){
			passTurn = !passTurn;
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
		if (currentDiceCount == 1){
			diceStatusDisplay.setText("Rolling 1 die.\n(" + maxDiceCount + " allowed)");
			decBtn.setDisable(true);
		}
		else{
			diceStatusDisplay.setText("Rolling " + currentDiceCount + " dice.\n(" + maxDiceCount + " allowed)");
			decBtn.setDisable(false);
		}
		
		if(maxDiceCount > currentDiceCount){
			incBtn.setDisable(false);
		}
		else{
			incBtn.setDisable(true);
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
		if(crossbar.isHumanEndingGame(this)){
	    	return null;
	    }
		
		crossbar.setCurrentHumanName(getName());
		final int sourceArmies = map.getCountryArmies(fromCountry);
		final AdvanceResponse rsp = new AdvanceResponse(minAdv);
		
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
	    dialog.initOwner(FXUIPlayer.owner);
	    
	    //dialog.setX(owner.getX());
	    //dialog.setY(owner.getY());
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
					//System.out.println(rsp.getNumArmies());
					rsp.setNumArmies(rsp.getNumArmies() + 1);
					//System.out.println("adv rsp +" + rsp.getNumArmies());
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
			  	  //System.out.println(rsp.getNumArmies());
			  	  rsp.setNumArmies(rsp.getNumArmies() - 1);
			  	  //System.out.println("adv rsp -" + rsp.getNumArmies());
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

	    dialog.setScene(new Scene(layout));
	    FXUIPlayer.crossbar.setCurrentPlayerDialog(dialog);
	    dialog.showAndWait();

	    if (exitDecider.isSystemExit())
	    {
	    	//throw new OSExitException("User pressed an OS-provided button to close a window or exit the program!");
	    }
		FXUIPlayer.crossbar.setCurrentPlayerDialog(null);
		System.out.println("return adv rsp " + rsp.getNumArmies());
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
		if(crossbar.isHumanEndingGame(this)){
	    	return null;
	    }
		
		crossbar.setCurrentHumanName(getName());
		final FortifyResponse rsp = new FortifyResponse();
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
		
		ScrollPane spane = new ScrollPane();
		final Stage dialog = new Stage();
		dialog.setTitle("Fortify? [optional]");
		dialog.initOwner(FXUIPlayer.owner);
		
		//dialog.setX(owner.getX());
		//dialog.setY(owner.getY());
		
		//if autosizing of windows fails, revert to ugly hardcoding of window sizes.
		//dialog.setWidth(500);
		//dialog.setHeight(690);
		
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
		sourceCountriesVBox.setAlignment(Pos.CENTER_LEFT);
		targetCountriesVBox.setAlignment(Pos.CENTER_LEFT);
		sourceCountriesVBox.getChildren().add(new Text("Source:"));
		
		for (Country source : sources) {
			final Button ctSrcBtn = new Button(source.getName());
			
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
				  	  statusText.setText("Current selection:\nFortifying\n" + rsp.getToCountry().getName() + "\nusing " + rsp.getNumArmies() + " troops from\n" + rsp.getFromCountry().getName() + ".");
				    }
			}
		});
		
		HBox plusMinusBtns = new HBox(4);
		plusMinusBtns.setAlignment(Pos.CENTER);
		plusMinusBtns.getChildren().addAll(minun,plusle);
		
		final String playaName = this.getName();
		//button to attempt to accept final reinforcement allocation
		Button acceptIt = new Button ("Accept/OK");
		acceptIt.setOnAction(new EventHandler<ActionEvent>() {
			@Override
		  	public void handle(ActionEvent event){
					if (FortifyResponse.isValidResponse(rsp, map, playaName))
					{
						exitDecider.setAsNonSystemClose();
						passTurn = false;
						dialog.close();
					}
					else
					{
						statusText.setText("Not a valid response; \nmake sure you select a target and source!.");
					}
			}
			
		});
		
		Button skipIt = new Button ("[skip/pass]");
		skipIt.setOnAction(new EventHandler<ActionEvent>() {
			@Override
		  	public void handle(ActionEvent event){
					passTurn = true;
					exitDecider.setAsNonSystemClose();
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
		
		//formally add linear layout to scene, and wait for the user to be done (click the OK button)
		spane.setContent(layout);
		dialog.setScene(new Scene(spane));
		//dialog.setScene(new Scene(layout));
		FXUIPlayer.crossbar.setCurrentPlayerDialog(dialog);
		dialog.showAndWait();
		if (exitDecider.isSystemExit())
		{
			 //throw new OSExitException("User pressed an OS-provided button to close a window or exit the program!");
		}  
		FXUIPlayer.crossbar.setCurrentPlayerDialog(null);
		if(passTurn){
			passTurn = !passTurn;
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
		if(crossbar.isHumanEndingGame(this)){
	    	return null;
	    }
		DefendResponse rsp = new DefendResponse();
		int numDice = map.getCountryArmies(dfdCountry);
		if (numDice > RiskConstants.MAX_DFD_DICE) {
			numDice = RiskConstants.MAX_DFD_DICE;
		}
		rsp.setNumDice(numDice);
		return rsp;
		// TODO this stolen from seth's cpu. must find a way to jiggle & juggle these bits later if necessary.
	}
}