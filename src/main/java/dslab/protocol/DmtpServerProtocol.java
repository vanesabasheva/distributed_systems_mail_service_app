package dslab.protocol;

import dslab.mailbox.MailboxServer;
import dslab.transfer.TransferServer;
import dslab.tcp.Email;

import java.util.Objects;

public class DmtpServerProtocol implements IProtocol {
  private static final int WAITING = 0;
  private static final int BEGIN = 1;
  private static final int WRITING = 2;
  private int state = WAITING;
  private Email email = new Email(null, null, null, null);
  private String[] answers = {"ok", "ok bye", "error", "ok DMTP"};

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
      if (command.equalsIgnoreCase("begin")  && arguments.equals("")) {
        state = WRITING;
        return "ok";
      } else {
        return "error no begin";
      }
    } else {
      if (command.equalsIgnoreCase("to")) {
        if (arguments == null || Objects.equals(arguments, "")) {
          return "error no recipients";
        }
        // check the number of recipients
        // check if the mails are valid
        // check if an email is sent to a known mailbox domain
        // check if the recipient/s is/are known to the mailbox/es

        String[] recipients = arguments.split(",");
        for (String recipientMail : recipients) {
          String[] parts = recipientMail.split("@");
          if (parts.length < 2) {
            return "error not a valid email";
          }
          if(TransferServer.getSocketAddressForDomain(parts[1]) == null) {
            return "error unknown mailbox domain";
          }
        }
        String recipientNo = String.valueOf(recipients.length);
        this.email.setRecipients(recipients);
        return "ok " + recipientNo;
      } else if (command.equalsIgnoreCase("from")) {
        if (Objects.equals(arguments, "")) {
          return "error no sender";
        }
        String[] sender = arguments.split("\\s");
        if (sender.length > 1) {
          return "error too many senders";
        }

        this.email.setSender(sender[0]);
        return "ok";
      } else if (command.equalsIgnoreCase("subject")) {
        this.email.setSubject(arguments);
        return "ok";
      } else if (command.equalsIgnoreCase("data")) {
        this.email.setData(arguments);
        return "ok";
      } else if (command.equalsIgnoreCase("send")) {
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
    if (command.equalsIgnoreCase("BREW") || command.equalsIgnoreCase("POST")
        || command.equalsIgnoreCase("GET") || command.equalsIgnoreCase("WHEN") ||
        command.equalsIgnoreCase("PROPFIND")) {
      return "error protocol error";
    }
    return "error unknown command";
  }

}
