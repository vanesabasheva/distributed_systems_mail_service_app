package dslab.protocol;

/**
 * The interface for protocol that is responsible for processing commands depending on the client and server relationship
 *
 */
public interface IProtocol {

  /**
   * Method that takes a client command as input and returns a response as output
   */
  String processCommand(String command);
}
