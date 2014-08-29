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
	
	public static int calculateReinforcements(RiskMap map, String playerName) {
		int reinforcements;
		int numCountries = getPlayerCountries(map, playerName).size();
		reinforcements = numCountries / RiskConstants.COUNTRY_ARMY_DIVISOR;
		if (reinforcements < RiskConstants.MIN_REINFORCEMENTS) {
			reinforcements = RiskConstants.MIN_REINFORCEMENTS;
		}
		for (Continent continent : map.getContinents().values()) {
			if (playerControlsContinent(continent, playerName)) {
				reinforcements += RiskConstants.CONTINENT_BONUSES.get(continent.getName());
			}
		}
		return reinforcements;
	}

	public static Collection<String> getPlayerCountries(RiskMap map, String playerName) {
		Collection<String> playerCountries = new ArrayList<String>();
		for (Country country : map.getCountries().values()) {
			if (country.getOwner().equals(playerName)) {
				playerCountries.add(country.getName());
			}
		}
		return playerCountries;
	}
	
	public static boolean playerControlsContinent(Continent continent, String playerName) {
		for (Country country : continent.getCountries()) {
			if (!playerName.equals(country.getOwner())) {
				return false;
			}
		}
		return true;
	}
	
	public static int countPlayerArmies(RiskMap map, String playerName) {
		int numArmies = 0;
		for (String countryName : getPlayerCountries(map, playerName)) {
			numArmies += map.getCountry(countryName).getNumArmies();
		}
		return numArmies;
	}
	
	public static boolean areConnected(Country start, Country end, String playerName) {
		Set<String> traversed = new HashSet<String>();
		Deque<Country> toSearch = new LinkedList<Country>();
		toSearch.addLast(start);
		
		while (toSearch.size() > 0) {
			Country current = toSearch.removeFirst();
			if (current.equals(end)) {
				return true;
			}
			else {
				for (Country neighbor : current.getNeighbors()) {
					if (neighbor.getOwner().equals(playerName) && !traversed.contains(neighbor.getName())) {
						toSearch.addLast(neighbor);
					}
				}
			}
		}
		return false;
	}
	
	public static Collection<Set<String>> getAllConnectedCountrySets(RiskMap map, String playerName) {
		Collection<String> myCountries = RiskUtils.getPlayerCountries(map, playerName);
		Collection<Set<String>> allConnectedSets = new ArrayList<Set<String>>();
		for (String countryName : myCountries) {
			boolean alreadyAssigned = false;
			for (Set<String> set : allConnectedSets) {
				if (set.contains(countryName)) {
					alreadyAssigned = true;
				}
			}
			if (!alreadyAssigned) {
				Set<String> newSet = new HashSet<String>(getConnectedCountries(map, countryName));
				allConnectedSets.add(newSet);
			}
		}
		return allConnectedSets;
	}
	
	/**
	 * The returned set includes the country of origin.
	 * @param map
	 * @param originName
	 * @return
	 */
	public static Set<String> getConnectedCountries(RiskMap map, String originName) {
		Set<String> connectedSet = new HashSet<String>();
		Deque<Country> toSearch = new LinkedList<Country>();
		toSearch.addLast(map.getCountry(originName));
		String playerName = map.getCountry(originName).getOwner();
		
		while (toSearch.size() > 0) {
			Country current = toSearch.removeFirst();
			connectedSet.add(current.getName());
			for (Country neighbor : current.getNeighbors()) {
				if (neighbor.getOwner().equals(playerName) && !connectedSet.contains(neighbor.getName())) {
					toSearch.addLast(neighbor);
				}
			}
		}
		return connectedSet;
	}
	
	public static Collection<String> selectCountriesByBorderStatus(RiskMap map, String playerName, Collection<String> allCountries, boolean selectInterior) {
		Collection<String> selectedCountries = new ArrayList<String>();
		
		for (String countryName : allCountries) {
			Country current = map.getCountry(countryName);
			boolean interior = true;
			for (Country neighbor : current.getNeighbors()) {
				if (!current.getOwner().equals(neighbor.getOwner())) {
					interior = false;
				}
			}
			if (interior && selectInterior || !interior && !selectInterior) {
				selectedCountries.add(current.getName());
			}
		}
		
		return selectedCountries;
	}
}