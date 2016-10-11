package net.frog_parrot.dungeon;

/**
 * This class contains the loop that keeps the game running.
 *
 * @author Carol Hamer
 */
public class GameThread extends Thread {

  //---------------------------------------------------------
  //   fields

  /**
   * Whether or not the main thread would like this thread 
   * to pause.
   */
  boolean myShouldPause;

  /**
   * Whether or not the main thread would like this thread 
   * to stop.
   */
  static boolean myShouldStop;

  /**
   * A handle back to the graphical components.
   */
  DungeonCanvas myDungeonCanvas;

  /**
   * The System.time of the last screen refresh, used 
   * to regulate refresh speed.
   */
  private long myLastRefreshTime;

  /**
   * The minimum amount of time to wait between frames.
   */
  private static long MIN_WAIT = 100; 

  //----------------------------------------------------------
  //   initialization

  /**
   * standard constructor.
   */
  GameThread(DungeonCanvas canvas) {
    myDungeonCanvas = canvas;
  }

  //----------------------------------------------------------
  //   utilities

  /**
   * Get the amount of time to wait between screen refreshes.
   * Normally we wait only a single millisecond just to give 
   * the main thread a chance to update the keystroke info, 
   * but this method ensures that the game will not attempt 
   * to show more than 20 frames per second.
   */
  private long getWaitTime() {
    long retVal = 1;
    long difference = System.currentTimeMillis() - myLastRefreshTime;
    if(difference < MIN_WAIT) {
      retVal = MIN_WAIT - difference;
    }
    return(retVal);
  }

  //----------------------------------------------------------
  //   actions

  /**
   * pause the game.
   */
  void pause() {
    myShouldPause = true;
  }

  /**
   * restart the game after a pause.
   */
  synchronized void resumeGame() {
    myShouldPause = false;
    notify();
  }

  /**
   * stops the game.
   */
  synchronized void requestStop() {
    myShouldStop = true;
    this.notify();
  }

  /**
   * start the game..
   */
  public void run() {
    // flush any keystrokes that occurred before the 
    // game started:
    myDungeonCanvas.flushKeys();
    myShouldStop = false;
    myShouldPause = false;
    while(true) {
      if(myShouldStop) {
	break;
      }
      myLastRefreshTime = System.currentTimeMillis();
      myDungeonCanvas.checkKeys();
      myDungeonCanvas.updateScreen();
      // we do a very short pause to allow the other thread 
      // to update the information about which keys are pressed:
      synchronized(this) {
	try {
	  wait(getWaitTime());
	} catch(Exception e) {}
      }
      if(myShouldPause) {
	synchronized(this) {
	  try {
	    wait();
	  } catch(Exception e) {}
	}
      }
    }
  }

}
