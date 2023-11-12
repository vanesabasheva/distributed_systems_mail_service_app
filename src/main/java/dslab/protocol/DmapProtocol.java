package dslab.protocol;

import dslab.mailbox.MailboxServer;
import dslab.util.Config;

import java.util.Map;
import java.util.concurrent.BlockingQueue;

public class DmapProtocol {
  private Map<String, Map<Integer, BlockingQueue<String>>> userMailboxes;
  private String username;
  private Map<Integer, BlockingQueue<String>> currentMailbox;
  private Config users;
  private static final int WAITING = 0;
  private static final int LOGIN = 1;
  private static final int LOGGEDIN = 2;
  private int state = WAITING;

  public DmapProtocol(Map<String, Map<Integer, BlockingQueue<String>>> userMailboxes,
                      Config users) {
    this.userMailboxes = userMailboxes;
    this.users = users;
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
        String loggedIn = this.authenticateUser(credentials[0], credentials[1]);
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

  // Checks if the username password pair is stored in this mailbox and returns a string
  // that notifies if the username/password is incorrect or if login is accepted
  private String authenticateUser(String username, String password) {
    System.out.println(username);
    if (!isKnownUser(username)) {
      return "error unknown user";
    } else if (!this.users.getString(username).equals(password)) {
      return "error wrong password";
    }
    return "ok";
  }

  private boolean isKnownUser(String username) {
    return users.containsKey(username);
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
