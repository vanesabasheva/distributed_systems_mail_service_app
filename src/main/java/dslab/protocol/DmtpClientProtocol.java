package dslab.protocol;

import dslab.transfer.tcp.Email;
import dslab.util.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class DmtpClientProtocol {
  private Map<String, Map<Integer, Email>> userMailboxes;
  private Map<String, AtomicInteger> emailIdGenerators;
  private static final int WAITING = 0;
  private static final int BEGIN = 1;
  private static final int SENDING = 2;
  private int state = WAITING;
  private List<String> recipients;
  private String domain;
  private Config users;
  private Email email;

  public DmtpClientProtocol(Map<String, Map<Integer, Email>> userMailboxes,
                            Map<String, AtomicInteger> emailIdGenerators,
                            String domain, Config users) {

    this.userMailboxes = userMailboxes;
    this.emailIdGenerators = emailIdGenerators;
    this.domain = domain;
    this.users = users;
  }

  public String processCommand(String clientCommand) {
    String command = "", arguments = "";
    if (clientCommand != null) {
      String[] tokens = clientCommand.split("\\s", 2); // Split the message into command and arguments
      command = tokens[0].toLowerCase();
      arguments = (tokens.length > 1) ? tokens[1] : "";
    }

    if (state == WAITING) {
      state = BEGIN;
      return "ok DMTP";


    } else if (state == BEGIN) {
      state = SENDING;
      this.email = new Email(null,null,null,null);
      this.recipients = new ArrayList<>();

      if (command.equalsIgnoreCase("quit")) {
        return "ok bye";

      } else {
        if (processUnknownCommand(command).equals("error protocol error")) {
          return "error protocol error";

        }
      }
      return "ok";


    } else {
      if (command.equalsIgnoreCase("to")) {
        if (arguments == null || Objects.equals(arguments, "")) {
          return "error no recipients";
        }

        String[] tokens = arguments.split(",");

        // add the accepted recipients (those stored in the mailbox) to the list of recipients
        for (String recipient : tokens) {
          String[] usernameAndDomain = recipient.split("@");

          // check domain of current recipient, if it is not managed by this mailbox, the recipient is not considered
          if (usernameAndDomain[1].equals(this.domain)) {

            // check if the user is a known user for the mailbox
            if (this.isKnownUser(usernameAndDomain[0])) {
              recipients.add(usernameAndDomain[0]);

            } else {
              state = BEGIN;
              return "error unknown user" + usernameAndDomain[0];
            }
          }
        }

        this.email.setRecipients(tokens);
        return "ok " + recipients.size();

      } else if (command.equalsIgnoreCase("from")) {
        this.email.setSender(arguments);
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

        String response = saveEmail();

        if (response.equals("ok")) {
          state = BEGIN;
          this.recipients.clear();
        }
        return response;

      } else if (command.equalsIgnoreCase("begin")) {
        // reset everything written in the mail
        this.email = new Email(null, null, null, null);
        this.recipients.clear();
        state = SENDING;
        return "ok";

      } else if (command.equalsIgnoreCase("quit")) {
        state = WAITING;
        return "ok bye";
      } else {
        return processUnknownCommand(command);
      }
    }
  }

  private boolean isKnownUser(String username) {
    return users.containsKey(username);
  }

  private String processUnknownCommand(String command) {
    if (command.toUpperCase().contains("BREW") || command.toUpperCase().contains("POST")
        || command.toUpperCase().contains("GET") || command.toUpperCase().contains("WHEN") ||
        command.toUpperCase().contains("PROPFIND")) {
      return "error protocol error";
    }
    return "error unknown command";
  }

  private String saveEmail() {
    if (recipients.size() > 0) {
      for (String username : recipients) {
        int emailId = this.emailIdGenerators.get(username).getAndIncrement();
        this.userMailboxes.get(username).put(emailId, this.email);
      }
      return "ok";
    }
    return "error no valid recipients";

  }
}
