package net.frog_parrot.dungeon;

import java.util.Vector;
import java.io.IOException;
import javax.microedition.lcdui.*;
import javax.microedition.lcdui.game.*;

import net.frog_parrot.util.*;

/**
 * This class is the display of the game.
 * 
 * @author Carol Hamer
 */
public class DungeonCanvas extends GameCanvas 
    implements CommandListener {

  //---------------------------------------------------------
  //   dimension fields
  //  (constant after initialization)

  /**
   * the top corner x coordinate according to this 
   * object's coordinate system:.
   */
  static int CORNER_X = 0;

  /**
   * the top corner y coordinate according to this 
   * object's coordinate system:.
   */
  static int CORNER_Y = 0;

  /**
   * the width of the portion of the screen that this 
   * canvas can use.
   */
  static int DISP_WIDTH;

  /**
   * the height of the portion of the screen that this 
   * canvas can use.
   */
  static int DISP_HEIGHT;

  /**
   * the height of the font used for this game.
   */
  static int FONT_HEIGHT;

  /**
   * the font used for this game.
   */
  static Font FONT;

  /**
   * color constant
   */
  public static final int BLACK = 0;

  /**
   * color constant
   */
  public static final int WHITE = 0xffffffff;

  /**
   * color constant
   */
  public static final int OPAQUE_BLACK = 0xff000000;

  /**
   * color constant
   */
  public static final int OPAQUE_BLUE = 0xff0000ff;

  //---------------------------------------------------------
  //   game object fields

  /**
   * a handle to the display.
   */
  Display myDisplay;

  /**
   * a handle to the MIDlet object (to keep track of buttons).
   */
  Dungeon myDungeon;

  /**
   * the LayerManager that handles the game graphics.
   */
  DungeonManager myManager;

  /**
   * the Customizer.
   */
  Customizer myCustomizer;

  /**
   * whether or not the game has ended.
   */
  static boolean myGameOver;

  /**
   * The number of ticks on the clock the last time the 
   * time display was updated.
   * This is saved to determine if the time string needs 
   * to be recomputed.
   */
  int myDisplayGameTicks = 0;

  /**
   * the number of game ticks that have passed since the 
   * beginning of the game.
   */
  int myGameTicks = myDisplayGameTicks;

  /**
   * An array of number sprites to hold the digit images
   * for the time display.
   */
  Sprite[] myNumberSprites = new Sprite[5];

  /**
   * The button to exit the game.
   */
  Command myExitCommand;

  /**
   * The button to display the command menu.
   */
  Command myMenuCommand;

  /**
   * The button to go to the next board.
   */
  Command myOkCommand;

  //---------------------------------------------------------
  //   menu-related fields

  /**
   * Whether the menu is currently displayed.
   */
  boolean myMenuMode;

  /**
   * The index (in the menu vector) of the currently focused
   * command.
   */
  int myFocusedIndex;

  /**
   * The images to use for the current menu items.
   */
  Vector myMenuVector = new Vector(5);

  /**
   * The space between menu items.
   */
  static int MENU_BUFFER;

  /**
   * The animated sprite that indicates the selected item 
   * in the menu.
   */
  Sprite myStars;

  /**
   * Menu sprite constant.
   */
  int FOCUSED = 0;

  /**
   * Menu sprite constant.
   */
  int UNFOCUSED = 1;

  /**
   * a menu image.
   */
  Sprite myNext;

  /**
   * a menu image.
   */
  Sprite myRestore;

  /**
   * a menu image.
   */
  Sprite mySave;

  /**
   * a softkey image.
   */
  Image myExit;

  /**
   * a softkey image.
   */
  Image myMenu;

  /**
   * a softkey image.
   */
  Image myOk;

  //-----------------------------------------------------
  //    gets/sets

  /**
   * This is called when the game ends.
   */
  void setGameOver() {
    myGameOver = true;
    myDungeon.pauseApp();
    if(!myCustomizer.useSoftkeys()) {
      removeCommand(myMenuCommand);
      addCommand(myOkCommand);
    }
  }

  /**
   * Get the DungeonManager.
   */
  DungeonManager getManager() {
    return myManager;
  }

  /**
   * Find out if the game has ended.
   */
  static boolean getGameOver() {
    return(myGameOver);
  }

  /**
   * Get the Customizer.
   */
  public Customizer getCustomizer() {
    return myCustomizer;
  }

  //-----------------------------------------------------
  //    initialization and game state changes

  /**
   * Constructor sets the data, performs dimension calculations, 
   * and creates the graphical objects.
   */
  public DungeonCanvas(Dungeon midlet) throws Exception {
    super(false);
    myDisplay = Display.getDisplay(midlet);
    myDungeon = midlet;
    // calculate the dimensions based on the full screen
    setFullScreenMode(true);
    DISP_WIDTH = getWidth();
    DISP_HEIGHT = getHeight();
    if((!myDisplay.isColor()) || (myDisplay.numColors() < 256)) {
      throw(new Exception("game requires full-color screen"));
    }
    if((DISP_WIDTH < 150) || (DISP_HEIGHT < 170)) {
      throw(new Exception("Screen too small"));
    }
    if((DISP_WIDTH > 375) || (DISP_HEIGHT > 375)) {
      throw(new Exception("Screen too large"));
    }
    // create the class that handles the differences among
    // the various platforms.
    myCustomizer = new Customizer(DISP_WIDTH, 
        DISP_HEIGHT);
    // create the LayerManager (where all of the interesting 
    // graphics go!) and give it the dimensions of the 
    // region it is supposed to paint:
    if(myManager == null) {
      myManager = new DungeonManager(CORNER_X, CORNER_Y, 
      DISP_WIDTH, DISP_HEIGHT, myCustomizer, this);
    } 
  }

  /**
   * Once the customizer has been initialized, this 
   * method loads and initializes the graphical objects 
   * for the timer and the menu.  
   */
  void start() throws IOException {
    myGameOver = false;
    // initialize the graphics for the timeclock:
    Image numberImage = myManager.getNumberImage();
    int width = numberImage.getWidth() / 11;
    int height = numberImage.getHeight();
    for(int i = 0; i < 5; i++) {
      myNumberSprites[i] = new Sprite(numberImage, width, height);
      myNumberSprites[i].setPosition(width*i, 0);
    }
    // frame 10 is the colon:
    myNumberSprites[2].setFrame(10);
    // if the customizer identifies the platform as 
    // one we have keycode data for, we can implement
    // the softkeys with images
    if(myCustomizer.useSoftkeys()) {
      setFullScreenMode(true);
      DISP_WIDTH = getWidth();
      DISP_HEIGHT = getHeight();
      myExit = myCustomizer.getLabelImage("exit");
      myMenu = myCustomizer.getLabelImage("menu");
      myOk = myCustomizer.getLabelImage("ok");
    } else {
      // if the customizer doesn't have keycodes 
      // for the current platform, then lcdui 
      // commands must be used:
      setFullScreenMode(false);
      myExitCommand = new Command(myCustomizer.getLabel("exit"), 
          Command.EXIT, 99);
      addCommand(myExitCommand);
      myMenuCommand = new Command(myCustomizer.getLabel("menu"), 
          Command.SCREEN, 1);
      addCommand(myMenuCommand);
      myOkCommand = new Command(myCustomizer.getLabel("ok"), 
          Command.SCREEN, 1);
      setCommandListener(this);
    }
    // Now that the timer and softkeys are ready, 
    // this screen can be displayed (since the menu is 
    // not shown initially)
    myDisplay.setCurrent(this);
    // initialize the menu graphics:
    MENU_BUFFER = myCustomizer.getInt("menu.buffer");
    // stars gives a sparkling animation shown 
    // behind the selected menu item:
    Image stars = myCustomizer.getImage("stars");
    width = stars.getWidth();
    height = stars.getHeight() / 3;
    myStars = new Sprite(stars, width, height);
    myStars.defineReferencePixel(width/2, 0);
    // now load the images of the menu choices
    // make sprites with selected and unselected 
    // versions of the image and add them 
    // to the menu vector:
    myNext = menuSprite("next");
    myRestore = menuSprite("restore");
    mySave = menuSprite("save");

    myMenuVector.addElement(myNext);
    myMenuVector.addElement(mySave);
    myMenuVector.addElement(myRestore);
  }

  /**
   * Internal to start.
   * 
   * Creates and initializes a menu item Sprite.
   */
  private Sprite menuSprite(String key) throws IOException {
    Image tempImage = myCustomizer.getLabelImage(key);
    int width = tempImage.getWidth();
    int height = tempImage.getHeight();
    Sprite retObj = new Sprite(ColorChanger.createFocused(tempImage, 
        OPAQUE_BLACK, OPAQUE_BLUE), width, height);
    retObj.defineReferencePixel(width/2, height/2);
    return(retObj);
  }

  /**
   * sets all variables back to their initial positions.
   */
  void reset() throws Exception {
    // most of the variables that need to be reset 
    // are held by the LayerManager:
    myManager.reset();
    myGameOver = false;
  }

  /**
   * sets all variables back to the positions 
   * from a previously saved game.
   */
  void revertToSaved() throws Exception {
    // most of the variables that need to be reset 
    // are held by the LayerManager, so we 
    // prompt the LayerManager to get the 
    // saved data:
    myGameOver = false;
    myDisplayGameTicks = myManager.revertToSaved();
  }

  /**
   * save the current game in progress.
   */
  void saveGame() throws Exception {
    myManager.saveGame(myDisplayGameTicks);
  }

  /**
   * clears the key states.
   */
  void flushKeys() {
    getKeyStates();
  }

  /**
   * Switch to showing the game action menu.
   */
  void setMenuMode() {
    myMenuMode = !myMenuMode;
  }

  /**
   * If the game is hidden by another app (or a menu)
   * ignore it since not much happens in this game 
   * when the user is not actively interacting with it.
   */
  protected void hideNotify() {
  }

  /**
   * There's nothing to do when it comes back into 
   * view either.
   */
  protected void showNotify() {
  }

  //-------------------------------------------------------
  //  graphics methods

  /**
   * paint the game graphics on the screen.
   */
  public void paint(Graphics g) {
    // The LayerManager paints the 
    // interesting part of the graphics:
    try {
      myManager.paint(g);
    } catch(Exception e) {
      myDungeon.errorMsg(e);
      return;
    }
    // the timer is painted on top of 
    // the game graphics:
    for(int i = 0; i < 5; i++) {
      myNumberSprites[i].paint(g);
    }
    // paint the menu on if in menu mode:
    if(myMenuMode) {
      int y = MENU_BUFFER;
      for(int i = 0; i < myMenuVector.size(); i++) {
        Sprite item = (Sprite)(myMenuVector.elementAt(i));
        if(i == myFocusedIndex) {
          myStars.setRefPixelPosition(DISP_WIDTH / 2, y);
          myStars.paint(g);
          item.setFrame(FOCUSED);
        } else {
          item.setFrame(UNFOCUSED);
        }
        y += myStars.getHeight()/2;
        item.setRefPixelPosition(DISP_WIDTH / 2, y);
        item.paint(g);
        y += myStars.getHeight()/2;
        y += MENU_BUFFER;
      }
    }
    if(myCustomizer.useSoftkeys()) {
      g.drawImage(myExit, 2, DISP_HEIGHT - 2, 
            Graphics.BOTTOM|Graphics.LEFT);
      if(myGameOver) {
        g.drawImage(myOk, DISP_WIDTH - 2, DISP_HEIGHT - 2, 
            Graphics.BOTTOM|Graphics.RIGHT);
      } else {
        g.drawImage(myMenu, DISP_WIDTH - 2, DISP_HEIGHT - 2, 
            Graphics.BOTTOM|Graphics.RIGHT);
      }
    }
    // write "Next Board" when the user finishes a board:
    if(myGameOver) {
      myNext.setFrame(UNFOCUSED);
      myNext.setRefPixelPosition(DISP_WIDTH / 2, DISP_HEIGHT / 2);
      myNext.paint(g);
    }
  }

  /**
   * a simple utility to make the number of ticks look 
   * like a time...
   */
  public void setTimeSprites() {
    // we advance the display ticks once 
    // for every twenty game ticks because
    // there are twenty frames per second:
    if(myGameTicks % 20 == 0) {
      // the number sprite is designed so that 
      // the frame number corresponds to the 
      // actual digit:
      myDisplayGameTicks++;
      int smallPart = myDisplayGameTicks % 60;
      myNumberSprites[3].setFrame(smallPart / 10);
      myNumberSprites[4].setFrame(smallPart % 10);
      int bigPart = myDisplayGameTicks / 60;
      myNumberSprites[0].setFrame((bigPart / 10) % 10);
      myNumberSprites[1].setFrame(bigPart % 10);
    }
  }

  //-------------------------------------------------------
  //  game movements and commands

  /**
   * update the display.
   */
  void updateScreen() {
    if(! myMenuMode) {
      myGameTicks++;
      setTimeSprites();
    } else {
      // in menu mode the game doesn't advance 
      // but the sparking animation behind the 
      // selected item must advance:
      myStars.nextFrame();
    }
    // paint the display:
    try {
      paint(getGraphics());
      flushGraphics(CORNER_X, CORNER_Y, DISP_WIDTH, DISP_HEIGHT);
    } catch(Exception e) {
      myDungeon.errorMsg(e);
    }
  }

  /**
   * Respond to keystrokes.
   */
  public void checkKeys() { 
    if(! myGameOver) {
      // determine which moves the user would like to make:
      int keyState = getKeyStates();
      if(myMenuMode) {
        menuAction(keyState);
      } else {
        int vertical = 0;
        int horizontal = 0;
        if((keyState & LEFT_PRESSED) != 0) {
          horizontal = -1;
        } 
        if((keyState & RIGHT_PRESSED) != 0) {
          horizontal = 1;
        }
        if((keyState & UP_PRESSED) != 0) {
          vertical = -1;
        } 
        if((keyState & DOWN_PRESSED) != 0) {
          // if the user presses the down key, 
          // we put down or pick up a key object
          // or pick up the crown:
          myManager.putDownPickUp();
        } 
        // tell the manager to move the player 
        // accordingly if possible:
        myManager.requestMove(horizontal, vertical);
      }
    }
  }

  /**
   * Respond to keystrokes on the menu.
   */
  public void menuAction(int keyState) { 
    try {
      if((keyState & FIRE_PRESSED) != 0) {
        Sprite selected = (Sprite)(myMenuVector.elementAt(myFocusedIndex));
        if(selected == myNext) {
          reset();
          myDungeon.resumeGame();
        } else if(selected == myRestore) {
          revertToSaved();
        } else if(selected == mySave) {
          saveGame();
        }
        myMenuMode = false;
      }
      // change which item is selected in 
      // response to up and down:
      if((keyState & UP_PRESSED) != 0) {
        if(myFocusedIndex > 0) {
          myFocusedIndex--;
        }
      } 
      if((keyState & DOWN_PRESSED) != 0) {
        if((myFocusedIndex + 1) < myMenuVector.size()) {
          myFocusedIndex++;
        }
      }
    } catch(Exception e) {
      myDungeon.errorMsg(e);
    }
  }

  /**
   * Respond to softkeys.
   * The keystates value won't give information 
   * about softkeys, so the keypressed method
   * must be implemented separately:
   */
  public void keyPressed(int keyCode) {
    int softkey = myCustomizer.whichSoftkey(keyCode);
    if(softkey == Customizer.SOFT_LEFT) {
      // left is exit:
      myDungeon.quit();
    } else if(softkey == Customizer.SOFT_RIGHT) {
      // right either pops the menu up and down
      // or advances to the next board if a board
      // is done:
      try {
        if(myGameOver) {
          reset();
          flushKeys();
          myDungeon.resumeGame();
        } else {
          setMenuMode();
        }
      } catch(Exception e) {
        myDungeon.errorMsg(e);
      }
    }
  }

  /**
   * Respond to softkeys in the case where 
   * lcdui commands are used instead of custom
   * graphical softkeys.
   */
  public void commandAction(Command c, Displayable s) {
    try {
      if(c == myMenuCommand) {
        setMenuMode();
      } else if(c == myOkCommand) {
        removeCommand(myOkCommand);
        addCommand(myMenuCommand);
        reset();
        flushKeys();
        myDungeon.resumeGame();
      } else if(c == myExitCommand) {
        myDungeon.quit();
      }
    } catch(Exception e) {
      myDungeon.errorMsg(e);
    }
  }

}
