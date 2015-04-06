//FXUI GameMaster-Player Crossbar Link
//Albert Wallace, 2015. (Version 00x04h, Stamp 2015.04.05.1930, Type Final(FF).
//for Seth Denney's RISK, JavaFX UI-capable version

package Util;

import Player.FXUIPlayer;
import Player.Player;
import javafx.stage.Stage;


/*
 * This class is the result of a lazy need to communicate between the primary FXUI components.
 * 
 * FXUIGamemaster and FXUIPlayer need to trade data at various times without being able to
 * 	use return types, etc etc. 
 * 	To facilitate the enhanced data transfer, this crossbar was created.
 * 
 * FXUI_Crossbar may change over time in the future, depending on what things may need to be transferred.
 * 	If so desired, FXUI_Crossbar may be tied into a virtual "clock" should major amounts of threading come into play.
 */
public class FXUI_Crossbar {
	
	private static Stage currentPlayerJFXStage = null;
	private static boolean fxPlayerQuit = false;
	private static String playerName = "";

	public FXUI_Crossbar() {
		// TODO Auto-generated constructor stub
	}
	
	public void setCurrentPlayerDialog(Stage stageIn)
	{
		currentPlayerJFXStage = stageIn;
	}
	
	public Stage getCurrentPlayerDialog()
	{
		return currentPlayerJFXStage;
	}
	
	public boolean playerDialogIsActive()
	{
		return currentPlayerJFXStage != null;
	}
	
	public void tryCloseCurrentPlayerDialog()
	{
		if (currentPlayerJFXStage != null)
		{
			currentPlayerJFXStage.close();
		}
	}
	
	public void setAppHiddenState(boolean isHidden)
	{
		// TODO fill out stub & make use
	}
	
	public void isAppHidden(){
		// TODO fill out stub and make use
	}
	
	public String getPlayerName(){
		return playerName;
	}
	
	public void setCurrentHumanName(String nameOfPlayer){
		playerName = nameOfPlayer;
	}
	
	public void signalHumanEndingGame()
	{
		fxPlayerQuit = true;
	}
	
	public void resetEndGameSignal()
	{
		fxPlayerQuit = false;
	}
	
	/**
	 * Asks if the human(s) have elected to end the game. (It only takes one human to agree,
	 * 		 as this flag is carried in one crossbar across all human player objects).
	 * @return returns "true" if any human has elected to end the game, "false" otherwise.
	 */
	public boolean isHumanEndingGame(){
		return fxPlayerQuit;
	}

}
