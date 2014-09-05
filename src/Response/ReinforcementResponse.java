package Response;
import java.util.Map;
import java.util.HashMap;
import java.util.Map.Entry;
import java.lang.String;

import Map.Country;
import Map.RiskMap;

public class ReinforcementResponse {
	private Map<Country, Integer> allocation;
	
	public ReinforcementResponse() {
		this.allocation = new HashMap<Country, Integer>();
	}
	
	public ReinforcementResponse(Map<Country, Integer> map) {
		if (map != null) {
			this.allocation = map;
		}
		else {
			this.allocation = new HashMap<Country, Integer>();
		}
	}
	
	public Map<Country, Integer> getAllocation() {
		return this.allocation;
	}
	
	public void setAllocation(Map<Country, Integer> map) {
		if (map != null) {
			this.allocation = map;
		}
	}
	
	public int reinforce(Country country, int numArmies) {
		if (country != null) {
			if (allocation.containsKey(country)) {
				allocation.put(country, allocation.get(country) + numArmies);
			}
			else {
				allocation.put(country, numArmies);
			}
			return numArmies;
		}
		else {
			return 0;
		}
	}
	
	//returns number of armies that this action takes from the player's pool of available reinforcements
	//if they had already allocated 10 armies to X, but now try to allocate 15 armies to X, it will only]
	//return 5, because the player's pool was already missing 10.
	public int setReinforcements(Country country, int numArmies) {
		if (country != null) {
			if (allocation.containsKey(country) && numArmies + allocation.get(country) > 0) {
				return numArmies - allocation.get(country);
			}
			else if (numArmies > 0) {
				allocation.put(country, numArmies);
				return numArmies;
			}
			else {
				return 0;
			}
		}
		else {
			return 0;
		}
	}
	
	public static boolean isValidResponse(ReinforcementResponse rsp, RiskMap map, String playerName, int reinforcements) {
		if (rsp != null) {
			int total = 0;
			for (Entry<Country, Integer> entry : rsp.getAllocation().entrySet()) {
				if (!map.getCountryOwner(entry.getKey()).equals(playerName) || entry.getValue() < 0) {
					return false;
				}
				else {
					total += entry.getValue();
				}
			}
			return total == reinforcements;
		}
		else 
		{
			return false;
		}
	}
}