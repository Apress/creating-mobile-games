package net.frog_parrot.net;

import java.io.*;
import javax.microedition.io.*;
import javax.wireless.messaging.*;

/**
 * This class handles listening for the SMS messages 
 * from the remote player.
 *
 * @author Carol Hamer
 */
public class SMSReceiver implements Runnable {

  //--------------------------------------------------------
  //  game instance fields

  /**
   * The class to report back to.
   */
  private SMSManager mySMSManager;

  /**
   * The connection to listen for messages on.
   */
  private MessageConnection myConnection;

  /**
   * whether it's time to clean up.
   */
  private boolean myShouldStop;

  //--------------------------------------------------------
  //  lifecycle

  /**
   * Find connections and start up the listener thread.
   */
  boolean init(SMSManager manager) throws IOException {
    myShouldStop = false;
    mySMSManager = manager;
    boolean retVal = false;
    // We start by checking for a connection with data to read
    // to see if the application was launched because of 
    // receiving an invitation.
    String[] connections = PushRegistry.listConnections(true);
    if (connections != null && connections.length > 0) {
      retVal = true;
    }
    // Here we get the name of all connections that are open 
    // to this application, whether they have data or not.
    connections = PushRegistry.listConnections(false);
    if (connections == null || connections.length == 0) {
      throw(new IOException("No push-port registered"));
    }
    myConnection = (MessageConnection)Connector.open(connections[0]);
    // Since listening for messages is a blocking operation, 
    // we need to start a new thread to do it:
    Thread thread = new Thread(this);
    thread.start();
    return(retVal);
  }

  /**
   * Set this thread to die, then close the connection.
   */
  void shutDown() {
    myShouldStop = true;
    try {
      myConnection.close();
    } catch (IOException e) {}
  }

  //--------------------------------------------------------
  //  listen for messages

  /**
   * Start the listener thread.
   */
  public void run() {
    try {
      while(true) {
        Message msg = myConnection.receive();
        if (msg != null) {
          String senderAddress = msg.getAddress();
          // accept only binary messages from the player
          // we're currently playing with -- ignore 
          // messages from other players.
          if((mySMSManager.checkPhoneNum(senderAddress))
             && (msg instanceof BinaryMessage)) {
            byte[] data = ((BinaryMessage)msg).getPayloadData();
            int index = 0;
            String taunt = null;
            while(index < data.length) {
              switch(data[index]) {
                // if we've just received an invitation to 
                // start the game, then we set the phone 
                // number of the other player
              case SMSManager.START_GAME_FLAG:
                index++;
                if(index != data.length) {
                  taunt = new String(data, index, data.length - index);
                }
                index = data.length;
                mySMSManager.receiveInvitation(taunt);
                break;
              case SMSManager.END_GAME_FLAG:
                index++;
                if(index != data.length) {
                  taunt = new String(data, index, data.length - index);
                }
                index = data.length;
                mySMSManager.receiveGameOver(taunt);
                break;
              case SMSManager.END_TURN_FLAG:
                index++;
                if(index != data.length) {
                  taunt = new String(data, index, data.length - index);
                }
                index = data.length;
                mySMSManager.endTurn(taunt);
                break;
              default:
                // the default case is that we've received
                // some move data to pass along to the 
                // game logic.
                byte[] move = new byte[4];
                System.arraycopy(data, index, move, 0, 4);
                mySMSManager.setMove(move);
                index += 4;
                break;
              }
            }
          }
        } // if (msg != null) {
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      try {
        myConnection.close();
      } catch (Exception e) {}
    }
  }

}
