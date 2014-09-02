package LogPlayer;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class LogPlayer extends Application {
 
    private static final int DEFAULT_APP_WIDTH = 1600;
    private static final int DEFAULT_APP_HEIGHT = 1062;
    private static final String LOG_FILE = "LOG.txt";
	private static final String EVENT_DELIM = "...";
    private Stage stage;
    private Pane root;
    private Scene scene;
    private Pane pane;
    private Text eventTitle;
    private Text round;
    private Text turn;
    private Text nextLogLine;
    private Map<String, Text> textNodeMap;
    private Map<String, Color> playerColorMap;
    
    private Scanner log;
    private String nextToken;
 
    @Override
    public void start(Stage primaryStage) {
        preSetup(primaryStage);
        setup();
        postSetup();
    }
 
    protected void setup() {
    	try {
    		this.log = new Scanner(new File(LOG_FILE));
    		this.nextToken = null;
    	
	        pane = new Pane();
	        pane.getStyleClass().add("RiskBoard");
	        pane.setPrefSize(DEFAULT_APP_WIDTH, DEFAULT_APP_HEIGHT);
	        
	        loadTextNodes("TextNodes.txt");
	        loadPlayers();
	        
	        eventTitle = new Text(1350, 515, "Initial Reinforcement\nStage");
	        eventTitle.setFont(Font.font("Verdana", FontWeight.BOLD, 20));
	        eventTitle.setFill(Color.LIGHTGRAY);
	        pane.getChildren().add(eventTitle);
	        
	        round = new Text(1460, 450, "");
	        round.setFont(Font.font("Verdana", FontWeight.BOLD, 20));
	        round.setFill(Color.LIGHTGRAY);
	        pane.getChildren().add(round);
	        
	        turn = new Text(1425, 470, "");
	        turn.setFont(Font.font("Verdana", FontWeight.BOLD, 20));
	        turn.setFill(Color.LIGHTGRAY);
	        pane.getChildren().add(turn);
	        
	        nextLogLine = new Text(600, 1030, "");
	        nextLogLine.setFont(Font.font("Verdana", FontWeight.BOLD, 20));
	        nextLogLine.setFill(Color.LIGHTGRAY);
	        pane.getChildren().add(nextLogLine);
	        
	        Button nextActionBtn = new Button("Next Event");
	        nextActionBtn.setLayoutX(1427);
	        nextActionBtn.setLayoutY(560);
	        nextActionBtn.setOnAction(new EventHandler<ActionEvent>() {
	        	@Override
	        	public void handle(ActionEvent event) {
	        		readNextLogEvent(LOG_FILE);
	        	}
	        });
	        
	        pane.getChildren().add(nextActionBtn);
	        root.getChildren().addAll(pane);
    	}
    	catch (FileNotFoundException e) {
    	}
    }
 
    protected void preSetup(Stage primaryStage) {
        stage = primaryStage;
        stage.setTitle(getAppTitle());
        root = new StackPane();
        root.getStyleClass().add("root-pane");
        scene = new Scene(root, getAppWidth(), getAppHeight());
        stage.setScene(scene);
        setupCss();
    }
 
    protected void postSetup() {
        stage.centerOnScreen();
        stage.show();
    }
 
    // override to change APP WIDTH
    protected double getAppWidth() {
        return DEFAULT_APP_WIDTH;
    }
 
    // override to change APP HEIGHT
    protected double getAppHeight() {
        return DEFAULT_APP_HEIGHT;
    }
 
    // override to set App Title
    protected String getAppTitle() {
        return "Risk Log Player";
    }
 
    // override to add CSS styles
    protected void setupCss() {
        scene.getStylesheets().add("style.css");
    }
    
    private void loadTextNodes(String nodeFile) {
		try {
			if (nodeFile != null) {
				this.textNodeMap = new HashMap<String, Text>();
				Scanner reader = new Scanner(new File(nodeFile));
				int i = 0;
				while (reader.hasNext()) {
					int nextX = reader.nextInt();
					int nextY = reader.nextInt();
					String nextCountry = reader.nextLine().trim();
					Text txt = new Text(nextX, nextY, nextCountry + "\n0");
			        txt.setFont(Font.font("Verdana", FontWeight.BOLD, 20));
			        this.textNodeMap.put(nextCountry, txt);
			        this.pane.getChildren().add(txt);
				}
				reader.close();
			}
		}
		catch (Exception e) {
		}
	}
    
    private void loadPlayers() {
    	try {
			ArrayList<Color> colors = new ArrayList<Color>();
			colors.add(Color.WHITE);
			colors.add(Color.AQUA);
			colors.add(Color.RED);
			colors.add(Color.GREENYELLOW);
			colors.add(Color.CORAL);
			colors.add(Color.VIOLET);
			this.playerColorMap = new HashMap<String, Color>();
			int i = 0;
			boolean finished = false;
			while (!finished && log.hasNext()) {
				//assign player colors
				if (log.nextLine().equals("Players:")) {
					if (nextToken == null) {
						nextToken = log.nextLine();
					}
					while (!nextToken.equals(EVENT_DELIM)) {
						this.playerColorMap.put(nextToken, colors.get(i++ % colors.size()));
						Text txt = new Text(200 * (i - 1) + 50, 20, nextToken);
						txt.setFont(Font.font("Verdana", FontWeight.BOLD, 20));
						txt.setFill(colors.get((i - 1) % colors.size()));
						this.pane.getChildren().add(txt);
						nextToken = log.nextLine();
					}
					finished = true;
				}
			}
		}
		catch (Exception e) {
		}
    }
    
    private void readNextLogEvent(String logFile) {
    	try {
    		boolean nextLineFound = nextLogLine.getText().equals("Next event: " + nextToken);
			while (nextToken != null) {
				if (nextToken.matches(".* reinforcing with .* armies.")) {
					if (!nextLineFound) {
						nextLineFound = true;
					}
					else {
						nextLineFound = false;
						String playerName = parsePlayerName(nextToken, " reinforcing ");
						eventTitle.setText(playerName + " reinforcing.");
						nextToken = log.nextLine();
						while (!nextToken.equals(EVENT_DELIM)) {
							int armies = parseReinforceAmt(nextToken);
							String countryName = parseReinforceCountry(nextToken);
							setCountryOwnership(countryName, playerName);
							addArmiesToCountry(countryName, armies);
							turn.setText(playerName + "'s Turn");
							nextToken = log.nextLine();
						}
					}
				}
				else if (nextToken.matches("Beginning Round .*!")) {
					if (!nextLineFound) {
						nextLineFound = true;
					}
					else {
						nextLineFound = false;
						round.setText(nextToken.substring(10, nextToken.length() - 1));
						eventTitle.setText("New Round.");
						nextToken = log.nextLine();
					}
				}
				else if (nextToken.matches(".* is attacking .* from .*!")) {
					if (!nextLineFound) {
						nextLineFound = true;
					}
					else {
						nextLineFound = false;
						String playerName = parsePlayerName(nextToken, " is attacking ");
						String atkCountry = parseAtkCountry(nextToken);
						String dfdCountry = parseDfdCountry(nextToken);
						eventTitle.setText(playerName + " attacked\n" + dfdCountry + " from " + atkCountry);
						nextToken = log.nextLine();
						int atkLosses = parseAtkLosses(nextToken);
						int dfdLosses = parseDfdLosses(nextToken);
						addArmiesToCountry(atkCountry, -1 * atkLosses);
						addArmiesToCountry(dfdCountry, -1 * dfdLosses);
						nextToken = log.nextLine();
					}
				}
				else if (nextToken.matches(".* has taken .* from .*!")) {
					if (!nextLineFound) {
						nextLineFound = true;
					}
					else {
						nextLineFound = false;
						String playerName = parsePlayerName(nextToken, " has taken ");
						eventTitle.setText(playerName + " has taken\n" + parseTakenCountry(nextToken));
						setCountryOwnership(parseTakenCountry(nextToken), playerName);
						nextToken = log.nextLine();
					}
				}
				else if (nextToken.matches(".* advanced .* into .*")) {
					if (!nextLineFound) {
						nextLineFound = true;
					}
					else {
						nextLineFound = false;
						String[] line = nextToken.split(" advanced ");
						eventTitle.setText(line[0] + " advanced\n" + line[1]);
						int armies = parseAdvanceArmies(nextToken);
						addArmiesToCountry(parseAdvanceSourceCountry(nextToken), -1 * armies);
						addArmiesToCountry(parseAdvanceDestinationCountry(nextToken), armies);
						nextToken = log.nextLine();
					}
				}
				else if (nextToken.matches(".* is transferring .* from .* to .*")) {
					if (!nextLineFound) {
						nextLineFound = true;
					}
					else {
						nextLineFound = false;
						String[] line = nextToken.split(" transferring ");
						eventTitle.setText(line[0] + " transferring\n" + line[1]);
						int armies = parseFortifyArmies(nextToken);
						String source = parseFortifySourceCountry(nextToken);
						String dst = parseFortifyDestinationCountry(nextToken);
						addArmiesToCountry(source, -1 * armies);
						addArmiesToCountry(dst, armies);
						nextToken = log.nextLine();
					}
				}
				else {
					nextToken = log.nextLine();
				}
				if (nextLineFound) {
					nextLogLine.setText("Next event: " + nextToken);
					return;
				}
				if (!log.hasNext()) {
					eventTitle.setText("Game over!");
					nextToken = null;
					log.close();
				}
			}
		}
		catch (Exception e) {
		}
    }
    
    private String parsePlayerName(String line, String afterName) {
    	return line.split(afterName)[0];
    }
    
    private int getPrevArmies(String countryName) {
    	Text txt = this.textNodeMap.get(countryName);
		return Integer.parseInt(txt.getText().split("\n")[1]);
    }
    
    private int parseReinforceAmt(String reinforceLine) {
		return Integer.parseInt(reinforceLine.split(" ")[0]);
    }
    
    private String parseReinforceCountry(String reinforceLine) {
    	String[] split = reinforceLine.split(" ");
		String countryName = split[1];
		if (split.length == 3) {
			countryName += " " + split[2];
		}
		return countryName;
    }
    
    private void addArmiesToCountry(String countryName, int armies) {
    	Text txt = this.textNodeMap.get(countryName);
		int oldArmies = getPrevArmies(countryName);
		txt.setText(countryName + "\n" + (oldArmies + armies));
    }
    
    private void setCountryOwnership(String countryName, String owner) {
    	this.textNodeMap.get(countryName).setFill(this.playerColorMap.get(owner));
    }
    
    private String parseAtkCountry(String atkLine) {
    	return atkLine.split("\\) from ")[1].split("\\(")[0];
    }
    
    private String parseDfdCountry(String atkLine) {
    	return atkLine.split(" is attacking ")[1].split("\\(")[0];
    }
    
    private int parseAtkLosses(String lossLine) {
    	String temp = lossLine.split("Attacker lost: ")[1];
    	return Integer.parseInt(temp.substring(0, temp.indexOf(';')));
    }
    
    private int parseDfdLosses(String lossLine) {
    	String temp = lossLine.split("Defender lost: ")[1];
    	return Integer.parseInt(temp.substring(0));
    }
    
    private String parseTakenCountry(String takenLine) {
    	return takenLine.split(" has taken ")[1].split(" from")[0];
    }
    
    private String parseAdvanceSourceCountry(String advLine) {
    	String temp = advLine.split(" from ")[1];
    	return temp.substring(0, temp.length() - 1);
    }
    
    private String parseAdvanceDestinationCountry(String advLine) {
    	String temp = advLine.split(" into ")[1];
    	return temp.split(" from ")[0];
    }
    
    private int parseAdvanceArmies(String advLine) {
    	String temp = advLine.split(" advanced ")[1];
    	return Integer.parseInt(temp.substring(0, temp.indexOf(' ')));
    }
    
    private int parseFortifyArmies(String fortifyLine) {
    	return Integer.parseInt(fortifyLine.split(" is transferring ")[1].split(" from ")[0]);
    }
    
    private String parseFortifySourceCountry(String fortifyLine) {
    	return fortifyLine.split(" from ")[1].split(" to ")[0];
    }
    
    private String parseFortifyDestinationCountry(String fortifyLine) {
    	String temp = fortifyLine.split(" to ")[1];
    	return temp.substring(0, temp.length() - 1);
    }
    
    public static void main(String[] args) {
    	launch(LogPlayer.class, args);
    }
}
