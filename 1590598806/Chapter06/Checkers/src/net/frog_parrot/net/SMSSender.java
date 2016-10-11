package net.frog_parrot.net;

import java.io.*;
import javax.microedition.io.*;
import javax.wireless.messaging.*;

import net.frog_parrot.checkers.MoveManager;

/**
 * This class handles the details of sending the 
 * SMS message.
 *
 * @author Carol Hamer
 */
public class SMSSender implements Runnable {

  //--------------------------------------------------------
  //  static fields

  /**
   * The protocol string.
   */
  public static final String SMS_PROTOCOL = "sms://";

  //--------------------------------------------------------
  //  data fields

  /**
   * The string with the routing information to send an 
   * SMS to the right desitination.
   */
  private String myAddress;

  /**
   * The data to send.
   */
  private byte[] myPayload;

  /**
   * The class that directs the data from the communications
   * module to game logic module and vice versa.
   */
  private MoveManager myManager;

  //--------------------------------------------------------
  //  initialization and lifecycle

  /**
   * Initialize the main data.
   */
  public SMSSender(String phoneNum, String portNum, 
        MoveManager manager) {
    myManager = manager;
    // Construct the address url for routing the 
    // message, of the form 
    // sms://<phonenum>:<pushportnum>
    StringBuffer buff = new StringBuffer(SMS_PROTOCOL);
    if(phoneNum != null) {
      buff.append(phoneNum);
    }
    buff.append(":");
    if(portNum != null) {
      buff.append(portNum);
    }
    myAddress = buff.toString();
  }

  /**
   * Set the current move.
   */
  public synchronized void setPayload(byte[] data) {
    myPayload = data;
  }

  //--------------------------------------------------------
  //  sending methods.

  /**
   * Sends the move data to the remote player.
   */
  public void run() {
    MessageConnection conn = null;
    byte[] currentPayload = null;
    // synchronize it so each payload 
    // gets sent exactly once:
    synchronized(this) {
      if(myPayload != null) {
        currentPayload = myPayload;
        myPayload = null;
      } else {
        return;
      }
    }
    try {
      // open the SMS connection and create the 
      // message instance:
      conn = (MessageConnection)Connector.open(myAddress);
      
      BinaryMessage msg = (BinaryMessage)conn.newMessage(
            MessageConnection.BINARY_MESSAGE);
      msg.setAddress(myAddress);
      msg.setPayloadData(currentPayload);
      conn.send(msg);
      myManager.doneSending();
    } catch(Exception e) {
      e.printStackTrace();
    }
    if (conn != null) {
      try {
        conn.close();
      } catch (IOException ioe) {
        ioe.printStackTrace();
      }
    }
  }

}
