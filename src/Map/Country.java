package Map;
import java.lang.IllegalArgumentException;
import java.lang.String;
import java.util.ArrayList;
import java.util.Collection;

public enum Country {
	ALASKA("Alaska"),
	ALBERTA("Alberta"),
	CENTRALAMERICA("Central America"),
	EASTERNUS("Eastern US"),
	GREENLAND("Greenland"),
	NWTERRITORY("Northwest Territory"),
	QUEBEC("Quebec"),
	WESTERNUS("Western US"),
	ONTARIO("Ontario"),
	ICELAND("Iceland"),
	GREATBRITAIN("Great Britain"),
	SCANDINAVIA("Scandinavia"),
	NORTHERNEUROPE("Northern Europe"),
	WESTERNEUROPE("Western Europe"),
	SOUTHERNEUROPE("Southern Europe"),
	UKRAINE("Ukraine"),
	VENEZUELA("Venezuela"),
	PERU("Peru"),
	BRAZIL("Brazil"),
	ARGENTINA("Argentina"),
	NORTHAFRICA("North Africa"),
	EGYPT("Egypt"),
	CONGO("Congo"),
	EASTAFRICA("East Africa"),
	SOUTHAFRICA("South Africa"),
	MADAGASCAR("Madagascar"),
	URAL("Ural"),
	AFGHANISTAN("Afghanistan"),
	MIDDLEEAST("Middle East"),
	INDIA("India"),
	SIAM("Siam"),
	SIBERIA("Siberia"),
	IRKUTSK("Irkutsk"),
	YAKUTSK("Yakutsk"),
	MONGOLIA("Mongolia"),
	JAPAN("Japan"),
	KAMCHATKA("Kamchatka"),
	CHINA("China"),
	INDONESIA("Indonesia"),
	NEWGUINEA("New Guinea"),
	WESTERNAUSTRALIA("Western Australia"),
	EASTERNAUSTRALIA("Eastern Australia");
	
	String name;
	String continent;
	Collection<Country> neighbors;
	
	private Country(String nameIn) {
		this.name = nameIn;
	}
	
	public void init() {
		this.neighbors = new ArrayList<Country>();
		if (this == Country.ALASKA) {
			this.continent = Continent.NORTHAMERICA.getName();
			this.neighbors.add(Country.NWTERRITORY);
			this.neighbors.add(ALBERTA);
			this.neighbors.add(KAMCHATKA);
		}
		else if (this == Country.ALBERTA) {
			this.continent = Continent.NORTHAMERICA.getName();
			this.neighbors.add(Country.ALASKA);
			this.neighbors.add(Country.NWTERRITORY);
			this.neighbors.add(Country.ONTARIO);
			this.neighbors.add(Country.WESTERNUS);
		}
		else if (this == Country.CENTRALAMERICA) {
			this.continent = Continent.NORTHAMERICA.getName();
			this.neighbors.add(Country.WESTERNUS);
			this.neighbors.add(Country.EASTERNUS);
			this.neighbors.add(Country.VENEZUELA);
		}
		else if (this == Country.EASTERNUS) {
			this.continent = Continent.NORTHAMERICA.getName();
			this.neighbors.add(Country.WESTERNUS);
			this.neighbors.add(Country.CENTRALAMERICA);
			this.neighbors.add(Country.ONTARIO);
			this.neighbors.add(Country.QUEBEC);
		}
		else if (this == Country.GREENLAND) {
			this.continent = Continent.NORTHAMERICA.getName();
			this.neighbors.add(Country.NWTERRITORY);
			this.neighbors.add(Country.ICELAND);
			this.neighbors.add(Country.ONTARIO);
			this.neighbors.add(Country.QUEBEC);
		}
		else if (this == Country.NWTERRITORY) {
			this.continent = Continent.NORTHAMERICA.getName();
			this.neighbors.add(Country.ALASKA);
			this.neighbors.add(Country.ONTARIO);
			this.neighbors.add(Country.ALBERTA);
			this.neighbors.add(Country.GREENLAND);
		}
		else if (this == Country.ONTARIO) {
			this.continent = Continent.NORTHAMERICA.getName();
			this.neighbors.add(Country.QUEBEC);
			this.neighbors.add(Country.NWTERRITORY);
			this.neighbors.add(Country.GREENLAND);
			this.neighbors.add(Country.ALBERTA);
			this.neighbors.add(Country.EASTERNUS);
			this.neighbors.add(Country.WESTERNUS);
		}
		else if (this == Country.QUEBEC) {
			this.continent = Continent.NORTHAMERICA.getName();
			this.neighbors.add(Country.ONTARIO);
			this.neighbors.add(Country.EASTERNUS);
			this.neighbors.add(Country.GREENLAND);
		}
		else if (this == Country.WESTERNUS) {
			this.continent = Continent.NORTHAMERICA.getName();
			this.neighbors.add(Country.EASTERNUS);
			this.neighbors.add(Country.CENTRALAMERICA);
			this.neighbors.add(Country.ALBERTA);
			this.neighbors.add(Country.ONTARIO);
		}
		else if (this == Country.ICELAND) {
			this.continent = Continent.EUROPE.getName();
			this.neighbors.add(Country.GREENLAND);
			this.neighbors.add(Country.GREATBRITAIN);
			this.neighbors.add(Country.SCANDINAVIA);
		}
		else if (this == Country.GREATBRITAIN) {
			this.continent = Continent.EUROPE.getName();
			this.neighbors.add(Country.ICELAND);
			this.neighbors.add(Country.SCANDINAVIA);
			this.neighbors.add(Country.WESTERNEUROPE);
			this.neighbors.add(Country.NORTHERNEUROPE);
		}
		else if (this == Country.SCANDINAVIA) {
			this.continent = Continent.EUROPE.getName();
			this.neighbors.add(Country.ICELAND);
			this.neighbors.add(Country.GREATBRITAIN);
			this.neighbors.add(Country.NORTHERNEUROPE);
			this.neighbors.add(Country.UKRAINE);
		}
		else if (this == Country.NORTHERNEUROPE) {
			this.continent = Continent.EUROPE.getName();
			this.neighbors.add(Country.GREATBRITAIN);
			this.neighbors.add(Country.SCANDINAVIA);
			this.neighbors.add(Country.UKRAINE);
			this.neighbors.add(Country.SOUTHERNEUROPE);
			this.neighbors.add(Country.WESTERNEUROPE);
		}
		else if (this == Country.WESTERNEUROPE) {
			this.continent = Continent.EUROPE.getName();
			this.neighbors.add(Country.GREATBRITAIN);
			this.neighbors.add(Country.SOUTHERNEUROPE);
			this.neighbors.add(Country.NORTHERNEUROPE);
			this.neighbors.add(Country.NORTHAFRICA);
		}
		else if (this == Country.SOUTHERNEUROPE) {
			this.continent = Continent.EUROPE.getName();
			this.neighbors.add(Country.WESTERNEUROPE);
			this.neighbors.add(Country.UKRAINE);
			this.neighbors.add(Country.NORTHERNEUROPE);
			this.neighbors.add(Country.EGYPT);
			this.neighbors.add(Country.NORTHAFRICA);
		}
		else if (this == Country.UKRAINE) {
			this.continent = Continent.EUROPE.getName();
			this.neighbors.add(Country.SCANDINAVIA);
			this.neighbors.add(Country.NORTHERNEUROPE);
			this.neighbors.add(Country.SOUTHERNEUROPE);
			this.neighbors.add(Country.MIDDLEEAST);
			this.neighbors.add(Country.URAL);
			this.neighbors.add(Country.AFGHANISTAN);
		}
		else if (this == Country.VENEZUELA) {
			this.continent = Continent.SOUTHAMERICA.getName();
			this.neighbors.add(Country.PERU);
			this.neighbors.add(Country.CENTRALAMERICA);
			this.neighbors.add(Country.BRAZIL);
		}
		else if (this == Country.BRAZIL) {
			this.continent = Continent.SOUTHAMERICA.getName();
			this.neighbors.add(Country.PERU);
			this.neighbors.add(Country.VENEZUELA);
			this.neighbors.add(Country.ARGENTINA);
			this.neighbors.add(Country.NORTHAFRICA);
		}
		else if (this == Country.ARGENTINA) {
			this.continent = Continent.SOUTHAMERICA.getName();
			this.neighbors.add(Country.BRAZIL);
			this.neighbors.add(Country.PERU);
		}
		else if (this == Country.PERU) {
			this.continent = Continent.SOUTHAMERICA.getName();
			this.neighbors.add(Country.VENEZUELA);
			this.neighbors.add(Country.BRAZIL);
			this.neighbors.add(Country.ARGENTINA);
		}
		else if (this == Country.NORTHAFRICA) {
			this.continent = Continent.AFRICA.getName();
			this.neighbors.add(Country.BRAZIL);
			this.neighbors.add(Country.EGYPT);
			this.neighbors.add(Country.WESTERNEUROPE);
			this.neighbors.add(Country.SOUTHERNEUROPE);
			this.neighbors.add(Country.EASTAFRICA);
			this.neighbors.add(Country.CONGO);
		}
		else if (this == Country.EGYPT) {
			this.continent = Continent.AFRICA.getName();
			this.neighbors.add(Country.NORTHAFRICA);
			this.neighbors.add(Country.EASTAFRICA);
			this.neighbors.add(Country.MIDDLEEAST);
			this.neighbors.add(Country.SOUTHERNEUROPE);
		}
		else if (this == Country.EASTAFRICA) {
			this.continent = Continent.AFRICA.getName();
			this.neighbors.add(Country.EGYPT);
			this.neighbors.add(Country.CONGO);
			this.neighbors.add(Country.MADAGASCAR);
			this.neighbors.add(Country.MIDDLEEAST);
			this.neighbors.add(Country.SOUTHAFRICA);
			this.neighbors.add(Country.NORTHAFRICA);
		}
		else if (this == Country.CONGO) {
			this.continent = Continent.AFRICA.getName();
			this.neighbors.add(Country.NORTHAFRICA);
			this.neighbors.add(Country.EASTAFRICA);
			this.neighbors.add(Country.SOUTHAFRICA);
		}
		else if (this == Country.SOUTHAFRICA) {
			this.continent = Continent.AFRICA.getName();
			this.neighbors.add(Country.MADAGASCAR);
			this.neighbors.add(Country.CONGO);
			this.neighbors.add(Country.EASTAFRICA);
		}
		else if (this == Country.MADAGASCAR) {
			this.continent = Continent.AFRICA.getName();
			this.neighbors.add(Country.SOUTHAFRICA);
			this.neighbors.add(Country.EASTAFRICA);
		}
		else if (this == Country.INDONESIA) {
			this.continent = Continent.AUSTRALIA.getName();
			this.neighbors.add(Country.SIAM);
			this.neighbors.add(Country.WESTERNAUSTRALIA);
			this.neighbors.add(Country.NEWGUINEA);
		}
		else if (this == Country.NEWGUINEA) {
			this.continent = Continent.AUSTRALIA.getName();
			this.neighbors.add(Country.INDONESIA);
			this.neighbors.add(Country.EASTERNAUSTRALIA);
		}
		else if (this == Country.WESTERNAUSTRALIA) {
			this.continent = Continent.AUSTRALIA.getName();
			this.neighbors.add(Country.INDONESIA);
			this.neighbors.add(Country.EASTERNAUSTRALIA);
		}
		else if (this == Country.EASTERNAUSTRALIA) {
			this.continent = Continent.AUSTRALIA.getName();
			this.neighbors.add(Country.WESTERNAUSTRALIA);
			this.neighbors.add(Country.NEWGUINEA);
		}
		else if (this == Country.URAL) {
			this.continent = Continent.ASIA.getName();
			this.neighbors.add(Country.UKRAINE);
			this.neighbors.add(Country.CHINA);
			this.neighbors.add(Country.AFGHANISTAN);
			this.neighbors.add(Country.SIBERIA);
		}
		else if (this == Country.AFGHANISTAN) {
			this.continent = Continent.ASIA.getName();
			this.neighbors.add(Country.INDIA);
			this.neighbors.add(Country.MIDDLEEAST);
			this.neighbors.add(Country.CHINA);
			this.neighbors.add(Country.URAL);
			this.neighbors.add(Country.UKRAINE);
		}
		else if (this == Country.MIDDLEEAST) {
			this.continent = Continent.ASIA.getName();
			this.neighbors.add(Country.UKRAINE);
			this.neighbors.add(Country.AFGHANISTAN);
			this.neighbors.add(Country.EGYPT);
			this.neighbors.add(Country.EASTAFRICA);
			this.neighbors.add(Country.INDIA);
		}
		else if (this == Country.INDIA) {
			this.continent = Continent.ASIA.getName();
			this.neighbors.add(Country.AFGHANISTAN);
			this.neighbors.add(Country.MIDDLEEAST);
			this.neighbors.add(Country.SIAM);
			this.neighbors.add(Country.CHINA);
		}
		else if (this == Country.SIAM) {
			this.continent = Continent.ASIA.getName();
			this.neighbors.add(Country.CHINA);
			this.neighbors.add(Country.INDIA);
			this.neighbors.add(Country.INDONESIA);
		}
		else if (this == Country.CHINA) {
			this.continent = Continent.ASIA.getName();
			this.neighbors.add(Country.SIAM);
			this.neighbors.add(Country.INDIA);
			this.neighbors.add(Country.AFGHANISTAN);
			this.neighbors.add(Country.URAL);
			this.neighbors.add(Country.SIBERIA);
			this.neighbors.add(Country.MONGOLIA);
		}
		else if (this == Country.SIBERIA) {
			this.continent = Continent.ASIA.getName();
			this.neighbors.add(Country.URAL);
			this.neighbors.add(Country.CHINA);
			this.neighbors.add(Country.YAKUTSK);
			this.neighbors.add(Country.IRKUTSK);
			this.neighbors.add(Country.MONGOLIA);
		}
		else if (this == Country.YAKUTSK) {
			this.continent = Continent.ASIA.getName();
			this.neighbors.add(Country.SIBERIA);
			this.neighbors.add(Country.IRKUTSK);
			this.neighbors.add(Country.KAMCHATKA);
		}
		else if (this == Country.IRKUTSK) {
			this.continent = Continent.ASIA.getName();
			this.neighbors.add(Country.YAKUTSK);
			this.neighbors.add(Country.KAMCHATKA);
			this.neighbors.add(Country.SIBERIA);
			this.neighbors.add(Country.MONGOLIA);
		}
		else if (this == Country.MONGOLIA) {
			this.continent = Continent.ASIA.getName();
			this.neighbors.add(Country.CHINA);
			this.neighbors.add(Country.JAPAN);
			this.neighbors.add(Country.IRKUTSK);
			this.neighbors.add(Country.KAMCHATKA);
			this.neighbors.add(Country.SIBERIA);
		}
		else if (this == Country.JAPAN) {
			this.continent = Continent.ASIA.getName();
			this.neighbors.add(Country.MONGOLIA);
			this.neighbors.add(Country.KAMCHATKA);
		}
		else if (this == Country.KAMCHATKA) {
			this.continent = Continent.ASIA.getName();
			this.neighbors.add(Country.MONGOLIA);
			this.neighbors.add(Country.YAKUTSK);
			this.neighbors.add(Country.IRKUTSK);
			this.neighbors.add(Country.JAPAN);
			this.neighbors.add(Country.ALASKA);
		}
	}
	
	public String getName() {
		return this.name;
	}
	
	public String getContinent() {
		return this.continent;
	}
	
	public Collection<Country> getNeighbors() {
		return this.neighbors;
	}
	
	public String toString() {
		return "[Name: " + this.name + "; Continent: " + this.continent + "]";
	}
}