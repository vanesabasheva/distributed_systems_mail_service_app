package dslab.protocol;

import dslab.transfer.tcp.Email;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class DmtpServerProtocol {
  private static final int WAITING = 0;
  private static final int BEGIN = 1;
  private static final int WRITING = 2;
  private int state = WAITING;
  private BlockingQueue<String> recipientsDomains = new LinkedBlockingQueue<>();
  private Email email;

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
      if (command.equalsIgnoreCase("begin") && arguments.equals("")) {
        state = WRITING;
        // reset email queue when beginning writing a new email
        this.email = new Email(null, null, null, null);
        return "ok";
      } else {
        if (processUnknownCommand(command).equals("error protocol error")) {
          return "error protocol error";
        }
        return "error no begin";
      }
    } else {
      if (command.equalsIgnoreCase("to")) {
        if (arguments == null || Objects.equals(arguments, "")) {
          return "error no recipients";
        }

        // check the number of recipients
        // check if the mails are valid
        String[] recipients = arguments.split(",");
        for (String recipientMail : recipients) {

          String[] parts = recipientMail.split("@");
          if (parts.length != 2) {
            return "error not a valid email";
          }

          // check if the current recipient domain is already present in the list, add him if not
          // this is done to prevent sending an email to the same mailbox more than once
          if (!recipientsDomains.contains(parts[1])) {
            this.recipientsDomains.add(parts[1]);
          }
        }

        this.email.setRecipients(recipients);

        String recipientNo = String.valueOf(recipients.length);
        return "ok " + recipientNo;


      } else if (command.equalsIgnoreCase("from")) {
        if (Objects.equals(arguments, "")) {
          return "error no sender";
        }

        String[] sender = arguments.split("\\s");
        if (sender.length > 1) {
          return "error too many senders";
        }

        sender = arguments.split("@");
        if (sender.length != 2) {
          return "error not a valid mail";
        }
        this.email.setSender(arguments);
        return "ok";


      } else if (command.equalsIgnoreCase("subject")) {
        this.email.setSubject(arguments);
        return "ok";


      } else if (command.equalsIgnoreCase("data")) {
        this.email.setData(arguments);
        return "ok";

      } else if (command.equalsIgnoreCase("begin")) {
        // reset everything written in the mail
        this.email = new Email(null, null, null, null);
        return "ok";

      } else if (command.equalsIgnoreCase("send")) {
        //check if all data for the email is given
        if (this.email.getSender() == null) {
          return "error no sender";
        }
        if (this.email.getRecipients() == null) {
          return "error no recipient";

        }
        if (this.email.getData() == null) {
          return "error no data";
        }
        if (this.email.getSubject() == null) {
          return "error no subject";
        }
        state = WRITING;
        return "ok";

      } else if (command.equalsIgnoreCase("quit")) {
        state = WAITING;
        return "ok bye";
      } else {
        return this.processUnknownCommand(command);
      }
    }
  }

  private String processUnknownCommand(String command) {
    if (command.toUpperCase().contains("BREW") || command.toUpperCase().contains("POST")
        || command.toUpperCase().contains("GET") || command.toUpperCase().contains("WHEN") ||
        command.toUpperCase().contains("PROPFIND")) {
      return "error protocol error";
    }
    return "error unknown command";
  }

  public Email getEmail() {
    return this.email;
  }

}
