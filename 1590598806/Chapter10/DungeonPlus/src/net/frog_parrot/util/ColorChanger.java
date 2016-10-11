package net.frog_parrot.util;

import javax.microedition.lcdui.*;

/**
 * This class modifies the colors and transparency 
 * of an image.
 * 
 * @author Carol Hamer
 */
public class ColorChanger {

  //---------------------------------------------------------
  //   Constants

  /**
   * A color constant.
   */
  public static int TRANSPARENT_WHITE = 0x00ffffff;

  /**
   * A color constant.
   */
  public static int OPAQUE = 0xff000000;

  //---------------------------------------------------------
  //   utilities

  /**
   * This method changes the transparency of the Image.
   *
   * @param image the source Image to modify
   * @param color the color value that the new transparency
   * is read from in ARGB.  The RGB part is ignored.
   */
  public static Image modifyTransparency(Image image, 
      int color) {
    int transparency = color & OPAQUE;
    int width = image.getWidth();
    int height = image.getHeight();
    int[] imageData = new int[width * height];
    image.getRGB(imageData, 0, width, 0, 0, width, height);
    for(int i = 0; i < imageData.length; i++) {
      // only modify pixels that aren't already 
      // fully transparent:
      if((imageData[i] & OPAQUE) != 0) {
        imageData[i] = transparency 
            + (imageData[i] & TRANSPARENT_WHITE);
      }
    }
    return(Image.createRGBImage(imageData, width, height, true));
  }

  /**
   * This method doubles the size of the image by 
   * adding a second copy of the image to the image file
   * with a new color.  This is used to create Sprites 
   * with a "focused" and "unfocused" version of 
   * the same image.
   *
   * @param image the source Image to modify
   * @param oldColor the color to replace.
   * @param newColor the color to replace it with.
   */
  public static Image createFocused(Image image, 
      int oldColor, int newColor) {
    int width = image.getWidth();
    int height = image.getHeight();
    int simpleSize = width * height;
    int[] imageData = new int[simpleSize * 2];
    // make two copies of the image data one 
    // after the other in the byte array:
    image.getRGB(imageData, 0, width, 0, 0, width, height);
    image.getRGB(imageData, simpleSize, width, 0, 0, width, height);
    for(int i = 0; i < simpleSize; i++) {
      // change the color in the first of the two copies:
      if(imageData[i] == oldColor) {
        imageData[i] = newColor;
      }
    }
    return(Image.createRGBImage(imageData, width, height * 2, true));
  }

  /**
   * This method doubles the size of the image by 
   * adding a second copy of the image to the image file
   * with a new color.  This is used to create Sprites 
   * with a "selected" and "unselected" version of 
   * the same image.
   *
   * @param image the source Image to modify
   * @param newColor the color to use for all non-transparent
   * pixels.
   */
  /*
  public static Image createSelected(Image image, 
      int newColor) {
    int width = image.getWidth();
    int height = image.getHeight();
    int simpleSize = width * height;
    int[] imageData = new int[simpleSize * 2];
    // make two copies of the image data one 
    // after the other in the byte array:
    image.getRGB(imageData, 0, width, 0, 0, width, height);
    image.getRGB(imageData, simpleSize, width, 0, 0, width, height);
    for(int i = 0; i < simpleSize; i++) {
      // only modify pixels that aren't 
      // fully transparent:
      if((imageData[i] & OPAQUE) != 0) {
        imageData[i] = newColor;
      }
    }
    return(Image.createRGBImage(imageData, width, height * 2, true));
  }
  */

}
