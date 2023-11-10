package dslab.protocol;

import dslab.mailbox.MailboxServer;

import java.util.Map;
import java.util.concurrent.BlockingQueue;

public class DmapProtocol implements IProtocol {
  private Map<String, Map<Integer, BlockingQueue<String>>> userMailboxes;
  private String username;
  private Map<Integer, BlockingQueue<String>> currentMailbox;
  private static final int WAITING = 0;
  private static final int LOGIN = 1;
  private static final int LOGGEDIN = 2;
  private int state = WAITING;

  public DmapProtocol(Map<String, Map<Integer, BlockingQueue<String>>> userMailboxes) {
    this.userMailboxes = userMailboxes;
  }

  public String processCommand(String clientCommand) {
    String command = "", arguments = "";

    if (clientCommand != null) {
      // Split the message into command and arguments
      String[] tokens = clientCommand.split("\\s", 2);
      command = tokens[0].toLowerCase();
      arguments = (tokens.length > 1) ? tokens[1] : "";
    }

    if (state == WAITING) {
      state = LOGIN;
      return "ok DMAP";

    } else if (state == LOGIN) {

      if (command.equalsIgnoreCase("login")) {

        // validate that the user has given the correct amount of params
        String[] credentials = arguments.split("\\s");
        if (credentials.length != 2) {
          return "error login <username password>";
        }

        // authenticate user with mailbox server method for credentials
        String loggedIn = MailboxServer.authenticateUser(credentials[0], credentials[1]);
        if (loggedIn.equals("ok")) {
          username = credentials[0];
          state = LOGGEDIN;
        }

        // return message sent from mailbox for the existence / login of the user
        return loggedIn;

      }
      if (command.equalsIgnoreCase("quit")) {
        return "ok bye";
      }

      return this.processUnknownCommand(command);

    } else if (state == LOGGEDIN) {
      // initialize the mailbox of the user currently logged in
      currentMailbox = this.userMailboxes.get(this.username);

      if (command.equalsIgnoreCase("list")) {
        if (currentMailbox.size() == 0) {
          return "no emails to list";
        }

        return "ok";

      } else if (command.equalsIgnoreCase("show")) {
        if (arguments.equals("")) {
          return "error specify email id";
        }
        if (arguments.split("\\s").length > 1) {
          return "error specify only one mail id";
        }

        int emailId;
        try {
          emailId = Integer.parseInt(arguments);
        } catch (NumberFormatException e) {
          return "error message-id could not be interpreted as a number.";
        }

        if (!this.currentMailbox.containsKey(emailId)) {
          return "error no mail with specified id found";
        }

        return "ok";

      } else if (command.equalsIgnoreCase("logout")) {
        state = LOGIN;
        return "ok";

      } else if (command.equalsIgnoreCase("delete")) {
        if (arguments.equals("")) {
          return "error specify email id";
        }

        if (arguments.split("\\s").length > 1) {
          return "error specify only one mail id";
        }

        int emailId;
        try {
          emailId = Integer.parseInt(arguments);
        } catch (NumberFormatException e) {
          return "error email id could not be interpreted as a number";
        }

        if (!this.currentMailbox.containsKey(emailId)) {
          return "error no mail with specified id found";
        }

        deleteEmail(emailId);
        return "ok";
      } else if (command.equalsIgnoreCase("quit")) {
        return "ok bye";
      }

      return this.processUnknownCommand(command);
    }
    return "";
  }

  private String processUnknownCommand(String command) {
    if (state == LOGIN) {
      if (command.equalsIgnoreCase("show") || command.equalsIgnoreCase("list")
          || command.equalsIgnoreCase("delete") || command.equalsIgnoreCase("logout")) {
        return "error not logged in";
      }

    } else if (state == LOGGEDIN) {
      if (command.equalsIgnoreCase("login")) {
        return "error already logged in";
      }
    }

    if (command.toUpperCase().contains("BREW") || command.toUpperCase().contains("POST")
        || command.toUpperCase().contains("GET") || command.toUpperCase().contains("WHEN") ||
        command.toUpperCase().contains("PROPFIND")) {
      return "error protocol error";
    }
    return "error unknown command";
  }

  public Map<Integer, BlockingQueue<String>> getCurrentMailbox() {
    return this.currentMailbox;
  }

  public BlockingQueue<String> getEmailWithId(Integer key) {
    return currentMailbox.get(key);
  }

  private void deleteEmail(Integer emailId) {
    this.currentMailbox.remove(emailId);
  }
}
