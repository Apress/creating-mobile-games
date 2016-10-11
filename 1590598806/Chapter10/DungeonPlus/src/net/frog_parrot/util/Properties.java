package net.frog_parrot.util;

import java.io.*;
import java.util.Hashtable;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.game.Sprite;

/**
 * This class is a helper class for reading a simple 
 * Java properties file.
 * 
 * @author Carol Hamer
 */
public class Properties {

  //---------------------------------------------------------
  //   instance data

  /**
   * The Hashtable to store the data in.
   */
  private Hashtable myData = new Hashtable();

  //---------------------------------------------------------
  //   initialization

  /**
   * load the data.
   * This method may block, so it should not be called 
   * from a thread that needs to return quickly.
   *
   * This method reads a file from an input stream 
   * and parses it as a Java properties file into 
   * a hashtable of values.
   *
   * @param is The input stream to read the file from
   * @param image for the special case where the properties 
   *        file is describing subimages of a single image,
   *        this is the larger image to cut subimages from.
   */
  public Properties(InputStream is, Image image) 
      throws IOException, NumberFormatException {
    // create a byte buffer to load the bytes into 
    // one by one.
    byte[] data = new byte[100];
    int endIndex = 0;
    String key = null;
    byte current = (byte)0;
    // read bytes from the file one by one until
    // hitting the end-of-file flag:
    while(current != -1) {
      current = (byte)(is.read());
      // build a string until hitting the end of a 
      // line or the end of the file:
      while(current != -1 && current != (byte)'\n') {
        if(current == (byte)':' && key == null) {
          key = new String(data, 0, endIndex, "utf-8");
          endIndex = 0;
        } else {
          data[endIndex] = current;
          endIndex++;
        }
        current = (byte)(is.read());
      }
      // continue only if the line is well-formed:
      if(key != null) {
        // if there is no image, then the keys and values
        // are just strings
        if(image == null) {
          myData.put(key, new String(data, 0, endIndex, "utf-8"));
        } else {
          // if there's an image, then the value string 
          // contains the dimensions of the subimage to 
          // cut from the image.  We parse the data string
          // and create the subimage:
          String dimStr = new String(data, 0, endIndex);
          int[] dimensions = new int[4];
          for(int i = 0; i < 3; i++) {
            int index = dimStr.indexOf(',');
            dimensions[i] = 
              Integer.parseInt(dimStr.substring(0, index).trim());
            dimStr = dimStr.substring(index + 1);
          }
          dimensions[3] = Integer.parseInt(dimStr.trim());
          Image subimage = Image.createImage(image, dimensions[0], 
              dimensions[1], dimensions[2] - dimensions[0], 
              dimensions[3] - dimensions[1], Sprite.TRANS_NONE);
          myData.put(key, subimage);
        }
      }
      // clear the data to read the next line:
      key = null;
      endIndex = 0;
    }
  }

  //---------------------------------------------------------
  //   data methods

  /**
   * Get a data string.
   */
  public String getString(String key) {
    return (String)(myData.get(key));
  }

  /**
   * Get a data int.
   */
  public int getInt(String key) throws NullPointerException, 
      NumberFormatException {
    String str = (String)(myData.get(key));
    return Integer.parseInt(str);
  }

  /**
   * Get an image.
   */
  public Image getImage(String key) throws NullPointerException, 
      IOException {
    String str = (String)(myData.get(key));
    return Image.createImage(str);
  }

  /**
   * Get a pre-initialized subimage.
   */
  public Image getSubimage(String key) throws NullPointerException, 
      IOException {
    return (Image)(myData.get(key));
  }

}
