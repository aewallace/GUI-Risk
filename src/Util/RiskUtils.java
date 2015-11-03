package Util;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import Map.Continent;
import Map.Country;
import Map.RiskMap;
import javafx.application.Platform;

public class RiskUtils {
	
	/**
	 * Calculates the number of country and continent reinforcements that a player will receive.
	 * @param map
	 * @param playerName
	 * @return
	 */
	public static int calculateReinforcements(RiskMap map, String playerName) {
		int reinforcements;
		int numCountries = getPlayerCountries(map, playerName).size();
		reinforcements = numCountries / RiskConstants.COUNTRY_ARMY_DIVISOR;
		if (reinforcements < RiskConstants.MIN_REINFORCEMENTS) {
			reinforcements = RiskConstants.MIN_REINFORCEMENTS;
		}
		for (Continent continent : Continent.values()) {
			if (playerControlsContinent(map, continent, playerName)) {
				reinforcements += RiskConstants.CONTINENT_BONUSES.get(continent);
			}
		}
		return reinforcements;
	}
	
	/**
	 * Returns all countries that are controlled by the specified player.
	 */
	public static Set<Country> getPlayerCountries(RiskMap map, String playerName) {
		Set<Country> playerCountries = new HashSet<Country>();
		for (Country country : Country.values()) {
			if (map.getCountryOwner(country).equals(playerName)) {
				playerCountries.add(country);
			}
		}
		return playerCountries;
	}
	
	/**
	 * Returns all continents that are controlled by the specified player.
	 */
	public static Set<Continent> getPlayerContinents(RiskMap map, String playerName) {
		Set<Continent> playerContinents = new HashSet<Continent>();
		for (Continent continent : Continent.values()) {
			if (playerControlsContinent(map, continent, playerName)) {
				playerContinents.add(continent);
			}
		}
		return playerContinents;
	}
	
	/**
	 * Returns true IFF the specified player controls the specified continent.
	 */
	public static boolean playerControlsContinent(RiskMap map, Continent continent, String playerName) {
		return playerName.equals(getContinentOwner(map, continent));
	}
	
	/**
	 * Returns the name of the player who controls the specified continent,
	 * or null if the continent is contested.
	 */
	public static String getContinentOwner(RiskMap map, Continent continent) {
		String owner = null;
		for (Country country : continent.getCountries()) {
			if (owner == null) {
				owner = map.getCountryOwner(country);
			}
			else if (!owner.equals(map.getCountryOwner(country))) {
				return null;
			}
		}
		return owner;
	}
	
	/**
	 * Counts the number of armies controlled by a given player.
	 */
	public static int countPlayerArmies(RiskMap map, String playerName, Set<Country> countrySet) {
		int numArmies = 0;
		for (Country country : countrySet == null ? getPlayerCountries(map, playerName) : countrySet) {
			numArmies += map.getCountryArmies(country);
		}
		return numArmies;
	}
	
	/**
	 * Returns true IFF start and end are connected by the given player's countries.
	 */
	public static boolean areConnected(RiskMap map, Country start, Country end, String playerName, boolean throughFriendlies) {
		Set<Country> traversed = new HashSet<Country>();
		Deque<Country> toSearch = new LinkedList<Country>();
		toSearch.addLast(start);
		
		while (toSearch.size() > 0) {
			Country current = toSearch.removeFirst();
			if (current == end) {
				return true;
			}
			else {
				traversed.add(current);
				for (Country neighbor : current.getNeighbors()) {
					if (!traversed.contains(neighbor) && throughFriendlies == map.getCountryOwner(neighbor).equals(playerName)) {
						toSearch.addLast(neighbor);
					}
				}
			}
		}
		return false;
	}
	
	/**
	 * For a given player, return all disjoint sets of connected countries that are owned by that player.
	 */
	public static Collection<Set<Country>> getAllConnectedCountrySets(RiskMap map, String playerName) {
		Collection<Country> myCountries = RiskUtils.getPlayerCountries(map, playerName);
		Collection<Set<Country>> allConnectedSets = new ArrayList<Set<Country>>();
		for (Country country : myCountries) {
			boolean alreadyAssigned = false;
			for (Set<Country> set : allConnectedSets) {
				if (set.contains(country)) {
					alreadyAssigned = true;
				}
			}
			if (!alreadyAssigned) {
				Set<Country> newSet = new HashSet<Country>(getConnectedCountries(map, country, playerName, true, false));
				allConnectedSets.add(newSet);
			}
		}
		return allConnectedSets;
	}
	
	/**
	 * The returned set includes the country of origin.
	 */
	public static Set<Country> getConnectedCountries(RiskMap map, Country origin, String playerName, boolean ownedByPlayer, boolean restrictToOriginContinent) {
		Set<Country> connectedSet = new HashSet<Country>();
		Deque<Country> toSearch = new LinkedList<Country>();
		toSearch.addLast(origin);
		
		while (toSearch.size() > 0) {
			Country current = toSearch.removeFirst();
			connectedSet.add(current);
			for (Country neighbor : current.getNeighbors()) {
				if (ownedByPlayer == map.getCountryOwner(neighbor).equals(playerName)
					&& !connectedSet.contains(neighbor)
					&& (!restrictToOriginContinent || neighbor.getContinent() == origin.getContinent())) {
					toSearch.addLast(neighbor);
				}
			}
		}
		return connectedSet;
	}
	
	/**
	 * Returns the subset of allCountries that is interior or exterior, based on the value of selectInterior.
	 */
	public static Set<Country> filterCountriesByBorderStatus(RiskMap map, String playerName, Set<Country> allCountries, boolean selectInterior) {
		Set<Country> selectedCountries = new HashSet<Country>();
		
		for (Country country : allCountries) {
			boolean interior = true;
			for (Country neighbor : country.getNeighbors()) {
				if (!map.getCountryOwner(country).equals(map.getCountryOwner(neighbor))) {
					interior = false;
				}
			}
			if (interior && selectInterior || !interior && !selectInterior) {
				selectedCountries.add(country);
			}
		}
		
		return selectedCountries;
	}
	
	/**
	 * Returns all border countries of a given continent.
	 */
	public static Set<Country> getContinentBorders(RiskMap map, Continent continent) {
		Set<Country> borderCountries = new HashSet<Country>();
		for (Country country : continent.getCountries()) {
			for (Country neighbor : country.getNeighbors()) {
				if (neighbor.getContinent() != continent) {
					borderCountries.add(country);
				}
			}
		}
		return borderCountries;
	}
	
	/**
	 * Returns only the countries that have more than one army (ie. can attack of fortify)
	 */
	public static Collection<Country> getPossibleSourceCountries(RiskMap map, Collection<Country> countries) {
		Collection<Country> possibleSources = new ArrayList<Country>();
		for (Country country : countries) {
			if (map.getCountryArmies(country) > 1) {
				possibleSources.add(country);
			}
		}
		return possibleSources;
	}
	

	/**
	 * Sleeps threads. Makes use of Thread.sleep(), but internally suppresses any exceptions 
	 * expected to occur. Not advised for use on JavaFX threads, 
	 * as Thread.sleep() will make UI (or the entire app) frozen until all sleeps are done!
	 * @param millisecs the length of time, in milliseconds, to sleep the thread. Type: 'long'.
	 * @return "true" if the sleep succeeded, "false" if an exception was caught
	 * or no sleeping occurred.
	 */
	public static boolean sleep(long millisecs){
		if(millisecs < 1){
			return false;
		}
		boolean didSucceed = false;
		try {
			Thread.sleep(millisecs);
			didSucceed = true;
		} catch (InterruptedException e) {
			//We never care about what caused the interruption so long as we're using this method.
		}
		catch (IllegalArgumentException e){
			System.out.println("Input must be positive. Attempted value: " + millisecs);
			e.printStackTrace();
		}
		return didSucceed;
	}
	
	/**
     * Equivalent to the default 
     * {@link javafx.application.Platform #runLater(Runnable)} method,
     * with the addition of doing a specific delay of some milliseconds before
     * the {@link java.lang.Runnable Runnable} is queued for execution.
     * 
     * @param delayTime time, in milliseconds, to delay before execution attempt
     * @param newRunnable Runnable to be executed in the future
     * @return -1 if input is invalid (will not run), -2 if exception occurred 
     * during setup (will not run), 0 if runnable is setup to run (should run)
     */
    public static int runLaterWithDelay(final long delayTime, final Runnable newRunnable){
    	if (newRunnable == null || delayTime < 0){
    		return -1;
    	}
    	try{
	    	Thread futureRun = new Thread(new Runnable() {
	            @Override
	            public void run() {
	            	try{
	                RiskUtils.sleep(delayTime);
	                Platform.runLater(newRunnable);
	            	}
	            	catch (Exception e){
	            		e.printStackTrace();
	            	}
	            }
	        });
	    	futureRun.setDaemon(true);
	    	futureRun.start();
    	}
    	catch (Exception e){
    		e.printStackTrace();
    		return -2;
    	}
    	return 0;
    }
}