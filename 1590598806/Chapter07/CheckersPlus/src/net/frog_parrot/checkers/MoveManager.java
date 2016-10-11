package net.frog_parrot.checkers;

import java.io.*;

import net.frog_parrot.net.SMSManager;

/**
 * This class keeps track of transferring local and 
 * remote moves from one player to the other..
 *
 * @author Carol Hamer
 */
public class MoveManager {

  //--------------------------------------------------------
  //  game instance fields

  /**
   * The MIDlet subclass, used to set the Display 
   * in the case where an error message needs to be sent..
   */
  private Checkers myCheckers;

  /**
   * The Canvas subclass, used to set the Display 
   * in the case where an error message needs to be sent..
   */
  private CheckersCanvas myCanvas;

  /**
   * The game logic class that we send the opponent's 
   * moves to..
   */
  private CheckersGame myGame;

  /**
   * The class that performs the actual network connections.
   */
  private SMSManager mySMSManager;

  /**
   * The message from the remote player to be displayed.
   */
  //private String myTaunt;

  //--------------------------------------------------------
  //  state fields

  /**
   * A possible game state.
   */
  public static final int NOT_STARTED = 0;

  /**
   * A possible game state.
   */
  public static final int FOUND_REMOTE_PLAYER = 1;

  /**
   * A possible game state.
   */
  public static final int LOCAL_TURN = 2;

  /**
   * A possible game state.
   */
  public static final int SENDING_LOCAL_TURN = 3;

  /**
   * A possible game state.
   */
  public static final int REMOTE_TURN = 4;

   /**
   * A possible game state.
   */
  public static final int GAME_OVER = 5;

   /**
   * A possible game state.
   */
  public static final int PAUSED = 6;

 /**
   * The code for the state the game is currently in.
   */
  private int myState = NOT_STARTED;

 /**
   * The code for the state to return to after a pause.
   */
  private int myPreviousState = NOT_STARTED;

  //--------------------------------------------------------
  //  lifecycle

  /**
   * Constructor initializes the handles back to other 
   * game objects.
   */
  MoveManager(Checkers checkers, CheckersCanvas canvas, 
	       CheckersGame game) {
    myCheckers = checkers;
    myCanvas = canvas;
    myGame = game;
    mySMSManager = new SMSManager();
    try {
      if(mySMSManager.init(this)) {
	myState = FOUND_REMOTE_PLAYER;
      }
    } catch(IOException e) {
      e.printStackTrace();
      myCheckers.errorMsg("communications error");
    }
    myCanvas.repaint();
    myCanvas.serviceRepaints();
  }

  /**
   * Stop the receiver.  
   * This method is called alone from destroyApp
   * since sending one last message is too time-consuming.
   */
  public void shutDown() {
    myState = GAME_OVER;
    if(mySMSManager != null) {
      mySMSManager.shutDown();
    }
  }

  /**
   * Stop the receiver and enter the paused state.  
   * This method is called alone from pauseApp
   * since sending one last message is too time-consuming.
   */
  public void pause() {
    myPreviousState = myState;
    myState = PAUSED;
    if(mySMSManager != null) {
      mySMSManager.shutDown();
    }
  }

  /**
   * If we're returning from a pause, restart the listner.
   */
  public void wakeUp() throws IOException {
    if(myState == PAUSED) {
      myState = myPreviousState;
      if(mySMSManager != null) {
	mySMSManager.init(this);
      }
    }
  }

  /**
   * Gets the current game state.
   */
  public int getState() {
    //System.out.println("getState-->returning: " + myState);
    return(myState);
  }

  //--------------------------------------------------------
  //  sending methods

  /**
   * Send the game invitation SMS.
   */
  public void sendInvitation(String phoneNumber) {
    mySMSManager.sendInvitation(phoneNumber);
    myCanvas.setWaitScreen(true);
    myCanvas.start();
    myCanvas.repaint();
    myCanvas.serviceRepaints();
  }

  /**
   * This is called when the player moves a piece.
   */
  synchronized void move(byte sourceX, byte sourceY, byte destinationX, 
		    byte destinationY) {
    //System.out.println("move-->setting local move");
    byte[] move = new byte[4];
    move[0] = sourceX;
    move[1] = sourceY;
    move[2] = destinationX;
    move[3] = destinationY;
    myState = LOCAL_TURN;
    mySMSManager.setLocalMove(move);
  }

  /**
   * This is called when the local player's turn is over.
   */
  synchronized void endTurn() {
    //System.out.println("move-->turn over");
    myState = SENDING_LOCAL_TURN;
    mySMSManager.sendLocalMove();
  }

  /**
   * Gets the message that the user has entered for the remote 
   * player, if any.  Then clears the text.
   */
  public String getTaunt() {
    return myCheckers.getTauntMessage();
  }

  /**
   * Stop the game entirely.  Notify the remote player that 
   * the user is exiting the game.
   */
  synchronized void endGame() {
    //System.out.println("MoveManager-->endGame");
    mySMSManager.shutDown();
    if(myState != GAME_OVER) {
      //System.out.println("MoveManager-->sending game over");
      myState = GAME_OVER;
      mySMSManager.sendGameOver();
      myCanvas.repaint();
      myCanvas.serviceRepaints();
    } else {
      myCheckers.quit();
    }
  }

  /**
   * End the game because the local player has no more moves..
   */
  void loseGame() {
    myCheckers.setWinTaunt();
    endGame();
  }

  /**
   * This method is called by the message sending utility
   * to indicate that the move has been sent.
   */
  public void doneSending() {
    //System.out.println("MoveManager.doneSending-->myState: " + myState);
    if(myState == GAME_OVER) {
      myCheckers.quit();
    } else {
      myState = REMOTE_TURN;
    }
    //System.out.println("MoveManager.doneSendingdone-->myState: " + myState);
  }

  //--------------------------------------------------------
  //  receiving methods

  /**
   * Receive the game invitation SMS.
   */
  public synchronized void receiveInvitation(String taunt, 
        String phoneNum) {
    //System.out.println("receiveInvitation-->myState: " + myState);
    myState = LOCAL_TURN;
    myCanvas.setWaitScreen(false);
    myCanvas.start();
    myCanvas.repaint();
    myCanvas.serviceRepaints();
    StringBuffer buff = new StringBuffer(phoneNum);
    buff.append(" invites you to play checkers");
    if(taunt != null) {
      buff.append(": ");
      buff.append(taunt);
    }
    myCheckers.displayTauntMessage(buff.toString());
  }
  
  /**
   * Receive and display a message from the remote player.
   */
  /*
  public synchronized void receiveTaunt(String taunt) {
    myTaunt = taunt;
  }
  */
  
  /**
   * Interpret one move by the remote player.
   */
  public synchronized void receiveRemoteMove(byte[] fourBytes) {
    //System.out.println("remote move!");
    myState = REMOTE_TURN;
    myGame.moveOpponent(fourBytes);
  }
  
  /**
   * Set the game to ended upon receiving the end game 
   * signal from the remote player.
   */
  public synchronized void receiveGameOver(String taunt) {
    myState = GAME_OVER;
    mySMSManager.shutDown();
    myCanvas.repaint();
    myCanvas.serviceRepaints();
    if(taunt != null) {
      myCheckers.displayTauntMessage(taunt);
    }
  }
  
  /**
   * Receive the signal that the remote player is done 
   * moving (no more jumps possible).
   */
  public synchronized void endRemoteTurn(String taunt) {
    myState = LOCAL_TURN;
    myGame.endOpponentTurn();
    myCanvas.setWaitScreen(false);
    myCanvas.repaint();
    myCanvas.serviceRepaints();
    if(taunt != null) {
      myCheckers.displayTauntMessage(taunt);
    }
  }

}
