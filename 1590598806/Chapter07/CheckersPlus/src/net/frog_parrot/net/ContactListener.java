package net.frog_parrot.net;

import java.util.Vector;

/**
 * This is a small interface to provide a callback method 
 * for the contact list loading code.
 *
 * @author Carol Hamer
 */
public interface ContactListener {

  /**
   * Set the contact list.
   */
  public void setContactList(Vector names, Vector phoneNumbers);

}


