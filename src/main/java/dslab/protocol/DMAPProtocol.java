package dslab.protocol;

import dslab.mailbox.MailboxServer;

public class DMAPProtocol {
  private static final int WAITING = 0;
  private static final int LOGIN = 1;
  private static final int LOGGEDIN = 2;

  private int state = WAITING;
  private String[] answers = {"ok", "ok bye", "error", "ok DMAP"};

  public String processCommand(String clientCommand) {
    String output = null;

    String command = "", arguments = "";
    if (clientCommand != null) {
      String[] tokens = clientCommand.split(" ", 2); // Split the message into command and arguments
      command = tokens[0].toLowerCase();
      arguments = (tokens.length > 1) ? tokens[1] : "";
    }

    if (state == WAITING) {
      state = LOGIN;
      return "ok DMAP";
    } else if (state == LOGIN) {

      if (command.equalsIgnoreCase("login")) {
        String[] credentials = arguments.split("\\s");
        if (credentials.length != 2) {
          return "error login with username and password";
        }

        String loggedIn = MailboxServer.loginUser(credentials[0], credentials[1]);
        if (loggedIn.equals("ok")) {
          state = LOGGEDIN;
        }
        return loggedIn;
      }
      if (command.equalsIgnoreCase("BREW") || command.equalsIgnoreCase("POST")
          || command.equalsIgnoreCase("GET") || command.equalsIgnoreCase("WHEN") ||
          command.equalsIgnoreCase("PROPFIND")) {
        return "error protocol error";
      }
      if (command.equalsIgnoreCase("quit")) {
        return "ok bye";
      }
      if (command.equalsIgnoreCase("show") || command.equalsIgnoreCase("list")
          || command.equalsIgnoreCase("delete") || command.equalsIgnoreCase("logout")) {
        return "error not logged in";
      }
      return "error unknown command";

    } else if (state == LOGGEDIN) {
      if (command.equalsIgnoreCase("quit")) {
        return "ok bye";
      }

      if (command.equalsIgnoreCase("list")) {
        return "ok";
      } else if (command.equalsIgnoreCase("show")) {
        return "ok";
      } else if (command.equalsIgnoreCase("logout")) {
        state = LOGIN;
        return "ok";
      } else if(command.equalsIgnoreCase("delete")) {
        return "ok";
      }
    }
    return "";
  }

}
