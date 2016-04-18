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
import javafx.scene.media.AudioClip;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

/**
 * Audio files courtesy of University of Iowa Electronic Music Studios.
 * URL at last retrieval: http://theremin.music.uiowa.edu/MISpiano.html
 * and http://theremin.music.uiowa.edu/MIS-Pitches-2012/MISFlute2012.html
 * and http://theremin.music.uiowa.edu/MIS-Pitches-2012/MISViolin2012.html
 * and http://theremin.music.uiowa.edu/MISxylophone.html
 * 
 * Class used to play notes as the game progresses.
 */
public class FXUIAudioAC {
	public static final String shortVersion = "FXUIAudio AC / 0.2.A.1128\n11 April 2016";
	protected static String canonicalClassName;
	public static final String audioFileOrigSrc = "Audio files courtesy of\nUniversity of Iowa\nElectronic Music Studios";
	protected static final String srcResourceFolderLocation = "src/resources/Audio/";
        protected static final String jarResourceFolderLocation = "/resources/Audio/";
        protected static final String jarRootLocation ="/";
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
	protected static int positionInClipList = 0;
	protected static int availableClipCount = 0;
	protected static Random rand = new Random();
	protected static int loadingMethod = 0;
	protected static boolean playAudio = true;
	protected static boolean audioLoadSuccess = true;
	protected static AtomicBoolean blockNextPlay = new AtomicBoolean(false);
	protected static boolean initialized = false;
	/**
	 * Volume, in percent, to use, where 0 is 0%, and 1.0 is 100%.
	 */
	protected static double audioVolumePercent = 0.5d;
	protected static Node visualIndicator = null;
	protected static boolean hasVisualIndicator = false;
	protected static AtomicBoolean nextOuterAnimStepAllowed = new AtomicBoolean(true);
	private static Map<String, AudioClip> mediaPlaybackMap = new HashMap<String, AudioClip>();
	private static AudioClip bootAudio = null;
	protected static int maxConcurrentClipCount = 3;
	private static LinkedList<AudioClip> playList = new LinkedList<AudioClip>();
	protected static long delayBetweenNextPlayMS = 900;

	public FXUIAudioAC() {
                System.out.println("AudioAC audio manager enabled.");
		canonicalClassName = this.getClass().getCanonicalName();
		this.playBootJingle();
		this.delayedLoadFiles();
		initialized = true;
	}
	
	
	
	/**
	 * When the music plays, a visual cue may be activated as a...visual cue.
	 * You may set that indicator here (any JavaFX Node that supports
	 * Effects may be used)
	 * @param priNode the primary Node guaranteed to pulse. Others will piggyback
	 * off of this node.
	 * @param assocNodes a list of {@code Node}(s) to be used as a pulsing cue 
	 * (null accepted, empty set accepted, one or more accepted)
	 * simultaneously modified during each call to {@link toTheBeat}.
	 */
	public static void setVisualIndicators(Node priNode, Node[] assocNodes){
		if(priNode != null){
			if(Platform.isFxApplicationThread()){
				priNode.setOpacity(0.5d);
				priNode.setEffect(new Glow(1.0d));
			}
			FXUIAudioAC.visualIndicator = priNode;
			for (int i = 0; assocNodes != null && i < assocNodes.length; i++){
				final Node nodeIn = assocNodes[i];
				priNode.opacityProperty().addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
	                try{
	                   nodeIn.setOpacity(newValue.doubleValue());
	                }
	                catch(Exception e){
	                }
	            });
			}
			FXUIAudioAC.hasVisualIndicator = true;
		}
		else{
			FXUIAudioAC.hasVisualIndicator = false;
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
            	if(FXUIAudioAC.hasVisualIndicator == true){
            		toTheBeatHelper();
        		}
            }
	    }, "FXUIA.toTheBeat");
	    pulse.setDaemon(true);
	    pulse.start();
	}
	
	protected static void toTheBeatHelper(){
		int animTime = 650;
		int discreteSteps = 20, startingStep = 10, stoppingStep = 0;
		long sleepTime = animTime/discreteSteps;
		final AtomicBoolean returnSoon = new AtomicBoolean(false);
		final AtomicBoolean nextInnerAnimStepAllowed = new AtomicBoolean(false);
		while(!nextOuterAnimStepAllowed.get()){
			RiskUtils.sleep(25);
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
        				FXUIAudioAC.visualIndicator.setOpacity((double)input/discreteSteps);
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
        				FXUIAudioAC.visualIndicator.setOpacity((double)input/discreteSteps);
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
	 * Plays the next musical note from the list of notes to play. Will block
	 * playback if the previous note was played too recently (delay defined in
	 * {@link #delayBetweenNextPlayMS})
	 *
	 * @return "true" if next note will be played, "false" if delayed or cannot
	 * play
	 */
	public static boolean playNextNote() {
	    FXUIGameMaster.diagnosticPrintln("Next Note being played");
	    try {
	        if (FXUIAudioAC.bootAudio != null) {
	            FXUIAudioAC.bootAudio.stop();
	        }
	        if (audioLoadSuccess == false) {
	            FXUIGameMaster.diagnosticPrintln("Next Note: audio load success FALSE");
	            return false;
	        }
	        if (FXUIAudioAC.blockNextPlay.get() || !FXUIAudioAC.playAudio) {
	            FXUIGameMaster.diagnosticPrintln("Next Note: playback blocked[time delay]");
	            return false;
	        }
	        if (!FXUIAudioAC.initialized) {
	            FXUIGameMaster.diagnosticPrintln("Next Note: not INITIALIZED!");
	            return false;
	        }
	        positionInClipList--;
	        if (positionInClipList < 0) {
	            positionInClipList = FXUIAudioAC.availableClipCount - 1;
	        }
	
	        final int indexToPlay = positionInClipList;
	        FXUIAudioAC.blockNextPlay.set(true);
	        FXUIGameMaster.diagnosticPrintln(audioFileNames.get(positionInClipList));
	        if (!Platform.isFxApplicationThread()) {
	            Platform.runLater(new Runnable() {
	                @Override
	                public void run() {
	                    playFileAtIndex(indexToPlay);
	                }
	            });
	        } else {
	            playFileAtIndex(indexToPlay);
	        }
	        toTheBeat();
	        RiskUtils.runLaterWithDelay(FXUIAudioAC.delayBetweenNextPlayMS,
	                new Runnable() {
	            @Override
	            public void run() {
	                FXUIAudioAC.blockNextPlay.set(false);
	            }
	        });
	    } catch (Exception e) {
	        return false;
	    }
	    return true;
	}

	/**
	 * Play an ending jingle of 2 specific notes from the list of possible
	 * notes. (Currently last two notes) ... This may also be used as the
	 * starting jingle in the future.
	 */
	public static void playEndJingle() {
	    if (!FXUIAudioAC.initialized) {
	        return;
	    }
	    try {
	        Thread jingle = new Thread(null, new Runnable() {
	            @Override
	            public void run() {
	                final int startingPosition = audioFileNames.size() - 2;
	                for (int i = 0; i < 7; i++) {
	                    try {
	                        playFileAtIndex(startingPosition + (i % 2));
	                        RiskUtils.sleep(550 * (1 + (i / 3)));
	                    } catch (Exception e) {
	                        e.printStackTrace();
	                    }
	                }
	            }
	        }, "playEndJingle");
	        jingle.setDaemon(true);
	        jingle.start();
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}

	protected static boolean playFileAtIndex(int indexToPlay) {
	    try {
	        FXUIAudioAC.playList.add(mediaPlaybackMap.get(FXUIAudioAC.audioFileNames.get(indexToPlay)));
	        limitFilePlaybackCount();
	        FXUIAudioAC.mediaPlaybackMap.get(FXUIAudioAC.audioFileNames.get(indexToPlay)).play();
	    } catch (Exception e) {
	        return false;
	    }
	    return true;
	}

	/**
	 * Call to ensure that there are a max of {@link #maxConcurrentClipCount}
	 * files playing at any given time. (Call each time a new file in the array
	 * is played, when playing in sequential order).
	 *
	 * @param currentAudioIdx
	 */
	protected static void limitFilePlaybackCount() {
	    if (FXUIAudioAC.playList.size() > maxConcurrentClipCount) {
	        FXUIAudioAC.playList.removeFirst().stop();
	    }
	}

	/**
	 * Loads all other audio files associated with this audio manager... Does so
	 * after a slight delay.
	 *
	 * @return returns "true" if thread to run is started without issue, or
	 * "false" if the thread couldn't be started.
	 */
	protected boolean delayedLoadFiles() {
	    try {
	        Thread delayedLoadAudioFiles = new Thread(null, new Runnable() {
	            @Override
	            public void run() {
	                RiskUtils.sleep(5000);
	                FXUIGameMaster.diagnosticPrintln("Initializing audio manager " + FXUIAudioAC.canonicalClassName
	                        + " version " + FXUIAudioAC.shortVersion + ". Please wait.");
	                FXUIGameMaster.diagnosticPrintln("Note that the full audio file"
	                        + " is going to be played, unless stopped"
	                        + " by internal logic or user interaction.");
	                for (int i = 0; i < FXUIAudioAC.audioFileNames.size(); i++) {
	                    /*
						 * Depending on compilation type, either load from base directory 
						 * or load from source location. Base directory: compilation into jar
						 * puts resources into base directory, because: why not.
	                     */
	                    AudioClip mediaPlayer = null;
	                    if ((mediaPlayer = loadAudioFileToAudioClip(FXUIAudioAC.audioFileNames.get(i)))
	                            != null) {
	                        //mediaPlayer.setStopTime(new Duration(3700));
	                        mediaPlayer.setVolume(audioVolumePercent);
	                        mediaPlaybackMap.put(FXUIAudioAC.audioFileNames.get(i), mediaPlayer);
	                    } else {
	                        audioLoadSuccess = false;
	                    }
	                }
	                if (!audioLoadSuccess) {
	                    FXUIGameMaster.diagnosticPrintln("Couldn't access necessary audio files."
	                            + " No extra audio will be played.");
	                } else {
	                    FXUIAudioAC.availableClipCount = FXUIAudioAC.mediaPlaybackMap.size();
	                    FXUIGameMaster.diagnosticPrintln("Audio manager loaded. Files loaded: " + FXUIAudioAC.availableClipCount);
	                }
	            }
	        }, "delayedLoadAudioFiles");
	        delayedLoadAudioFiles.setDaemon(true);
	        delayedLoadAudioFiles.start();
	    } catch (Exception e) {
	        e.printStackTrace();
	        return false;
	    }
	    return true;
	}
	
	

	/**
	 * Play the boot sound, as a way to test that this instance of the audio
	 * manager can load & play the associated audio files correctly.
	 *
	 * @return true if can load and play audio, false if cannot load audio file
	 */
	public boolean playBootJingle() {
	    AudioClip mediaPlayer = null;
	    if ((mediaPlayer = loadAudioFileToAudioClip(FXUIAudioAC.bootAudioFileName))
	            != null) {
	        mediaPlayer.setVolume(audioVolumePercent);
	        FXUIGameMaster.diagnosticPrintln("Note that the full boot audio file"
	                + " is going to be played, unless stopped"
	                + " by internal logic or user interaction.");
	        mediaPlayer.play();
	        bootAudio = mediaPlayer;
	        RiskUtils.runLaterWithDelay(7300, new Runnable() {
	
	            @Override
	            public void run() {
	                bootAudio.stop();
	                bootAudio = null;
	            }
	
	        });
	        return true;
	    }
	    return false;
	}

	/**
	 * Take a file with given file name (not file path), attempt to open it, and
	 * (if can be open) create an associated AudioClip object for play back.
	 * From there, use the returned {@link AudioClip} object with the stock
	 * play(), stop(), etc. calls to control play back.
	 *
	 * @param fileName the name of the file to be opened
	 * @return "null" if the file could not be opened, or a {@link AudioClip}
	 * object associated with the audio file if it could be opened.
	 */
	protected AudioClip loadAudioFileToAudioClip(String fileName) {
	    AudioClip mediaPlayer = null;
	    if (loadingMethod == 0) { //loading from within the jar, type 1 (BASH BUILD SCRIPT)
	        try {
	            URL mFia = this.getClass().getResource(jarRootLocation.concat(fileName));
	            FXUIGameMaster.diagnosticPrintln("mFIA to string: " + mFia.toString());
	            mediaPlayer = new AudioClip(mFia.toString());
	        } catch (Exception e) {
	            //e.printStackTrace();
	            loadingMethod += 1;
	            FXUIGameMaster.diagnosticPrintln("********************************************"
	                    + "\nTest: Switching to welcome audio file load type " + (loadingMethod + 1)
	                    + "\n********************************************");
	        }
	    }
            if (loadingMethod == 1) { //loading from the OS file system, relative location (ECLIPSE)
	        try {
	            File mBia = new File(srcResourceFolderLocation.concat(fileName));
	            FXUIGameMaster.diagnosticPrintln("mBia to string: " + mBia.toString());
	            mediaPlayer = new AudioClip(mBia.toURI().toString());
	        } catch (Exception e) {
	            //e.printStackTrace();
	            loadingMethod += 1;
	            FXUIGameMaster.diagnosticPrintln("********************************************"
	                    + "\nSwitching to welcome audio file load type " + (loadingMethod + 1)
	                    + "\n********************************************");
	        }
	    }
            if(loadingMethod == 2) { //loading from within the jar, type 2 (NETBEANS)
	        try {
	            URL mFia = this.getClass().getResource(jarResourceFolderLocation.concat(fileName));
	            FXUIGameMaster.diagnosticPrintln("mFIA to string: " + mFia.toString());
	            mediaPlayer = new AudioClip(mFia.toString());
	        } catch (Exception e) {
	            //e.printStackTrace();
	            loadingMethod += 1;
	            FXUIGameMaster.diagnosticPrintln("********************************************"
	                    + "\nTest: Switching to welcome audio file load type " + (loadingMethod + 1)
	                    + "\n********************************************");
	        }
	    } 
            if(loadingMethod>2){
	        FXUIGameMaster.diagnosticPrintln("lAFTAC: There is no other known loading method at"
	                + " this time for the welcome audio. Please invent loading "
	                + "method #" + (loadingMethod + 1) + " to load the welcome"
	                + " audio file.");
	    }
	    return mediaPlayer;
	}

	/**
	 * Sets the volume of the various audio objects. Sets values for all in
	 * {@link #mediaPlaybackMap} Map, not just currently playing.
	 *
	 * @param volume
	 * @param mute
	 * @return "true" on success, "false" on failure.
	 */
	protected boolean setAudioVolumeForPlayback(double volume, boolean mute) {
	    boolean success = true;
	    if (mediaPlaybackMap == null || mediaPlaybackMap.size() < 1) {
	        success = false;
	        return success;
	    }
	    if (volume <= 0) {
	        success = true;
	        mute = true;
	        FXUIAudioAC.audioVolumePercent = 0.0d;
	    } else if (volume > 1.0d) {
	        success = false;
	        FXUIAudioAC.audioVolumePercent = 1.0d;
	    } else {
	        success = true;
	        audioVolumePercent = volume;
	    }
	    for (Entry<String, AudioClip> audioFile : mediaPlaybackMap.entrySet()) {
	        if (mute) {
	            audioFile.getValue().setVolume(0);
	        } else {
	            audioFile.getValue().setVolume(volume);
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
	        Line bufferLine = new Line(0, 0, widthOfLines, 0);
	        bufferLine.setStrokeWidth(strokeThicknessOfLines);
	        bufferLine.setStroke(colorOfLines);
	
	        Text spaceBuffer = new Text("\n");
	        spaceBuffer.setTextAlignment(TextAlignment.CENTER);
	        spaceBuffer.setFont(Font.font("Arial", FontWeight.LIGHT, 16));
	
	        final Button yeah = new Button("Apply Changes");
	        final Button nah = new Button("Close Window");
	
	        final Slider audioVolSlider = new Slider(0.0f, 1.0f, FXUIAudioAC.audioVolumePercent);
	        final CheckBox doPlayAudio = new CheckBox("Play audio?");
	        final Text audioSliderLabel = new Text("Audio Volume [" + String.format("%.2f", FXUIAudioAC.audioVolumePercent * 100) + "%]");
	
	        audioVolSlider.setSnapToTicks(false);
	        audioVolSlider.setShowTickMarks(true);
	        audioVolSlider.setMajorTickUnit(0.25f);
	        audioVolSlider.setMinorTickCount(0);
	        audioVolSlider.setTooltip(new Tooltip("Audio volume, in percentage"
	                + ": 0% to 100%.\n0% will mute audio."));
	        audioVolSlider.valueProperty().addListener(new ChangeListener<Number>() {
	            @Override
	            public void changed(ObservableValue<? extends Number> ov,
	                    Number old_val, Number new_val) {
	                yeah.setDisable(false);
	                audioSliderLabel.setText("Audio Volume [" + String.format("%.2f", new_val.doubleValue() * 100) + "%]");
	            }
	        });
	
	        doPlayAudio.setSelected(FXUIAudioAC.playAudio);
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
	                    FXUIAudioAC.playAudio = true;
	                } else if (!doPlayAudio.isSelected()) {
	                    FXUIAudioAC.playAudio = false;
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
