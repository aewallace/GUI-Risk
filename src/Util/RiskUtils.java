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
}