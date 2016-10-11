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
public class SMSSender extends Thread {

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
  //  initialization

  /**
   * Set the data and prepare the address string.
   */
  public SMSSender(String phoneNum, String portNum, 
		   byte[] data, MoveManager manager) {
    myManager = manager;
    myPayload = data;
    // prepare the address string to route the message:
    // start with the remote player's phone number 
    // then append this game's push-port nubmer.
    StringBuffer buff = new StringBuffer();
    if(phoneNum != null) {
      buff.append(phoneNum);
    }
    buff.append(":");
    if(portNum != null) {
      buff.append(portNum);
    }
    myAddress = buff.toString();
  }

  //--------------------------------------------------------
  //  sending methods.

  /**
   * Sends the move data to the remote player.
   */
  public void run() {
    //System.out.println("SMSSender-->sending move to: " + myAddress);
    MessageConnection conn = null;
    try {
      conn = (MessageConnection)Connector.open(myAddress);
      
      BinaryMessage msg = (BinaryMessage)conn.newMessage(
          MessageConnection.BINARY_MESSAGE);
      msg.setAddress(myAddress);
      msg.setPayloadData(myPayload);
      conn.send(msg);
      //System.out.println("SMSSender-->done");
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
