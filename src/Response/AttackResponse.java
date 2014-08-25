package Response;
import Map.RiskMap;
import Util.RiskConstants;

public class AttackResponse {
	private String atkCountry;
	private String dfdCountry;
	private int numDice;
	
	public AttackResponse() {
		this.atkCountry = null;
		this.dfdCountry = null;
		this.numDice = 1;
	}
	
	public AttackResponse(String atk, String dfd, int numIn) {
		this.atkCountry = atk;
		this.dfdCountry = dfd;
		this.numDice = numIn;
	}
	
	public AttackResponse(AttackResponse original) {
		this.atkCountry = original.getAtkCountry();
		this.dfdCountry = original.getDfdCountry();
		this.numDice = original.getNumDice();
	}
	
	public String getAtkCountry() {
		return this.atkCountry;
	}
	
	public void setAtkCountry(String countryIn) {
		this.atkCountry = countryIn;
	}
	
	public String getDfdCountry() {
		return this.dfdCountry;
	}
	
	public void setDfdCountry(String countryIn) {
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
				&& map.getCountries().containsKey(rsp.getAtkCountry())
				&& map.getCountries().get(rsp.getAtkCountry()).getNumArmies() > rsp.getNumDice()) {
				if (rsp.getDfdCountry() != null
					&& map.getCountries().containsKey(rsp.getDfdCountry())
					&& !map.getCountries().get(rsp.getDfdCountry()).getOwner().equals(playerName)) {
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