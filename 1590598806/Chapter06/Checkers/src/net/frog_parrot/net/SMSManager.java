package net.frog_parrot.net;

import java.io.*;

import net.frog_parrot.checkers.MoveManager;

/**
 * This class keeps track of transferring local and 
 * remote moves from one player to the other..
 *
 * @author Carol Hamer
 */
public class SMSManager {

  //--------------------------------------------------------
  //  static fields

  /**
   * The int to signal that the game is to begin.
   */
  public static final byte START_GAME_FLAG = -4;

  /**
   * The byte to signal that the game is to end.
   */
  public static final byte END_GAME_FLAG = -3;

  /**
   * The byte to signal the end of a turn.
   */
  public static final byte END_TURN_FLAG = -2;

  /**
   * The default port number to send to.
   */
  private static final String PORT_NUM = "16474";

  //--------------------------------------------------------
  //  game instance fields

  /**
   * The class that directs the data from the communications
   * module to game logic module and vice versa.
   */
  private MoveManager myManager;

  /**
   * The class that receives and reads the SMSMessages.
   */
  private SMSReceiver myReceiver;

  /**
   * The class that sends the SMSMessages.
   */
  private SMSSender mySender;

  //--------------------------------------------------------
  //  data exchange instance fields

  /**
   * The data from the local player that is to 
   * be sent to the opponent.
   */
  private byte[] myMove;

  /**
   * The phone number of the opponent:
   */
  private String myPhoneNum;

  //--------------------------------------------------------
  //  lifecycle

  /**
   * Start the processes to send and receive messages.
   * @return whether the game was started by receiving
   * an invitation from another player.
   */
  public boolean init(MoveManager manager) 
      throws IOException {
    myManager = manager;
    myReceiver = new SMSReceiver();
    return(myReceiver.init(this));
  }

  /**
   * Stop the receiver and sender threads.  
   * This method is called alone from pauseApp or destroyApp
   * since sending one last message is too time-consuming.
   */
  public void shutDown() {
    myReceiver.shutDown();
  }

  /**
   * Sets the current opponent if none is set, and verifies
   * that subsequent messages came from the right opponent.
   * We cut the first six characters off the beginning of 
   * the phone number because these are just the SMS protocol
   * string.
   *
   * @returns true if the message can be accepted.
   */
  public boolean checkPhoneNum(String phoneNumber) {
    if(myPhoneNum == null) {
      myPhoneNum = phoneNumber.substring(6);
      return true;
    } else if (myPhoneNum.equals(phoneNumber.substring(6))) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * This is called when the game is started by receiving 
   * an invitation from a remote player.  This triggers the 
   * game logic to let the local player make the first move.
   */
  public void receiveInvitation(String taunt) {
    int state = myManager.getState();
    if((state == MoveManager.NOT_STARTED) 
        || (state == MoveManager.FOUND_REMOTE_PLAYER)) {
      mySender = new SMSSender(myPhoneNum, PORT_NUM, myManager);
      myManager.receiveInvitation(taunt, myPhoneNum);
    }
  }

  //--------------------------------------------------------
  //  sending methods.

  /**
   * Send the game invitation SMS.
   */
  public void sendInvitation(String phoneNumber) {
    myPhoneNum = phoneNumber;
    mySender = new SMSSender(myPhoneNum, PORT_NUM, myManager);
    byte[] invitation = new byte[1];
    invitation[0] = START_GAME_FLAG;
    invitation = addTaunt(invitation);
    mySender.setPayload(invitation);
    Thread thread = new Thread(mySender);
    thread.start();
  }

  /**
   * Send the message to the other
   * player that this player has quit.
   */
  public void sendGameOver() {
    byte[] data = new byte[1];
    data[0] = END_GAME_FLAG;
    data = addTaunt(data);
    mySender.setPayload(data);
    Thread thread = new Thread(mySender);
    thread.start();
  }

  /**
   * Records the local move in a byte array to prepare it 
   * to be sent to the remote player.
   */
  public void setLocalMove(byte[] move) {
    if(myMove == null) {
      myMove = new byte[5];
      System.arraycopy(move, 0, myMove, 0, 4);
      myMove[4] = END_TURN_FLAG;
    } else {
      // here we're dealing with the case of a 
      // series of jumps.  This isn't the typical case, so 
      // it shouldn't be too inefficient to just 
      // create a new, larger array each time 
      // we enlarge the move payload.
      byte[] newMove = new byte[myMove.length + 4];
      System.arraycopy(myMove, 0, newMove, 0, myMove.length);
      System.arraycopy(move, 0, newMove, myMove.length - 1, 4);
      newMove[newMove.length - 1] = END_TURN_FLAG;
      myMove = newMove;
    }
  }

  /**
   * Sends the current local move to the remote player
   * then clears the move data.
   */
  public void sendLocalMove() {
    if(myMove != null) {
      myMove = addTaunt(myMove);
      mySender.setPayload(myMove);
      myMove = null;
      Thread thread = new Thread(mySender);
      thread.start();
    }
  }

  //--------------------------------------------------------
  //  receiving methods

  /**
   * Pass the remote player's move data to the game logic.
   */
  public void setMove(byte[] move) {
    myManager.receiveRemoteMove(move);
  }

  /**
   * Signal that the remote player's turn is over.
   */
  public void endTurn(String taunt) {
    myManager.endRemoteTurn(taunt);
  }

  /**
   * Signal that the remote player has ended the game.
   */
  public void receiveGameOver(String taunt) {
    myManager.receiveGameOver(taunt);
  }

  //--------------------------------------------------------
  //  utilities

  /**
   * Adds the taunt message data to the message being sent.
   */
  public byte[] addTaunt(byte[] data) {
    String taunt = myManager.getTaunt();
    if(taunt != null) {
      try {
        byte[] tauntdata = taunt.getBytes("utf-8");
        byte[] retData = new byte[tauntdata.length + data.length];
        System.arraycopy(data, 0, retData, 0, data.length);
        System.arraycopy(tauntdata, 0, retData, data.length, 
                         tauntdata.length);
        return retData;
      } catch (UnsupportedEncodingException e) {
        // if the encoding is not supported, ignore message.
      }
    }
    return data;
  }

}
