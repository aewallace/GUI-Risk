package Map;
import java.io.IOException;
import java.io.File;
import java.lang.Exception;
import java.lang.String;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Set;

import Map.Continent;
import Map.Country;

public class RiskMap {
	private Set<Continent> continents;
	private Set<Country> countries;
	private Map<Country, String> owners;
	private Map<Country, Integer> armies;
	
	public RiskMap(RiskMap map) {
		this.continents = new HashSet<Continent>();
		this.countries = new HashSet<Country>();
		this.owners = new HashMap<Country, String>();
		this.armies = new HashMap<Country, Integer>();
		copyMap(map);
	}
	
	public RiskMap() {
		this.continents = new HashSet<Continent>();
		this.countries = new HashSet<Country>();
		this.owners = new HashMap<Country, String>();
		this.armies = new HashMap<Country, Integer>();
		
		loadMap();
	}
	
	public Set<Continent> getContinents() {
		return this.continents;
	}
	
	public Set<Country> getCountries() {
		return this.countries;
	}
	
	public String getCountryOwner(Country country) {
		return this.owners.get(country);
	}
	
	public void setCountryOwner(Country country, String owner) {
		this.owners.put(country, owner);
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
	
	private boolean loadMap() {
		for (Continent continent : Continent.values()) {
			continent.init();
			this.continents.add(continent);
		}
		for (Country country : Country.values()) {
			country.init();
			this.countries.add(country);
			this.owners.put(country, null);
			this.armies.put(country, 0);
		}
		return verifyMap();
		
	}
	
	private boolean verifyMap() {
		for (Continent continent : this.continents) {
			for (Country country : continent.getCountries()) {
				//check country-continent mapping symmetry
				if (!continent.getName().equals(country.getContinent())
				|| !this.countries.contains(country)) {
					return false;
				}
				//check neighbor symmetry
				for (Country neighbor : country.getNeighbors()) {
					if (!neighbor.getNeighbors().contains(country)) {
						return false;
					}
				}
			}
		}
		return true;
	}
	
	public RiskMap getCopy() {
		return new RiskMap(this);
	}
	
	private void copyMap(RiskMap map) {
		this.continents = new HashSet<Continent>(map.getContinents());
		this.countries = new HashSet<Country>(map.getCountries());
		this.owners = new HashMap<Country, String>(map.owners);
		this.armies = new HashMap<Country, Integer>(map.armies);
		
	}
}