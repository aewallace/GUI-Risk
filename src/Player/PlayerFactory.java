package Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory Class for Players.
 */
public class PlayerFactory {
	private static int numEasy = 0;
	private static int numNormal = 0;
	private static int numHard = 0;
	private static int numSeth = 0;
	private static int numConsole = 0;

	public static final String EASY = "Easy";
	public static final String NORMAL = "Normal";
	public static final String HARD = "Hard";
	public static final String SETH = "Seth";
	public static final String CONSOLE = "Console";
	
	/**
	 * Builds a Player object of the specified sub-type.
	 * @param type
	 * @return Player object
	 */
	public static Player getPlayer(String type) {
		if (EASY.equals(type)) {
			return new EasyDefaultPlayer(EASY + " " + numEasy++);
		}
		else if (NORMAL.equals(type)) {
			return new NormalDefaultPlayer(NORMAL + " " + numNormal++);
		}
		else if (HARD.equals(type)) {
			return new HardDefaultPlayer(HARD + " " + numHard++);
		}
		else if (SETH.equals(type)) {
			return new Seth(SETH + " " + numSeth++);
		}
		else if (CONSOLE.equals(type)) {
			return new CommandConsole(CONSOLE + " " + numConsole++);
		}
		else {
			throw new RuntimeException("Cannot instantiate Player type: " + type);
		}
	}
	
	/**
	 * Given a comma-separated String of Player types, return a list of
	 * Players that correspond to those types.
	 * @param playerList
	 * @return list of Players
	 */
	public static List<Player> getPlayersFromString(String playerList) {
		String[] playerTypes = playerList.split(",");
		List<Player> players = new ArrayList<Player>();
		
		for (String type : playerTypes) {
			players.add(getPlayer(type));
		}
		
		return players;
	}
	
	public static void resetPlayerCounts() {
		numEasy = 0;
		numNormal = 0;
		numHard = 0;
		numSeth = 0;
		numConsole = 0;
	}
}
