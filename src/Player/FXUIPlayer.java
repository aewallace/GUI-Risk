/*//Current build Albert Wallace, Version 003, Stamp y2015.mdB16.hm0056.sMNT
//Base build from original "player" interface, 
//incorporating elements of nothing but http://stackoverflow.com/questions/16823644/java-fx-waiting-for-user-input
//so thanks stackoverflow!*/

package Player;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import sun.applet.Main;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
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
import Util.RiskConstants;
import Util.RiskUtils;

/**
 * Encapsulates the functionality required of any automated Risk player.
 *
 */
public class FXUIPlayer implements Player {
	private String name;
	//private String currentFocus = null;
	private int reinforcementsApplied = 0;
	private static boolean instanceAlreadyCreated = false;
	private boolean passTurn = false;
	private String attackTarget = "-----", attackSource = "------";
	
	public FXUIPlayer() {
		if (instanceAlreadyCreated)
		{
			throw new UnsupportedOperationException("One instance of FXUIPlayer allowed at a time!");
		}
		else
		{
			this.name = "FXUIPlayer";
		}
	}
	
	public FXUIPlayer(String ignoredNameIn) {
		if (instanceAlreadyCreated){
			throw new UnsupportedOperationException("One instance of FXUIPlayer allowed at a time!");
		}
		else{
			this.name = "FXUIPlayer";
		}
	}
	
	
	/**
	 * Specify an allocation of the player's initial reinforcements.
	 * RESPONSE REQUIRED
	 * @param map
	 * @param reinforcements
	 * @return initial allocation
	 */
	public ReinforcementResponse getInitialAllocation(RiskMap map, int reinforcements){
		return null;
	}
	
	public ReinforcementResponse getInitialAllocation(RiskMap map, int reinforcements, Window owner)
	{
		ReinforcementResponse rsp = new ReinforcementResponse();
		Set<Country> myCountries = RiskUtils.getPlayerCountries(map, this.name);
		HashMap<String, Integer> countryUsedReinforcementCount = new HashMap<String, Integer>();
		HashMap<String, Text> countryTextCache = new HashMap<String, Text>();
		
				try{
			      final Stage dialog = new Stage();
			      dialog.setTitle("Initial Troop Allocation!");
			      dialog.initOwner(owner);
			      dialog.initStyle(StageStyle.UTILITY);
			      dialog.initModality(Modality.WINDOW_MODAL);
			      dialog.setX(owner.getX()/* + owner.getWidth()*/);
			      dialog.setY(owner.getY());
			      
			      final VBox layout = new VBox(10);
			      layout.setAlignment(Pos.CENTER);
			      layout.setStyle("-fx-background-color: azure; -fx-padding: 10;");
			      /*layout.getChildren().setAll(
			        textField, 
			        submitButton
			      );*/
			      
			      //generic instructions for initial allocation
			      Text guideText = new Text();
			      guideText.setText("You have been assigned initial countries\nand " + reinforcements + " initial troops;"
			    		  + "\nplease allocate those troops now.\nOne troop per country minimum;\nMust use all available troops.");
			      guideText.setTextAlignment(TextAlignment.CENTER);
			      layout.getChildren().add(guideText);
			      
			      //status text: total reinforcements available, reinf used, reinf available.
			      Text statusText = new Text();
			      statusText.setText("Total: " + reinforcements + "\nUsed:" + reinforcementsApplied + "\nAvailable: " + (reinforcements - reinforcementsApplied));
			      
			      
			      //buttons for countries you own, and text to display *additional* units to deplor to each country
			      for (Country ctIn : myCountries)
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
					    plus.setOnAction(t -> { //lambda expression
							  final String countryAffected = ctIn.getName();
							  if (reinforcementsApplied + 1 <= reinforcements){
								  reinforcementsApplied++;
								  //System.out.println(countryAffected);
								  countryUsedReinforcementCount.put(countryAffected, countryUsedReinforcementCount.get(countryAffected)+1);
							  }
							  updateReinforcementCountGIA(false,countryTextCache,countryUsedReinforcementCount, statusText, reinforcements);
						});
					    //button to decrement reinforcement count for selected country
					    Button minus = new Button ("-");
					    minus.setOnAction(t -> { //lambda expression
					    		  final String countryAffected = ctIn.getName();
					    		  if (reinforcementsApplied - 1 >= 0 && countryUsedReinforcementCount.get(countryAffected) - 1 >= 1/* TODO use variable here*/){
					    			  reinforcementsApplied--;
					    			  countryUsedReinforcementCount.put(countryAffected, countryUsedReinforcementCount.get(countryAffected)-1);
					    		  }
					    		  updateReinforcementCountGIA(false,countryTextCache,countryUsedReinforcementCount, statusText, reinforcements);
					    });
						singleCountryDisp.getChildren().addAll(plus, minus);
						layout.getChildren().add(singleCountryDisp);						
					}
			      
			      updateReinforcementCountGIA(false,countryTextCache,countryUsedReinforcementCount, statusText, reinforcements);
			    
			      //button to attempt to accept final reinforcement allocation
			      Button acceptIt = new Button ("Accept/OK");
			      acceptIt.setOnAction(new EventHandler<ActionEvent>(){
			    	  @Override public void handle(ActionEvent t){
			    		  if (reinforcementsApplied == reinforcements)
			    		  {
			    			  for (Country country : myCountries){
			    				  rsp.reinforce(country, countryUsedReinforcementCount.get(country.getName()));
			    			  }
			    			  dialog.close();
			    		  }
			    		  else{
			    			  updateReinforcementCountGIA(true,countryTextCache,countryUsedReinforcementCount, statusText, reinforcements);
			    		  }
			    	  }
			      });
			      
			      layout.getChildren().addAll(statusText, acceptIt);
			      
			      //formally add linear layout to scene, and wait for the user to be done (click the OK button)
			      dialog.setScene(new Scene(layout));
			      //dialog.show();
			      dialog.showAndWait();
			
			      
			      }
					catch(Exception e){System.out.println(e);}
				finally{reinforcementsApplied = 0; /* TODO remove currentFocus=null;*/}
				//return result;
				if (ReinforcementResponse.isValidResponse(rsp, map, this.name, reinforcements)){
						System.out.println("returning rsp");}
		return rsp;
	}
	
	private void updateReinforcementCountGIA(boolean isError, HashMap<String, Text> textElements, HashMap<String, Integer> dataSource, Text statusText, int reinforcements){
		for(String countryToUpdate : textElements.keySet()){
			textElements.get(countryToUpdate).setText(countryToUpdate + " ::: " + dataSource.get(countryToUpdate));
			statusText.setText("Total: " + reinforcements + "\nUsed:" + reinforcementsApplied + "\nAvailable: " + (reinforcements - reinforcementsApplied));
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
	 * Propose a subset of the player's cards that can be redeemed for additional reinforcements.
	 * RESPONSE REQUIRED WHEN PLAYER HOLDS AT LEAST 5 CARDS, OTHERWISE OPTIONAL
	 * @param map
	 * @param myCards
	 * @param playerCards
	 * @param turnInRequired
	 * @return subset of the player's cards
	 */
	public CardTurnInResponse proposeTurnIn(RiskMap map, Collection<Card> myCards, Map<String, Integer> playerCards, boolean turnInRequired){
		return null;
	}
	
	/*
	 * @param owner: Window from which the associated UI dialog will spawn; given a JavaFX Pane object 'sourcePane', equals sourcePane.getScene().getWindow()
	 */
	public CardTurnInResponse proposeTurnIn(RiskMap map, Collection<Card> myCards, Map<String, Integer> playerCards, boolean turnInRequired, Window owner)
	{
		CardTurnInResponse rsp = new CardTurnInResponse();
		HashMap<Integer, Card> cardsToTurnIn = new HashMap<Integer, Card>();
		HashMap<Integer, Text> cardStatusMapping = new HashMap<Integer, Text>();
		try{
			
		      final Stage dialog = new Stage();
		      final String selected = "*SELECTED*";
		      final String deselected = "not selected";
		      dialog.setTitle(turnInRequired ? "Please Turn In Cards (required)" : "Turn In Cards? (optional)");
		      dialog.initOwner(owner);
		      dialog.initStyle(StageStyle.UTILITY);
		      dialog.initModality(Modality.WINDOW_MODAL);
		      dialog.setX(owner.getX()/* + owner.getWidth()*/);
		      dialog.setY(owner.getY());
		      
		      final VBox layout = new VBox(10);
		      layout.setAlignment(Pos.CENTER);
		      layout.setStyle("-fx-background-color: azure; -fx-padding: 10;");
		      /*layout.getChildren().setAll(
		        textField, 
		        submitButton
		      );*/
		      
		      //generic instructions for initial allocation
		      Text guideText = new Text();
		      guideText.setText(turnInRequired ? "As you have " + myCards.size() + " cards,\nplease turn in a selection of 3 cards:\n3x same type\nOR\n3x different type\nOR\nWild+Any combo\n"
		      		+ "[This action is required for this round]" : "Turn In Cards?\nIf you can form a set of cards with...\n3x same type\nOR\n3x different type\nOR\nWild+Any combo\n"
		      		+ "...You are allowed to do so at this point.\nOtherwise, you may review your cards for later use.");
		      guideText.setTextAlignment(TextAlignment.CENTER);
		      layout.getChildren().add(guideText);
		      
		      
		      
		      
		      //buttons for countries you own, and text to display *additional* units to deplor to each country
		      final HBox cardArrayDisplayRowA = new HBox(4);
		      final HBox cardArrayDisplayRowB = new HBox(4);
		      cardArrayDisplayRowA.setAlignment(Pos.CENTER);
		      cardArrayDisplayRowB.setAlignment(Pos.CENTER);
		      int indexInCards = 0;
		      for (Card cdIn : myCards)
				{
		    	  	final VBox cardWithStatus = new VBox(4);
		    	  	cardsToTurnIn.put(indexInCards, cdIn);
		    	  	Text subText = new Text(deselected);
		    	  	cardStatusMapping.put(indexInCards, subText);
					//button to increment reinforcement count for selected country
				    Button card = new Button ("******\n\n" + cdIn.toString() + "\n\n******");
				    card.setOnAction(new EventHandler<ActionEvent>(){
				    	  @Override public void handle(ActionEvent t){
				    		final Integer cardAffected = (Integer)indexInCards;
				    		final Card myCard = cdIn;
				    		if (cardsToTurnIn.containsKey(cardAffected)){
				    			cardsToTurnIn.remove(cardAffected); 
				    			cardStatusMapping.get(cardAffected).setText(deselected);
				    			cardStatusMapping.get(cardAffected).setFont(Font.getDefault());
				    			//selectedCardCount--;
				    		}
				    		else
				    		{
				    			cardsToTurnIn.put(cardAffected, cdIn);
				    			cardStatusMapping.get(cardAffected).setText(selected);
				    			cardStatusMapping.get(cardAffected).setFont(Font.font(null, FontWeight.BOLD, -1));
				    			//selectedCardCount++;
				    		}
				    		  //updateReinforcementCountGIA(false,countryTextCache,countryUsedReinforcementCount, statusText, reinforcements);
				    	  }
				    }
				    );
				    cardWithStatus.getChildren().addAll(card, subText);
				    if(indexInCards > 2){cardArrayDisplayRowB.getChildren().add(cardWithStatus);} else {cardArrayDisplayRowA.getChildren().add(cardWithStatus);}
										
				}

		      layout.getChildren().addAll(cardArrayDisplayRowA, cardArrayDisplayRowB);	
		      
		    //status text: used to indicate if an error occurred upon attempted submission
		      Text statusText = new Text("----");
		      
		      Button acceptIt = new Button ("Accept/OK");
		      acceptIt.setOnAction(new EventHandler<ActionEvent>(){
		    	  @Override public void handle(ActionEvent t){
		    		  if (cardsToTurnIn.size() == RiskConstants.NUM_CARD_TURN_IN){
		    			  for (Card cdOut : cardsToTurnIn.values()){
		    				  rsp.addCard(cdOut);
		    			  }
		    			  if (CardTurnInResponse.isValidResponse(rsp, myCards)){
		    				  dialog.close();
		    			  }
		    		  }
		    		  else if(!turnInRequired)
		    		  {
		    			  dialog.close();
		    		  }
		    		  else{
		    			  statusText.setText("invalid selection.\n(cards not a valid set)");
		    			  //updateReinforcementCountGIA(true,countryTextCache,countryUsedReinforcementCount, statusText, reinforcements);
		    		  }
		    	  }
		      });
		      
		      //add status and buttons to layout
		      layout.getChildren().addAll(statusText, acceptIt);
		      
		      //formally add linear layout to scene, and wait for the user to be done (click the OK button)
		      dialog.setScene(new Scene(layout));
		      dialog.showAndWait();
		}
		catch(Exception e){System.out.println(e);}
		return rsp;
	}
	
	//was to be used in above code should CardTurnInResponse class method fail
	private void depricatedIsValidCardResponse(){
		/*if (cardsToTurnIn.size() == RiskConstants.NUM_CARD_TURN_IN)
		  {
			  boolean isWild = false;
			  short card_type_0 = 0, card_type_1 = 0, card_type_2 = 0;
			  Collection<Card> cardCollectionOut = new ArrayList<Card>();
			  for (Card cdOut : cardsToTurnIn.values()){
				cardCollectionOut.add(cdOut);
				if (cdOut.getType() == RiskConstants.WILD_CARD)
					isWild = true;
				else if (cdOut.getType() ==  RiskConstants.REG_CARD_TYPES[0])
					card_type_0++;
				else if (cdOut.getType() == RiskConstants.REG_CARD_TYPES[1])
					card_type_1++;
				else if (cdOut.getType() == RiskConstants.REG_CARD_TYPES[2])
					card_type_2++;
			  }//end of for card loop
			  if (isWild || card_type_0 == RiskConstants.NUM_CARD_TURN_IN || card_type_1 == RiskConstants.NUM_CARD_TURN_IN || card_type_2 == RiskConstants.NUM_CARD_TURN_IN){
				  //make a response; we're golden
			  }
			  else if (card_type_0 + card_type_1 == 2 || card_type_1 + card_type_2 == 2 || card_type_0 + card_type_2 == 2)
			  {
				  //make a response; we're golden
			  }
			  else
			  {
				  //not a match
			  }
			  dialog.close();
		  }
		  else{
			  statusText.setText("invalid selection.");
			  //updateReinforcementCountGIA(true,countryTextCache,countryUsedReinforcementCount, statusText, reinforcements);
		  }*/
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
		return null;
	}
	
	public ReinforcementResponse reinforce(RiskMap map, Collection<Card> myCards, Map<String, Integer> playerCards, int reinforcements, Window owner){
		ReinforcementResponse rsp = new ReinforcementResponse();
		Set<Country> myCountries = RiskUtils.getPlayerCountries(map, this.name);
		HashMap<String, Integer> countryUsedReinforcementCount = new HashMap<String, Integer>();
		HashMap<String, Text> countryTextCache = new HashMap<String, Text>();
		
		//if, say, USA offered 5 reinfocements, with Mexico using 2 and Canada using 1, then <USA <Mexico, 2>> is the expected format
		//use in FORTIFY, not REINFORCE...
		//HashMap<String, HashMap<String, Integer>> countryNeighborReinforcementAllocation = new HashMap<String, HashMap<String, Integer>>();
		
				try{
			      final Stage dialog = new Stage();
			      dialog.setTitle("Reinforcement with new troops!");
			      dialog.initOwner(owner);
			      dialog.initStyle(StageStyle.UTILITY);
			      dialog.initModality(Modality.WINDOW_MODAL);
			      dialog.setX(owner.getX()/* + owner.getWidth()*/);
			      dialog.setY(owner.getY());
			      
			      final VBox layout = new VBox(10);
			      layout.setAlignment(Pos.CENTER);
			      layout.setStyle("-fx-background-color: azure; -fx-padding: 10;");
			      /*layout.getChildren().setAll(
			        textField, 
			        submitButton
			      );*/
			      
			      //Generic instructions for reinforcement
			      Text guideText = new Text();
			      guideText.setText("Please place your reinforcements\nin the countries you own.");
			      guideText.setTextAlignment(TextAlignment.CENTER);
			      layout.getChildren().add(guideText);
			      
			      //status text: total reinforcements available, reinf used, reinf available.
			      Text statusText = new Text();
			      statusText.setText("Total: " + reinforcements + "\nUsed:" + reinforcementsApplied + "\nAvailable: " + (reinforcements - reinforcementsApplied));
			      
			      
			      //buttons for countries you own, and text to display *additional* units to deplor to each country
			      for (Country ctIn : myCountries)
					{
			    	  	final HBox singleCountryDisp = new HBox(4);
			    	  	singleCountryDisp.setAlignment(Pos.CENTER);
						map.getCountryArmies(ctIn);
						countryUsedReinforcementCount.put(ctIn.getName(), 0);
						countryTextCache.put(ctIn.getName(), new Text(ctIn.getName() + " + 0"));
						singleCountryDisp.getChildren().add(countryTextCache.get(ctIn.getName()));
						final Button ctBtn = new Button(ctIn.getName());
						//button to increment reinforcement count for selected country
					    Button plus = new Button ("+");
					    plus.setOnAction(new EventHandler<ActionEvent>(){
					    	  @Override public void handle(ActionEvent t){
					    		  final String countryAffected = ctIn.getName();
					    		  if (reinforcementsApplied + 1 <= reinforcements){
					    			  reinforcementsApplied++;
					    			  countryUsedReinforcementCount.put(countryAffected, countryUsedReinforcementCount.get(countryAffected)+1);
					    		  }
					    		  updateReinforcementCountGIA(false,countryTextCache,countryUsedReinforcementCount, statusText, reinforcements);
					    	  }
					    }
					    );
					    //button to decrement reinforcement count for selected country
					    Button minus = new Button ("-");
					      minus.setOnAction(new EventHandler<ActionEvent>(){
					    	  @Override public void handle(ActionEvent t){
					    		  final String countryAffected = ctIn.getName();
					    		  if (reinforcementsApplied - 1 >= 0 && countryUsedReinforcementCount.get(countryAffected) - 1 >= 0 /* TODO use variable here*/){
					    			  reinforcementsApplied--;
					    			  countryUsedReinforcementCount.put(countryAffected, countryUsedReinforcementCount.get(countryAffected)-1);
					    		  }
					    		  updateReinforcementCountGIA(false,countryTextCache,countryUsedReinforcementCount, statusText, reinforcements);
					    	  }
					    }
					    );
						singleCountryDisp.getChildren().addAll(plus, minus);
						layout.getChildren().add(singleCountryDisp);						
					}
			      
			      updateReinforcementCountRIF(false,countryTextCache,countryUsedReinforcementCount, statusText, reinforcements);
			    
			      
			      //button to attempt to accept final reinforcement allocation
			      Button acceptIt = new Button ("Accept/OK");
			      acceptIt.setOnAction(new EventHandler<ActionEvent>(){
			    	  @Override public void handle(ActionEvent t){
			    		  if (reinforcementsApplied == reinforcements)
			    		  {
			    			  for (Country country : myCountries){
			    				  rsp.reinforce(country, countryUsedReinforcementCount.get(country.getName()));
			    			  }
			    			  dialog.close();
			    		  }
			    		  else{
			    			  updateReinforcementCountRIF(true,countryTextCache,countryUsedReinforcementCount, statusText, reinforcements);
			    		  }
			    	  }
			      }
			    		  );
			      
			      //add status and buttons to layout
			      //final HBox adjustmentButtons = new HBox(4);
		    	  //adjustmentButtons.setAlignment(Pos.CENTER);
			      layout.getChildren().addAll(statusText, acceptIt);
			      
			      //formally add linear layout to scene, and wait for the user to be done (click the OK button)
			      dialog.setScene(new Scene(layout));
			      //dialog.show();
			      dialog.showAndWait();
			
			      
			      }
					catch(Exception e){System.out.println(e);}
				finally{reinforcementsApplied = 0; /* TODO remove currentFocus=null;*/}
				//return result;
		return rsp;
	}
	
	private void updateReinforcementCountRIF(boolean isError, HashMap<String, Text> textElements, HashMap<String, Integer> dataSource, Text statusText, int reinforcements){
		for(String countryToUpdate : textElements.keySet()){
			textElements.get(countryToUpdate).setText(countryToUpdate + " ::: " + dataSource.get(countryToUpdate));
			statusText.setText("Total: " + reinforcements + "\nUsed:" + reinforcementsApplied + "\nAvailable: " + (reinforcements - reinforcementsApplied));
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
		return null;
	}
	
	public AttackResponse attack(RiskMap map, Collection<Card> myCards, Map<String, Integer> playerCards, Window owner){
		AttackResponse rsp = new AttackResponse();
		HashMap<String, Country> myCountries = new HashMap<String, Country>();
		HashMap<String, HashMap<String, Country>> countryNeighbors = new HashMap<String, HashMap<String, Country>>();
		HashMap<String, ArrayList<String>> countryNeighborsAsStrings = new HashMap<String, ArrayList<String>>();
		ArrayList<String> myCountriesAsStrings = new ArrayList<String>();
		//get the countries in an easily sorted string array representation, and store them in a map for easy reference
		for (Country country : RiskUtils.getPlayerCountries(map, this.name))
		{
			myCountries.put(country.getName(), country);
			myCountriesAsStrings.add(country.getName());
			//countryNeighbors.put(country.getName(), (ArrayList<Country>) country.getNeighbors());
			ArrayList<String> stng = new ArrayList<String>();
			HashMap<String, Country> cyAn = new HashMap<String, Country>();
			for (Country tgtCt : country.getNeighbors()){
				stng.add(tgtCt.getName());
				cyAn.put(tgtCt.getName(), tgtCt);
			}
			stng.sort(null);
			countryNeighborsAsStrings.put(country.getName(), stng);
			countryNeighbors.put(country.getName(), cyAn);
		}
		myCountriesAsStrings.sort(null);
		
		try{
			  ScrollPane spane = new ScrollPane();
		      final Stage dialog = new Stage();
		      dialog.setTitle("Attack? [optional]");
		      dialog.initOwner(owner);
		      dialog.initStyle(StageStyle.UTILITY);
		      dialog.initModality(Modality.WINDOW_MODAL);
		      dialog.setX(owner.getX()/* + owner.getWidth()*/);
		      dialog.setY(owner.getY());
		      
		      final VBox layout = new VBox(10);
		      layout.setAlignment(Pos.CENTER);
		      layout.setStyle("-fx-background-color: azure; -fx-padding: 10;");
		      
		      
		      //Generic instructions for reinforcement
		      Text guideText = new Text();
		      guideText.setText("Select the country from which you want to attack [left],\nthen select the target of your attack [right].\n[attacking is optional; you may pass]");
		      guideText.setTextAlignment(TextAlignment.CENTER);
		      layout.getChildren().add(guideText);
		      
		      //status text: total reinforcements available, reinf used, reinf available.
		      Text statusText = new Text();
		      statusText.setText("Current selection: [No Selection]");
		      
		      final VBox sourceCountriesVBox = new VBox(10);
		      VBox targetCountriesVBox = new VBox(10);
		      sourceCountriesVBox.setAlignment(Pos.CENTER);
		      targetCountriesVBox.setAlignment(Pos.CENTER);
		      
		      
		      //buttons for countries you own, and text to display *additional* units to deplor to each country
		      for (String ctSource : myCountriesAsStrings)
				{
					final Button ctSrcBtn = new Button(ctSource);
					//button to increment reinforcement count for selected country
				    ctSrcBtn.setOnAction(new EventHandler<ActionEvent>(){
				    	  @Override public void handle(ActionEvent t){
				    		  final String srcID = ctSource;
			    			  attackSource = srcID;
			    			  attackTarget = "-----"; /* TODO represent as variable*/
			    			  statusText.setText("Current selection: Attacking " + attackTarget + " from " + attackSource + ".");
				    		  targetCountriesVBox.getChildren().clear();
				    		  for (String ctTarget : countryNeighborsAsStrings.get(srcID))
				    		  {
				    			  final Button ctTgtBtn = new Button(ctTarget);
								  ctTgtBtn.setOnAction(new EventHandler<ActionEvent>(){
								    	  @Override public void handle(ActionEvent t){
							    			  final String tgtID = ctTarget;
							    			  attackTarget = tgtID;
							    			  statusText.setText("Current selection: Attacking " + attackTarget + " from " + attackSource + ".");
								    	  }//end of actionevent definition
								  });
								  targetCountriesVBox.getChildren().add(ctTgtBtn);
				    		  }//end of outer button for loop
				    	  }
				    }
				    );
				    //button to decrement reinforcement count for selected country
				    sourceCountriesVBox.getChildren().add(ctSrcBtn);
				}
		      
		      final HBox bothCountryGroups = new HBox(10);
		      bothCountryGroups.getChildren().addAll(sourceCountriesVBox, targetCountriesVBox);
		      bothCountryGroups.setAlignment(Pos.CENTER);
		    
		      
		      //button to attempt to accept final reinforcement allocation
		      Button acceptIt = new Button ("Accept/OK");
		      acceptIt.setOnAction(new EventHandler<ActionEvent>(){
		    	  @Override public void handle(ActionEvent t){
		    		  if (myCountriesAsStrings.contains(attackSource) && countryNeighborsAsStrings.get(attackSource).contains(attackTarget))
		    		  {
		    			int maxDiceAvailable = map.getCountryArmies(myCountries.get(attackSource)) > RiskConstants.MAX_ATK_DICE ? 3 : map.getCountryArmies(myCountries.get(attackSource)) - 1;
		    			if (maxDiceAvailable > 0)
		    			{
		    				rsp.setAtkCountry(myCountries.get(attackSource));
		    				rsp.setDfdCountry(countryNeighbors.get(attackSource).get(attackTarget));
		    				rsp.setNumDice(maxDiceAvailable);
		    				if(!AttackResponse.isValidResponse(rsp, map, getName()))
		    				{
		    					statusText.setText("Not a valid response; try another combo.");
		    				}
		    				else{
		    					dialog.close();
		    				}
		    			}
		    			else
		    			{
		    				statusText.setText("Not a valid response;\nAttack from a country with 2+ troops.");
		    			} 
		    		  }
		    		  else
		    		  {
		    			  statusText.setText("Not a valid response; \nmake sure you select a target and source!.");
		    		  }
		    		  
		    	  }
		      });
		      
		      Button skipIt = new Button ("[skip/pass]");
		      skipIt.setOnAction(new EventHandler<ActionEvent>(){
		    	  @Override public void handle(ActionEvent t){
		    		  passTurn = true;
		    		  dialog.close();
		    	  }
		      });
		      
		      //add status and buttons to layout
		      Text buttonBuffer = new Text("***********");
		      buttonBuffer.setTextAlignment(TextAlignment.CENTER);
		      layout.getChildren().addAll(statusText, bothCountryGroups, buttonBuffer, acceptIt, skipIt);
		      layout.setAlignment(Pos.CENTER);
		      
		      //formally add linear layout to scene, and wait for the user to be done (click the OK button)
		      spane.setContent(layout);
		      dialog.setScene(new Scene(spane));
		      //dialog.show();
		      dialog.showAndWait();
		
		      
		      }
				catch(Exception e){System.out.println(e);}
			finally{attackSource = "-----"; attackTarget = "-----";}
	
		
		if(passTurn){
			passTurn = !passTurn;
			return null;
		}
		return rsp;
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
	public AdvanceResponse advance(RiskMap map, Collection<Card> myCards, Map<String, Integer> playerCards, Country fromCountry, Country toCountry, int min){
		return null;
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
		return null;
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
		DefendResponse rsp = new DefendResponse();
		int numDice = map.getCountryArmies(dfdCountry);
		if (numDice > RiskConstants.MAX_DFD_DICE) {
			numDice = RiskConstants.MAX_DFD_DICE;
		}
		rsp.setNumDice(numDice);
		return rsp;
		// TODO this stolen from seth's cpu. must find a way to jiggle & juggle these bits later.
	}
	
	public String testRequiredInputPrompt(Window owner) {
			System.out.println("Hi Hi HIII");
			String result = "";
			
		try{
	      final Stage dialog = new Stage();
	      System.out.println("Hi Hi HIII T2");
	      
	      dialog.setTitle("Enter Missing Text");
	      dialog.initOwner(owner);
	      
	      System.out.println("Hi Hi HIII X2");
	      dialog.initStyle(StageStyle.UTILITY);
	      dialog.initModality(Modality.WINDOW_MODAL);
	      dialog.setX(owner.getX()/* + owner.getWidth()*/);
	      dialog.setY(owner.getY());
	      
	      System.out.println("Hi Hi HIII X3");

	      final TextField textField = new TextField();
	      final Button submitButton = new Button("Submit");
	      submitButton.setDefaultButton(true);
	      submitButton.setOnAction(new EventHandler<ActionEvent>() {
	        @Override public void handle(ActionEvent t) {
	          dialog.close();
	        }
	      });
	      textField.setMinHeight(TextField.USE_PREF_SIZE);
	      
	      System.out.println("Hi Hi HIII");

	      final VBox layout = new VBox(10);
	      layout.setAlignment(Pos.CENTER_RIGHT);
	      layout.setStyle("-fx-background-color: azure; -fx-padding: 10;");
	      layout.getChildren().setAll(
	        textField, 
	        submitButton
	      );

	      dialog.setScene(new Scene(layout));
	      dialog.showAndWait();

	      result = textField.getText();
	      
	      }
			catch(Exception e){System.out.println(e);}
		return result;
	      
	    }
	
	/**
	 * Getter for the player name.
	 * @return name
	 */
	public String getName(){
		return this.name;
	}
}