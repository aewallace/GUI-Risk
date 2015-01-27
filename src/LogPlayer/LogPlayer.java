//created by Seth Denney, ver y2014.mdI15.hmW59
//edited by Albert Wallace (aew0024@auburn.edu), ver y2015.mdA27.hm1217A

package LogPlayer;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;
//import javafx.animation.Timeline;
//import javafx.animation.KeyFrame;



public class LogPlayer extends Application {
 
    private static final int DEFAULT_APP_WIDTH = 1600;
    private static final int DEFAULT_APP_HEIGHT = 1062;
    private static final int RAPID_PLAY_TIME_DELTA = 1500;
    private static final int NORMAL_PLAY_TIME_DELTA = 6500;
   /* private static final int RAPID_PLAY_TIME_DELTA = 0;
    private static final int NORMAL_PLAY_TIME_DELTA = 0;*/
    private static final String LOG_FILE = "LOG.txt";
	private static final String EVENT_DELIM = "...";
	private static final int PLAY_FWD = 1;
	private static final int FAST_FWD = -1;
	private static final int REWIND = 2;
	private static final int PAUSE = 0;
	
	private static final Duration PROBE_FREQUENCY = Duration.seconds(2);

	//private Timeline timeline;
	
    private ScrollPane scrollPane;
    private Scene scene;
    private Pane pane;
    private Text eventTitle;
    private Text round;
    private Text turn;
    private Text nextLogLine;
    private Text errorDisplay;
    private String errorText;
    private boolean errorDisplayBit;
    private Map<String, Text> textNodeMap;
    private Map<String, Color> playerColorMap;
    
    private Scanner log;
    private String nextToken;
    private String dlTokenHelper;
    private ArrayList<String> tokenCollection;
    private ArrayList<Map<String, Text>> mapStateCollection;
    private int positionInTokenCollection;
    private boolean inREWIND;
    private boolean initialPlay;
    private boolean cancelActiveActions;
    private int currentButton;
 
    @Override
    public void start(Stage primaryStage) {
    	try {
			this.log = new Scanner(new File(LOG_FILE));
			this.nextToken = null;
			currentButton = PAUSE;
			
			inREWIND = false;
			initialPlay = true;
			dlTokenHelper = "";
			positionInTokenCollection = -1;
			tokenCollection = new ArrayList<String>();
			mapStateCollection = new ArrayList<Map<String, Text>>();
			cancelActiveActions = false;
			
	        pane = new Pane();
	        pane.setPrefSize(DEFAULT_APP_WIDTH + 200, DEFAULT_APP_HEIGHT + 30);
	        /*pane.setStyle....
	        * we set the image in the pane based on whether there was an error or not.
	        *  for reference, please see later in the start() method
	        * it will be similar to...
	        * pane.setStyle("-fx-background-image: url(\"RiskBoard.jpg\")");*/
	       
	        errorDisplayBit = false;
	        errorText = "";
	        
	        loadTextNodes("TextNodes.txt");
	        loadPlayers();
	        
	        
	        //if there is an error on loading necessary resources,
	        // render the "negated" map image as a visual cue to indicate failure
	        if (errorDisplayBit){
	        	pane.setStyle("-fx-background-image: url(\"RiskBoardAE.jpg\")");
		        errorDisplay = new Text(100, 100, errorText);
		        errorDisplay.setFont(Font.font("Verdana", FontWeight.BOLD, 24));
		        errorDisplay.setFill(Color.RED);
		        pane.getChildren().add(errorDisplay);
		        
	        }
	        
	        //if there was no error, populate the window with appropriate elements
	        if(!errorDisplayBit){ 
	        	pane.setStyle("-fx-background-image: url(\"RiskBoard.jpg\")");
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
		        		currentButton = PAUSE;
		        		readNextLogEvent(LOG_FILE, cancelActiveActions);
		        	}
		        });
		        
		        pane.getChildren().add(nextActionBtn);
		        
		        
		        //The Play-Forward (normal speed) Button
		        Button playFwdBtn = new Button("Auto-play Events");
		        playFwdBtn.setLayoutX(1359);
		        playFwdBtn.setLayoutY(560);
		        
		        playFwdBtn.setOnAction(new EventHandler<ActionEvent>() {
		        	@Override
		        	public void handle(ActionEvent event) {
		        		//cancelActiveActions = true;
				        Runnable task = new Runnable() {
				        	  @Override public void run() {
				        		  try
				        		  {
				        			  java.lang.Thread.sleep(1000);
				        			  //cancelActiveActions = false;
				        			  java.lang.Thread.sleep(1000);
				        			  runButtonRunnable(PLAY_FWD, cancelActiveActions);
				        			  /*currentButton = PLAY_FWD;
				        			  while(true && currentButton == PLAY_FWD){
						        			java.lang.Thread.sleep(1000);
											Platform.runLater(new Runnable() {
												  @Override public void run(){
											    		//performAutoPlayback(LOG_FILE, PLAY_FWD);
											    		readNextLogEvent(LOG_FILE);
											    		//java.lang.Thread.sleep(1000);
											    	} 
											 });
				        			  }*/
				        		  }//end try
				        		  catch(Exception e)
				        		  {	
				        		  } //end catch	
				        	      
				        	      }
				        	  };
				        	  
				        	
				        	Thread th = new Thread(task);
				        	th.setDaemon(true);
				        	th.start();
				        	
		        	}
		        });
		        
		        pane.getChildren().add(playFwdBtn);
		        
		        //The fast forward (rapid-speed forward) button:
		        Button fastFwdBtn = new Button("Fast-Forward Events");
		        fastFwdBtn.setLayoutX(1159);
		        fastFwdBtn.setLayoutY(500);
		        
		        fastFwdBtn.setOnAction(new EventHandler<ActionEvent>() {
		        	@Override
		        	public void handle(ActionEvent event) {
		        		//cancelActiveActions = true;
				        Runnable task = new Runnable() {
				        	  @Override public void run() {
				        		  try
				        		  {
				        			  java.lang.Thread.sleep(1000);
				        			  //cancelActiveActions = false;
				        			  java.lang.Thread.sleep(1000);
				        			  runButtonRunnable(FAST_FWD, cancelActiveActions);
				 
				        		  }//end try
				        		  catch(Exception e)
				        		  {	
				        			  //todo: in case any uncaught exceptions occur, catch 'em here.
				        		  } //end catch	
				        	      
				        	      }
				        	  };
				        	  
				        	
				        	Thread fth = new Thread(task);
				        	fth.setDaemon(true);
				        	fth.start();
				        	
		        	}
		        });
		        pane.getChildren().add(fastFwdBtn);
		        //end FFWD button
		        
		      //The rewind (dual-speed reverse) button:
		        Button dsRewindBtn = new Button("Slow/Fast Rewind Events");
		        dsRewindBtn.setLayoutX(1159);
		        dsRewindBtn.setLayoutY(600);
		        
		        dsRewindBtn.setOnAction(new EventHandler<ActionEvent>() {
		        	@Override
		        	public void handle(ActionEvent event) {
		        		//cancelActiveActions = true;
				        Runnable task = new Runnable() {
				        	  @Override public void run() {
				        		  try
				        		  {
				        			  java.lang.Thread.sleep(1000);
				        			  //cancelActiveActions = false;
				        			  java.lang.Thread.sleep(1000);
				        			  runButtonRunnable(REWIND, cancelActiveActions);
				 
				        		  }//end try
				        		  catch(Exception e)
				        		  {	
				        			  //todo: in case any uncaught exceptions occur, catch 'em here.
				        		  } //end catch	
				        	      
				        	      }
				        	  };
				        	  
				        	
				        	Thread fth = new Thread(task);
				        	fth.setDaemon(true);
				        	fth.start();
				        	
		        	}
		        });
		        pane.getChildren().add(dsRewindBtn);
		        //end RWND button
		        
		        
	        } //END: layout of buttons displayed upon successful launch ends here.
	        
	       
			scrollPane = new ScrollPane();
			scrollPane.setContent(pane);
			if (!errorDisplayBit){
				scrollPane.setOnKeyPressed(new EventHandler<KeyEvent>() {
		        	@Override
		        	public void handle(KeyEvent event) {
		        		readNextLogEvent(LOG_FILE, cancelActiveActions);
		        	}
		        });
			}
	        
			
			scene = new Scene(scrollPane, DEFAULT_APP_WIDTH, DEFAULT_APP_HEIGHT);
	        primaryStage.setScene(scene);
	        primaryStage.show();
    	}
    	catch (FileNotFoundException e) {
    	}
    }
    
    void runButtonRunnable(int playType, boolean cancelIMCurrentAction){
    	int waitTime = 0; //will be set to a certain number of milliseconds to alter rapid-vs-normal FWD/REWIND
    	switch(playType)
    	{
    		case PLAY_FWD:
    			inREWIND = false;
    			waitTime = NORMAL_PLAY_TIME_DELTA;
    			break;
    		case FAST_FWD:
    			inREWIND = false;
    			waitTime = RAPID_PLAY_TIME_DELTA;
    			break;
    		case REWIND:
    			//if (inREWIND){waitTime = RAPID_PLAY_TIME_DELTA;}
    			//if (!inREWIND){waitTime = NORMAL_PLAY_TIME_DELTA; inREWIND = true;}
    			inREWIND = true;
    			waitTime = RAPID_PLAY_TIME_DELTA;
    			break;
    		case PAUSE:
    			inREWIND = false;
    			break;
    		default:
    			break;
    	}
	  currentButton = playType;
	  try{
		  final int OLDBUTTON = playType;
		  while(!cancelActiveActions && currentButton == OLDBUTTON){
				java.lang.Thread.sleep(waitTime);
				Platform.runLater(new Runnable() {
					  @Override public void run(){
				    		performAutoPlayback(LOG_FILE, OLDBUTTON, cancelActiveActions);
				    		//readNextLogEvent(LOG_FILE, cancelActiveActions);
				    		//java.lang.Thread.sleep(1000);
				    	} 
				 });
		  }
	  }
	  catch(Exception e)
	  {
		  System.out.println("runButtonRunnable: Exception: " + e);
		  //todo: insert recovery code here
	  }
	  
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
    
    private void loadTextNodes(String nodeFile) {
		try {
			if (nodeFile != null) {
				this.textNodeMap = new HashMap<String, Text>();
				File fileRepresentation = new File(nodeFile);
						//basic check for existence of country list file
				if (!fileRepresentation.exists()){
					System.out.print("Warning: no known list of countries found!");
					System.out.print("\nExpected: \"" + nodeFile + "\"\n");
					System.out.print("Undefined behavior WILL occur!");
					errorDisplayBit = true;
					errorText = "File not found in working directory;\nsee console for details.";
				}
						//and basic check for valid file contents
				else if (fileRepresentation.length() < 25){
					System.out.print("Warning: malform input file detected!");
					System.out.print("\nExpected \"" + nodeFile + "\" to be of a certain size.\n");
					System.out.print("Please check the file and restart the LogyPlayer GUI.\n");
					errorDisplayBit = true;
					errorText = "Malformed input file detected;\nsee console for details.";
				}
				else{
					Scanner reader = new Scanner(fileRepresentation);
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
		}
		catch (Exception e) {
			//errorDisplay.setText(e.getMessage());
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
    
    
    private void performAutoPlayback(String logFile, int playType, boolean cancelIMCurrentAction){
    	/*int waitTime = 0; //set this to a certain number of milliseconds to alter rapid-vs-normal FWD/REWIND
    	switch(playType){
    		case PLAY_FWD:
    			inREWIND = false;
    			waitTime = NORMAL_PLAY_TIME_DELTA;
    			break;
    		case FAST_FWD:
    			inREWIND = false;
    			waitTime = RAPID_PLAY_TIME_DELTA;
    			break;
    		case REWIND:
    			if (inREWIND){waitTime = RAPID_PLAY_TIME_DELTA;}
    			if (!inREWIND){waitTime = NORMAL_PLAY_TIME_DELTA; inREWIND = true;}
    			break;
    		case PAUSE:
    			inREWIND = false;
    			break;
    		default:
    			break;
    	}*/
    	if (cancelActiveActions){return;}
    	try{
		    	if (initialPlay){
			    	switch(playType){
				    	case PLAY_FWD:
			    		case FAST_FWD:
				    			System.out.println("FWD Recording + Playback in use! ..SZ:" + tokenCollection.size() + "...PSTN: " + positionInTokenCollection);
			    				if (!tokenCollection.isEmpty() && positionInTokenCollection < tokenCollection.size() - 1)
			    				{
			    					positionInTokenCollection++;
			    					processCaptiveToken(tokenCollection.get(positionInTokenCollection), (positionInTokenCollection == tokenCollection.size()-1), cancelActiveActions);
			    					//this.textNodeMap = mapStateCollection.get(positionInTokenCollection);
			    					//Map<String, Text> mSCSubset = mapStateCollection.get(positionInTokenCollection);
			    					for (Map.Entry<String, Text> entry : mapStateCollection.get(positionInTokenCollection).entrySet())
			    					{
				    					Text txt = this.textNodeMap.get(entry.getKey());
				    					txt.setFill(entry.getValue().getFill());
				    					txt.setText(entry.getValue().getText());
			    					}
			    					if (this.textNodeMap.isEmpty()){System.out.println("PerformAutoPlayback genericE: textNodeMap reported as empty");}
			    				}
			    				else if (nextToken != null)
						    	{
						    		readNextLogEvent(logFile, cancelActiveActions);
						    		tokenCollection.add(nextToken);
						    		mapStateCollection.add(textNodeMap);
					    			positionInTokenCollection++;
					    			System.out.println(nextToken + "\n");
						    		System.out.println("positionInTokenCollection :B= " + positionInTokenCollection + " ....VS Size: " + tokenCollection.size());
						    	}
			    				else{currentButton = PAUSE;}
				    			break;
			    		case REWIND:
			    				System.out.println("PerformAutoPlayback genericE: Rewind entered");
					    		if (!tokenCollection.isEmpty() && positionInTokenCollection >= 0 && inREWIND)
						    		{
					    			System.out.println("PerformAutoPlayback genericE: Rewind middle1");
					    			if(tokenCollection.get(positionInTokenCollection) != null){ //more error handling; todo: remove in final version
					    				System.out.println("PerformAutoPlayback genericE: Rewind middle2 + " + positionInTokenCollection);
					    				processCaptiveToken(tokenCollection.get(positionInTokenCollection), (positionInTokenCollection == 0), cancelActiveActions);
					    				System.out.println("PerformAutoPlayback genericE: Rewind middle2 + " + positionInTokenCollection);
					    				//this.textNodeMap = mapStateCollection.get(positionInTokenCollection);
					    				for (Map.Entry<String, Text> entry : mapStateCollection.get(positionInTokenCollection).entrySet())
				    					{
					    					System.out.println(entry.getValue().getText());
					    					Text txtN = this.textNodeMap.get(entry.getKey());
					    					txtN.setFill(entry.getValue().getFill());
					    					txtN.setText(entry.getValue().getText());
					   
				    					}
					    				if (this.textNodeMap == null || this.textNodeMap.isEmpty()){System.out.println("PerformAutoPlayback genericE: textNodeMap reported as empty");}
					    			}
					    			else{System.out.println("genericE: null entry found in token collection");}
						    		positionInTokenCollection--;
						    		System.out.println("PerformAutoPlayback genericE: Rewind exiting...");
						    		}
					    		else{inREWIND = false; currentButton = PAUSE;}
					    		if (positionInTokenCollection < 0){positionInTokenCollection = 0; currentButton = PAUSE;}
					    		System.out.println("PerformAutoPlayback genericE: Rewind done.");
				    			break;
			    		case PAUSE:
			    			inREWIND = false;
			    			break;
			    		default:
			    			break;
			    	}
		    	}
		    	else{
		    		if (cancelActiveActions){return;}
		    		switch(playType){
			    	case PLAY_FWD:
		    		case FAST_FWD:
		    				if (!tokenCollection.isEmpty() && positionInTokenCollection < tokenCollection.size())
		    				{
		    					positionInTokenCollection++;
		    					processCaptiveToken(tokenCollection.get(positionInTokenCollection), (positionInTokenCollection == tokenCollection.size()-1), cancelActiveActions);
		    					this.textNodeMap = mapStateCollection.get(positionInTokenCollection);
		    				}
		    				if (positionInTokenCollection >= tokenCollection.size()){positionInTokenCollection = tokenCollection.size() - 1; currentButton = PAUSE;}
		    				break;
		    				
		    		case REWIND:
				    		if (!tokenCollection.isEmpty() && positionInTokenCollection >= 0 && inREWIND)
				    		{
				    			if(tokenCollection.get(positionInTokenCollection) != null){ //more error handling; todo: remove in final version
				    				processCaptiveToken(tokenCollection.get(positionInTokenCollection), (positionInTokenCollection == 0), cancelActiveActions);
				    				//this.textNodeMap = mapStateCollection.get(positionInTokenCollection);
				    				if (this.textNodeMap == null || this.textNodeMap.isEmpty()){System.out.println("PerformAutoPlayback genericE: textNodeMap reported as empty");}
				    			}
				    			else{System.out.println("PerformAutoPlayback genericE: null entry found in token collection");}
					    		positionInTokenCollection--;
				    		}
				    		else{inREWIND = false; currentButton = PAUSE; cancelActiveActions = true;}
				    		if (positionInTokenCollection < 0){positionInTokenCollection = 0; currentButton = PAUSE;}
				    		break;
				    		
		    		case PAUSE:
		    			inREWIND = false;
		    			break;
		    		default:
		    			break;
		    		}
		    		
		    	}
    	}
    	catch(Exception e)
    	{
    		System.out.println("PerformAutoPlayback exception:" + e);
    	}
    }
 
    private void processCaptiveToken(String currentTokenIn, boolean isLastToken, boolean cancelIMCurrentAction){
    	if (cancelActiveActions){return;}
    	processCaptiveToken(currentTokenIn, "", isLastToken, cancelIMCurrentAction);
    }
    
    private void processCaptiveToken(String currentTokenIn, String nextTokenIn, boolean isLastToken, boolean cancelIMCurrentAction){
    	if (cancelActiveActions){return;}
    	System.out.println("ProcessCaptiveToken genericE: Inner 0");
    	try{
    		/*boolean nextLineFound = nextLogLine.getText().equals("Next event: " + nextToken);*/
			/*while (nextToken != null) {*/
				if (currentTokenIn.matches(".* reinforcing with .* armies.")) {
					System.out.println("ProcessCaptiveToken genericE: Inner 1.1");
					/*if (!nextLineFound) {
						nextLineFound = true;
					}
					else {
						nextLineFound = false;*/
						String playerName = parsePlayerName(currentTokenIn, " reinforcing ");
						System.out.println("ProcessCaptiveToken genericE: Inner 1.2");
						eventTitle.setText(playerName + " reinforcing.");
						/*nextToken = log.nextLine();*/
						//while (!currentTokenIn.equals(EVENT_DELIM)) {
							//int armies = parseReinforceAmt(currentTokenIn);
							//String countryName = parseReinforceCountry(currentTokenIn);
							//setCountryOwnership(countryName, playerName);
							//addArmiesToCountry(countryName, armies);
							System.out.println("ProcessCaptiveToken genericE: Inner 1.3");
							turn.setText(playerName + "'s Turn");
							/*currentTokenIn = log.nextLine();*/
						//}
						/*nextToken = log.nextLine();*/
					/*}*/
						System.out.println("ProcessCaptiveToken genericE: Inner 1.4");
				}
				else if (currentTokenIn.matches("Beginning Round .*!")) {
					System.out.println("ProcessCaptiveToken genericE: Inner 2.1");
					/*if (!nextLineFound) {
						nextLineFound = true;
					}
					else {
						nextLineFound = false;*/
						round.setText(currentTokenIn.substring(10, currentTokenIn.length() - 1));
						eventTitle.setText("New Round.");
						/*currentTokenIn = log.nextLine();*/
					/*}*/
						System.out.println("ProcessCaptiveToken genericE: Inner 2.2");
				}
				else if (currentTokenIn.matches(".* is attacking .* from .*!")) {
					System.out.println("ProcessCaptiveToken genericE: Inner 3.1");
					/*if (!nextLineFound) {
						nextLineFound = true;
					}
					else {
						nextLineFound = false;*/
						if (!inREWIND){ //if going forward, store our info for later parsing
							dlTokenHelper = currentTokenIn;
							System.out.println("ProcessCaptiveToken genericE: Inner 3.2");
							}
						else{ //if going in reverse, parse current info + old info
							System.out.println("ProcessCaptiveToken genericE: Inner 3.3");
							String playerName = parsePlayerName(currentTokenIn, " is attacking ");
							String atkCountry = parseAtkCountry(currentTokenIn);
							String dfdCountry = parseDfdCountry(currentTokenIn);
							System.out.println("ProcessCaptiveToken genericE: Inner 3.4");
							eventTitle.setText(playerName + " attacked\n" + dfdCountry + " from " + atkCountry);
							/*nextToken = log.nextLine();*/
							//int atkLosses = parseAtkLosses(dlTokenHelper);
							//int dfdLosses = parseDfdLosses(dlTokenHelper);
							//addArmiesToCountry(atkCountry, -1 * atkLosses);
							//addArmiesToCountry(dfdCountry, -1 * dfdLosses);
							/*nextToken = log.nextLine();*/
							System.out.println("ProcessCaptiveToken genericE: Inner 3.5");
						}
						System.out.println("ProcessCaptiveToken genericE: Inner 3.6");
				}
				else if (currentTokenIn.matches("Attacker lost: .*; Defender lost: .*")){
					System.out.println("ProcessCaptiveToken genericE: Inner 4.1");
						if (inREWIND){ //if rewinding, store our info for parsing up the chain
							dlTokenHelper = currentTokenIn;
							System.out.println("ProcessCaptiveToken genericE: Inner 4.2");
							}
						else{ //if forwarding, parse old info + current info
							System.out.println("ProcessCaptiveToken genericE: Inner 4.3");
							String playerName = parsePlayerName(dlTokenHelper, " is attacking ");
							String atkCountry = parseAtkCountry(dlTokenHelper);
							String dfdCountry = parseDfdCountry(dlTokenHelper);
							System.out.println("ProcessCaptiveToken genericE: Inner 4.4");
							eventTitle.setText(playerName + " attacked\n" + dfdCountry + " from " + atkCountry);
							/*nextToken = log.nextLine();*/
							//int atkLosses = parseAtkLosses(currentTokenIn);
							//int dfdLosses = parseDfdLosses(currentTokenIn);
							//addArmiesToCountry(atkCountry, -1 * atkLosses);
							//addArmiesToCountry(dfdCountry, -1 * dfdLosses);
							/*nextToken = log.nextLine();*/
							System.out.println("ProcessCaptiveToken genericE: Inner 4.5");
						}
					/*}*/
						System.out.println("ProcessCaptiveToken genericE: Inner 4.6");
				}
				else if (currentTokenIn.matches(".* has taken .* from .*!")) {
					System.out.println("ProcessCaptiveToken genericE: Inner 5.1");
					/*if (!nextLineFound) {
						nextLineFound = true;
					}
					else {
						nextLineFound = false;*/
						String playerName = parsePlayerName(currentTokenIn, " has taken ");
						System.out.println("ProcessCaptiveToken genericE: Inner 5.2");
						eventTitle.setText(playerName + " has taken\n" + parseTakenCountry(currentTokenIn));
						System.out.println("ProcessCaptiveToken genericE: Inner 5.3");
						//setCountryOwnership(parseTakenCountry(currentTokenIn), playerName);
						/*nextToken = log.nextLine();*/
					/*}*/
				}
				else if (currentTokenIn.matches(".* advanced .* into .*")) {
					System.out.println("ProcessCaptiveToken genericE: Inner 6.1");
					/*if (!nextLineFound) {
						nextLineFound = true;
					}
					else {
						nextLineFound = false;*/
						String[] line = currentTokenIn.split(" advanced ");
						System.out.println("ProcessCaptiveToken genericE: Inner 6.2");
						eventTitle.setText(line[0] + " advanced\n" + line[1]);
						//int armies = parseAdvanceArmies(currentTokenIn);
						//addArmiesToCountry(parseAdvanceSourceCountry(currentTokenIn), -1 * armies);
						//addArmiesToCountry(parseAdvanceDestinationCountry(currentTokenIn), armies);
						/*nextToken = log.nextLine();*/
					/*}*/
						System.out.println("ProcessCaptiveToken genericE: Inner 6.3");
				}
				else if (currentTokenIn.matches(".* is transferring .* from .* to .*")) {
					System.out.println("ProcessCaptiveToken genericE: Inner 7.1");
					/*if (!nextLineFound) {
						nextLineFound = true;
					}
					else {
						nextLineFound = false;*/
						String[] line = currentTokenIn.split(" transferring ");
						System.out.println("ProcessCaptiveToken genericE: Inner 7.2");
						eventTitle.setText(line[0] + " transferring\n" + line[1]);
						//int armies = parseFortifyArmies(currentTokenIn);
						//String source = parseFortifySourceCountry(currentTokenIn);
						//String dst = parseFortifyDestinationCountry(currentTokenIn);
						//addArmiesToCountry(source, -1 * armies);
						//addArmiesToCountry(dst, armies);
						/*nextToken = log.nextLine();*/
					/*}*/
						System.out.println("ProcessCaptiveToken genericE: Inner 7.3");
				}
				else {
					System.out.println("ProcessCaptiveToken genericE: Inner 8.1");
					/*nextToken = log.nextLine();*/
				}
				/*if (nextLineFound) {*/
					System.out.println("ProcessCaptiveToken genericE: Inner 9.1");
					nextLogLine.setText("Next event: " + currentTokenIn);
					System.out.println("ProcessCaptiveToken genericE: Inner 9.2");
					/*return;
				}*/
				if (isLastToken && !initialPlay) {
					System.out.println("ProcessCaptiveToken genericE: Inner 10.1");
					nextLogLine.setText("Game over!");
					System.out.println("ProcessCaptiveToken genericE: Inner 10.2");
					//nextToken = null;
					/*log.close();*/
				}
			/*}*/ //end while
		} //end try
 		catch (Exception e) {  
 			System.out.println(e.getMessage());
 			//errorDisplay.setText(e.getMessage());
		}
    }
    
    private void readNextLogEvent(String logFile, boolean cancelIMCurrentAction) {
    	if (cancelActiveActions){return;}
    	try{
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
						nextToken = log.nextLine();
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
					initialPlay = false;
				}
			}
		}
 		catch (Exception e) {  
 			System.out.println(e.getMessage());
 			errorDisplay.setText(e.getMessage());
		}
    }
    
    private String parsePlayerName(String line, String afterName) {
    	return line.split(afterName)[0];
    }
    
    private int getPrevArmies(String countryName) {
    	Text txt = this.textNodeMap.get(countryName);
    	if (txt == null){
    	System.out.println("gPA:A:::: country-text-value map null for given value : " + countryName);}
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
    
    //for string similar to...
    //Hard 3 advanced 3 into Northern Europe from Southern Europe.
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

