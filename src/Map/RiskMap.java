package Map;
import java.io.IOException;
import java.io.File;
import java.lang.Exception;
import java.lang.String;
import java.util.Map;
import java.util.HashMap;
import java.util.Scanner;

import Map.Continent;
import Map.Country;

public class RiskMap {
	private Map<String, Continent> continents;
	private Map<String, Country> countries;
	
	public RiskMap(RiskMap map) {
		this.continents = new HashMap<String, Continent>();
		this.countries = new HashMap<String, Country>();
		copyMap(map);
	}
	
	public RiskMap(String mapFile) throws IOException {
		this.continents = new HashMap<String, Continent>();
		this.countries = new HashMap<String, Country>();
		
		if (!loadMap(mapFile)) {
			throw new IOException("RiskMap.loadMap: Invalid map file!");
		}
	}
	
	public Map<String, Continent> getContinents() {
		return this.continents;
	}
	
	public Continent getContinent(String continent) {
		if (this.continents.containsKey(continent)) {
			return this.continents.get(continent);
		}
		else {
			return null;
		}
	}
	
	public Map<String, Country> getCountries() {
		return this.countries;
	}
	
	public Country getCountry(String country) {
		if (this.countries.containsKey(country)) {
			return this.countries.get(country);
		}
		else {
			return null;
		}
	}
	
	private boolean loadMap(String mapFile) {
		try {
			if (mapFile == null) {
				return false;
			}
			else {
				Scanner reader = new Scanner(new File(mapFile));
				Country country = null;
				while (reader.hasNext()) {
					String next = reader.nextLine();
					if (next.equals("$")) {
						String countryName = reader.nextLine();
						String continentName = reader.nextLine();
						//current country
						if (!this.countries.containsKey(countryName)) {
							this.countries.put(countryName, new Country(countryName));
						}
						country = this.countries.get(countryName);
						country.setContinent(continentName);
						//current continent
						if (!this.continents.containsKey(continentName)) {
							this.continents.put(continentName, new Continent(continentName));
						}
						this.continents.get(continentName).addCountry(country);
						next = reader.nextLine();
					}
					//neighbor country
					String neighborName = next;
					Country neighborCountry;
					if (!this.countries.containsKey(neighborName)) {
						this.countries.put(neighborName, new Country(neighborName));
					}
					neighborCountry = this.countries.get(neighborName);
					//add neighbor to current country
					country.addNeighbor(neighborCountry);
				}
				reader.close();
				return verifyMap();
			}
		}
		catch (Exception e) {
			//any new data cannot be trusted
			this.continents.clear();
			this.countries.clear();
			return false;
		}
	}
	
	private boolean verifyMap() {
		for (Continent continent : this.continents.values()) {
			for (Country country : continent.getCountries()) {
				//check country-continent mapping symmetry
				if (!continent.getName().equals(country.getContinent())
				|| !this.countries.containsKey(country.getName())) {
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
		for (Continent continent : map.getContinents().values()) {
			Continent newContinent = new Continent(continent.getName());
			this.continents.put(newContinent.getName(), newContinent);
			for (Country country : continent.getCountries()) {
				Country newCountry = new Country(country.getName());
				newContinent.addCountry(newCountry);
				newCountry.setContinent(country.getContinent());
				newCountry.setOwner(country.getOwner());
				newCountry.addArmies(country.getNumArmies());
				this.countries.put(newCountry.getName(), newCountry);
			}
		}
		for (Country country : map.getCountries().values()) {
			for (Country neighbor : country.getNeighbors()) {
				this.countries.get(country.getName()).addNeighbor(this.countries.get(neighbor.getName()));
			}
		}
		verifyMap();
	}
}