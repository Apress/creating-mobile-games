package net.frog_parrot.test;

import javax.microedition.lcdui.*;
import javax.microedition.midlet.MIDlet;

/**
 * A simple 3D example.
 */
public class TestMIDlet extends MIDlet implements CommandListener {
  
  private Command myExitCommand = new Command("Exit", Command.EXIT, 1);
  private DemoCanvas myCanvas = new DemoCanvas();
  
  /**
   * Initialize the Displayables.
   */
  public void startApp() {
    myCanvas.addCommand(myExitCommand);
    myCanvas.setCommandListener(this);
    Display.getDisplay(this).setCurrent(myCanvas);
    myCanvas.repaint();
  }
  
  public void pauseApp() {
  }
  
  public void destroyApp(boolean unconditional) {
  }
  
  /**
   * Change the display in response to a command action.
   */
  public void commandAction(Command command, Displayable screen) {
    if(command == myExitCommand) {
      destroyApp(true);
      notifyDestroyed();
    }
  }

}
 
