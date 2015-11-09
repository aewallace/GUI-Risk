/**
 * Originally created 28 October 2015
 * by Albert Wallace
 */
package Util;

import Master.FXUIGameMaster;
import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Slider;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.Glow;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import javafx.util.Duration;

/**
 * Audio files courtesy of University of Iowa Electronic Music Studios.
 * URL at last retrieval: http://theremin.music.uiowa.edu/MISpiano.html
 * and http://theremin.music.uiowa.edu/MIS-Pitches-2012/MISFlute2012.html
 * and http://theremin.music.uiowa.edu/MIS-Pitches-2012/MISViolin2012.html
 * and http://theremin.music.uiowa.edu/MISxylophone.html
 * 
 * Class used to play notes as the game progresses.
 */
public class FXUIAudio {
	public static final String shortVersion = "FXUIAudio MP/AC 0.1.8.1300\n06 Nov 2015";
	protected static String canonicalClassName;
	public static final String audioFileOrigSrc = "Audio files courtesy of\nUniversity of Iowa\nElectronic Music Studios";
	protected static final String srcResourceFolderLocation = "src/resources/Audio/";
	protected static List<String> audioFileNames = Arrays.asList(
			"Piano.mf.D2.m4a", "Piano.mf.Db2.m4a", "Piano.mf.D3.m4a", "Piano.mf.Db3.m4a", 
			"Piano.mf.D4.m4a", "Piano.mf.Db4.m4a", "Piano.mf.D5.m4a", "Piano.mf.Db5.m4a",
			"Piano.mf.D6.m4a", "Piano.mf.Db6.m4a", "Piano.mf.D7.m4a", "Piano.mf.Db7.m4a"
			);
	protected static List<String> xtraAudioFileNames = Arrays.asList(
			"Piano.mf.A5.m4a", "Piano.mf.Ab5.m4a", "Piano.mf.B5.m4a", "Piano.mf.Bb5.m4a",
			"Piano.mf.E4.m4a", "Piano.mf.Eb4.m4a"
			);
	protected static final String bootAudioFileName = "xylophone.rosewood.roll.ff.F4B4.m4a";
	protected static int positionInList = 0;
	protected static int listSize = 0;
	private static Map<String, MediaPlayer> mediaPlaybackMap = new HashMap<String, MediaPlayer>();
	protected static Random rand = new Random();
	protected static int loadingMethod = 0;
	protected static boolean playAudio = true;
	protected static boolean audioLoadSuccess = true;
	protected static AtomicBoolean blockNextPlay = new AtomicBoolean(false);
	protected static long delayBetweenNextPlayMS = 1500;
	
	protected static boolean initialized = false;
	/**
	 * Volume, in percent, to use, where 0 is 0%, and 1.0 is 100%.
	 */
	protected static double audioVolumePercent = 0.5d;
	private static MediaPlayer bootAudioMP = null;
	protected static int maxConcurrentClipCount = 1;
	private static LinkedList<MediaPlayer> playList = new LinkedList<MediaPlayer>();
	protected static Node visualIndicator = null;
	protected static boolean hasVisualIndicator = false;
	protected static AtomicBoolean nextOuterAnimStepAllowed = new AtomicBoolean(true);

	public FXUIAudio() {
		canonicalClassName = this.getClass().getCanonicalName();
		this.playBootJingle();
		this.delayedLoadFiles();
		initialized = true;
	}
	
	/**
	* Loads all other audio files associated with this audio manager...
	* Does so after a slight delay.
	* @return returns "true" if thread to run is started without issue,
	* or "false" if the thread couldn't be started.
	*/
	protected boolean delayedLoadFiles(){
		try{
		Thread delayedLoadAudioFiles = new Thread(null, new Runnable() {
                @Override
                public void run() {
					RiskUtils.sleep(5000);
					FXUIGameMaster.diagnosticPrintln("Initializing audio manager " + FXUIAudio.canonicalClassName
						+ " version " + shortVersion +". Please wait.");
                	for(int i = 0; i < audioFileNames.size(); i++){
						/*
						 * Depending on compilation type, either load from base directory 
						 * or load from source location. Base directory: compilation into jar
						 * puts resources into base directory, because: why not.
						 */
						MediaPlayer mediaPlayer = null;
						if( (mediaPlayer = loadAudioFileToMediaPlayer(audioFileNames.get(i)))
								!= null )
						{
							mediaPlayer.setStopTime(new Duration(3700));
							mediaPlayer.setVolume(audioVolumePercent);
							mediaPlaybackMap.put(audioFileNames.get(i), mediaPlayer);
						}
						else{
							audioLoadSuccess = false;
						}
					}
					if(!audioLoadSuccess){
						FXUIGameMaster.diagnosticPrintln("Couldn't access necessary audio files."
								+ " No extra audio will be played.");
					}
					else{
						FXUIAudio.listSize = FXUIAudio.mediaPlaybackMap.size();
						FXUIGameMaster.diagnosticPrintln("Audio manager loaded. Files loaded: " + FXUIAudio.listSize);
					}
                }
        }, "delayedLoadAudioFiles");
		delayedLoadAudioFiles.setDaemon(true);
		delayedLoadAudioFiles.start();
		}
		catch(Exception e){
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	/**
	 * When the music plays, a visual cue may be activated as a...visual cue.
	 * You may set that indicator here (any JavaFX Node that supports
	 * Effects may be used)
	 * @param nds a list of {@code Node}(s) to be used as a pulsing cue 
	 * (null accepted); should more than one be passed in, all will be 
	 * simultaneously modified during each call to {@link toTheBeat}.
	 */
	public static void setVisualIndicators(Node ne, Node[] nds){
		if(ne != null){
			if(Platform.isFxApplicationThread()){
				ne.setOpacity(0.5d);
				ne.setEffect(new Glow(1.0d));
			}
			FXUIAudio.visualIndicator = ne;
			FXUIAudio.hasVisualIndicator = true;
			if (nds != null){
				final int otherIndicCount = nds.length;
				ne.opacityProperty().addListener(new ChangeListener<Number>(){
					@Override
					public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
						for (int i = 0; i < otherIndicCount; i++){
							try{
								nds[i].setOpacity(newValue.doubleValue());
							}
							catch(Exception e){
							}
						}
					}
		        });
			}

		}
		else{
			FXUIAudio.hasVisualIndicator = false;
		}
	}
	
	/**
	 * Flash/pulse visual indicator. Pair with the start of an audio clip
	 * to produce a sort of "to the beat" visual effect.
	 */
	protected static void toTheBeat()
	{
		Thread pulse = new Thread(null, new Runnable() {
            @Override
            public void run() {
            	if(FXUIAudio.hasVisualIndicator == true){
            		toTheBeatHelper();
        		}
            }
	    }, "FXUIA.toTheBeat");
	    pulse.setDaemon(true);
	    pulse.start();
	}
	
	protected static void toTheBeatHelper(){
		int animTime = 650;
		int discreteSteps = 20, startingStep = 10, stoppingStep = 1;
		long sleepTime = animTime/discreteSteps;
		final AtomicBoolean returnSoon = new AtomicBoolean(false);
		final AtomicBoolean nextInnerAnimStepAllowed = new AtomicBoolean(false);
		while(!nextOuterAnimStepAllowed.get()){
			RiskUtils.sleep(50);
		}
		nextOuterAnimStepAllowed.set(false);
		for (int i = startingStep; i < discreteSteps; i++){
			RiskUtils.sleep(sleepTime);
			final int input = i;
			final boolean lastStroke = (i == discreteSteps - 1);
			Platform.runLater(new Runnable() {
                @Override
                public void run() {
                	try{
        				FXUIAudio.visualIndicator.setOpacity((double)input/discreteSteps);
						if(lastStroke){
							nextInnerAnimStepAllowed.set(true);
						}
        			}
        			catch(Exception e){
        				e.printStackTrace();
        				returnSoon.set(true);
						nextInnerAnimStepAllowed.set(true);
        			}
                }
            });
			if(returnSoon.get()){ return; }
		}
		while(!nextInnerAnimStepAllowed.get()){
			RiskUtils.sleep(50);
		}
		for (int i = discreteSteps; i > stoppingStep; i--){
			nextInnerAnimStepAllowed.set(false);
			RiskUtils.sleep(sleepTime);
			final int input = i;
			final boolean lastStroke = (i == stoppingStep + 1);
			Platform.runLater(new Runnable() {
                @Override
                public void run() {
                	try{
        				FXUIAudio.visualIndicator.setOpacity((double)input/discreteSteps);
						if(lastStroke){
							nextInnerAnimStepAllowed.set(true);
						}
        			}
        			catch(Exception e){
        				e.printStackTrace();
        				returnSoon.set(true);
						nextInnerAnimStepAllowed.set(true);
        			}
                }
            });
			if(returnSoon.get()){ return; }
		}
		while(!nextInnerAnimStepAllowed.get()){
			RiskUtils.sleep(50);
		}
		nextOuterAnimStepAllowed.set(true);
	}
	
	/**
	 * Plays the next musical note from the list of notes to play.
	 * Will block playback if the previous note was played too recently
	 * (delay defined in {@link #delayBetweenNextPlayMS})
	 * @return "true" if next note will be played, "false" if delayed or cannot
	 * play
	 */
	public static boolean playNextNote(){
		try{
			if(FXUIAudio.bootAudioMP != null){
				FXUIAudio.bootAudioMP.stop();
			}
			if(audioLoadSuccess == false){
				return false;
			}
			if(FXUIAudio.blockNextPlay.get() || !FXUIAudio.playAudio){
				return false;
			}
			if(!FXUIAudio.initialized){
				return false;
			}
			positionInList++;
			if(positionInList >= FXUIAudio.listSize){
				positionInList = 0;
			}
			
			final int indexToPlay = positionInList;
			FXUIAudio.blockNextPlay.set(true);
			if (!Platform.isFxApplicationThread()) {
				Platform.runLater(new Runnable() {
	                @Override
	                public void run() {
	                	playFileAtIndex(indexToPlay);
	                }
	            });
	        }
			else{
				playFileAtIndex(indexToPlay);
			}
			toTheBeat();
			RiskUtils.runLaterWithDelay(FXUIAudio.delayBetweenNextPlayMS,
			new Runnable() {
	            @Override
	            public void run() {
	            	FXUIAudio.blockNextPlay.set(false);
	            }
	        });
		}
		catch(Exception e){
			return false;
		}
		return true;
	}
	
	/**
	 * Play an ending jingle of pseudorandom notes from the list of possible
	 * notes. ... This may also be used as the starting jingle in the future.
	 */
	public static void playEndJingle(){
		if(!FXUIAudio.initialized){
			return;
		}
		try{
			Thread jingle = new Thread(null, new Runnable() {
	            @Override
	            public void run() {
	            	for(int i = 0; i < 4; i++){
	            		try{
		            		//int positionToUse = rand.nextInt((listSize - 1) + 1);
		            		playFileAtIndex(i);
		            		RiskUtils.sleep(1000);
	            		} catch(Exception e) {
	            			e.printStackTrace();
	            		}
	            	}
	            }
		    }, "playEndJingle");
		    jingle.setDaemon(true);
		    jingle.start();
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	protected static boolean playFileAtIndex(int indexToPlay){
		try{
			FXUIAudio.playList.add(mediaPlaybackMap.get(audioFileNames.get(indexToPlay)));
			limitFilePlaybackCount();
			//FXUIAudio.mediaPlaybackMap.get(audioFileNames.get(indexToPlay)).stop();
			FXUIAudio.mediaPlaybackMap.get(audioFileNames.get(indexToPlay)).play();
		}
		catch(Exception e){
			return false;
		}
		return true;
	}
	
	/**
	 * Call to ensure that there are a max of {@link #maxConcurrentClipCount}
	 * files playing at any given time. (Call each time a new file in the array
	 * is played, when playing in sequential order).
	 * @param currentAudioIdx
	 */
	protected static void limitFilePlaybackCount(){
		if(FXUIAudio.playList.size() > maxConcurrentClipCount){
			FXUIAudio.playList.removeFirst().stop();
		}
	}
	
	public boolean playBootJingle(){
		if (!Platform.isFxApplicationThread()) {
			FXUIGameMaster.diagnosticPrintln("I'm not going to play the joyful sound you"
					+ " wanted STRICTLY BECAUSE you didn't code me properly."
					+ " If you really wanted me to play, you would be en-"
					+ "suring that I run on the JavaFX thread. Instead, you"
					+ "hit \"select all\" on the contents of this code block"
					+ " (which has begun to take up over 5 lines since you"
					+ " started writing this comment) and hit backspace. Happy?");
        }
		else{
			bootJingleHelper();
		}
		return true && audioLoadSuccess;
	}
	
	/**
	 * Play the boot sound, as a way to test that this instance of the audio
	 * manager can load & play the associated audio files correctly.
	 * @return true if can load and play audio, false if cannot load audio file
	 */
	protected boolean bootJingleHelper(){
		MediaPlayer mediaPlayer = null;
		if( (mediaPlayer = loadAudioFileToMediaPlayer(FXUIAudio.bootAudioFileName))
				!= null )
		{
			mediaPlayer.setVolume(audioVolumePercent);
			mediaPlayer.setStartTime(new Duration(3500));
			mediaPlayer.setStopTime(new Duration(11500));
			mediaPlayer.play();
			bootAudioMP = mediaPlayer;
			return true;
		}
		return false;
	}
	
	/**
	 * Take a file with given file name (not file path), attempt to open it,
	 * and (if can be open) create an associated MediaPlayer object for
	 * play back. From there, use the returned {@link MediaPlayer} object with 
	 * the stock play(), stop(), etc. calls to control play back.
	 * @param fileName the name of the file to be opened
	 * @return "null" if the file could not be opened, or a {@link MediaPlayer} 
	 * object associated with the audio file if it could be opened.
	 */
	protected MediaPlayer loadAudioFileToMediaPlayer(String fileName){
		Media mDia = null;
		MediaPlayer mediaPlayer = null;
		if(loadingMethod == 0){
			try{ 
				URL mFia = this.getClass().getResource("/".concat(fileName));
				mDia = new Media(mFia.toString());
				mediaPlayer = new MediaPlayer(mDia);
			}
			catch (Exception e){
				//e.printStackTrace();
				loadingMethod += 1;
				FXUIGameMaster.diagnosticPrintln("********************************************"
						+ "\nTest: Switching to welcome audio file load type " + (loadingMethod + 1)
						+ "\n********************************************");
			}
		}
		if(loadingMethod == 1){
			try{
				File mBia = new File(srcResourceFolderLocation.concat(fileName));
				mDia = new Media(mBia.toURI().toString());
				mediaPlayer = new MediaPlayer(mDia);
			}
			catch (Exception e){
				//e.printStackTrace();
				loadingMethod += 1;
				FXUIGameMaster.diagnosticPrintln("********************************************"
						+ "\nSwitching to welcome audio file load type " + (loadingMethod + 1) 
						+ "\n********************************************");
			}
		}
		else if (loadingMethod > 1){
			FXUIGameMaster.diagnosticPrintln("There is no other known loading method at"
					+ " this time for the welcome audio. Please invent loading "
					+ "method #" + (loadingMethod + 1) + " to load the welcome"
							+ " audio file.");
		}
		return mediaPlayer;
	}
	
	/**
	 * Sets the volume of the various audio MediaPlayback objects. Sets values
	 * for all in {@link #mediaPlaybackMap} Map, not just currently playing.
	 * @param volume
	 * @param mute
	 * @return "true" on success, "false" on failure.
	 */
	protected boolean setAudioVolumeForPlayback(double volume, boolean mute){
		boolean success = true;
		if(mediaPlaybackMap == null || mediaPlaybackMap.size() < 1)
		{
			success = false;
			return success;
		}
		if(volume <= 0){
			success = true;
			mute = true;
			FXUIAudio.audioVolumePercent = 0.0d;
		}
		else if(volume > 1.0d){
			success = false;
			FXUIAudio.audioVolumePercent = 1.0d;
		}
		else{
			success = true;
			audioVolumePercent = volume;
		}
		for(Entry<String, MediaPlayer> audioFile : mediaPlaybackMap.entrySet()){
			if(mute){
				audioFile.getValue().setMute(true);
			}
			else{
				audioFile.getValue().setVolume(volume);
				audioFile.getValue().setMute(false);
			}
		}
		return success;
	}
	
	/**
     * Create a non-JavaFX thread (if necessary) to build & display size options
     * for the associated window (Stage) Tries to run the dialog's code on a
     * non-JFX thread as much as possible.
     */
    public int showAudioOptions(Window owner) {
        //represents the dialog; true: the dialog is visible (& code is waiting), false: window isn't showing.
        AtomicBoolean dialogIsShowing = new AtomicBoolean(true);

        if (Platform.isFxApplicationThread()) { //if this is the FX thread, make it all happen, and use showAndWait
            audioOptionsHelper(dialogIsShowing, owner);
        } else { //if this isn't the FX thread, we can pause logic with a call to RiskUtils.sleep()
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    audioOptionsHelper(dialogIsShowing, owner);
                }
            });

            do {
                RiskUtils.sleep(100);
            } while (dialogIsShowing.get());
        }
        return 0; // TODO decide on better return type
    }

    /**
     * Build & show the dialog.
     *
     * @param dialogIsShowing used to control the flow of code; will be set to
     * "false" when dialog is closed.
     * @param ownder the Window object of the Scene assigned to the main Stage
     * (used to assist in window positioning). If null, position will be set to 
     * a generic location on the screen.
     */
    protected void audioOptionsHelper(AtomicBoolean dialogIsShowing, Window owner) {
        try {
            final Stage dialog = new Stage();
            dialog.setTitle("Audio Options");
            dialog.initOwner(owner);
            dialog.setX(owner.getX());
            dialog.setY(owner.getY() + 50);
            
            final VBox layout = new VBox(10);
            layout.setAlignment(Pos.CENTER);
            layout.setStyle("-fx-background-color: cornflowerblue; -fx-padding: 4");

            final Text querySymbol = new Text("db+/db-");
            querySymbol.setTextAlignment(TextAlignment.CENTER);
            querySymbol.setFont(Font.font("Arial", FontWeight.BOLD, 24));
            querySymbol.setFill(Color.WHITE);
            
            final Text queryText = new Text("     Alter audio settings?     ");
            queryText.setTextAlignment(TextAlignment.CENTER);
            queryText.setFont(Font.font("Arial", FontWeight.BOLD, 16));
            queryText.setFill(Color.WHITE);

            double widthOfLines = 250d;
            double strokeThicknessOfLines = 3.0d;
            Color colorOfLines = Color.WHEAT;
            Line bufferLine = new Line(0,0,widthOfLines,0);
            bufferLine.setStrokeWidth(strokeThicknessOfLines);
            bufferLine.setStroke(colorOfLines);

            Text spaceBuffer = new Text("\n");
            spaceBuffer.setTextAlignment(TextAlignment.CENTER);
            spaceBuffer.setFont(Font.font("Arial", FontWeight.LIGHT, 16));

            final Button yeah = new Button("Apply Changes");
            final Button nah = new Button("Close Window");

            final Slider audioVolSlider = new Slider(0.0f, 1.0f, FXUIAudio.audioVolumePercent);
            final CheckBox doPlayAudio = new CheckBox("Play audio?");
            final Text audioSliderLabel = new Text("Audio Volume [" + String.format("%.2f", FXUIAudio.audioVolumePercent*100) + "%]");
            
            audioVolSlider.setSnapToTicks(false);
            audioVolSlider.setShowTickMarks(true);
            audioVolSlider.setMajorTickUnit(0.25f);
            audioVolSlider.setMinorTickCount(0);
            //audioVolSlider.set
            audioVolSlider.setTooltip(new Tooltip("Audio volume, in percentage"
            		+ ": 0% to 100%.\n0% will mute audio."));
            audioVolSlider.valueProperty().addListener(new ChangeListener<Number>() {
                @Override
                public void changed(ObservableValue<? extends Number> ov,
                        Number old_val, Number new_val) 
                {
                	yeah.setDisable(false);
					audioSliderLabel.setText("Audio Volume [" + String.format("%.2f", new_val.doubleValue()*100) + "%]");
                }
            });
            
            doPlayAudio.setSelected(FXUIAudio.playAudio);
            doPlayAudio.setTooltip(new Tooltip("Enable (checked) or disable "
            		+ "(unchecked) playback of audio."));
            doPlayAudio.setTextFill(Color.ANTIQUEWHITE);
            doPlayAudio.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent t) {
                    yeah.setDisable(false);
                }
            });
            
            audioSliderLabel.setFont(Font.font("Verdana", FontWeight.LIGHT, 14));
            audioSliderLabel.setFill(Color.WHITE);
            
            yeah.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent t) {
                	yeah.setDisable(true);
                	if (doPlayAudio.isSelected()) {
                    	FXUIAudio.playAudio = true;
                    } else if (!doPlayAudio.isSelected()) {
                        FXUIAudio.playAudio = false;
                    }
                	doPlayAudio.setDisable(true);
                	audioVolSlider.setDisable(true);
                	setAudioVolumeForPlayback(audioVolSlider.getValue(), !doPlayAudio.isSelected());
                	doPlayAudio.setDisable(false);
                	audioVolSlider.setDisable(false);
                }
            });

            nah.setDefaultButton(true);
            nah.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent t) {
                    dialogIsShowing.set(false);
                    dialog.close();
                }
            });

            layout.getChildren().setAll(
                    querySymbol, queryText, bufferLine, 
                    doPlayAudio, audioSliderLabel, audioVolSlider, nah, yeah, 
                    spaceBuffer
            );

            dialog.setOnCloseRequest(new EventHandler<WindowEvent>() {
                @Override
                public void handle(WindowEvent t) {
                    dialogIsShowing.set(false);
                }
            });

            dialog.setScene(new Scene(layout));
            dialog.show();
        } catch (Exception e) {
            FXUIGameMaster.diagnosticPrintln("ERROR: audio control display failed:: " + e);
        }
    }

}
