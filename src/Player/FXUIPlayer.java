//Current build Albert Wallace, Version 001, Stamp y2015.mdB12.hm2302.sMNT
//Base build from original "player" interface, 
//incorporating elements of nothing but http://stackoverflow.com/questions/16823644/java-fx-waiting-for-user-input
//so thanks stackoverflow!

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
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
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
import Util.RiskUtils;

/**
 * Encapsulates the functionality required of any automated Risk player.
 *
 */
public class FXUIPlayer implements Player {
	private String name;
	private String currentFocus = "";
	private int reinforcementsApplied = 0;
	private static boolean instanceAlreadyCreated = false;
	
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
		HashMap<String, Integer> countryReinfCache = new HashMap<String, Integer>();
		String result = "";
		
				try{
			      final Stage dialog = new Stage();
			      dialog.setTitle("Enter Missing Text");
			      dialog.initOwner(owner);
			      dialog.initStyle(StageStyle.UTILITY);
			      dialog.initModality(Modality.WINDOW_MODAL);
			      dialog.setX(owner.getX()/* + owner.getWidth()*/);
			      dialog.setY(owner.getY());
			      final TextField textField = new TextField();
			      HashMap<String, Text> countryTextCache = new HashMap<String, Text>();
			      final Button submitButton = new Button("Submit");
			      submitButton.setDefaultButton(true);
			      submitButton.setOnAction(new EventHandler<ActionEvent>() {
			        @Override public void handle(ActionEvent t) {
			          dialog.close();
			        }
			      });
			      textField.setMinHeight(TextField.USE_PREF_SIZE);
			      
			      final VBox layout = new VBox(10);
			      layout.setAlignment(Pos.CENTER_RIGHT);
			      layout.setStyle("-fx-background-color: azure; -fx-padding: 10;");
			      layout.getChildren().setAll(
			        textField, 
			        submitButton
			      );
			      
			      for (Country ctIn : myCountries)
					{
						map.getCountryArmies(ctIn);
						countryReinfCache.put(ctIn.getName(), 1);
						reinforcementsApplied++;
						countryTextCache.put(ctIn.getName(), new Text(ctIn.getName() + " + 1"));
						layout.getChildren().add(countryTextCache.get(ctIn.getName()));
						final Button ctBtn = new Button(ctIn.getName());
						ctBtn.setOnAction(new EventHandler<ActionEvent>(){
							@Override public void handle(ActionEvent t){
								final String ctBtnCty = ctIn.getName();
								currentFocus = ctBtnCty;
							}
						});
						layout.getChildren().add(ctBtn);
						
					}
			      
			      updateReinforcementCount(false,countryTextCache,countryReinfCache);
			      
			      //button to increment reinforcement count for selected country
			      Button plus = new Button ("+");
			      plus.setOnAction(new EventHandler<ActionEvent>(){
			    	  @Override public void handle(ActionEvent t){
			    		  if (reinforcementsApplied + 1 <= reinforcements){
			    			  reinforcementsApplied++;
			    			  countryReinfCache.put(currentFocus, countryReinfCache.get(currentFocus)+1);
			    		  }
			    		  updateReinforcementCount(false,countryTextCache,countryReinfCache);
			    	  }
			      }
			    		  );
			      //button to decrement reinforcement count for selected country
			      Button minus = new Button ("-");
			      minus.setOnAction(new EventHandler<ActionEvent>(){
			    	  @Override public void handle(ActionEvent t){
			    		  if (reinforcementsApplied - 1 >= 0 && countryReinfCache.get(currentFocus) - 1 >= 1){
			    			  reinforcementsApplied--;
			    			  countryReinfCache.put(currentFocus, countryReinfCache.get(currentFocus)-1);
			    		  }
			    		  updateReinforcementCount(false,countryTextCache,countryReinfCache);
			    	  }
			      }
			    		  );
			      
			      //button to attempt to accept final reinforcement allocation
			      Button acceptIt = new Button ("Accept/OK");
			      acceptIt.setOnAction(new EventHandler<ActionEvent>(){
			    	  @Override public void handle(ActionEvent t){
			    		  if (reinforcementsApplied == reinforcements)
			    		  {
			    			  for (Country country : myCountries){
			    				  rsp.reinforce(country, countryReinfCache.get(country.getName()));
			    			  }
			    			  dialog.close();
			    		  }
			    		  else{
			    			  updateReinforcementCount(true,countryTextCache,countryReinfCache);
			    		  }
			    	  }
			      }
			    		  );
			      
			      //add all buttons to linear layout
			      layout.getChildren().addAll(plus, minus, acceptIt);
			      
			      //formally add linear layout to scene, and wait for the user to be done (click the OK button)
			      dialog.setScene(new Scene(layout));
			      dialog.showAndWait();
			
			      result = textField.getText();
			      
			      }
					catch(Exception e){System.out.println(e);}
				//return result;
		return rsp;
	}
	
	public void updateReinforcementCount(boolean isError, HashMap<String, Text> textElements, HashMap<String, Integer> dataSource){
		for(String countryToUpdate : textElements.keySet()){
			textElements.get(countryToUpdate).setText(countryToUpdate + " ::: " + dataSource.get(countryToUpdate));
			if (!isError){
				textElements.get(countryToUpdate).setFill(Color.RED);
			}
			else{
				textElements.get(countryToUpdate).setFill(Color.BLACK);
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
		return null;
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