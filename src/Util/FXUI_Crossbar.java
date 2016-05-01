//FXUI GameMaster-Player Crossbar Link
//Albert Wallace, 2015. (Version 0006, Stamp 2016.5.01, 00:03, Type Alpha(01).
//for Seth Denney's RISK, JavaFX UI-capable version

package Util;

import java.util.ArrayList;
import javafx.application.Platform;
import javafx.scene.paint.Color;
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
	private static String lastHPlayerName = "";
	private static ArrayList<String> allHPlayerNames = new ArrayList<String>();
	private static double britenessOpacity = 1.0d;
	private static Color strainReliefColor = Color.BLACK;

	public FXUI_Crossbar() {
		// TODO Auto-generated constructor stub
	}
	
	public void setCurrentPlayerDialog(Stage stageIn)
	{
		currentPlayerJFXStage = stageIn;
	}
	
	public void setCurrentPlayerDialog(Stage stageIn, String pName)
	{
		currentPlayerJFXStage = stageIn;
		lastHPlayerName = pName;
	}
	
	public Stage getCurrentPlayerDialog()
	{
		return currentPlayerJFXStage;
	}
	
	public boolean playerDialogIsActive()
	{
		return currentPlayerJFXStage != null;
	}
	
        /**
         * Attempts to close the current player dialog being shown.
         * Assumes success so long as there is a dialog whatsoever.
         * @return "true" if dialog exists (assume success); "false" if no
         * such dialog exists.
         */
	public boolean tryCloseCurrentPlayerDialog()
	{
		if (currentPlayerJFXStage != null)
		{
			Platform.runLater(new Runnable(){
				@Override public void run(){
					if(currentPlayerJFXStage != null){
					currentPlayerJFXStage.close();
					}
				}
			});
			return true;
		}
		return false;
	}
	
	public void setAppHiddenState(boolean isHidden)
	{
		// TODO fill out stub & make use
	}
	
	public void isAppHidden(){
		// TODO fill out stub and make use
	}
	
	public String getLastHumanName(){
		return lastHPlayerName;
	}
	
	public ArrayList<String> getAllHumanNames(){
		return null;
	}
	
	public void setCurrentHumanName(String nameOfPlayer){
		lastHPlayerName = nameOfPlayer;
		if (!FXUI_Crossbar.allHPlayerNames.contains(nameOfPlayer)){
			FXUI_Crossbar.allHPlayerNames.add(nameOfPlayer);	
		}
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

	public static ArrayList<String> getAllHPlayerNames() {
		return allHPlayerNames;
	}

	/**
	 * @return the britenessOpacity to use when setting up windows in FXUIPlayer.
	 */
	public static double getBritenessOpacity() {
		return britenessOpacity;
	}

	/**
	 * @param britenessOpacity the brightness to store. (with a black background, 0
	 * is darkest, and 1 is brightest. With strain relief background, depends
	 * on the color used; a dark yellow--default--works the same as black. This 
	 * method is passive, however, and will not validate your choices.) Used
	 * by FXUIPlayer.
	 */
	public static void storeBritenessOpacity(double britenessOpacity) {
		FXUI_Crossbar.britenessOpacity = britenessOpacity;
	}

	/**
	 * @return the strainReliefColor to use when settings up windows in FXUIPlayer.
	 */
	public static Color getStrainReliefColor() {
		return strainReliefColor;
	}

	/**
	 * @param strainReliefColor the strainReliefColor to set. (a black background
	 * is default, and for strain relief, a dark yellow background is default and
	 * is the official recommendation to be passed in. This method is passive,
	 * however, and will not validate your choices.) Used by FXUIPlayer.
	 */
	public static void storeStrainReliefColor(Color strainReliefColor) {
		FXUI_Crossbar.strainReliefColor = strainReliefColor;
	}
	
	

}
