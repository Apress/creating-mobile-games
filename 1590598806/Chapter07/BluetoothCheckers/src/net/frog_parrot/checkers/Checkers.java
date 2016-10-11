package net.frog_parrot.checkers;

import javax.microedition.midlet.*;
import javax.microedition.lcdui.*;
import java.util.Vector;

import net.frog_parrot.net.*;
/**
 * This is the main class of the checkers game.
 *
 * @author Carol Hamer
 */
public class Checkers extends MIDlet implements CommandListener {

  //-----------------------------------------------------
  //    game object fields

  /**
   * The canvas that the checkerboard is drawn on.
   */
  private CheckersCanvas myCanvas;

  /**
   * The class that handles turn taking and communication.
   */
  private MoveManager myMoveManager;

  /**
   * The List that allows the user to choose 
   * between server mode and client mode.
   */
  private List myModeList;

  //-----------------------------------------------------
  //    command fields

  /**
   * The button to exit the game.
   */
  private Command myExitCommand = new Command("Exit", Command.EXIT, 99);

  /**
   * The button to send the initial invitation.
   */
  private Command myOkCommand = new Command("Ok", Command.OK, 0);

  //-----------------------------------------------------
  //    initialization and data

  /**
   * Initialize the canvas and the commands.
   */
  public Checkers() {
    try { 
      // create the canvas and set up the commands:
      myCanvas = new CheckersCanvas(Display.getDisplay(this));
      myCanvas.addCommand(myExitCommand);
      myCanvas.setCommandListener(this);
      CheckersGame game = myCanvas.getGame();
      myMoveManager = new MoveManager(this, myCanvas, game);
      game.setMoveManager(myMoveManager);
      String[] modes = { "server mode", "client mode"
      };
      myModeList = new List("Choose Mode", Choice.IMPLICIT, 
          modes, null);
      myModeList.addCommand(myExitCommand);
      myModeList.setCommandListener(this);
    } catch(Exception e) {
      // if there's an error during creation, 
      // display it as an alert.
      errorMsg(e);
    }
  }

  //----------------------------------------------------------------
  //  implementation of MIDlet
  // these methods may be called by the application management 
  // software at any time, so we always check fields for null 
  // before calling methods on them.

  /**
   * Start the application.
   */
  public void startApp() {
    // This version doesn't come back after a pause, so 
    // we assume this is the initial startup:
    Display.getDisplay(this).setCurrent(myModeList);
  }
  
  /**
   * Throw out the garbage.
   */
  public void destroyApp(boolean unconditional) 
      throws MIDletStateChangeException {
    // tell the communicator to send the end game 
    // message to the other player and then disconnect:
    if(myMoveManager != null) {
      myMoveManager.shutDown();
    }
    // throw the larger game objects in the garbage:
    myMoveManager = null;
    myCanvas = null;
  }

  /**
   * End the program now.
   */
  public void quit() {
    try {
      destroyApp(false);
      notifyDestroyed();
    } catch (MIDletStateChangeException ex) {
    }
  }

  /**
   * Pause the game.
   * Because of the complexity of restarting a game in course, 
   * this method merely ends the game.
   */
  public void pauseApp() {
    quit();
  }

  //----------------------------------------------------------------
  //  implementation of CommandListener

  /*
   * Respond to a command issued on the Canvas.
   */
  public void commandAction(Command c, Displayable s) {
    if((c == myExitCommand) || (c == Alert.DISMISS_COMMAND)) {
      if((myMoveManager != null) 
         && (myMoveManager.getState() != MoveManager.NOT_STARTED)) {
        myMoveManager.endGame();
      } else {
        quit();
      }
    } else if(s == myModeList) {
      myMoveManager.setMode(myModeList.getSelectedIndex());
      Display.getDisplay(this).setCurrent(myCanvas);
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
      errorMsg(e.getMessage());
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
