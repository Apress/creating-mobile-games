package net.frog_parrot.net;

import java.util.Enumeration;
import java.util.Vector;

import javax.microedition.pim.*;

/**
 * A simple PIM utility to load a list of contacts.
 */
public class PIMRunner extends Thread {

  /**
   * A callback listener for this 
   * class to call when the PIM list is filled.
   */
  ContactListener myListener;

  /**
   * The list of name fields to check to try to find the name
   * to display to the user.
   */
  int[] NAME_INDICES = {
    Contact.NAME,
    Contact.FORMATTED_NAME,
    Contact.NAME_GIVEN,
    Contact.NAME_FAMILY,
  };

  /**
   * The constructor just sets the callback listener for this 
   * class to call when the PIM list is filled.
   */
  public PIMRunner(ContactListener listener) {
    myListener = listener;
  }

  /**
   * The method that fills the data fields.
   */
  public void run() {
    ContactList addressbook = null;
    Contact contact = null;
    Enumeration items = null;
    Vector names = new Vector();
    Vector phoneNumbers = new Vector();
    try {
      addressbook = (ContactList)(PIM.getInstance(
          ).openPIMList(PIM.CONTACT_LIST, PIM.READ_ONLY));
      items = addressbook.items();
    } catch(Exception e) {
      // if the addressbook can't be opened, then we're done.
      myListener.setContactList(names, phoneNumbers);
    }
    // Now load the contents of the addressbook:
    while(items.hasMoreElements()) {
      try {
        contact = (Contact)(items.nextElement());
        // only continue if the contact has at least one 
        // phone number listed:
        int phoneNumCount = contact.countValues(Contact.TEL);
        if(phoneNumCount > 0) {
          String phoneNum = null;
          for(int i = 0; i < phoneNumCount; i++) {
            int attr = contact.getAttributes(Contact.TEL, i);
            if(i == 0 || attr == Contact.ATTR_MOBILE) {
              phoneNum = contact.getString(Contact.TEL, i);
            }
          }
          // now we assume that this handset lists all 
          // mobile phone numbers with the MOBILE attribute, 
          // so if we didn't find a mobile number, we skip 
          // this contact:
          if(phoneNum != null) {
            // now try to find the name.
            // since we don't know which name fields this 
            // handset supports, we keep trying until we 
            // find something:
            int fieldIndex = -1;
            for(int i = 0; i < NAME_INDICES.length; i++) {
              if(addressbook.isSupportedField(NAME_INDICES[i]) 
                  && contact.countValues(NAME_INDICES[i]) > 0) {
                fieldIndex = NAME_INDICES[i];
                break;
              }
            }
            // we've found a contact with a name and 
            // a mobile number, so we add it to the list:
            if(fieldIndex != -1) {
              // logically each type of name field will have 
              // only one entry, so we take the first one, 
              // of index 0:
              names.addElement(contact.getString(fieldIndex, 0));
              phoneNumbers.addElement(phoneNum);
            }
          }
        }
      } catch(Exception e) {
        e.printStackTrace();
        // if an individual contact provokes an exception, 
        // we skip it and move on.
      }
    } // while(items.hasMoreElements())
    myListener.setContactList(names, phoneNumbers);
  }

}
