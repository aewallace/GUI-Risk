package Map;
import java.lang.String;
import java.lang.IllegalArgumentException;
import java.util.Collection;
import java.util.Map;
import java.util.HashMap;

public enum Continent {
	NORTHAMERICA("North America"),
	EUROPE("Europe"),
	SOUTHAMERICA("South America"),
	AFRICA("Africa"),
	ASIA("Asia"),
	AUSTRALIA("Australia");
	
	private String name;
	private Map<String, Country> countries;
	
	private Continent(String nameIn) {
		this.name = nameIn;
	}
	
	public void init() {
		this.countries = new HashMap<String, Country>();
		if (this.name.equals("North America")) {
			this.countries.put(Country.ALASKA.getName(), Country.ALASKA);
			this.countries.put(Country.ALBERTA.getName(), Country.ALBERTA);
			this.countries.put(Country.CENTRALAMERICA.getName(), Country.CENTRALAMERICA);
			this.countries.put(Country.EASTERNUS.getName(), Country.EASTERNUS);
			this.countries.put(Country.GREENLAND.getName(), Country.GREENLAND);
			this.countries.put(Country.NWTERRITORY.getName(), Country.NWTERRITORY);
			this.countries.put(Country.QUEBEC.getName(), Country.QUEBEC);
			this.countries.put(Country.WESTERNUS.getName(), Country.WESTERNUS);
			this.countries.put(Country.ONTARIO.getName(), Country.ONTARIO);
		}
		else if (this.name.equals("Europe")) {
			this.countries.put(Country.ICELAND.getName(), Country.ICELAND);
			this.countries.put(Country.GREATBRITAIN.getName(), Country.GREATBRITAIN);
			this.countries.put(Country.SCANDINAVIA.getName(), Country.SCANDINAVIA);
			this.countries.put(Country.NORTHERNEUROPE.getName(), Country.NORTHERNEUROPE);
			this.countries.put(Country.WESTERNEUROPE.getName(), Country.WESTERNEUROPE);
			this.countries.put(Country.SOUTHERNEUROPE.getName(), Country.SOUTHERNEUROPE);
			this.countries.put(Country.UKRAINE.getName(), Country.UKRAINE);
		}
		else if (this.name.equals("South America")) {
			this.countries.put(Country.VENEZUELA.getName(), Country.VENEZUELA);
			this.countries.put(Country.PERU.getName(), Country.PERU);
			this.countries.put(Country.BRAZIL.getName(), Country.BRAZIL);
			this.countries.put(Country.ARGENTINA.getName(), Country.ARGENTINA);
		}
		else if (this.name.equals("Africa")) {
			this.countries.put(Country.NORTHAFRICA.getName(), Country.NORTHAFRICA);
			this.countries.put(Country.EGYPT.getName(), Country.EGYPT);
			this.countries.put(Country.CONGO.getName(), Country.CONGO);
			this.countries.put(Country.EASTAFRICA.getName(), Country.EASTAFRICA);
			this.countries.put(Country.SOUTHAFRICA.getName(), Country.SOUTHAFRICA);
			this.countries.put(Country.MADAGASCAR.getName(), Country.MADAGASCAR);
		}
		else if (this.name.equals("Asia")) {
			this.countries.put(Country.URAL.getName(), Country.URAL);
			this.countries.put(Country.AFGHANISTAN.getName(), Country.AFGHANISTAN);
			this.countries.put(Country.MIDDLEEAST.getName(), Country.MIDDLEEAST);
			this.countries.put(Country.INDIA.getName(), Country.INDIA);
			this.countries.put(Country.SIAM.getName(), Country.SIAM);
			this.countries.put(Country.SIBERIA.getName(), Country.SIBERIA);
			this.countries.put(Country.IRKUTSK.getName(), Country.IRKUTSK);
			this.countries.put(Country.YAKUTSK.getName(), Country.YAKUTSK);
			this.countries.put(Country.MONGOLIA.getName(), Country.MONGOLIA);
			this.countries.put(Country.JAPAN.getName(), Country.JAPAN);
			this.countries.put(Country.KAMCHATKA.getName(), Country.KAMCHATKA);
			this.countries.put(Country.CHINA.getName(), Country.CHINA);
		}
		else if (this.name.equals("Australia")) {
			this.countries.put(Country.INDONESIA.getName(), Country.INDONESIA);
			this.countries.put(Country.NEWGUINEA.getName(), Country.NEWGUINEA);
			this.countries.put(Country.WESTERNAUSTRALIA.getName(), Country.WESTERNAUSTRALIA);
			this.countries.put(Country.EASTERNAUSTRALIA.getName(), Country.EASTERNAUSTRALIA);
		}
	}
	
	public String getName() {
		return this.name;
	}
	
	public void addCountry(Country country) {
		if (country != null) {
			this.countries.put(country.getName(), country);
		}
	}
	
	public Country getCountry(String countryName) {
		return this.countries.get(countryName);
	}
	
	public Collection<Country> getCountries() {
		return this.countries.values();
	}
	
	public String toString() {
		return "[Name: " + this.name + "]";
	}
}