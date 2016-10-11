package net.frog_parrot.net;

import java.io.*;
import java.util.Vector;

import javax.microedition.io.*;

import javax.bluetooth.*;

import net.frog_parrot.checkers.MoveManager;

/**
 * This class keeps track of transferring local and 
 * remote moves from one player to the other..
 *
 * @author Carol Hamer
 */
public class BluetoothManager extends Thread 
    implements DiscoveryListener {

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
   * The byte to signal the remote connection has 
   * been closed.
   */
  public static final byte EOF_FLAG = -1;

  /**
   * A possible connection mode.
   */
  public static final byte SERVER_MODE = 0;

  /**
   * A possible connection mode.
   */
  public static final byte CLIENT_MODE = 1;

  /**
   * The protocol string for the bluetooth stream protocol.
   */
  public static final String BLUETOOTH_PROTOCOL = "btspp://";

  /**
   * The string that uniquely identifies this Bluetooth service:
   */
  private static final String CHECKERS_UUID 
      = "2bbc2d287c8c11dba1500040f45842ef";

  /**
   * The user-friendly name of the service:
   */
  private static final String CHECKERS_NAME = "Checkers";

  //--------------------------------------------------------
  //  game instance fields

  /**
   * Whether the MIDlet will be acting as a client or as a 
   * server for this round.
   */
  private int myMode;

  /**
   * The results of the client search for available services.
   */
  private int myDiscoveryType;

  /**
   * The results of the client search for available devices.
   */
  private Vector myRemoteDevices = new Vector();

  /**
   * The results of the client search for available devices.
   */
  private ServiceRecord myRemoteServiceRecord;

  /**
   * Whether the to break out of the communications loop and 
   * end the game.
   */
  private boolean myShouldStop;

  /**
   * The class that directs the data from the communications
   * module to game logic module and vice versa.
   */
  private MoveManager myManager;

  /**
   * The instance of the BlueTooth UUID class that is needed to 
   * make the connection.
   */
  private UUID myUUID = new UUID(CHECKERS_UUID, false);

  /**
   * The network connection.
   */
  private StreamConnection myStreamConnection;

  /**
   * The corresponding input stream.
   */
  private InputStream myInputStream;

  /**
   * The corresponding output stream.
   */
  private OutputStream myOutputStream;

  //--------------------------------------------------------
  //  data exchange instance fields

  /**
   * The data from the local player that is to 
   * be sent to the opponent.
   */
  private byte[] myMove;

  //--------------------------------------------------------
  //  lifecycle

  /**
   * Start the processes to send and receive messages.
   */
  public void setMode(int mode, MoveManager manager) {
    myManager = manager;
    myMode = mode;
    start();
  }

  /**
   * Start the thread that communicates with the remote
   * player.
   */
  public void run() {
    try {
      if(myMode == SERVER_MODE) {
        serverRun();
      } else {
        clientRun();
      }
    } catch(Exception e) {
      myManager.errorMsg("failed: " + e.getMessage());
      debug("run");
      e.printStackTrace();
    }
  }

  /**
   * Stop the receiver.  
   * This method is called alone from pauseApp or destroyApp
   * since sending one last message is too time-consuming.
   */
  public synchronized void shutDown() {
    myShouldStop = true;
    notify();
  }

  /**
   * Close all of the streams.
   */
  public void cleanUp() {
    try {
      if(myInputStream == null) {
        myInputStream.close();
      }
      if(myOutputStream == null) {
        myOutputStream.close();
      }
      if(myStreamConnection == null) {
        myStreamConnection.close();
      }
    } catch(Exception e) {
    }
  }

  /**
   * This is called when the game is in server mode and has been
   * contacted by a remote client player.  This triggers the 
   * game logic to let the local player make the first move.
   */
  public void receiveInvitation() {
    debug("receiveInvitation");
    int state = myManager.getState();
    if(state == MoveManager.NOT_STARTED) {
      myManager.receiveInvitation();
    }
  }

  //--------------------------------------------------------
  //  server methods.

  /**
   * Perform the initial steps to start up a very simple 
   * server connection that accepts only one client connection.
   */
  void serverRun() {
    debug("serverRun");
    StreamConnectionNotifier notifier;
    try {
      // get a handle to the local device and set it 
      // to accept client connections:
      LocalDevice localDevice = LocalDevice.getLocalDevice();
      // GIAC is the standard general discovery mode:
      localDevice.setDiscoverable(DiscoveryAgent.GIAC);

      // create the URL and the server connection
      StringBuffer buff = new StringBuffer(BLUETOOTH_PROTOCOL);
      // setting the host to localhost opens this as a
      // server connection.
      buff.append("localhost").append(':');
      buff.append(myUUID.toString());
      buff.append(";name=");
      buff.append(CHECKERS_NAME);
      buff.append(";authorize=false");
      
      debug("serverRun-->url: " + buff.toString());
      // Since this is a server connection, Connector.open
      // returns a connection notifier rather than a 
      // simple connection
      notifier 
          = (StreamConnectionNotifier)Connector.open(buff.toString());
      debug("serverRun-->notifier: " + notifier);

      // accept exactly one client connection:
      myStreamConnection = notifier.acceptAndOpen();
      debug("serverRun-->conn: " + myStreamConnection);
      myInputStream = myStreamConnection.openInputStream();
      debug("serverRun-->is: " + myInputStream);
      // the client player starts by sending the start game 
      // flag, triggering the server player to take a turn:
      byte[] oneByte = new byte[1];
      myInputStream.read(oneByte);
      debug("serverRun-->read: " + oneByte[0]);
      myManager.receiveInvitation();
      debug("serverRun-->myManager: " + myManager);
      // we don't want any more clients to try to connect, 
      // so we close up the notifier:
      notifier.close();
      // Even though the stream is closed, it's cleaner to  
      // set the server to "not discoverable" so further 
      // clients don't query this service and attempt to connect:
      localDevice.setDiscoverable(DiscoveryAgent.NOT_DISCOVERABLE);
      myOutputStream = myStreamConnection.openOutputStream();
      runGame(myOutputStream, myInputStream);
    } catch (IOException e) {
      myManager.errorMsg("failed: " + e.getMessage());
      e.printStackTrace();
    }
  }

  //--------------------------------------------------------
  //  client methods.

  /**
   * Perform the initial steps to start up a very simple 
   * client connection.
   */
  void clientRun() {
    debug("clientRun");
    try {
      // start looking for available Bluetooth services:
      LocalDevice localDevice = LocalDevice.getLocalDevice();
      DiscoveryAgent discoveryAgent = localDevice.getDiscoveryAgent();
      debug("clientRun-->discoveryAgent: " + discoveryAgent);
      // Set this as the discovery listener, then wait for 
      // the inquiryCompleted call:
      discoveryAgent.startInquiry(DiscoveryAgent.GIAC, this);
      synchronized(this) {
        wait();
      }
      if(myDiscoveryType == INQUIRY_COMPLETED) {
        // now for each device, we search for services:
        debug("clientRun-->myRemoteDevices: " + myRemoteDevices.size());
        for(int i = 0; i < myRemoteDevices.size(); i++) {
          RemoteDevice rd = (RemoteDevice)myRemoteDevices.elementAt(i);
          // we assume that the user has arranged to have 
          // exactly one player in range, so we search until 
          // we find a checkers service and stop:
          UUID[] uuids = new UUID[2];
          // this indicates socket communications:
          uuids[0] = new UUID(0x1101);
          // and this is the Checkers service specifically:
          uuids[1] = new UUID(CHECKERS_UUID, false);
          // The return value of the search call is an id int 
          // that can be used to cancel the search if 
          // something goes wrong:
          debug("clientRun-->about to search services");
          int id = discoveryAgent.searchServices(null, uuids,
                        rd, this);
          // now wait to see if we found the service:
          synchronized(this) {
            wait();
          }
          if(myRemoteServiceRecord != null) {
            break;
          }
        }
        debug("clientRun-->myRemoteServiceRecord: " + myRemoteServiceRecord);
        if(myRemoteServiceRecord != null) {
          // now let's open the connection:
          String url = myRemoteServiceRecord.getConnectionURL(
              ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false);
          debug("clientRun-->url: " + url);
          myStreamConnection = (StreamConnection)Connector.open(url);
          myOutputStream = myStreamConnection.openOutputStream();
          debug("clientRun-->os: " + myOutputStream);
          byte[] oneByte = new byte[1];
          oneByte[0] = (byte)START_GAME_FLAG;
          myOutputStream.write(oneByte);
          myManager.foundOpponent();
          myInputStream = myStreamConnection.openInputStream();
          runGame(myOutputStream, myInputStream);
        } else {
          myManager.errorMsg("failed to find remote player");
        }
      } else {
        myManager.errorMsg("failed to find remote player");
      }
    } catch (Exception e) {
      myManager.errorMsg("failed: " + e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * Implementation of DiscoveryListener.
   */
  public void deviceDiscovered(RemoteDevice device, DeviceClass dc) {
    myRemoteDevices.addElement(device);
  }

  /**
   * Implementation of DiscoveryListener.
   */
  public void inquiryCompleted(int discoveryType) {
    myDiscoveryType = discoveryType;
    synchronized(this) {
      notify();
    }
  }

  /**
   * Implementation of DiscoveryListener.
   * For a given remote device, find its services.
   */
  public synchronized void servicesDiscovered(int id, 
      ServiceRecord[] sr) {
    if(myRemoteServiceRecord == null) {
      myRemoteServiceRecord = sr[0];
      notify();
    }
  }
  
  /**
   * Implementation of DiscoveryListener.
   * For a given remote device, find its services.
   */
  public synchronized void serviceSearchCompleted(int id, int respCode) {
    // if none were found, notify that the search for services
    // on this device is done.
    if(myRemoteServiceRecord == null) {
      notify();
    }
  }
  
  //--------------------------------------------------------
  //  main game method

  /**
   * This is the main loop that controls the game play
   * back and forth.
   */
  void runGame(OutputStream os, InputStream is) throws IOException {
    debug("runGame");
    byte[] fourBytes = new byte[4];
    while(! myShouldStop) {
      int state = myManager.getState();
      debug("runGame-->state: " + state);
      if(state == MoveManager.LOCAL_TURN) {
        try {
          synchronized(this) {
            wait();
          }
        } catch(InterruptedException e) {
        }
        debug("runGame-->about to write move");
        try {
          if(myMove != null) {
            os.write(myMove);
          }
        } catch(IOException e) {
          // if we can't write anymore, the remote 
          // player has probably closed the connection:
          myManager.errorMsg("remote player has quit");
        }
        myMove = null;
      } else if(state == MoveManager.REMOTE_TURN) {
        debug("runGame-->about to read move");
        byte moveData = (byte)is.read();
        debug("runGame-->moveData: " + moveData);
        if((moveData == END_GAME_FLAG) 
            || (moveData == EOF_FLAG)) {
          debug("runGame-->remote player quit");
          myShouldStop = true;
          myManager.receiveGameOver();
        } else if (moveData == END_TURN_FLAG) {
          myManager.endRemoteTurn();
        } else {
          fourBytes[0] = moveData;
          for(int i = 1; i < 4; i++) {
            moveData = (byte)is.read();
            fourBytes[i] = moveData;
          }
          myManager.receiveRemoteMove(fourBytes);
        }
        debug("runGame-->done reading");
      } else {
        myShouldStop = true;
      }
    }
    debug("runGame-->done");
    cleanUp();
  }

  //--------------------------------------------------------
  //  sending methods.

  /**
   * Send the message to the other
   * player that this player has quit.
   */
  public synchronized void sendGameOver() {
    myMove = new byte[1];
    myMove[0] = END_GAME_FLAG;
    notify();
  }

  /**
   * Records the local move in a byte array to prepare it 
   * to be sent to the remote player.
   */
  public void setLocalMove(byte[] move) {
    debug("setLocalMove");
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
   * Sends the current local move to the remote player.
   */
  public synchronized void sendLocalMove() {
    debug("sendLocalMove-->myMove: " + myMove);
    if(myMove != null) {
      debug("sendLocalMove-->length: " + myMove.length);
    }
    notify();
  }

  //--------------------------------------------------------
  //  utilities

  /**
   * print a debug statemeent.
   */
  public void debug(String message) {
    if(myMode == SERVER_MODE) {
      System.out.print("SERVER: ");
    } else {
      System.out.print("CLIENT: ");
    }
    System.out.println(message);
  }


}
