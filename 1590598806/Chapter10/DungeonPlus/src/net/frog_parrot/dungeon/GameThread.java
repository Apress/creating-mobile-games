package net.frog_parrot.dungeon;

import net.frog_parrot.util.*;

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
   * A handle back to the splashscreen.
   */
  SplashScreen mySplashScreen;

  /**
   * A handle back to the graphical components.
   */
  DungeonCanvas myDungeonCanvas;

  /**
   * A handle back to the midlet object.
   */
  Dungeon myDungeon;

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
  GameThread(SplashScreen ss, DungeonCanvas canvas, 
      Dungeon dungeon) {
    mySplashScreen = ss;
    myDungeonCanvas = canvas;
    myDungeon = dungeon;
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
    try {
      // first initialize the custom data while the 
      // simple splashscreen is on the screen:
      myDungeonCanvas.getManager().init();
      // once the initialization is complete, 
      // play the opening animation:
      while(mySplashScreen.advance()) {
        mySplashScreen.repaint();
        synchronized(this) {
          try {
            wait(getWaitTime());
          } catch(Exception e) {}
        }
      }
    } catch(Exception e) {
      myDungeon.errorMsg(e);
    }
    // wait a full second at the end of the 
    // animation to show the final image:
    synchronized(this) {
      try {
        wait(1000);
      } catch(Exception e) {}
    }
    // since the splashscreen is done, let go 
    // of the data:
    mySplashScreen = null;
    // now the actual game begins
    // flush any keystrokes that occurred before the 
    // game started:
    try {
      myDungeonCanvas.start();
    } catch(Exception e) {
      myDungeon.errorMsg(e);
    }
    myDungeonCanvas.flushKeys();
    myShouldStop = false;
    myShouldPause = false;
    // this is the main animation loop of the 
    // game which advances the animation 
    // and checks for keystrokes:
    while(true) {
      if(myShouldStop) {
        break;
      }
      myLastRefreshTime = System.currentTimeMillis();
      myDungeonCanvas.checkKeys();
      myDungeonCanvas.updateScreen();
      // pause to make sure not more than 20 frames 
      // per second are shown:
      synchronized(this) {
        try {
          wait(getWaitTime());
        } catch(Exception e) {}
      }
      // don't advance while the game is paused:
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
