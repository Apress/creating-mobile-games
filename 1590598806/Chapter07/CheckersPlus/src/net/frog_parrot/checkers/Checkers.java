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
public class Checkers extends MIDlet implements CommandListener, 
    ContactListener {

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
   * The helper thread to load the contact list.
   */
  private PIMRunner myPIMRunner;

  /**
   * The field that takes in the remote player's phone number.
   */
  private TextField myPhoneNumberField;

  /**
   * The vector listing the contacts' phone numbers.
   */
  private Vector myPhoneNumbers;

  /**
   * The list of the user's initial choices.
   */
  private List myStartList;

  /**
   * The form where the user can enter an opponent manually.
   */
  private Form myInvitationForm;

  /**
   * The menu screen where the user selects a friend's number
   * from a list.
   */
  private List myContactMenu;

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
      // create the canvas and set up the commands:
      myCanvas = new CheckersCanvas(Display.getDisplay(this));
      myCanvas.addCommand(myExitCommand);
      myCanvas.addCommand(myTauntCommand);
      myCanvas.setCommandListener(this);
      CheckersGame game = myCanvas.getGame();
      myMoveManager = new MoveManager(this, myCanvas, game);
      game.setMoveManager(myMoveManager);
      // create the screen where the user can optionally enter 
      // a message to send to the opponent.
      myTauntBox = new TextBox("message", null, 100, TextField.ANY);
      myTauntBox.addCommand(myOkCommand);
      myTauntBox.addCommand(myCancelCommand);
      myTauntBox.setCommandListener(this);
      // Start with a screen to offer the the user the choice of 
      // whether to enter an opponent's phone number manually or 
      // select an opponent from the user's addressbook.
      String[] choices = { "choose from list", "enter manually" };
      myStartList = new List("select an opponent", List.IMPLICIT, 
                                choices, null);
      myStartList.addCommand(myExitCommand);
      myStartList.setCommandListener(this);
    } catch(Exception e) {
      // if there's an error during creation, display it as an alert.
      errorMsg(e);
    }
  }

  /**
   * Create the form for entering the invitations.
   */
  synchronized void createInvitationForm() {
    if(myInvitationForm == null) {
      myInvitationForm = new Form("Checkers");
      myPhoneNumberField = new TextField(null, null, 15, 
                                         TextField.PHONENUMBER);
      myInvitationForm.append("Please enter the phone number " 
                              + "of the remote player:");
      myInvitationForm.append(myPhoneNumberField);
      myInvitationForm.addCommand(myOkCommand);
      myInvitationForm.addCommand(myExitCommand);
      myInvitationForm.setCommandListener(this);
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
      Display.getDisplay(this).setCurrent(myStartList);
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
        myMoveManager.sendInvitation(myPhoneNumberField.getString());
        myPhoneNumberField = null;
        myCanvas.setWaitScreen(true);
        myCanvas.start();
        myCanvas.repaint();
        myCanvas.serviceRepaints();
        Display.getDisplay(this).setCurrent(myCanvas);  
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
      Display.getDisplay(this).setCurrent(myTauntBox);      
    } else if(c == myTauntCommand) {
      Display.getDisplay(this).setCurrent(myTauntBox);      
    } else if(s == myStartList) {
      // since we've already checked for the exit command, 
      // a command action means that the user has selected
      // whether to (0) choose a number from the contacts 
      // list or (1) enter a number manually:
      int selection = myStartList.getSelectedIndex();
      if(selection == 0) {
        // Now we start the thread to load the contact list and
        // change the screen.  This could have been loaded in 
        // the background at startup time, however accessing 
        // the PIM system causes the AMS to ask the user for
        // permission, which may cause confusion if the user 
        // hasn't yet requested an action that requires PIM access.
        synchronized(this) {
          // synchronize to avoid accidentally creating 
          // multiple threads if the user presses the select 
          // key more than once.
          if(myPIMRunner == null) {
            myPIMRunner = new PIMRunner(this);
            myPIMRunner.start();
          }
        }
      } else {
        // set the screen so the user can enter the opponent's
        // number manually:
        createInvitationForm();
        Display.getDisplay(this).setCurrent(myInvitationForm);
      }
    } else if(s == myContactMenu) {
      // since we've already checked for the exit command
      // and taunt command, a command action means that the 
      // user has selected a contact:
      int selection = myContactMenu.getSelectedIndex();
      // store the selected phone number in the phone 
      // number field, then move on to requesting a message:
      myPhoneNumberField = new TextField(null, 
           (String)(myPhoneNumbers.elementAt(selection)), 15, 
               TextField.PHONENUMBER);
      Display.getDisplay(this).setCurrent(myTauntBox);      
    }
  }
  
  /**
   * Set the contact list.
   */
  public void setContactList(Vector names, Vector phoneNumbers) {
    myPhoneNumbers = phoneNumbers;
    String[] nameArray = new String[names.size()];
    names.copyInto(nameArray);
    myContactMenu = new List("select an opponent", List.IMPLICIT, 
                             nameArray, null);
    myContactMenu.addCommand(myExitCommand);
    myContactMenu.setCommandListener(this);
    Display.getDisplay(this).setCurrent(myContactMenu);
  }

  //-------------------------------------------------------
  //  message methods

  /**
   * Display's the remote player's message as an Alert.
   */
  public void displayTauntMessage(String taunt) {
    System.out.println("displayTauntMessage-->taunt: " + taunt);
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
    System.out.println("returning message from box: " + retVal);
    return retVal;
  }

  /**
   * Manually set the taunt message to tell the remote 
   * player that he has won.
   */
  public void setWinTaunt() {
    System.out.println("setWinTaunt");
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
