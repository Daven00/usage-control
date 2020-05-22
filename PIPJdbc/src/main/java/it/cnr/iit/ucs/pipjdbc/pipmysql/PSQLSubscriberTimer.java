package it.cnr.iit.ucs.pipjdbc.pipmysql;

import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import iit.cnr.it.ucsinterface.contexthandler.ContextHandlerPIPInterface;
import iit.cnr.it.ucsinterface.message.PART;
import iit.cnr.it.ucsinterface.message.remoteretrieval.MessagePipCh;
import iit.cnr.it.ucsinterface.message.remoteretrieval.PipChContent;
import iit.cnr.it.ucsinterface.pip.exception.PIPException;
import iit.cnr.it.xacmlutilities.Attribute;
import iit.cnr.it.xacmlutilities.Category;
import iit.cnr.it.xacmlutilities.DataType;
import it.cnr.iit.sqlmiddlewareinterface.SQLMiddlewarePIPInterface;

/**
 * Subscriber timer for the PipUserAttributes.
 * <p>
 * Basically the subscriber timer is in hcarge of performing the task of
 * refreshing periodically the value of a certain attribute, if that value
 * changes, then it has to update the value in the subscriptions queue.
 * 
 * <p>
 * By removing the public attribute to this class we have allowed only classes
 * in the same package to create or instantiate such a class
 * </p>
 * 
 * @author antonio
 *
 */


final class PSQLSubscriberTimer extends TimerTask {

  // logger to be used to log the actions
  private Logger LOGGER = Logger.getLogger(PSQLSubscriberTimer.class.getName());

  // the queue of attributes that have been subscribed
  private final BlockingQueue<Attribute> subscriptions;

  // the interface to communicate with the context handler
  private ContextHandlerPIPInterface contextHandler;

  private SQLMiddlewarePIPInterface sqlInterface;
  
  PIPUserAttributes pip;

  /**
   * Constructor for a new Subscriber timer
   * 
   * @param contextHandler
   *          the interface to the context handler
   * @param map
   *          the list of attributes that have been subscribed
   * @param path
   *          the path to the file to be read
   */
  PSQLSubscriberTimer(ContextHandlerPIPInterface contextHandler,
      BlockingQueue<Attribute> map, SQLMiddlewarePIPInterface sqlInterface, PIPUserAttributes pip) {
    subscriptions = map;
    this.sqlInterface = sqlInterface;
    this.pip = pip;
  }

  @Override
  public void run() {

    for (Attribute entry : subscriptions) {
      System.out.println("Subscriber MYSQL: " + entry.getAttributeId());
      Category category = entry.getCategory();
      String newValue = "";
      newValue = read(entry);
      /*
      if (category == Category.ENVIRONMENT) {
        newValue = read(entry);
      } else {
        newValue = read(entry.getAdditionalInformations());
      }*/
      
      DataType attrDataType = entry.getAttributeDataType();
      List<String> attributeValues = entry.getAttributeValues(attrDataType);
      if (!attributeValues.isEmpty() && attributeValues.get(0).equals(newValue)) {
        LOGGER.log(Level.INFO,
            "[TIME] value of the attribute changed at "
                + System.currentTimeMillis() + "\t" + newValue + "\t"
                + entry.getAdditionalInformations());
        entry.setValue(entry.getAttributeDataType(), newValue);
        PipChContent pipChContent = new PipChContent();
        pipChContent.addAttribute(entry);
        MessagePipCh messagePipCh = new MessagePipCh(PART.PIP.toString(),
            PART.CH.toString());
        messagePipCh.setMotivation(pipChContent);
        contextHandler.attributeChanged(messagePipCh);
      }
    }
  }

  /**
   * Reads the file looking for the line containing the filter we are passing as
   * argument and the role stated as other parameter
   * 
   * <br>
   * NOTE we suppose that in the file each line has the following structure:
   * filter\tattribute.
   * 
   * @param filter
   *          the string to be used to search for the item we're interested into
   * @param role
   *          the role of the string
   * @return the string or null
   * 
   * 
   * @throws PIPException
   */
  private String read(String filter) {
    return "";
  }

  /**
   * Effective retrieval of the monitored value, before this retrieval many
   * checks may have to be performed
   * 
   * @return the requested string
   * @throws PIPException
   */
  private String read(Attribute attribute) {
	  if (attribute.getAttributeId().contains("organisation")) {
	        LOGGER.log(Level.INFO, "[PIPUserAttributes] AttributeId: " + attribute
	            .getAttributeId() + " for " + pip.filter + " Value:" + pip.userAttributes
	                .getOrganizationName());
	        return pip.userAttributes.getOrganizationName();
	      }
	      if (attribute.getAttributeId().contains("role")) {
		        LOGGER.log(Level.INFO, "[PIPUserAttributes] AttributeId: " + attribute
		            .getAttributeId() + " for " + pip.filter + " Value:" + pip.userAttributes
		                .getRole());
		        return pip.userAttributes.getRole();

	      }
	      if (attribute.getAttributeId().contains("ismemberof")) {
	  	        LOGGER.log(Level.INFO, "[PIPUserAttributes] AttributeId: " + attribute
	  	            .getAttributeId() + " for " + pip.filter + " Value:" + pip.userAttributes
	  	                .getGroup());
	  	        return pip.userAttributes.getGroup();
	        	
	        }
	      if (attribute.getAttributeId().contains("country")) {
	  	        LOGGER.log(Level.INFO, "[PIPUserAttributes] AttributeId: " + attribute
	  	            .getAttributeId() + " for " + pip.filter + " Value:" + pip.userAttributes
	  	                .getCountry());
	  	        return pip.userAttributes.getCountry();
	        	
	        }
		return "";
  }

  /**
   * Sets the context handler interface
   * 
   * @param contextHandler
   */
  public void setContextHandlerInterface(
      ContextHandlerPIPInterface contextHandler) {
    // BEGIN parameter checking
    if (contextHandler == null) {
      LOGGER.log(Level.SEVERE, "Context handler is null");
      return;
    }
    // END parameter checking
    this.contextHandler = contextHandler;
  }

  public ContextHandlerPIPInterface getContextHandler() {
    return contextHandler;
  }

}
