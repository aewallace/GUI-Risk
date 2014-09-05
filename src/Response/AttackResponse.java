package Response;
import Map.Country;
import Map.RiskMap;
import Util.RiskConstants;

public class AttackResponse {
	private Country atkCountry;
	private Country dfdCountry;
	private int numDice;
	
	public AttackResponse() {
		this.atkCountry = null;
		this.dfdCountry = null;
		this.numDice = 1;
	}
	
	public AttackResponse(Country atk, Country dfd, int numIn) {
		this.atkCountry = atk;
		this.dfdCountry = dfd;
		this.numDice = numIn;
	}
	
	public AttackResponse(AttackResponse original) {
		this.atkCountry = original.getAtkCountry();
		this.dfdCountry = original.getDfdCountry();
		this.numDice = original.getNumDice();
	}
	
	public Country getAtkCountry() {
		return this.atkCountry;
	}
	
	public void setAtkCountry(Country countryIn) {
		this.atkCountry = countryIn;
	}
	
	public Country getDfdCountry() {
		return this.dfdCountry;
	}
	
	public void setDfdCountry(Country countryIn) {
		this.dfdCountry = countryIn;
	}
	
	public int getNumDice() {
		return this.numDice;
	}
	
	public void setNumDice(int numIn) {
		this.numDice = numIn;
	}
	
	public static boolean isValidResponse(AttackResponse rsp, RiskMap map, String playerName) {
		if (rsp != null && rsp.getNumDice() > 0 && rsp.getNumDice() <= RiskConstants.MAX_ATK_DICE) {
			if (rsp.getAtkCountry() != null
				&& map.getCountryArmies(rsp.getAtkCountry()) > rsp.getNumDice()) {
				if (rsp.getDfdCountry() != null
					&& !map.getCountryOwner(rsp.getDfdCountry()).equals(playerName)) {
					return true;
				}
				else {
					return false;
				}
			}
			else {
				return false;
			}
			
		}
		else {
			return false;
		}
	}
}