//Current build Albert Wallace, Version 007, Stamp y2015.mdB02.hm2022.sALP
//Base build by Seth Denney, Sept 10 2014 

//todo: adding to cache does not occur at proper time. One or two lines may be nipped from the end,
// as well as the fact that a single step does not result in the cache being updated.
//EDIT: slightly ameliorated.

//todo: ensure you only have one rewind or one ff at a time
//slightly ameliorated.

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
import javafx.scene.paint.Paint;
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
    private static final int RAPID_PLAY_TIME_DELTA = 1170;
    private static final int NORMAL_PLAY_TIME_DELTA = 5650;
    private static final int BUSYROUTINE_THRESH = 0;
    private static final int BUSYROUTINE_RETRY_COUNT = 7;
    private static final int BUSYROUTINE_WAIT_TIME = 350;
    private static final String LOG_FILE = "LOG.txt";
	private static final String EVENT_DELIM = "...";
	private static final int PLAY_FWD = 1;
	private static final int FAST_FWD = -1;
	private static final int REWIND = 2;
	private static final int PAUSE = 0;
	private static final int STEP_FWD = -2;
	private static final int iRoNMAX = 21;
	private static double EXPON_SPEED_UP_PCT = 1.0;
	

	//private Timeline timeline;
	
    private ScrollPane scrollPane;
    private Scene scene;
    private Pane pane;
    private Text eventTitle;
    private Text round;
    private Text turn;
    private Text nextLogLine;
    private Text errorDisplay;
    private Text currentPlayStatus;
    private String errorText;
    private boolean errorDisplayBit;
    private HashMap<String, Text> textNodeMap;
    private Map<String, Color> playerColorMap;
    
    private Scanner log;
    private String nextToken;
    private String dlTokenHelper;
    private ArrayList<String> logCache;
    private ArrayList<HashMap<String, Text>> mapStateCache;
    private int positionInCaches;
    private boolean inREWIND;
    private boolean initialPlay = true;
    private boolean cancelActiveActions;
    private int currentButton;
    private String currentSimpleStatus;
    private int iRoN; //todo: fix bad name
    private int busyRoutines; //to perform basic resource locks
    private int routinesRequestingPriority;
 
    @Override
    public void start(Stage primaryStage) {
    	try {
			this.log = new Scanner(new File(LOG_FILE));
			this.nextToken = null;
			this.currentButton = PAUSE;
			this.currentSimpleStatus = "";
			iRoN = 0;
			this.busyRoutines = 0;
			this.routinesRequestingPriority = 0;
			inREWIND = false;
			dlTokenHelper = "";
			positionInCaches = -1;
			this.logCache = new ArrayList<String>();
			this.mapStateCache = new ArrayList<HashMap<String, Text>>();
			cancelActiveActions = false;
			
	        pane = new Pane();
	        pane.setPrefSize(DEFAULT_APP_WIDTH + 200, DEFAULT_APP_HEIGHT + 30);
	        /*pane.setStyle....
	        * we set the image in the pane based on whether there was an error or not.
	        *  for reference, please see later in the start() method
	        * it will be similar to...
	        * pane.setStyle("-fx-background-image: url(\"RiskBoard.jpg\")");*/
	       
	        errorDisplayBit = false;
	        errorText = "Status...";
	        
	        loadTextNodes("TextNodes.txt");
	        loadPlayers();
	        
	        
	        //if there is an error on loading necessary resources,
	        // render the "negated" map image as a visual cue to indicate failure
        	pane.setStyle("-fx-background-image: url(\"RiskBoardAE.jpg\")");
	        errorDisplay = new Text(29, 560, errorText);
	        errorDisplay.setFont(Font.font("Verdana", FontWeight.BOLD, 24));
	        if(errorDisplayBit){errorDisplay.setFill(Color.RED);}
	        else{errorDisplay.setFill(Color.WHITE);}
	        	
	        pane.getChildren().add(errorDisplay);
	        
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
		        
		        currentPlayStatus = new Text(29, 600, "Hello! ^.^");
		        currentPlayStatus.setFont(Font.font("Verdana", FontWeight.BOLD, 40));
		        currentPlayStatus.setFill(Color.WHITE);
		        pane.getChildren().add(currentPlayStatus);
	        	
		       //The original single-seek/step-through "Next Event" button 
		        Button nextActionBtn = new Button("Single-Step to Next Event");
		        nextActionBtn.setLayoutX(29);
		        nextActionBtn.setLayoutY(770);
		        nextActionBtn.setOnAction(new EventHandler<ActionEvent>() {
		        	@Override
		        	public void handle(ActionEvent event) {
				        Runnable task = new Runnable() {
				        	  @Override public void run() {
					        		  try
					        		  {
					        			  java.lang.Thread.sleep(1000);
					        			  runButtonRunnable(STEP_FWD, cancelActiveActions);
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
		        pane.getChildren().add(nextActionBtn);
		        
		        
		      //The Play-Forward (normal speed) Button
		        Button pauseAllBtn = new Button("Pause Event Playback");
		        pauseAllBtn.setLayoutX(29);
		        pauseAllBtn.setLayoutY(650);
		        
		        pauseAllBtn.setOnAction(new EventHandler<ActionEvent>() {
		        	@Override
		        	public void handle(ActionEvent event) {
				        Runnable task = new Runnable() {
				        	  @Override public void run() {
				        		  try
				        		  {
				        			  java.lang.Thread.sleep(1000);
				        			  runButtonRunnable(PAUSE, cancelActiveActions);
				        			 
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
		        
		        pane.getChildren().add(pauseAllBtn);
		        
		        
		        //The Play-Forward (normal speed) Button
		        Button playFwdBtn = new Button("Auto-play Events");
		        playFwdBtn.setLayoutX(29);
		        playFwdBtn.setLayoutY(610);
		        
		        playFwdBtn.setOnAction(new EventHandler<ActionEvent>() {
		        	@Override
		        	public void handle(ActionEvent event) {
				        Runnable task = new Runnable() {
				        	  @Override public void run() {
				        		  try
				        		  {
				        			  java.lang.Thread.sleep(1000);
				        			  runButtonRunnable(PLAY_FWD, cancelActiveActions);
				        			 
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
		        fastFwdBtn.setLayoutX(29);
		        fastFwdBtn.setLayoutY(690);
		        
		        fastFwdBtn.setOnAction(new EventHandler<ActionEvent>() {
		        	@Override
		        	public void handle(ActionEvent event) {
				        Runnable task = new Runnable() {
				        	  @Override public void run() {
				        		  try
				        		  {
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
		        dsRewindBtn.setLayoutX(29);
		        dsRewindBtn.setLayoutY(730);
		        
		        dsRewindBtn.setOnAction(new EventHandler<ActionEvent>() {
		        	@Override
		        	public void handle(ActionEvent event) {
				        Runnable task = new Runnable() {
				        	  @Override public void run() {
				        		  try
				        		  {
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
				        Runnable task = new Runnable() {
				        	  @Override public void run() {
					        		  try
					        		  {
					        			  java.lang.Thread.sleep(1000);
					        			  runButtonRunnable(STEP_FWD, cancelActiveActions);
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
			}
	        
			
			scene = new Scene(scrollPane, DEFAULT_APP_WIDTH, DEFAULT_APP_HEIGHT);
	        primaryStage.setScene(scene);
	        primaryStage.show();
    	}
    	catch (FileNotFoundException e) {
    	}
    }
    
    void runButtonRunnable(int btnTypeIn, boolean cancelIMCurrentAction){
    	int waitTime = 0; //will be set to a certain number of milliseconds to alter rapid-vs-normal FWD/REWIND
    	this.routinesRequestingPriority++;
    	iRoN = 0;
    	System.out.println("EPRO-A this.initialPlay = " + this.initialPlay);
    	switch(btnTypeIn)
    	{
    		case PLAY_FWD:
    			inREWIND = false;
    			this.currentSimpleStatus = ">";
    			waitTime = NORMAL_PLAY_TIME_DELTA;
    			break;
    		case FAST_FWD:
    			inREWIND = false;
    			if(this.currentSimpleStatus.equals(">>>")){
	    			this.currentSimpleStatus = ">>>>>";
	    			waitTime = (int)(0.7*RAPID_PLAY_TIME_DELTA);
    			}
    			else
    			{
    				this.currentSimpleStatus = ">>>";
        			waitTime = RAPID_PLAY_TIME_DELTA;
    			}
    			break;
    		case REWIND:
    			if (inREWIND){waitTime = (int) (0.7 * RAPID_PLAY_TIME_DELTA); this.currentSimpleStatus = "<<<<<";}
    			if (!inREWIND){waitTime = RAPID_PLAY_TIME_DELTA; inREWIND = true; this.currentSimpleStatus = "<<<";}
    			break;
    		case STEP_FWD:
    			inREWIND = false;
    			this.currentSimpleStatus = ">||";
    			break;
    		case PAUSE:
    			inREWIND = false;
    			this.currentSimpleStatus = "||/☐";
    			break;
    		default:
    			inREWIND = false;
    			this.currentSimpleStatus = "||/☐";
    			break;
    	}
	  this.currentButton = btnTypeIn;
	  int waitCount = 1;
	  while (this.busyRoutines > BUSYROUTINE_THRESH && waitCount < BUSYROUTINE_RETRY_COUNT)
	  {
		  animateWaitStatus(waitCount);
		  try{
			  java.lang.Thread.sleep(BUSYROUTINE_WAIT_TIME);
		  }
		  catch (Exception e)
		  {}//todo: catch unexpected termination during sleep()
		  finally{waitCount++;}
	  }
	  EXPON_SPEED_UP_PCT = 1.0;
	  try{
		  this.routinesRequestingPriority--;
		  this.busyRoutines++;
		  final int OLDBUTTON = btnTypeIn;
		  final String OLDshPLAYSTATE = this.currentSimpleStatus;
		  while(this.routinesRequestingPriority == 0 && !cancelActiveActions && this.currentButton == OLDBUTTON && OLDshPLAYSTATE == this.currentSimpleStatus && !Thread.interrupted())
		  {
				int q = 3;
				double qRatio = (double)1/q;
				if (0.95 * EXPON_SPEED_UP_PCT *waitTime >= 0.05 * waitTime)
				{
					EXPON_SPEED_UP_PCT = 0.9*EXPON_SPEED_UP_PCT;
				}
				for(; q > 0 && this.currentButton == OLDBUTTON && OLDshPLAYSTATE == this.currentSimpleStatus; q--){
					if(iRoN < iRoNMAX){
							Platform.runLater(new Runnable()
							{
								  @Override public void run(){
									 animateStatus(false);
							    	} 
							});
					}
					java.lang.Thread.sleep((int)(qRatio*waitTime*EXPON_SPEED_UP_PCT));
				}
				
				if(this.currentButton == OLDBUTTON && OLDshPLAYSTATE == this.currentSimpleStatus && this.routinesRequestingPriority == 0)
				{
					Platform.runLater(new Runnable()
					{
						  @Override public void run(){
					    		stepThroughBtnLogic(LOG_FILE, OLDBUTTON, cancelActiveActions);
					    	} 
					});
				}
				
				if(OLDBUTTON == STEP_FWD)
				{
					cancelActiveActions = true;
				}
				
		  }
		  cancelActiveActions = false;
		  System.out.println("Task done.");
		  if(OLDshPLAYSTATE == this.currentSimpleStatus)
			{
			  for (int m = 0; m < 8; m++)
			  {
				  animateStopStatus(m,false);
				  try{
					  java.lang.Thread.sleep(3*BUSYROUTINE_WAIT_TIME);
				  }
				  catch (Exception e)
				  {}//todo: catch unexpected termination during sleep()
			  }
			  animateStopStatus(0,true);
			}
			
	  }
	  catch(Exception e)
	  {
		  System.out.println("runButtonRunnable: Exception: " + e);
		  //todo: insert recovery code here
	  }
	  finally
	  {
		  this.busyRoutines--;
		  System.out.println("EPRO-B this.initialPlay = " + this.initialPlay);
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
    
    private void animateStatus(boolean isEndOfTask)
    {
    	if (isEndOfTask)
    		currentPlayStatus.setText("||/☐");
    	else if(iRoN % 2==0)
    		currentPlayStatus.setText(this.currentSimpleStatus);
    	else if (iRoN + 2 >= iRoNMAX)
    		currentPlayStatus.setText(this.currentSimpleStatus);
    	else
    		currentPlayStatus.setText("————");
    	iRoN++;
    }
    
    private void animateStopStatus(int clkIn,final boolean setFinalStatus)
    {
    	final int clk = clkIn % 4;
    	Platform.runLater(new Runnable()
		{
			  @Override public void run(){
				 if(setFinalStatus){currentPlayStatus.setText("Idle."); return;}
				 switch (clk){
					 case 0:
						 currentPlayStatus.setText("STOP");
						 break;
					 case 1:
						 currentPlayStatus.setText("——");;
						 break;
					 case 2:
						 currentPlayStatus.setText("————");
						 break;
					 case 3:
						 currentPlayStatus.setText("||/☐");
						 break;
				 }
		    	} 
		});
    }
    
    private void animateWaitStatus(int clkIn)
    {
    	final int clk = clkIn % 4;
    	Platform.runLater(new Runnable()
		{
			  @Override public void run(){
				 switch (clk){
					 case 0:
						 currentPlayStatus.setText("Wait");
						 break;
					 case 1:
						 currentPlayStatus.setText("Busy");
						 break;
					 case 2:
						 currentPlayStatus.setText("Wait");
						 break;
					 case 3:
						 currentPlayStatus.setText("....");
						 break;
				 }
		    	} 
		});
    }
    
    private void stepThroughBtnLogic(String logFile, int btnTypeIn, boolean cancelIMCurrentAction){
    	if (cancelActiveActions){return;}
    	try{
		    	if (this.initialPlay){ //if we haven't filled the cache, we step through, or we make use of the little that IS in the cache up to this point
			    	switch(btnTypeIn){
				    	case PLAY_FWD:
				    	case STEP_FWD:
			    		case FAST_FWD:
			    				//if we have cached up to this point (i.e., we played forward, rewound, and played forward again)
			    				if (!logCache.isEmpty() && positionInCaches < logCache.size() - 1 && nextToken != null)
			    				{
			    					/*System.out.println("FWD _________ _ Playback in use! ..SZ:" + logCache.size() + "...PSTN: " + positionInCaches);*/
			    					positionInCaches++;
			    					processCaptiveToken(logCache.get(positionInCaches), false, cancelActiveActions);
			    					/*if (this.textNodeMap.isEmpty()){System.out.println("PerformAutoPlayback genericE: textNodeMap reported as empty");}*/
			    				}
			    				//else if there is still more in the log, but we haven't cached up to the current point
			    				else if (nextToken != null)
						    	{
			    					/*System.out.println("FWD Recording + Playback in use! ..SZ:" + logCache.size() + "...PSTN: " + positionInCaches);*/
						    		readNextLogEvent(logFile, cancelActiveActions);
						    	}
			    				//else, we have reached the end of the log for the initial playthrough (and everything is cached by now)
			    				else{this.currentButton = PAUSE;}
			    				/*System.out.println("this.initialPlay = " + this.initialPlay);*/
				    			break;
			    		case REWIND:
			    				System.out.println("PerformAutoPlayback genericE: Single rewind step started");
					    		if (!logCache.isEmpty() && positionInCaches >= 0 && inREWIND)
						    		{
					    			/*System.out.println("PerformAutoPlayback genericE: Rewind middle1");*/
					    			if(logCache.get(positionInCaches) != null){ //more error handling; todo: remove in final version
					    				/*System.out.println("PerformAutoPlayback genericE: Rewind middle2 + " + positionInCaches);*/
					    				processCaptiveToken(logCache.get(positionInCaches), (positionInCaches == 0), cancelActiveActions);
					    				/*System.out.println("PerformAutoPlayback genericE: Rewind middle2 + " + positionInCaches);*/
					    				
					    				if (this.textNodeMap == null || this.textNodeMap.isEmpty()){System.out.println("PerformAutoPlayback genericE: textNodeMap reported as empty");}
					    			}
					    			else{System.out.println("genericE: null entry found in token collection");}
						    		positionInCaches--;
						    		/*System.out.println("PerformAutoPlayback genericE: Rewind exiting...");*/
						    		}
					    		else{inREWIND = false; this.currentButton = PAUSE;}
					    		if (positionInCaches < 0){positionInCaches = 0; this.currentButton = PAUSE;}
					    		/*System.out.println("PerformAutoPlayback genericE: Single rewind step done.");*/
				    			break;
			    		case PAUSE:
			    			inREWIND = false;
			    			cancelActiveActions = true;
			    			break;
			    		default:
			    			break;
			    	}
		    	}
		    	else{ //else, everything has been cached, and we make use of said cache for all actions
		    		if (cancelActiveActions){return;}
		    		switch(btnTypeIn){
			    	case PLAY_FWD:
			    	case STEP_FWD:
		    		case FAST_FWD:
		    				if (!logCache.isEmpty() && positionInCaches < logCache.size() - 1)
		    				{
		    					positionInCaches++;
		    					processCaptiveToken(logCache.get(positionInCaches), (positionInCaches == logCache.size()-1), cancelActiveActions);
		    				}
		    				if (positionInCaches >= logCache.size()){positionInCaches = logCache.size() - 1; this.currentButton = PAUSE;}
		    				break;
		    				
		    		case REWIND:
				    		if (!logCache.isEmpty() && positionInCaches >= 0 && inREWIND)
				    		{
				    			if(logCache.get(positionInCaches) != null){ //more error handling; todo: remove in final version
				    				processCaptiveToken(logCache.get(positionInCaches), (positionInCaches == 0), cancelActiveActions);
				    				//this.textNodeMap = mapStateCache.get(positionInCaches);
				    				if (this.textNodeMap == null || this.textNodeMap.isEmpty()){System.out.println("PerformAutoPlayback genericE: textNodeMap reported as empty");}
				    			}
				    			else{System.out.println("PerformAutoPlayback genericE: null entry found in token collection");}
					    		positionInCaches--;
				    		}
				    		else{inREWIND = false; this.currentButton = PAUSE;/* cancelActiveActions = true;*/}
				    		if (positionInCaches < 0){positionInCaches = 0; this.currentButton = PAUSE;}
				    		break;
		    		case PAUSE:
		    			inREWIND = false;
		    			cancelActiveActions = true;
		    			break;
		    		default:
		    			break;
		    		}	
		    	}
    	}
    	catch (IndexOutOfBoundsException e)
    	{
    		if(positionInCaches > 0){positionInCaches--;System.out.println("Spilled over index; position auto-reset enabled; programmer, please check!");cancelActiveActions=true;}
    		if(positionInCaches < 0){positionInCaches--;System.out.println("Seeked under index; position auto-reset to zero; programmer, please check!");cancelActiveActions=true;}
    	}
    	catch(Exception e)
    	{
    		System.out.println("PerformAutoPlayback exception:" + e);
    	}
    }
 
    
    private void processCaptiveToken(String currentTokenIn, boolean isLastToken, boolean cancelIMCurrentAction){
    	if (cancelActiveActions || this.routinesRequestingPriority != 0){return;}
    	//System.out.println("ProcessCaptiveToken genericE: Inner 0");
    	updateMapFromCache();
    	try{
    			//todo: incorporate more logic for segments that are multi-line and do not update properly on rewind
				if (currentTokenIn.matches(".* reinforcing with .* armies.")) {
					String playerName = parsePlayerName(currentTokenIn, " reinforcing ");
					eventTitle.setText(playerName + " reinforcing.");	
					turn.setText(playerName + "'s Turn");
				}
				else if (currentTokenIn.matches("Beginning Round .*!")) {
						round.setText(currentTokenIn.substring(10, currentTokenIn.length() - 1));
						eventTitle.setText("New Round.");
				}
				else if (currentTokenIn.matches(".* is attacking .* from .*!")) {
					if (!inREWIND){ //if going forward, store our info for later parsing
						dlTokenHelper = currentTokenIn;
						}
					else{ //if going in reverse, parse current info + old info
						String playerName = parsePlayerName(currentTokenIn, " is attacking ");
						String atkCountry = parseAtkCountry(currentTokenIn);
						String dfdCountry = parseDfdCountry(currentTokenIn);
						eventTitle.setText(playerName + " attacked\n" + dfdCountry + " from " + atkCountry);
					}
				}
				else if (currentTokenIn.matches("Attacker lost: .*; Defender lost: .*")){
						if (inREWIND){ //if rewinding, store our info for parsing up the chain
							dlTokenHelper = currentTokenIn;
							
							}
						else{ //if forwarding, parse old info + current info
							String playerName = parsePlayerName(dlTokenHelper, " is attacking ");
							String atkCountry = parseAtkCountry(dlTokenHelper);
							String dfdCountry = parseDfdCountry(dlTokenHelper);
							eventTitle.setText(playerName + " attacked\n" + dfdCountry + " from " + atkCountry);
						
						}
					/*}*/
						//System.out.println("ProcessCaptiveToken genericE: Inner 4.6");
				}
				else if (currentTokenIn.matches(".* has taken .* from .*!")) {
						String playerName = parsePlayerName(currentTokenIn, " has taken ");
						eventTitle.setText(playerName + " has taken\n" + parseTakenCountry(currentTokenIn));
				}
				else if (currentTokenIn.matches(".* advanced .* into .*")) {
						String[] line = currentTokenIn.split(" advanced ");
						eventTitle.setText(line[0] + " advanced\n" + line[1]);
				}
				else if (currentTokenIn.matches(".* is transferring .* from .* to .*")) {
						String[] line = currentTokenIn.split(" transferring ");
						eventTitle.setText(line[0] + " transferring\n" + line[1]);
				}
				
				
				nextLogLine.setText("Next event: " + currentTokenIn);
					
				if (isLastToken /*&& this.initialPlay == false*/) {
					if(inREWIND){nextLogLine.setText("[Beginning of game.]\n" + currentTokenIn);}
					else{nextLogLine.setText("Game over!\n" + currentTokenIn);}
				}
			/*}*/ //end while
		} //end try
 		catch (Exception e) {  
 			System.out.println("processCaptiveToken:::" + e.getMessage());
 			//errorDisplay.setText(e.getMessage());
		}
    }
    
    private void readNextLogEvent(String logFile, boolean cancelIMCurrentAction) {
    	if (cancelActiveActions || this.routinesRequestingPriority != 0){return;}
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
					this.initialPlay = false;
					this.currentButton = PAUSE;
					
					logCache.add("[End of game log]");
		    		this.mapStateCache.add(duplicateTextNodeMap(this.textNodeMap));
	    			positionInCaches++;
	    			/*
	    			System.out.println("readNextLogEvent::: nextToken=" + nextToken + " ...null marker inserted");
		    		System.out.println("readNextLogEvent::: positionInCaches END: " + positionInCaches + " ....VS Size: " + logCache.size());*/
				}
				else{
					logCache.add(nextToken);
		    		this.mapStateCache.add(duplicateTextNodeMap(this.textNodeMap));
	    			positionInCaches++;/*
	    			System.out.println("readNextLogEvent::: nextToken=" + nextToken + "\n");
		    		System.out.println("readNextLogEvent::: positionInCaches PCS= " + positionInCaches + " ....VS Size: " + logCache.size());*/
				}
			}
		}
 		catch (Exception e) {  
 			System.out.println("readNextLine::: " + e.getMessage());
 			errorDisplay.setText(e.getMessage());
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
    
    private void updateMapFromCache()
    {
    	if(positionInCaches<0){System.out.println("updateMapFromCaches: No value at this pstn");return;}
    	for(String keyC : this.textNodeMap.keySet())
    	{
    		Text dstTxt = this.textNodeMap.get(keyC);
    		Text srcTxt = this.mapStateCache.get(positionInCaches).get(keyC);
    		dstTxt.setFill(srcTxt.getFill());
    		dstTxt.setText(srcTxt.getText());
    	}
    }
    
    private HashMap<String, Text> duplicateTextNodeMap(HashMap<String, Text> incomingTextNodeMap)
    {
    	HashMap<String, Text> outgoingTextNodeMap = new HashMap<String, Text>();
    	for (String keyC : incomingTextNodeMap.keySet()) {
			double nextX = incomingTextNodeMap.get(keyC).getLayoutX();
			double nextY = incomingTextNodeMap.get(keyC).getLayoutY();
			Text txt = new Text(nextX, nextY, incomingTextNodeMap.get(keyC).getText());
	        txt.setFont(incomingTextNodeMap.get(keyC).getFont());
	        txt.setFill(incomingTextNodeMap.get(keyC).getFill());
	        outgoingTextNodeMap.put(keyC, txt);
		}
    	return outgoingTextNodeMap;
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

