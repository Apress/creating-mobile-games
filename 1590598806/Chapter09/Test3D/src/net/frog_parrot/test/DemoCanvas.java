package net.frog_parrot.test;

import javax.microedition.lcdui.*;
import javax.microedition.m3g.*;

/**
 * This is a very simple example class to illustrate 3-D coordinates.
 */
public class DemoCanvas extends Canvas {

  /**
   * A 2D image for textures and backgrounds.
   */
  private Image2D myHelloDiagonal;

  /**
   * A 2D image for textures and backgrounds.
   */
  private Image2D myHelloTransparent;

  /**
   * The information about where the scene is viewed from.
   */
  private Camera myCamera;

  /**
   * The information about how to move the camera.
   */
  private Transform myCameraTransform = new Transform();

  /**
   * The information about how to move the pyramid.
   */
  private Transform myObjectTransform = new Transform();

  /**
   * The distance to move the camera in response to a keypress.
   */
  public static final float DEFAULT_DISTANCE = 10.0f;

  /**
   * The background. (self-explanatory ;-) )
   */
  private Background myBackground = new Background();

  /**
   * The set of vertices.
   */
  private VertexBuffer myVertexBuffer;

  /**
   * The object that defines how to map the set of vertices into 
   * a polygon.
   */
  private IndexBuffer myIndexBuffer;

  /**
   * Information on how the polygon should look in terms of 
   * color, texture, shading, etc..
   */
  private Appearance myAppearance;

  /**
   * The list of vertices for the first example pyramid.
   */
  private short[] myVertices1 =  {
    0, 0, 10,  10, 0, 0,  0, 10, 0,  0, -10, 0,  -10, 0, 0,  0, 0, 10,
  };

  /**
   * The rule for how to piece together the vertices into a polygon.
   */
  private int[] myTriangleStrip1 = { 6 };

  /**
   * The list of vertices for the second example pyramid.
   */
  private short[] myVertices2 =  {
    0, 0, 10,  10, 0, 0,  0, 10, 0,  0, -10, 0,  -10, 0, 0,  0, 0, 10,
    0, -10, 0,  10, 0, 0,  0, 0, 10
  };

  /**
   * The list of normals for the second example pyramid.
   */
  private short[] myNormals2 =  {
    0, 0, 10,  10, 0, 0,  0, 10, 0,  0, -10, 0,  -10, 0, 0,  0, 0, 10,
    1, -1, 1,  1, -1, 1,  1, -1, 1
  };

  /**
   * The list of crazy normals for the second example pyramid.
   */
  private short[] myAbbeyNormals2 =  {
    0, 1, 1,  -1, 0, 0,  1, 0, 1,  0, 1, -1,  -1, 0, 0,  1, 1, 1,
    -1, 1, 1,  1, -1, 1,  1, 1, -1
  };

  /**
   * The texture coordinates of the second example pyramid.
   */
  private short[] myTextures2 =  {
    0, 0,  2, 2,  2, 0,  4, 2,  4, 0,  2, 2,
    1, 1,  1, 0,  0, 1
  };

  /**
   * The rule for how to piece together the vertices into a polygon.
   */
  private int[] myTriangleStrip2 = { 6, 3 };

  /**
   * Initialize everything.
   */
  public DemoCanvas() {
    try {
      // Start by creating the 2D images to use
      Image image = Image.createImage("/images/hello_diagonal.png");
      myHelloDiagonal = new Image2D(Image2D.RGB, image);
      image = Image.createImage("/images/hello_transparent.png");
      image = fixTransparent(image);
      myHelloTransparent = new Image2D(Image2D.RGBA, image);

      // Create the camera object to define where the polygon is being
      // viewed from and in what way:
      myCamera = new Camera();
      // Set the camera so that it will project the 3-D picture onto the 
      // screen in perspective, with a vanishing point in the distance:
      myCamera.setPerspective(60.0f, (float)getWidth() / (float)getHeight(),
      			      1.0f, 10000.0f);

      // Here we construct the VertexArray, which is a generic data 
      // structure for storing collections of coordinate points:
      int numVertices = myVertices2.length / 3;
      // specify how many vertices, plus the fact that each vertex has 
      // three coordinates, and each coordinate is coded on two bytes:
      VertexArray va = new VertexArray(numVertices, 3, 2);
      // set the data, starting from index 0:
      va.set(0, numVertices, myVertices2);

      // define the normals:
      VertexArray na = new VertexArray(numVertices, 3, 2);
      // set the data, starting from index 0:
      na.set(0, numVertices, myNormals2);

      // define the texture coordinates:
      VertexArray ta = new VertexArray(numVertices, 2, 2);
      ta.set(0, numVertices, myTextures2);

      // Now create a 3-D object of it.
      // Here we can group a set of different VertexArrays, one 
      // giving positions, one, giving colors, one giving normals:
      myVertexBuffer = new VertexBuffer();
      myVertexBuffer.setPositions(va, 1.0f, null);
      myVertexBuffer.setNormals(na);
      myVertexBuffer.setTexCoords(0, ta, 1.0f, null);
      myVertexBuffer.setTexCoords(1, ta, 1.0f, null);

      // Here we define how to piece together the vertices into 
      // a polygon:
      myIndexBuffer = new TriangleStripArray(0, myTriangleStrip2);

      // Let's try creating a more complex appearance:
      myAppearance = new Appearance();

      // first a reflective material
      Material material = new Material();
      material.setShininess(100.0f);
      myAppearance.setMaterial(material);

      // and now for a couple of textures:
      Texture2D texture = null;
      texture = new Texture2D(myHelloDiagonal);
      texture.setWrapping(Texture2D.WRAP_REPEAT, Texture2D.WRAP_REPEAT);
      texture.setBlending(Texture2D.FUNC_MODULATE);
      myAppearance.setTexture(0, texture);
      texture = new Texture2D(myHelloTransparent);
      texture.setWrapping(Texture2D.WRAP_REPEAT, Texture2D.WRAP_REPEAT);
      texture.setBlending(Texture2D.FUNC_MODULATE);
      myAppearance.setTexture(1, texture);
      
      PolygonMode pm = new PolygonMode();
      pm.setCulling(PolygonMode.CULL_NONE);
      pm.setTwoSidedLightingEnable(true);
      myAppearance.setPolygonMode(pm);

      // color the background black:
      //myBackground.setColor(0x000000);
      myBackground.setImage(myHelloDiagonal);
      myBackground.setImageMode(Background.REPEAT, Background.REPEAT);

      // We set the camera's X position and Y position to 0 
      // so that we're looking straight down at the origin 
      // of the x-y plane.  The Z coordinate tells how far 
      // away the camera is -- increasing this value takes 
      // you farther from the polygon, making it appear 
      // smaller.
      myCameraTransform.postTranslate(0.0f, 0.0f, 25.0f);

      // reset the object's original orientation:
      myObjectTransform.setIdentity();
      myObjectTransform.postRotate(DEFAULT_DISTANCE, -1.0f, 0.0f, 0.0f);
      //myObjectTransform.postRotate(75.0f, 2.0f, 0.5f, -0.5f);
    } catch(Exception e) {
      e.printStackTrace();
    }
  }


  /**
   * Paint the graphics onto the screen.
   */
  protected void paint(Graphics g) {
    try {
      // Start by getting a handle to the Graphics3D
      // object which does the work of projecting the 
      // 3-D scene onto the 2-D screen (rendering):
      Graphics3D g3d = Graphics3D.getInstance();
      // Bind the Graphics3D object to the Graphics
      // instance of the current canvas:
      g3d.bindTarget(g);
      // Clear the screen by painting it with the 
      // background image:
      g3d.clear(myBackground);

      // now add the light:
      Light light = new Light();
      light.setMode(Light.OMNI);
      light.setIntensity(20.0f);
      Transform lightTransform = new Transform();
      lightTransform.postTranslate(0.0f, 0.0f, 50.0f);
      g3d.resetLights();
      g3d.addLight(light, lightTransform);

      g3d.setCamera(myCamera, myCameraTransform);

      // Now render, project the 3D scene onto the flat screen:
      g3d.render(myVertexBuffer, myIndexBuffer, myAppearance, 
          myObjectTransform);

      // Done, the canvas graphics can be freed now:
      g3d.releaseTarget();

    } catch(Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Move the object in response to game commands.
   */
  public void keyPressed(int keyCode) {
    switch(getGameAction(keyCode)) {
    case Canvas.UP:
      myObjectTransform.postRotate(DEFAULT_DISTANCE, -1.0f, 0.0f, 0.0f);
      break;
    case Canvas.DOWN:
      myObjectTransform.postRotate(DEFAULT_DISTANCE, 1.0f, 0.0f, 0.0f);
      break;
    case Canvas.RIGHT:
      myObjectTransform.postRotate(DEFAULT_DISTANCE, 0.0f, 1.0f, 0.0f);
      break;
    case Canvas.LEFT:
      myObjectTransform.postRotate(DEFAULT_DISTANCE, 0.0f, -1.0f, 0.0f);
      break;
    case Canvas.FIRE:
      myCameraTransform.postTranslate(0.0f, 0.0f, 1.0f);
      break;
    default:
      break;
    }
    repaint();
  }

  /**
   * Move the Camera in response to game commands.
   */
  /*
  public void keyPressed(int keyCode) {
    float[] fourFloats = new float[4];
    myCamera.getOrientation(fourFloats);
    System.out.println("orientation: " + fourFloats[0] + " * " + fourFloats[1] + ", " + fourFloats[2] + ", " + fourFloats[3]);
    switch(getGameAction(keyCode)) {
    case Canvas.UP:
      myCameraTransform.postRotate(DEFAULT_DISTANCE, -1.0f, 0.0f, 0.0f);
      break;
    case Canvas.DOWN:
      myCameraTransform.postRotate(DEFAULT_DISTANCE, 1.0f, 0.0f, 0.0f);
      break;
    case Canvas.RIGHT:
      myCameraTransform.postRotate(DEFAULT_DISTANCE, 0.0f, 1.0f, 0.0f);
      break;
    case Canvas.LEFT:
      myCameraTransform.postRotate(DEFAULT_DISTANCE, 0.0f, -1.0f, 0.0f);
      break;
    case Canvas.FIRE:
      myCameraTransform.postTranslate(-10.0f * fourFloats[1], -10.0f * fourFloats[2], -10.0f * fourFloats[3]);
      break;
    default:
      break;
    }
    repaint();
  }
  */

  /**
   * Test of changing the transparency.
   */
  public Image fixTransparent(Image image) {
    int width = image.getWidth();
    int height = image.getHeight();
    int[] imageData = new int[width * height];
    image.getRGB(imageData, 0, width, 0, 0, width, height);
    for(int i = 0; i < imageData.length; i++) {
      //imageData[i] = imageData[i] & 0x00ffffff;
      imageData[i] = ~imageData[i];
    }
    return(Image.createRGBImage(imageData, width, height, true));
  }



}

