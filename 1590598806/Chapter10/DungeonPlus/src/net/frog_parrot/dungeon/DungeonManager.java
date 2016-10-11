package net.frog_parrot.dungeon;

import javax.microedition.lcdui.*;
import javax.microedition.lcdui.game.*;

import net.frog_parrot.util.*;

/**
 * This class handles the graphics objects.
 * 
 * @author Carol Hamer
 */
public class DungeonManager extends LayerManager {

  //---------------------------------------------------------
  //   dimension fields
  //  (constant after initialization)

  /**
   * The x-coordinate of the place on the game canvas where 
   * the LayerManager window should appear, in terms of the 
   * coordiantes of the game canvas.
   */
  static int CANVAS_X;

  /**
   * The y-coordinate of the place on the game canvas where 
   * the LayerManager window should appear, in terms of the 
   * coordiantes of the game canvas.
   */
  static int CANVAS_Y;

  /**
   * The width of the display window.
   */
  static int DISP_WIDTH;

  /**
   * The height of this object's visible region. 
   */
  static int DISP_HEIGHT;

  /**
   * the (right or left) distance the player 
   * goes in a single keystroke.
   */
  static int MOVE_LENGTH;

  /**
   * the minimum (right or left) distance the player 
   * must stay away from the walls (to avoid getting 
   * stuck when the sprite image changes).
   */
  static int MOVE_BUFFER;

  /**
   * The width of the square tiles that this game is divided into.
   * This is the width of the stone walls as well as the princess.
   */
  static int SQUARE_WIDTH;

  /**
   * The number of background tiles per row.
   */
  static int BACK_TILES;

  /**
   * A constant number of pixels to use in calculating the 
   * height of a jump.
   */
  static int JUMP_INT;

  /**
   * A constant number of pixels to use in calculating the 
   * height of a jump.
   */
  static int JUMP_FRAC_NUM;

  /**
   * A constant number of pixels to use in calculating the 
   * height of a jump.
   */
  static int JUMP_FRAC_DENOM;

  /**
   * The jump index that indicates that no jump is 
   * currently in progress..
   */
  static final int NO_JUMP = -6;

  /**
   * The maximum speed for the player's fall.
   */
  static final int MAX_FREE_FALL = 3;

  /**
   * The maximum horizontal running speed.
   */
  static final int MAX_SPEED = 3;

  //---------------------------------------------------------
  //   game object fields

  /**
   * the handle back to the canvas.
   */
  DungeonCanvas myCanvas;

  /**
   * the class that handles the differences from one handset to 
   * another.
   */
  Customizer myCustomizer;

  /**
   * the walls of the dungeon.
   */
  TiledLayer myWalls;

  /**
   * the tiled layer that goes behind the walls.
   */
  TiledLayer myBackground;

  /**
   * the player.
   */
  Sprite myPrincess;

  /**
   * the goal.
   */
  Sprite myCrown;

  /**
   * the image to construct the doors and keys.
   * cached because it is used more than once.
   */
  Image myKeyImage;

  /**
   * the image to construct the numbers.
   * cached because it is used more than once.
   */
  Image myNumberImage;

  /**
   * the doors.
   */
  DoorKey[] myDoors;

  /**
   * the keys.
   */
  DoorKey[] myKeys;

  /**
   * the key currently held by the player.
   */
  DoorKey myHeldKey;

  /**
   * The leftmost x-coordinate that should be visible on the 
   * screen in terms of this objects internal coordinates.
   */
  int myViewWindowX;

  /**
   * The top y-coordinate that should be visible on the 
   * screen in terms of this objects internal coordinates.
   */
  int myViewWindowY;

  /**
   * Where the princess is in the jump sequence.
   */
  int myIsJumping = NO_JUMP;

  /**
   * Where the princess is moving horizontally.
   */
  int myIsRunning = 0;

  /**
   * Which board we're playing on.
   */
  int myCurrentBoardNum = 0;

  /**
   * Whether the menu is currently being displayed.
   */
  boolean myMenuMode;

  //-----------------------------------------------------
  //    gets/sets

  /**
   * .
   */
  public Image getNumberImage() {
    return myNumberImage;
  }

  //-----------------------------------------------------
  //    initialization
  //    set up or save game data.

  /**
   * Constructor merely sets the data.
   * @param x The x-coordinate of the place on the game canvas where 
   * the LayerManager window should appear, in terms of the 
   * coordiantes of the game canvas.
   * @param y The y-coordinate of the place on the game canvas where 
   * the LayerManager window should appear, in terms of the 
   * coordiantes of the game canvas.
   * @param width the width of the region that is to be 
   * occupied by the LayoutManager.
   * @param height the height of the region that is to be 
   * occupied by the LayoutManager.
   * @param customizer the object that loads the correct 
   * custom data for the current platform.
   * @param canvas the DungeonCanvas that this LayerManager 
   * should appear on.
   */
  public DungeonManager(int x, int y, int width, int height, 
      Customizer customizer, DungeonCanvas canvas) {
    myCustomizer = customizer;
    myCanvas = canvas;
    CANVAS_X = x;
    CANVAS_Y = y;
    DISP_WIDTH = width;
    DISP_HEIGHT = height;
  }

  /**
   * Set up all of the data.
   * 
   * This is called from a separate init method in order
   * to limit the amount of resource loading that is done 
   * by the thread that called the startApp method.
   */
  public void init() throws Exception {
    myCustomizer.init();
    MOVE_LENGTH = myCustomizer.getInt("move.length");
    MOVE_BUFFER = myCustomizer.getInt("move.buffer");
    SQUARE_WIDTH = myCustomizer.getInt("square.width");
    BACK_TILES = myCustomizer.getInt("back.tiles");
    JUMP_INT = myCustomizer.getInt("jump.int");
    JUMP_FRAC_NUM = myCustomizer.getInt("jump.frac.numerator");
    JUMP_FRAC_DENOM = myCustomizer.getInt("jump.frac.denominator");
    // create a decoder object that creates the dungeon and 
    // its associated Sprites from data.  
    BoardDecoder decoder = new BoardDecoder(myCurrentBoardNum, 
        myCustomizer);
    // get the dungeon walls layer:
    myWalls = decoder.getLayer();
    // the background behind the walls is a single image,
    // so the easiest way to add it to the layer manager
    // is to make it a sprite:
    Image bi = myCustomizer.getImage("background");
    myBackground = new TiledLayer(BACK_TILES, BACK_TILES, 
        bi, bi.getWidth(), bi.getHeight());
    // set all cells to use tile 1 instead of the default
    // (blank) tile 0:
    myBackground.fillCells(0, 0, BACK_TILES, BACK_TILES, 1);
    // get the coordinates of the square that the princess 
    // starts on.
    int[] playerCoords = decoder.getPlayerSquare();
    // create the player sprite
    myPrincess = new Sprite(myCustomizer.getImage("princess"), 
                            SQUARE_WIDTH, SQUARE_WIDTH);
    myPrincess.setFrame(1);
    // we define the reference pixel to be in the middle 
    // of the princess image so that when the princess turns 
    // from right to left (and vice versa) she does not 
    // appear to move to a different location.
    myPrincess.defineReferencePixel(SQUARE_WIDTH/2, 0);
    // the dungeon is a 16x16 grid, so the array playerCoords
    // gives the player's location in terms of the grid, and 
    // then we multiply those coordinates by the SQUARE_WIDTH
    // to get the precise pixel where the player should be 
    // placed (in terms of the LayerManager's coordinate system)
    myPrincess.setPosition(SQUARE_WIDTH * playerCoords[0], 
                           SQUARE_WIDTH * playerCoords[1]);
    // we append all of the Layers (TiledLayer and Sprite) 
    // so that this LayerManager will paint them when 
    // flushGraphics is called.
    append(myPrincess);
    // get the coordinates of the square where the crown 
    // should be placed.
    int[] goalCoords = decoder.getGoalSquare();
    Image crownImage = myCustomizer.getImage("crown");
    myCrown = new Sprite(crownImage);
    myCrown.defineReferencePixel(crownImage.getWidth()/2, 
        crownImage.getHeight());
    myCrown.setRefPixelPosition(
        (SQUARE_WIDTH * goalCoords[0]) + (SQUARE_WIDTH/2), 
        (SQUARE_WIDTH * goalCoords[1]) + SQUARE_WIDTH);
    append(myCrown);
    // The decoder creates the door and key sprites and places 
    // them in the correct locations in terms of the LayerManager's
    // coordinate system.
    myNumberImage = myCustomizer.getImage("numbers");
    myKeyImage = myCustomizer.getImage("keys");
    myDoors = decoder.createDoors(myKeyImage);
    myKeys = decoder.createKeys(myKeyImage);
    for(int i = 0; i < myDoors.length; i++) {
      append(myDoors[i]);
    }
    for(int i = 0; i < myKeys.length; i++) {
      append(myKeys[i]);
    }
    // append the background last so it will be painted first.
    append(myWalls);
    append(myBackground);
    // this sets the view screen so that the player is 
    // in the center.
    myViewWindowX = SQUARE_WIDTH * playerCoords[0] 
      - ((DISP_WIDTH - SQUARE_WIDTH)/2);
    myViewWindowY = SQUARE_WIDTH * playerCoords[1] 
      - ((DISP_HEIGHT - SQUARE_WIDTH)/2);
    // a number of objects are created in order to set up the game,
    // but they should be eliminated to free up memory:
    decoder = null;
    System.gc();
  }

  /**
   * sets all variables back to their initial positions.
   */
  void reset() throws Exception {
    // first get rid of the old board:
    for(int i = 0; i < myDoors.length; i++) {
      remove(myDoors[i]);
    }
    myHeldKey = null;
    for(int i = 0; i < myKeys.length; i++) {
      remove(myKeys[i]);
    }
    remove(myBackground);
    remove(myWalls);
    // now create the new board:
    myCurrentBoardNum++;
    // in this version we go back to the beginning if 
    // all boards have been completed.
    if(myCurrentBoardNum >= BoardReader.getNumBoards() 
        + BoardDecoder.getNumDefaultBoards()) {
      myCurrentBoardNum = 0;
    }
    // we create a new decoder object to read and interpret 
    // all of the data for the current board.
    BoardDecoder decoder = new BoardDecoder(myCurrentBoardNum, 
        myCustomizer);
    // get the background TiledLayer
    myWalls = decoder.getLayer();
    // get the coordinates of the square that the princess 
    // starts on.
    int[] playerCoords = decoder.getPlayerSquare();
    // the dungeon is a 16x16 grid, so the array playerCoords
    // gives the player's location in terms of the grid, and 
    // then we multiply those coordinates by the SQUARE_WIDTH
    // to get the precise pixel where the player should be 
    // placed (in terms of the LayerManager's coordinate system)
    myPrincess.setPosition(SQUARE_WIDTH * playerCoords[0], 
                           SQUARE_WIDTH * playerCoords[1]);
    myPrincess.setFrame(1);
    // get the coordinates of the square where the crown 
    // should be placed.
    int[] goalCoords = decoder.getGoalSquare();
    myCrown.setPosition((SQUARE_WIDTH * goalCoords[0]) + (SQUARE_WIDTH/4), 
                        (SQUARE_WIDTH * goalCoords[1]) + (SQUARE_WIDTH/2));
    // The decoder creates the door and key sprites and places 
    // them in the correct locations in terms of the LayerManager's
    // coordinate system.
    myDoors = decoder.createDoors(myKeyImage);
    myKeys = decoder.createKeys(myKeyImage);
    for(int i = 0; i < myDoors.length; i++) {
      append(myDoors[i]);
    }
    for(int i = 0; i < myKeys.length; i++) {
      append(myKeys[i]);
    }
    // append the background last so it will be painted first.
    append(myWalls);
    append(myBackground);
    // this sets the view screen so that the player is 
    // in the center.
    myViewWindowX = SQUARE_WIDTH * playerCoords[0] 
      - ((DISP_WIDTH - SQUARE_WIDTH)/2);
    myViewWindowY = SQUARE_WIDTH * playerCoords[1] 
      - ((DISP_HEIGHT - SQUARE_WIDTH)/2);
    // a number of objects are created in order to set up the game,
    // but they should be eliminated to free up memory:
    decoder = null;
    System.gc();
  }

  /**
   * sets all variables back to the position in the saved game.
   * @return the time on the clock of the saved game.
   */
  int revertToSaved() throws Exception {
    int retVal = 0;
    // first get rid of the old board:
    for(int i = 0; i < myDoors.length; i++) {
      remove(myDoors[i]);
    }
    myHeldKey = null;
    for(int i = 0; i < myKeys.length; i++) {
      remove(myKeys[i]);
    }
    remove(myBackground);
    remove(myWalls);
    // now get the info of the saved game
    // only one game is saved at a time, and the GameInfo object 
    // will read the saved game's data from memory.
    GameInfo info = new GameInfo();
    if(info.getIsEmpty()) {
      // if no game has been saved, we start from the beginning.
      myCurrentBoardNum = 0;
      reset();
    } else {
      // get the time on the clock of the saved game.
      retVal = info.getTime();
      // get the number of the board the saved game was on.
      myCurrentBoardNum = info.getBoardNum();
      // create the BoradDecoder that gives the data for the 
      // desired board.
      BoardDecoder decoder = new BoardDecoder(myCurrentBoardNum, 
          myCustomizer);
      // get the background TiledLayer
      myWalls = decoder.getLayer();
      //myNegative = decoder.getNegative();
      // get the coordinates of the square that the princess 
      // was on in the saved game.
      int[] playerCoords = info.getPlayerSquare();
      myPrincess.setPosition(SQUARE_WIDTH * playerCoords[0], 
                             SQUARE_WIDTH * playerCoords[1]);
      myPrincess.setFrame(1);
      // get the coordinates of the square where the crown 
      // should be placed (this is given by the BoardDecoder 
      // and not from the data of the saved game because the 
      // crown does not move during the game.
      int[] goalCoords = decoder.getGoalSquare();
      myCrown.setPosition((SQUARE_WIDTH * goalCoords[0]) + (SQUARE_WIDTH/4), 
                          (SQUARE_WIDTH * goalCoords[1]) + (SQUARE_WIDTH/2));
      // The decoder creates the door and key sprites and places 
      // them in the correct locations in terms of the LayerManager's
      // coordinate system.
      myDoors = decoder.createDoors(myKeyImage);
      myKeys = decoder.createKeys(myKeyImage);
      // get an array of ints that lists whether each door is 
      // open or closed in the saved game
      int[] openDoors = info.getDoorsOpen();
      for(int i = 0; i < myDoors.length; i++) {
        append(myDoors[i]);
        if(openDoors[i] == 0) {
          // if the door was open, make it invisible
          myDoors[i].setVisible(false);
        }
      }
      // the keys can be moved by the player, so we get their 
      // coordinates from the GameInfo saved data.
      int[][] keyCoords = info.getKeyCoords();
      for(int i = 0; i < myKeys.length; i++) {
        append(myKeys[i]);
        myKeys[i].setPosition(SQUARE_WIDTH * keyCoords[i][0], 
                             SQUARE_WIDTH * keyCoords[i][1]);
      }
      // if the player was holding a key in the saved game, 
      // we have the player hold that key and set it to invisible.
      int heldKey = info.getHeldKey();
      if(heldKey != -1) {
        myHeldKey = myKeys[heldKey];
        myHeldKey.setVisible(false);
      }
      // append the background last so it will be painted first.
      append(myWalls);
      append(myBackground);
      // this sets the view screen so that the player is 
      // in the center.
      myViewWindowX = SQUARE_WIDTH * playerCoords[0] 
        - ((DISP_WIDTH - SQUARE_WIDTH)/2);
      myViewWindowY = SQUARE_WIDTH * playerCoords[1] 
        - ((DISP_HEIGHT - SQUARE_WIDTH)/2);
      // a number of objects are created in order to set up the game,
      // but they should be eliminated to free up memory:
      decoder = null;
      System.gc();
    }
    return(retVal);
  }

  /**
   * save the current game in progress.
   */
  void saveGame(int gameTicks) throws Exception {
    int[] playerSquare = new int[2];
    // the coordinates of the player are given in terms of 
    // the 16 x 16 dungeon grid. We divide the player's 
    // pixel coordinates to ge the right grid square.
    // If the player was not precisely alligned with a
    // grid square when the game was saved, the difference 
    // will be shaved off.
    playerSquare[0] = myPrincess.getX()/SQUARE_WIDTH;
    playerSquare[1] = myPrincess.getY()/SQUARE_WIDTH;  
    // save the coordinates of the current locations of
    // the keys, and if a key is currently held by the 
    // player, we save the info of which one it was.  
    int[][] keyCoords = new int[4][];
    int heldKey = -1;
    for(int i = 0; i < myKeys.length; i++) {
      keyCoords[i] = new int[2];
      keyCoords[i][0] = myKeys[i].getX()/SQUARE_WIDTH;
      keyCoords[i][1] = myKeys[i].getY()/SQUARE_WIDTH;
      if((myHeldKey != null) && (myKeys[i] == myHeldKey)) {
        heldKey = i;
      }
    }
    // save the information of which doors were open.
    int[] doorsOpen = new int[8];
    for(int i = 0; i < myDoors.length; i++) {
      if(myDoors[i].isVisible()) {
        doorsOpen[i] = 1;
      }
    }
    // take all of the information we've gathered and 
    // create a GameInfo object that will save the info 
    // in the device's memory.
    GameInfo info = new GameInfo(myCurrentBoardNum, gameTicks, 
                                 playerSquare, keyCoords, 
                                 doorsOpen, heldKey);
  }

  //-------------------------------------------------------
  //  graphics methods

  /**
   * paint the game graphic on the screen.
   */
  public void paint(Graphics g) throws Exception {
    g.setColor(DungeonCanvas.WHITE);
    // paint the background white to cover old game objects
    // that have changed position since last paint.
    // here coordinates are given 
    // with respect to the graphics (canvas) origin:
    g.fillRect(0, 0, DISP_WIDTH, DISP_HEIGHT);
    // here coordinates are given 
    // with respect to the LayerManager origin:
    setViewWindow(myViewWindowX, myViewWindowY, DISP_WIDTH, DISP_HEIGHT);
    // call the paint funstion of the superclass LayerManager
    // to paint all of the Layers
    paint(g, CANVAS_X, CANVAS_Y);
  }

  //-------------------------------------------------------
  //  game movements

  /**
   * respond to keystrokes by deciding where to move 
   * and then moving the pieces and the view window correspondingly.
   */
  void requestMove(int hdirection, int vdirection) { 
    // vdirection < 0 indicates that the user has 
    // pressed the UP button and would like to jump.
    // therefore, if we're not currently jumping, 
    // we begin the jump.
    if((myIsJumping == NO_JUMP) && (vdirection < 0)) {
      myIsJumping++;
    } else if(myIsJumping == NO_JUMP) {
      // if we're not jumping at all, we need to check 
      // if the princess should be falling:  
      // we (temporarily) move the princess down and see if that 
      // causes a collision with the floor:
      myPrincess.move(0, 1);
      // if the princess can move down without colliding 
      // with the floor, then we set the princess to 
      // be falling.  The variable myIsJumping starts 
      // negative while the princess is jumping up and 
      // is zero or positive when the princess is coming 
      // back down.  We therefore set myIsJumping to 
      // zero to indicate that the princess should start 
      // falling.
      if(! checkCollision()) {
        myIsJumping = 0;
      } 
      // we move the princess Sprite back to the correct 
      // position she was at before we (temporarily) moved 
      // her down to see if she would fall.
      myPrincess.move(0, -1);
    }
    // if the princess is currently jumping or falling, 
    // advance the jump (change the vertical distance
    // the princess is supposed to move)
    if((myIsJumping <= MAX_FREE_FALL) && (myIsJumping != NO_JUMP)) {
      myIsJumping++;
    }
    // also accellerate the horizontal motion if the princess
    // runs runs in the same horizontal direction for more than 
    // one game tick:
    myIsRunning++;
    // But don't accellerate past the maximum speed:
    if(myIsRunning > MAX_SPEED) {
      myIsRunning = MAX_SPEED;
    }
    int horizontal = MOVE_LENGTH * myIsRunning;
    // if the princess is currently jumping or falling, 
    // we calculate the vertical distance she should move.
    // This is a parabola given by y = (x*x) * (a + b/c)
    // where x is how far we have advanced in the jump 
    // or fall (myIsJumping), and a, b, and c are constants
    // based on the screen size. (The actual values are
    // read from a properties file and were originally 
    // computed through trial and error.)
    int vertical = 0;
    if(myIsJumping != NO_JUMP) {
      vertical = myIsJumping * myIsJumping * JUMP_INT
        + (myIsJumping * myIsJumping * JUMP_FRAC_NUM) 
            / JUMP_FRAC_DENOM;
      // for the first half of the jump we go up, 
      // then for the second half go down:
      if(myIsJumping < 0) {
        vdirection = -1;
      } else {
        vdirection = 1;
      }
    }
    // set the sprite to the correct frame based 
    // on the princess's current motion:
    updateSprite(hdirection, vdirection);
    boolean vcrash = false;
    boolean hcrash = false;
    // now calculate the motion one pixel at a time
    // (vertically then horizontally) to see precisely
    // how far the princess can move in each of the
    // requested directions:
    while((vertical >= 1 && !vcrash) 
        || (horizontal >= 1 && !hcrash)) {
      myPrincess.move(0, vdirection);
      if(checkCollision()) {
        myPrincess.move(0, -vdirection);
        vcrash = true;
      } else {
        vertical -= 1;
        vcrash = false;
        myViewWindowY += vdirection;
      }
      myPrincess.move(MOVE_BUFFER * hdirection, 0);
      if(checkCollision()) {
        myPrincess.move(-MOVE_BUFFER * hdirection, 0);
        hcrash = true;
      } else {
        myPrincess.move(-MOVE_BUFFER * hdirection, 0);
        myPrincess.move(hdirection, 0);
        horizontal -= 1;
        hcrash = false;
        myViewWindowX += hdirection;
      }
    }
    // If the princess is blocked vertically,
    // then the jump or fall in progress stops:
    if(vcrash) {
      myIsJumping = NO_JUMP;
    }
    // If the princess is blocked horizontally,
    // forget any horizontal accelleration:
    if(hcrash) {
      myIsRunning = 0;
    }
  }

  /**
   * Internal to requestMove.  Set the princess sprite 
   * to the correct frame depending on her movements..
   */
  private void updateSprite(int hdirection, int vdirection) {
    // if the princess is moving left or right, we set 
    // her image to be facing the right direction:
    if(hdirection > 0) {
      myPrincess.setTransform(Sprite.TRANS_NONE);
    } else if(hdirection < 0) {
      myPrincess.setTransform(Sprite.TRANS_MIRROR);
    }
    // if she's jumping or falling, we set the image to 
    // the frame where the skirt is inflated:
    if(vdirection != 0) {
      myPrincess.setFrame(0);
      // if she's just running, we alternate between the 
      // two frames:
    } else if(hdirection != 0) {
      if(myPrincess.getFrame() == 1) {
        myPrincess.setFrame(0);
      } else {
        myPrincess.setFrame(1);
      }
    }
  }

  //-------------------------------------------------------
  //  sprite interactions

  /**
   * Drops the currently held key and picks up another.
   */
  void putDownPickUp() {
    // we do not want to allow the player to put 
    // down the key in the air, so we verify that 
    // we're not jumping or falling first:
    if((myIsJumping == NO_JUMP) && 
       (myPrincess.getY() % SQUARE_WIDTH == 0)) {
      // since we're picking something up or putting 
      // something down, the display changes and needs 
      // to be repainted:
      //setNeedsRepaint();
      // if the thing we're picking up is the crown, 
      // we're done, the player has won:
      if(myPrincess.collidesWith(myCrown, true)) {
        myCanvas.setGameOver();
        return;
      }
      // keep track of the key we're putting down in 
      // order to place it correctly:
      DoorKey oldHeld = myHeldKey;
      myHeldKey = null;
      // if the princess is on top of another key, 
      // that one becomes the held key and is hence 
      // made invisible:
      for(int i = 0; i < myKeys.length; i++) {
        // we check myHeldKey for null because we don't 
        // want to accidentally pick up two keys.
        if((myPrincess.collidesWith(myKeys[i], true)) && 
           (myHeldKey == null)) {
          myHeldKey = myKeys[i];
          myHeldKey.setVisible(false);
        }
      }
      if(oldHeld != null) {
        // place the key we're putting down in the Princess's
        // current position and make it visible:
        oldHeld.setPosition(myPrincess.getX(), myPrincess.getY());
        oldHeld.setVisible(true);
      }
    }
  }

  /**
   * Checks of the player hits a stone wall or a door.
   */
  boolean checkCollision() {
    boolean retVal = false;
    // the "true" arg meand to check for a pixel-level 
    // collision (so merely an overlap in image 
    // squares does not register as a collision)
    if(myPrincess.collidesWith(myWalls, true)) {
      retVal = true;
    } else {
      // Note: it is not necessary to synchronize
      // this block because the thread that calls this 
      // method is the same as the one that puts down the 
      // keys, so there's no danger of the key being put down 
      // between the moment we check for the key and 
      // the moment we open the door:
      for(int i = 0; i < myDoors.length; i++) {
        // if she's holding the right key, then open the door
        // otherwise bounce off
        if(myPrincess.collidesWith(myDoors[i], true)) {
          if((myHeldKey != null) && 
             (myDoors[i].getColor() == myHeldKey.getColor())) {
            //setNeedsRepaint();
            myDoors[i].setVisible(false);
          } else {
            // if she's not holding the right key, then 
            // she has collided with the door just the same 
            // as if she had collided with a wall:
            retVal = true;
          }
        }
      }
    }
    return(retVal);
  }

}
