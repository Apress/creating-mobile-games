package net.frog_parrot.util;

import java.io.*;
import java.util.Hashtable;
import javax.microedition.lcdui.Image;

/**
 * This class is a helper class for storing data that 
 * varies from one handset or language to another.
 * 
 * @author Carol Hamer
 */
public class Customizer {

  //---------------------------------------------------------
  //   Constants

  /**
   * a flag.
   */
  public static final int SOFT_NONE = 0;

  /**
   * a flag.
   */
  public static final int SOFT_LEFT = 1;

  /**
   * a flag.
   */
  public static final int SOFT_RIGHT = 2;

  //---------------------------------------------------------
  //   instance data

  /**
   * The width of the handset's screen.
   */
  int myWidth;

  /**
   * The height of the handset's screen.
   */
  int myHeight;

  /**
   * Whether to create the softkeys for the current handset.
   */
  boolean myUseSoftkeys;

  /**
   * A key code for the current handset.
   */
  int myLeftSoftkey;

  /**
   * A key code for the current handset.
   */
  int myRightSoftkey;

  //---------------------------------------------------------
  //   data for internal use

  /**
   * The custom data corresponding to the current handset.
   */
  Properties myProperties;

  /**
   * The labels corresponding to the current language.
   */
  Properties myLabels;

  /**
   * The image file containing all of the labels for the 
   * current handset.
   */
  Image myLabelImage;

  /**
   * The names of the image files for the current language
   * and handset.
   */
  Properties myLabelImages;

  //---------------------------------------------------------
  //   initialization

  /**
   * construct the custom data.
   * @param width the width of the display. 
   * @param height the height of the display. 
   */
  public Customizer(int width, int height) {
    myWidth = width;
    myHeight = height;
  }

  /**
   * construct the custom data.
   */
  public void init() throws IOException {
    InputStream is = null;
    // step 1 is to determine the correct language:
    String locale = System.getProperty("microedition.locale");
    try {
      // Here we use just the language part of the locale:
      // the country part isn't relevant since this game 
      // doesn't display prices.
      locale = locale.substring(0, 2);
      // Attempt to load the label strings 
      // in the correct language:
      StringBuffer buff = new StringBuffer("/");
      buff.append(locale);
      buff.append(".properties");
      is = this.getClass().getResourceAsStream(buff.toString());
    } catch(Exception e) {
      // If the handset's language is not present,
      // default to English:
      locale = "en";
      is = this.getClass().getResourceAsStream("/en.properties");
    }
    myLabels = new Properties(is, null);
    // Since some of the labels are drawn as images,
    // here we load label images for the correct language.
    // At the same time, load all of the graphical properties
    // for the given screen size:
    StringBuffer buff = new StringBuffer("/");
    buff.append(locale);
    // Here only two screen sizes are implemented, but this
    // could easily be extended to support a range of sizes:
    if((myWidth > 250) || (myHeight > 250)) {
      is = this.getClass().getResourceAsStream("/large.properties");
      myProperties = new Properties(is, null);
      buff.append("_large.properties");
      is = this.getClass().getResourceAsStream(buff.toString());
    } else {
      is = this.getClass().getResourceAsStream("/small.properties");
      myProperties = new Properties(is, null);
      buff.append("_small.properties");
      is = this.getClass().getResourceAsStream(buff.toString());
    }
    myLabelImage = myProperties.getImage(locale);
    myLabelImages = new Properties(is, myLabelImage);
    // Last, see if we can create custom softkeys
    // instead of using lcdui commands:
    try {
      // Get the system property that identifies the platform:
      String platform 
	= System.getProperty("microedition.platform");
      if(platform.length() > 5) {
	platform = platform.substring(0,5);
      }
      // check if the platform is one that we have softkey 
      // codes for:
      String softkeys = myProperties.getString(platform);
      if(softkeys != null) {
        int index = softkeys.indexOf(",");
        myLeftSoftkey 
            = Integer.parseInt(softkeys.substring(0, index).trim());
        myRightSoftkey 
            = Integer.parseInt(softkeys.substring(index + 1).trim());
        myUseSoftkeys = true;
      }
    } catch(Exception e) {
      // if there's any problem with reading the softkey info,
      // just don't use softkeys
    }
  }

  //---------------------------------------------------------
  //   data methods

  /**
   * Return whether to use softkeys instead of commands.
   */
  public boolean useSoftkeys() {
    return myUseSoftkeys;
  }

  /**
   * Return a data value of type int.
   */
  public int getInt(String key) {
    return myProperties.getInt(key);
  }

  /**
   * Return a label.
   */
  public String getLabel(String key) {
    return myLabels.getString(key);
  }

  /**
   * Return an image.
   */
  public Image getImage(String key) throws IOException {
    return myProperties.getImage(key);
  }

  /**
   * Return a label image.
   */
  public Image getLabelImage(String key) throws IOException {
    return myLabelImages.getSubimage(key);
  }

  //---------------------------------------------------------
  //   utilities

  /**
   * Check if the given keycode corresponds to a softkey
   * for this platform, and if so, which one.
   */
  public int whichSoftkey(int keycode) {
    if(myUseSoftkeys) {
      if(keycode == myLeftSoftkey) {
        return SOFT_LEFT;
      }
      if(keycode == myRightSoftkey) {
        return SOFT_RIGHT;
      }
    }
    return SOFT_NONE;
  }

}
