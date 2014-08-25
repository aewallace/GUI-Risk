package Map;
import java.lang.IllegalArgumentException;
import java.lang.String;
import java.util.ArrayList;
import java.util.Collection;

public class Country {
	String name;
	String owner;
	String continent;
	int numArmies;
	Collection<Country> neighbors;
	
	public Country(String nameIn) throws IllegalArgumentException {
		if (nameIn == null) {
			throw new IllegalArgumentException("Country.Country: Name must not be null.");
		}
		else {
			this.name = nameIn;
			this.owner = null;
			this.continent = null;
			this.numArmies = 0;
			this.neighbors = new ArrayList<Country>();
		}
	}
	
	public String getName() {
		return this.name;
	}
	
	public String getOwner() {
		return this.owner;
	}
	
	public void setOwner(String ownerIn) {
		this.owner = ownerIn;
	}
	
	public String getContinent() {
		return this.continent;
	}
	
	public void setContinent(String continentIn) {
		this.continent = continentIn;
	}
	
	public int getNumArmies() {
		return this.numArmies;
	}
	
	public void addArmies(int armiesIn) {
		this.numArmies += armiesIn;
	}
	
	public void setArmies(int armiesIn) {
		this.numArmies = armiesIn;
	}
	
	public void addNeighbor(Country neighbor) {
		if (neighbor != null && !neighbors.contains(neighbor)) {
			neighbors.add(neighbor);
		}
	}
	
	public Collection<Country> getNeighbors() {
		return this.neighbors;
	}
	
	@Override
	public boolean equals(Object obj) {
		Country other = (Country) obj;
		return other != null && this.name.equals(other.getName());
	}
	
	public String toString() {
		return "[Name: " + this.name + "; Continent: " + this.continent + "]";
	}
}