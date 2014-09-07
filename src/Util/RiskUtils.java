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
	public static Collection<Country> getPlayerCountries(RiskMap map, String playerName) {
		Collection<Country> playerCountries = new ArrayList<Country>();
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
	public static Collection<Continent> getPlayerContinents(RiskMap map, String playerName) {
		Collection<Continent> playerContinents = new ArrayList<Continent>();
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
		for (Country country : continent.getCountries()) {
			if (!playerName.equals(map.getCountryOwner(country))) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Counts the number of armies controlled by a given player.
	 */
	public static int countPlayerArmies(RiskMap map, String playerName) {
		int numArmies = 0;
		for (Country country : getPlayerCountries(map, playerName)) {
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
					&& restrictToOriginContinent == (neighbor.getContinent() == origin.getContinent())) {
					toSearch.addLast(neighbor);
				}
			}
		}
		return connectedSet;
	}
	
	/**
	 * Returns the subset of allCountries that is interior or exterior, based on the value of selectInterior.
	 */
	public static Collection<Country> filterCountriesByBorderStatus(RiskMap map, String playerName, Collection<Country> allCountries, boolean selectInterior) {
		Collection<Country> selectedCountries = new ArrayList<Country>();
		
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
}