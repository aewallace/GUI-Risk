package Util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import Map.Country;
import Map.RiskMap;
import Master.FXUIGameMaster;
import Player.Player;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * Class to store the state of a game at a given point in time.
 * Relies heavily on {@link FXUIGameMaster} being able to bring itself
 * 		to a state relatively similar to the game being restored
 * 		through use of very limited information.
 */
public class SavePoint implements Serializable {
	private static final long serialVersionUID = 2015022600000001L;
	public static final String versionInfo = "FXUI-RISK-SavePoint\nVersion 00x03h\nStamp 2015.11.01.0000\nType:Beta(02)";
	public HashMap<String, String> countriesPlusOwners;
	public HashMap<String, Integer> countriesPlusArmies;
	public transient boolean readyToSave = false;
	private transient SaveStatus readSaveStatus = new SaveStatus();
	private transient SaveStatus writeSaveStatus = new SaveStatus();

	public HashMap<String, ArrayList<String>> playersPlusCards;
	public HashMap<String, String> activePlayersAndTheirTypes;
	
	public List<String> logCache;

	public HashMap<String, Boolean> playerIsEliminatedMap;

	public int roundsPlayed; //save the round; roundNew > roundOld for it to be a newer save
	public Date dateOriginallySaved = null; //save the original date that the current working game save was created
	public Date latestGameSaveDate = null;
	//use the round info to get fine detail info

	public SavePoint() {
		countriesPlusOwners = new HashMap<String, String>();
		countriesPlusArmies = new HashMap<String, Integer>();

		playersPlusCards = new HashMap<String, ArrayList<String>>();
		activePlayersAndTheirTypes = new HashMap<String, String>();

		playerIsEliminatedMap = new HashMap<String, Boolean>();
		dateOriginallySaved = new Date();
		latestGameSaveDate = new Date();

		roundsPlayed = 0;
		
		logCache = new ArrayList<String>();
		// TODO Auto-generated constructor stub
	}
	
	public HashMap<String, String> getCountriesAndOwners() {return countriesPlusOwners;}
	public HashMap<String, Integer> getCountriesAndArmyCount() {return countriesPlusArmies;}

	public HashMap<String, ArrayList<String>> getPlayersAndTheirCards() {return playersPlusCards;}
	public HashMap<String, String> getActivePlayersAndTheirTypes() {return activePlayersAndTheirTypes;}

	public HashMap<String, Boolean> getPlayerIsEliminatedMap(){return playerIsEliminatedMap;}

	public int getRoundsPlayed(){return roundsPlayed;}
	
	public List<String> getLogCache(){return logCache;}
	

	public Date getLatestSaveDate(){return latestGameSaveDate;}
	
	public Date getOriginalSaveDate(){return dateOriginallySaved;}
	
	public SaveStatus getReadStatus(){return new SaveStatus(readSaveStatus);}
	public SaveStatus getWriteStatus(){return new SaveStatus(writeSaveStatus);}
	
	
	
	/**
	 * 
	 * @param originalGameSaveDate the date for the game that originally used this save
	 * @param currentSaveDate the date on which this save was updated
	 * @param roundIn the round of the game at which point this save was made
	 * @return TRUE if the game signature was updated, FALSE if you attempted to save invalid info
	 */
	public boolean updateSaveIdentificationInfo(Date originalGameSaveDate, Date currentSaveDate, int roundIn)
	{
		boolean updateOccurredWithoutError = true;
		
		//game save date must never be null after first setup, and cannot be overwritten once set
		if (originalGameSaveDate != null && dateOriginallySaved == null){
			dateOriginallySaved = originalGameSaveDate;
		}
		else{
			updateOccurredWithoutError = false;
		}
		
		//we can update the info if the
		//  latest date in the save file is the same or earlier than what's being put in as the new date
		//  or if the latest date in the save file had never been set.
		// otherwise, we cannot update the save date
		if (latestGameSaveDate == null || latestGameSaveDate.compareTo(currentSaveDate) < 0){
			latestGameSaveDate = currentSaveDate;
		}
		else{
			updateOccurredWithoutError = false;
		}
		
		if(roundIn >= roundsPlayed)
		{
			roundsPlayed = roundIn;
		}
		else{
			updateOccurredWithoutError = false;
		}
		
		
		return updateOccurredWithoutError;
	}
	
	
	public void prepAllCountryDetails(RiskMap map){
		for (Country country : Country.values()) {
			country.init();
			countriesPlusArmies.put(country.getName(), (Integer) map.getCountryArmies(country));
			countriesPlusOwners.put(country.getName(), map.getCountryOwner(country));
		}
	}
	
	public void prepAllPlayerDetails(HashMap<String, Player> currentPlayers, List<String> originalPlayers){
		for (String playerName : originalPlayers){
			if(currentPlayers.containsKey(playerName)){
				activePlayersAndTheirTypes.put(playerName, currentPlayers.get(playerName).getClass().toString());
				playerIsEliminatedMap.put(playerName, false);
			}
			else{
				playerIsEliminatedMap.put(playerName, true);
			}
		}
	}
	
	public void prepCardsForGivenPlayer(String player, Collection<Card> cardsForPlayer){
		ArrayList<String> cards = new ArrayList<String>();
		if (cardsForPlayer == null || cardsForPlayer.size() < 1){
			playersPlusCards.put(player, null);
			return;
		}
		else{
			for (Card card : cardsForPlayer){
				String cName;
				if (card.getCountry() != null){
					cName = card.getCountry().getName();
				}
				else{
					cName = "null";
				}
				cards.add(card.getType()+","+cName);
			}
			playersPlusCards.put(player, cards);
			return;
		}
	}
	
	public void prepRoundsCompleted(int roundsCompleted){
		roundsPlayed = roundsCompleted;
	}
	
	public void prepLogCache(List<String> internalLogCache){
		logCache = internalLogCache;
	}
	
	public boolean prepareOverallSave(Date originalSDate, Date currentSDate, int roundIn, RiskMap map,
			HashMap<String, Player> currentPlayers, List<String> originalPlayers,
			List<String> internalLogCache,
			List<String> players,
			HashMap<String, Collection<Card>> playerCardsetMap)
	{
		updateSaveIdentificationInfo(originalSDate, currentSDate, roundIn);
		prepAllCountryDetails(map);
		prepAllPlayerDetails(currentPlayers, originalPlayers);
		prepLogCache(internalLogCache);
		for (String player : players){
			prepCardsForGivenPlayer(player, playerCardsetMap.get(player));
		}
		return this.readyToSave = true;
	}
	
	/**
     * Performs actual write to disc using most recent checkpoint available.
     * Checkpoints are acquired with prepareSave(), automatically performed
     * after initial player allocation. Write to secondary storage is triggered
     * either automatically at each new round, or manually with the "Save"
     * button (with no discernible difference between the two).
     *
     * @return returns true on successful save, or false when a show-stopping
     * exception was thrown.
     */
	public boolean performSave(boolean customSave, String defaultSaveName) {
		String saveto_filename = defaultSaveName;
        // TODO add informative error messages
        boolean succeeded = false;
        Exception eOut = null;
        String messageOut = "";
        try {
            OutputStream fileOutStream = null;
            if (customSave) {
                FileChooser fileChooser = new FileChooser();
                File fileOut = fileChooser.showSaveDialog(new Stage());
                fileChooser.setTitle("Set your save location & save now:");
                if (fileOut == null) {
                    customSave = false;
                } else {
                    fileOutStream = new FileOutputStream(fileOut);
                    saveto_filename = fileOut.getAbsolutePath();
                }
            }
            if (!customSave) {
                fileOutStream = new FileOutputStream(saveto_filename);
            }
            OutputStream buffer = new BufferedOutputStream(fileOutStream);
            ObjectOutput output = new ObjectOutputStream(buffer);
            output.writeObject(this);
            output.close();
            succeeded = true;
            System.out.println(FXUIGameMaster.INFO + "Checkpoint saved to " + saveto_filename + ".");
            //setSubStatus("checkpoint saved");
        } 
        catch (FileNotFoundException fnfe){
        	messageOut+=("\nCouldn't create or open save file " + saveto_filename);
        	eOut = fnfe;
        }
        catch (SecurityException se){
        	messageOut+=("\nCouldn't access save file " + saveto_filename + "."
        			+ " (Do you have read/write permission in the save location?)");
        	eOut = se;
        }
        catch (NullPointerException npe){
        	messageOut+=("\nCouldn't write the save file; data incorrectly referenced"
        			+ "/made reference to null object. (Are you attempting to "
        			+ "access this SavePoint object in an unsafe manner?");
        	eOut = npe;
        }
        catch (IOException ioe){
        	messageOut.concat("\nCouldn't write the save file; generic I/O error "
        			+ "occurred. (Is your Java instalaltion up-to-date? Is your "
        			+ "OS up to date? Do I have write access to your folder? Is "
        			+ " your hardware functioning correctly? Etc etc etc.)");
        	eOut = ioe;
        }
        catch (Exception e) {
        	messageOut+=(FXUIGameMaster.ERROR + "Save failed. ::: " + e);
            eOut = e;
        }
        messageOut+=this.getLatestSaveDate().toString();
        writeSaveStatus.setCustomDescription(messageOut);
        writeSaveStatus.setSuccess(succeeded);
        writeSaveStatus.setExceptionThrown(eOut);
        return succeeded;
    }
	
	public boolean getIsReady(){
		return this.readyToSave;
	}
	
	/**
	 * Used to store extended status information for a {@link SavePoint} read
	 * or write operation.
	 */
	public class SaveStatus{
		private Exception exceptionThrown;
		private String customDescription;
		private boolean success;
		final boolean mutable;
		
		/**
		 * Creates a new mutable instance of a SaveStatus, with default values
		 * of {@link #success} = {@code false},
		 * {@link #customDescription} = {@code null},
		 * and {@link #exceptionThrown} = {@code null}.
		 */
		SaveStatus(){
			this(false, null, null);
		}
		
		/**
		 * Creates a new mutable instance of a SaveStatus, meant for use with a SavePoint
		 * to store read/write success/failure & potential causes.
		 * @param success "true" if the operation succeeded, "false" otherwise.
		 * @param customDescription stores an extended description of the 
		 * success or failure, if so desired. 
		 * (may be {@code null} or an empty string.)
		 * @param exceptionThrown stores an exception that may have occurred 
		 * during the attempted operation. (may be {@code null}.)
		 */
		SaveStatus(boolean success, String customDescription, Exception exceptionThrown){
			this.success = success;
			this.customDescription = customDescription;
			this.exceptionThrown = exceptionThrown;
			this.mutable = true;
		}
		
		/**
		 * Creates a new immutable copy of {@code statusToDuplicate}.
		 * @param statusToDuplicate The status to duplicate.
		 */
		public SaveStatus(SaveStatus statusToDuplicate){
			this.success = statusToDuplicate.success;
			this.customDescription = statusToDuplicate.customDescription;
			this.exceptionThrown = statusToDuplicate.exceptionThrown;
			this.mutable = false;
		}
		
		/**
		 * @return null if no exception thrown; otherwise, returns a copy of 
		 * the exception thrown during the method call.
		 */
		public Exception getExceptionThrown() {
			return this.exceptionThrown;
		}
		/**
		 * @param exceptionThrown the exception thrown during the call
		 */
		public void setExceptionThrown(Exception exceptionThrown) {
			if(!this.mutable){
				throw new UnsupportedOperationException("This SaveStaus object "
						+ "is meant to be an immutable copy. Please revise the "
						+ "source to correct this error.");
			}
			this.exceptionThrown = exceptionThrown;
		}
		/**
		 * @return the customDescription
		 */
		public String getCustomDescription() {
			return this.customDescription;
		}
		/**
		 * @param customDescription the customDescription to set
		 */
		public void setCustomDescription(String customDescription) {
			if(!this.mutable){
				throw new UnsupportedOperationException("This SaveStaus object "
						+ "is meant to be an immutable copy. Please revise the "
						+ "source to correct this error.");
			}
			this.customDescription = customDescription;
		}
		/**
		 * @return true if success, false otherwise
		 */
		public boolean isSuccess() {
			return this.success;
		}
		/**
		 * @param success the success to set
		 */
		public void setSuccess(boolean success) {
			if(!this.mutable){
				throw new UnsupportedOperationException("This SaveStaus object "
						+ "is meant to be an immutable copy. Please revise the "
						+ "source to correct this error.");
			}
			this.success = success;
		}
	}
}


