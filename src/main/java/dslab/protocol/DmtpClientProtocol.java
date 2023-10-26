package dslab.protocol;

import dslab.mailbox.MailboxServer;

public class DmtpClientProtocol implements IProtocol {
  private static final int WAITING = 0;
  private static final int BEGIN = 1;
  private static final int SENDING = 2;
  private int state = WAITING;

  @Override
  public String processCommand(String clientCommand) {
    String command = "", arguments = "";
    if (clientCommand != null) {
      String[] tokens = clientCommand.split(" ", 2); // Split the message into command and arguments
      command = tokens[0].toLowerCase();
      arguments = (tokens.length > 1) ? tokens[1] : "";
    }
    if (state == WAITING) {
      state = BEGIN;
      return "ok DMTP";
    } else if (state == BEGIN) {
      if (command.equals("begin")) {
        state = SENDING;
        return "ok";
      }
      state = SENDING;
    }
    if (state == SENDING) {
      if (command.equalsIgnoreCase("from")) {
        return "ok";
      } else if (command.equalsIgnoreCase("to")) {
        String[] recipients = arguments.split(",");
        // calculate the accepted recipients based on their domains
        int acceptedRecipients = 0;
        for (String recipient : recipients) {
          String[] usernameAndDomain = recipient.split("@");
          // check domain of current recipient, if it is not managed by this mailbox, the recipient is not considered
          if (usernameAndDomain[1].equals(MailboxServer.getDOMAIN())) {
            // check if the user is a known user for the mailbox
            if (MailboxServer.isKnownUser(usernameAndDomain[0])) {
              acceptedRecipients++;
            } else {
              return "error unknown user" + usernameAndDomain[0];
            }
          }
        }
        return "ok " + acceptedRecipients;
      } else if(command.equalsIgnoreCase("subject")){
        return "ok";
      } else if (command.equalsIgnoreCase("data")) {
        return "ok";
      } else if (command.equalsIgnoreCase("send")) {
        return "ok";
      } else if (command.equalsIgnoreCase("quit")) {
        return "ok bye";
      }
    }
    return "";
  }
}
