package net.frog_parrot.checkers;

import java.io.*;

import net.frog_parrot.net.BluetoothManager;

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
  private BluetoothManager myBluetoothManager;

  //--------------------------------------------------------
  //  state fields

  /**
   * A possible game state.
   */
  public static final int NOT_STARTED = 0;

  /**
   * A possible game state.
   */
  public static final int LOCAL_TURN = 2;

  /**
   * A possible game state.
   */
  public static final int REMOTE_TURN = 4;

   /**
   * A possible game state.
   */
  public static final int GAME_OVER = 5;

 /**
   * The code for the state the game is currently in.
   */
  private int myState = NOT_STARTED;

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
    myBluetoothManager = new BluetoothManager();
  }

  /**
   * Stop the receiver.  
   * This method is called alone from destroyApp
   * since sending one last message is too time-consuming.
   */
  public void shutDown() {
    myState = GAME_OVER;
    if(myBluetoothManager != null) {
      myBluetoothManager.shutDown();
    }
  }

  /**
   * Gets the current game state.
   */
  public int getState() {
    return(myState);
  }

  /**
   * End the game with an error screen.
   */
  public void errorMsg(String msg) {
    myCheckers.errorMsg(msg);
    myBluetoothManager.cleanUp();
  }

  //--------------------------------------------------------
  //  sending methods

  /**
   * Set mode.
   * This method triggers the BluetoothManager to start seeking
   * a remote player, either in client mode or server mode.
   */
  public void setMode(int mode) {
    myBluetoothManager.setMode(mode, this);
    myCanvas.setWaitScreen(true);
    myCanvas.start();
    myCanvas.repaint();
    myCanvas.serviceRepaints();
  }

  /**
   * This is called when the client player finds a server
   * player.
   */
  public void foundOpponent() {
    myState = REMOTE_TURN;
  }

  /**
   * This is called when the player moves a piece.
   */
  synchronized void move(byte sourceX, byte sourceY, byte destinationX, 
                     byte destinationY) {
    byte[] move = new byte[4];
    move[0] = sourceX;
    move[1] = sourceY;
    move[2] = destinationX;
    move[3] = destinationY;
    myState = LOCAL_TURN;
    myBluetoothManager.setLocalMove(move);
  }

  /**
   * This is called when the local player's turn is over.
   */
  synchronized void endTurn() {
    myState = REMOTE_TURN;
    myBluetoothManager.sendLocalMove();
  }

  /**
   * Stop the game entirely.  Notify the remote player that 
   * the user is exiting the game.
   */
  synchronized void endGame() {
    myBluetoothManager.shutDown();
    if(myState != GAME_OVER) {
      myState = GAME_OVER;
      myBluetoothManager.sendGameOver();
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
    endGame();
  }

  /**
   * This method is called by the message sending utility
   * to indicate that the move has been sent.
   */
  public void doneSending() {
    if(myState == GAME_OVER) {
      myCheckers.quit();
    } else {
      myState = REMOTE_TURN;
    }
  }

  //--------------------------------------------------------
  //  receiving methods

  /**
   * Receive the game invitation message.
   */
  public synchronized void receiveInvitation() {
    myState = LOCAL_TURN;
    myCanvas.setWaitScreen(false);
    myCanvas.start();
    myCanvas.repaint();
    myCanvas.serviceRepaints();
  }
  
  /**
   * Interpret one move by the remote player.
   */
  public synchronized void receiveRemoteMove(byte[] fourBytes) {
    myState = REMOTE_TURN;
    myGame.moveOpponent(fourBytes);
  }
  
  /**
   * Set the game to ended upon receiving the end game 
   * signal from the remote player.
   */
  public synchronized void receiveGameOver() {
    myState = GAME_OVER;
    myBluetoothManager.shutDown();
    myCanvas.repaint();
    myCanvas.serviceRepaints();
  }
  
  /**
   * Receive the signal that the remote player is done 
   * moving (no more jumps possible).
   */
  public synchronized void endRemoteTurn() {
    myState = LOCAL_TURN;
    myGame.endOpponentTurn();
    myCanvas.setWaitScreen(false);
    myCanvas.repaint();
    myCanvas.serviceRepaints();
  }

}
