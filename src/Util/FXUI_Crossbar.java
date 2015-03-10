//FXUI GameMaster-Player Crossbar Link
//Albert Wallace, 2015. (Version 00x02h, Stamp 2015.03.05.2144, Type Final(FF).
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
	
	public String getPlayerName(){
		return playerName;
	}
	
	public void setPlayerName(String nameOfPlayer){
		playerName = nameOfPlayer;
	}
	
	public void signalPlayerEndingGame()
	{
		fxPlayerQuit = true;
	}
	
	public void resetEndGameSignal()
	{
		fxPlayerQuit = false;
	}
	
	public boolean isPlayerEndingGame(Player currentPlayer){
		return fxPlayerQuit && currentPlayer.getClass().toString().equals(FXUIPlayer.class.toString());
	}

}
