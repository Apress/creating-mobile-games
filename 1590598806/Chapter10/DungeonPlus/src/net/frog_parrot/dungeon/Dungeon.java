package net.frog_parrot.dungeon;

import javax.microedition.midlet.*;
import javax.microedition.lcdui.*;

import net.frog_parrot.util.*;

/**
 * This is the main class of the dungeon game.
 *
 * @author Carol Hamer
 */
public class Dungeon extends MIDlet implements CommandListener {

  //-----------------------------------------------------
  //    game object fields

  /**
   * The canvas that the dungeon is drawn on.
   */
  DungeonCanvas myCanvas;

  /**
   * the thread that advances the game clock.
   */
  GameThread myGameThread;

  //-----------------------------------------------------
  //    initialization

  /**
   * Initialize the canvas.
   */
  public Dungeon() {
    try { 
      myCanvas = new DungeonCanvas(this);
    } catch(Exception e) {
      // if there's an error during creation, 
      // display it as an alert.
      errorMsg(e);
    }
  }

  //----------------------------------------------------------------
  //  implementation of MIDlet

  /**
   * Start the application.
   */
  public void startApp() throws MIDletStateChangeException {
    if(myCanvas != null) {
      if(myGameThread == null) {
        // put the simple logo splashscreen up first:
        Display d = Display.getDisplay(this);
        SplashScreen ss 
            = new SplashScreen(myCanvas.getCustomizer(), 
              d.numAlphaLevels());
        d.setCurrent(ss);
        // create the thread and start the game:
        myGameThread = new GameThread(ss, myCanvas, this);
        myGameThread.start();
        // it's not technically necessary to clear the 
        // splashscreen, but may help in garbage collection:
        ss = null;
      } else {
        // in case this gets called again after 
        // the application has been started once:
        myCanvas.flushKeys();
        myGameThread.resumeGame();
      }
    }
  }
  
  /**
   * Stop the threads and throw out the garbage.
   */
  public void destroyApp(boolean unconditional) 
      throws MIDletStateChangeException {
    myCanvas = null;
    if(myGameThread != null) {
      myGameThread.requestStop();
    }
    myGameThread = null;
    System.gc();
  }

  /**
   * Pause the game.
   */
  public void pauseApp() {
    if(myGameThread != null) {
      myGameThread.pause();
    }
  }

  /*
   * End the game.
   */
  public void quit() {
    try {
      destroyApp(false);
      notifyDestroyed();
    } catch(Exception e) {
    }
  }

  /*
   * restart after a pause.
   */
  public void resumeGame() {
    myGameThread.resumeGame();
  }
  
  //----------------------------------------------------------------
  //  implementation of CommandListener

  /*
   * Respond to a command issued on an error alert.
   */
  public void commandAction(Command c, Displayable s) {
    if(c == Alert.DISMISS_COMMAND) {
      // if there was a serious enough error to 
      // cause an alert, then we end the game 
      // when the user is done reading the alert:
      // (Alert.DISMISS_COMMAND is the default 
      // command that is placed on an Alert 
      // whose timeout is FOREVER)
      quit();
    }
  }

  //-------------------------------------------------------
  //  error methods

  /**
   * Converts an exception to a message and displays 
   * the message..
   */
  void errorMsg(Exception e) {
    e.printStackTrace();
    if(e.getMessage() == null) {
      errorMsg(e.getClass().getName());
    } else {
      errorMsg(e.getClass().getName() + ":" + e.getMessage());
    }
  }

  /**
   * Displays an error message alert if something goes wrong.
   */
  void errorMsg(String msg) {
    Alert errorAlert = new Alert("error", 
                                 msg, null, AlertType.ERROR);
    errorAlert.setCommandListener(this);
    errorAlert.setTimeout(Alert.FOREVER);
    Display.getDisplay(this).setCurrent(errorAlert);
  }

}
