package net.frog_parrot.checkers;

import javax.microedition.midlet.*;
import javax.microedition.lcdui.*;

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
   * The field that takes in the remote player's phone number.
   */
  private TextField myPhoneNumberField;

  /**
   * The screen where the local player enters a message for 
   * the remote player.
   */
  private TextBox myTauntBox;

  //-----------------------------------------------------
  //    command fields

  /**
   * The button to exit the game.
   */
  private Command myExitCommand = new Command("Exit", Command.EXIT, 99);

  /**
   * The button to cancel a message in progress.
   */
  private Command myCancelCommand = new Command("Cancel", Command.CANCEL, 99);

  /**
   * The button to send the initial invitation.
   */
  private Command myOkCommand = new Command("Ok", Command.OK, 0);

  /**
   * The button to enter a message for the remote player.
   */
  private Command myTauntCommand = new Command("add message", 
      Command.SCREEN, 1);

  //-----------------------------------------------------
  //    initialization and data

  /**
   * Initialize the canvas and the commands.
   */
  public Checkers() {
    try { 
      //create the canvas and set up the commands:
      myCanvas = new CheckersCanvas(Display.getDisplay(this));
      myCanvas.addCommand(myExitCommand);
      myCanvas.addCommand(myTauntCommand);
      myCanvas.setCommandListener(this);
      CheckersGame game = myCanvas.getGame();
      myMoveManager = new MoveManager(this, myCanvas, game);
      game.setMoveManager(myMoveManager);
      myTauntBox = new TextBox("message", null, 100, TextField.ANY);
      myTauntBox.addCommand(myOkCommand);
      myTauntBox.addCommand(myCancelCommand);
      myTauntBox.setCommandListener(this);
    } catch(Exception e) {
      // if there's an error during creation, display it as an alert.
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
    // If the game wasn't launched by receiving an invitation, 
    // start with a screen to prompt the user to send an 
    // invitation to another player.
    if(myMoveManager.getState() == MoveManager.NOT_STARTED) {
      Display.getDisplay(this).setCurrent(myTauntBox);
    } else {
      try {
        myMoveManager.wakeUp();
      } catch(Exception e) {
        errorMsg(e);
      }
    }
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
   * This closes the receiving thread.
   */
  public void pauseApp() {
    myMoveManager.pause();
  }

  //----------------------------------------------------------------
  //  implementation of CommandListener

  /*
   * Respond to a command issued on the Canvas.
   */
  public void commandAction(Command c, Displayable s) {
    if(c == myCancelCommand) {
      myTauntBox.setString(null);
    }
    if(s == myTauntBox) {
      if(myMoveManager.getState() == MoveManager.NOT_STARTED) {
        Form invitationForm = new Form("Checkers");
        myPhoneNumberField = new TextField(null, null, 15, 
                                           TextField.PHONENUMBER);
        invitationForm.append("Please enter the phone number " 
                              + "of the remote player:");
        invitationForm.append(myPhoneNumberField);
        invitationForm.addCommand(myOkCommand);
        invitationForm.addCommand(myExitCommand);
        invitationForm.setCommandListener(this);
        Display.getDisplay(this).setCurrent(invitationForm);
      } else {
        Display.getDisplay(this).setCurrent(myCanvas);
      }
    } else if((c == myExitCommand) || (c == Alert.DISMISS_COMMAND)) {
      if((myMoveManager != null) 
         && (myMoveManager.getState() != MoveManager.NOT_STARTED)) {
        myMoveManager.endGame();
      } else {
        quit();
      }
    } else if(c == myOkCommand) {
      myMoveManager.sendInvitation(myPhoneNumberField.getString());
      myPhoneNumberField = null;
      myCanvas.setWaitScreen(true);
      myCanvas.start();
      myCanvas.repaint();
      myCanvas.serviceRepaints();
      Display.getDisplay(this).setCurrent(myCanvas);  
    } else if(c == myTauntCommand) {
      Display.getDisplay(this).setCurrent(myTauntBox);      
    }
  }
  
  //-------------------------------------------------------
  //  message methods

  /**
   * Display's the remote player's message as an Alert.
   */
  public void displayTauntMessage(String taunt) {
    Alert tauntScreen = new Alert("message");
    tauntScreen.setString(taunt);
    Display.getDisplay(this).setCurrent(tauntScreen, 
        myCanvas);
  }

  /**
   * Gets the message that the user has entered for the remote 
   * player, if any.  Then clears the text.
   */
  public String getTauntMessage() {
    String retVal = myTauntBox.getString();
    myTauntBox.setString(null);
    return retVal;
  }

  /**
   * Manually set the taunt message to tell the remote 
   * player that he has won.
   */
  public void setWinTaunt() {
    myTauntBox.setString("You Win!");
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
