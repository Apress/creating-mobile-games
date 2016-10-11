package net.frog_parrot.test;

import javax.microedition.lcdui.*;
import javax.microedition.m3g.*;

/**
 * This is a very simple example class to illustrate 3-D coordinates.
 */
public class SimpleCanvas extends Canvas {

  /**
   * Paint the graphics onto the screen.
   */
  protected void paint(Graphics g) {
    Graphics3D g3d = null;
    try {
      // Every set of three elements of this array 
      // gives a vertex of the pyramid. The top row gives 
      // all of the vertices needed, however we include some 
      // extra copies of some of the vertices to try 
      // different things with them:
      short[] vertices =  {
	0, 0, 10,  10, 0, 0,  0, 10, 0,  0, -10, 0,  -10, 0, 0,  0, 0, 10,
	0, -10, 0,  10, 0, 0,  0, 0, 10
      };
      
      // Here we construct the VertexArray, which is 
      // a generic data structure for storing collections 
      // of coordinate points:
      int numVertices = vertices.length / 3;
      
      // specify how many vertices, plus the fact that 
      // each vertex has three coordinates, and each coordinate 
      // is coded on two bytes (a short):
      VertexArray va = new VertexArray(numVertices, 3, 2);
      // set the data, starting from index 0:
      va.set(0, numVertices, vertices);
      
      // Now create a 3-D object of it.
      VertexBuffer vb = new VertexBuffer();
      // the second and third tell how to scale and translate all of 
      // the coordinates; for simplicity we set them to identity:
      vb.setPositions(va, 1.0f, null);

      // Here we define the triangle strips
      // use the first six vertices to make one strip of triangles,
      // then make a triangle strip from the next three vertices:
      int[] strip =  { 6, 3 };
      
      // Then construct the corresponding IndexBuffer 
      // as an implicitly-defined TriangleStripArray
      IndexBuffer tsa = new TriangleStripArray(0, strip);

      // Render by getting a handle to the Graphics3D
      // object which does the work of projecting the 
      // 3-D scene onto the 2-D screen (rendering):
      g3d = Graphics3D.getInstance();
      // Bind the Graphics3D object to the Graphics
      // instance of the current canvas:
      g3d.bindTarget(g);
      // Clear the screen by painting it with a plain
      // black background:
      Background background = new Background();
      background.setColor(0x000000);
      g3d.clear(background);

      // Create the camera object to define where the polygon 
      // is being viewed from and in what way:
      Camera camera = new Camera();
      // Set the camera so that it will project the 3-D 
      // picture onto the screen in perspective, with a 
      // vanishing point in the distance. The arguments 
      // give information about what region of 3-space is
      // visible to the camera:
      camera.setPerspective(60.0f,
          (float)getWidth() / (float)getHeight(),
            1.0f, 1000.0f);

      // Now set where we're viewing the scene from:
      Transform cameraTransform = new Transform();
      // We set the camera's X position and Y position to 0 
      // so that we're looking straight down at the origin 
      // of the x-y plane.  The Z coordinate tells how far 
      // away the camera is -- increasing this value takes 
      // you farther from the polygon, making it appear 
      // smaller.  Try changing these values to view the 
      // polygon from different places: 
      cameraTransform.postTranslate(0.0f, 0.0f, 100.0f);
      g3d.setCamera(camera, cameraTransform);
      
      // Now set the location of the object.
      // if this were an animation we would probably 
      // translate or rotate it here:
      Transform objectTransform = new Transform();
      objectTransform.setIdentity();
      
      // Now render: Project from a 3D object to a 2D screen
      g3d.render(vb, tsa, new Appearance(),
		 objectTransform);
      
    } catch(Exception e) {
      e.printStackTrace();
    } finally {
      // Done, the canvas graphics can be freed now:
      if(g3d != null) {
	g3d.releaseTarget();
      }
    }
  }

}
