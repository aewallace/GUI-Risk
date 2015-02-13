package Map;
import java.io.IOException;
import java.io.File;
import java.lang.Exception;
import java.lang.String;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Set;

import Map.Continent;
import Map.Country;

public class RiskMap {
	private static final boolean READ_ONLY = true;
	private Map<Country, String> owners;
	private Map<Country, Integer> armies;
	
	public RiskMap(RiskMap map, boolean access) {
		if (access == READ_ONLY) {
			copyMapReadOnly(map);
		}
		else {
			copyMap(map);
		}
	}
	
	public RiskMap() {
		this.owners = new HashMap<Country, String>();
		this.armies = new HashMap<Country, Integer>();
		
		loadMap();
	}
	
	public String getCountryOwner(Country country) {
		return this.owners.get(country);
	}
	
	public void setCountryOwner(Country country, String owner) {
		try{
		this.owners.put(country, owner);
		}
		catch(Exception e)
		{
			System.out.println("RMAPe " + e);
		}
	}
	
	public int getCountryArmies(Country country) {
		return this.armies.get(country);
	}
	
	public void addCountryArmies(Country country, int numArmies) {
		this.armies.put(country, this.armies.get(country) + numArmies);
	}
	
	public void setCountryArmies(Country country, int numArmies) {
		this.armies.put(country, numArmies);
	}
	
	private void loadMap() {
		for (Continent continent : Continent.values()) {
			continent.init();
		}
		for (Country country : Country.values()) {
			country.init();
			this.owners.put(country, null);
			this.armies.put(country, 0);
		}
	}
	
	public RiskMap getCopy() {
		return new RiskMap(this, !READ_ONLY);
	}
	
	public RiskMap getReadOnlyCopy() {
		return new RiskMap(this, READ_ONLY);
	}
	
	private void copyMap(RiskMap map) {
		this.owners = map.owners;
		this.armies = map.armies;
	}

	private void copyMapReadOnly(RiskMap map) {
		this.owners = Collections.unmodifiableMap(map.owners);
		this.armies = Collections.unmodifiableMap(map.armies);
	}
}