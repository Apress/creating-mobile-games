package net.frog_parrot.util;

import java.io.IOException;
import javax.microedition.lcdui.*;

/**
 * This class displays the opening sequence.
 * 
 * @author Carol Hamer
 */
public class SplashScreen extends Canvas {

  //---------------------------------------------------------
  //   Constants

  /**
   * color constant.
   */
  public static final int WHITE = 0xffffff;

  /**
   * color constant.
   */
  public static final int GRAY = 0x9b9999;

  /**
   * The distance to move during animation.
   */
  public static final int INCREMENT = 10;

  //---------------------------------------------------------
  //   instance data

  /**
   * The customizer giving the data about the image sizes
   * to use for this platform.
   */
  Customizer myCustomizer;

  /**
   * What step in the opening animation we're on.
   */
  int myStep;

  /**
   * The step value that indicates that the opening
   * animation is done.
   */
  int myLastStep;

  /**
   * whether the animation makes the title fade in 
   * instead of sliding in.
   */
  boolean myUseAlpha;

  /**
   * An image used in the opening animation.
   */
  Image myBackground;

  /**
   * An image used in the opening animation.
   */
  Image myKeys;

  /**
   * An image used in the opening animation.
   */
  Image myTitle;

  /**
   * A screen dimension.
   */
  int myWidth;

  /**
   * A screen dimension.
   */
  int myHeight;

  /**
   * A screen dimension.
   */
  int myHalfHeight;

  /**
   * A screen dimension.
   */
  int myHalfWidth;

  //---------------------------------------------------------
  //   initialization

  /**
   * Set the initial data
   *
   * @param numAlphaLevels how much blending is supported.
   */
  public SplashScreen(Customizer customizer, int numAlphaLevels) {
    myCustomizer = customizer;
    setFullScreenMode(true);
    myWidth = getWidth();
    myHeight = getHeight();
    myHalfHeight = myHeight >> 1;
    myHalfWidth = myWidth >> 1;
    // if the platform supports a sufficient amount of blending,
    // we set the opening animation to fade in the title and 
    // the keys, otherwise we slide them in:
    if(numAlphaLevels > 15) {
      myLastStep = 15;
      myUseAlpha = true;
    } else {
      myLastStep = myHalfWidth / INCREMENT;
    }
  }

  //---------------------------------------------------------
  //   business methods

  /**
   * Advance the animation and return whether
   * the animation is done.
   */
  public boolean advance() {
    myStep++;
    return (myStep < myLastStep);
  }

  /**
   * Paint the screen.
   */
  public void paint(Graphics g) {
    try {
      switch(myStep) {
        // the initial step is to quickly cover the screen
        // with a very simple opening image to look at while 
        // the Customizer is loading the rest of the data
        // behind the scenes:
      case 0:
        g.setColor(WHITE);
        g.fillRect(0, 0, myWidth, myHeight);
        Image logo = Image.createImage("/images/logo.png");
        g.drawImage(logo, myHalfWidth, 
                    myHalfHeight, Graphics.VCENTER|Graphics.HCENTER);
        break;
        // by step 1, the Customizer has found the right 
        // data for the opening animation that fits this
        // platform, so it is loaded and displayed:
      case 1:
        myBackground = myCustomizer.getImage("splash.background");
        myKeys = myCustomizer.getImage("splash.keys");
        myTitle = myCustomizer.getLabelImage("title");
        // paint gray over everything in case the background
        // image isn't quite big enough for the whole screen:
        g.setColor(GRAY);
        g.fillRect(0, 0, myWidth, myHeight);
        g.drawImage(myBackground, myHalfWidth, 
                  myHalfHeight, Graphics.VCENTER|Graphics.HCENTER);
        break;
      default:
        // move the title and the keys from the edges to 
        // the middle of the screen or fade them in:
        int move = myStep * INCREMENT;
        g.setColor(GRAY);
        g.fillRect(0, 0, myWidth, myHeight);
        g.drawImage(myBackground, myHalfWidth, 
                  myHalfHeight, Graphics.VCENTER|Graphics.HCENTER);
        if(myUseAlpha) {
          int transparency = 0x11000000 * myStep;
          myKeys = ColorChanger.modifyTransparency(myKeys, transparency);
          myTitle = ColorChanger.modifyTransparency(myTitle, transparency);
          g.drawImage(myTitle, myHalfWidth, 
                    myHalfHeight >> 1, Graphics.VCENTER|Graphics.HCENTER);
          g.drawImage(myKeys, myHalfWidth, 
                  myHalfHeight + (myHalfHeight >> 1), 
                  Graphics.VCENTER|Graphics.HCENTER);
        } else {
          g.drawImage(myTitle, myWidth - move, 
                    myHalfHeight >> 1, Graphics.VCENTER|Graphics.HCENTER);
          g.drawImage(myKeys, move, 
                  myHalfHeight + (myHalfHeight >> 1), 
                  Graphics.VCENTER|Graphics.HCENTER);
        }
        break;
      }
    } catch(IOException e) {
      // the game should continue even if 
      // the the opening animation fails
    }
  }

}

