package dslab.mailbox;

import at.ac.tuwien.dsg.orvell.Shell;
import at.ac.tuwien.dsg.orvell.StopShellException;
import at.ac.tuwien.dsg.orvell.annotation.Command;
import dslab.ComponentFactory;
import dslab.mailbox.tcp.dmap.UserMailServerThread;
import dslab.mailbox.tcp.dmtp.MailServerThread;
import dslab.util.Config;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class MailboxServer implements IMailboxServer, Runnable {
  private static String DOMAIN = "";
  private static Config USERS;
  private String domain = "";
  private Config users;
  private Config config;
  private String componentId;
  private Shell shell;
  private ServerSocket dmapListener;
  private ServerSocket dmtpListener;
  private Set<Socket> socketSet;
  // a map containing the username as key, and his personal mailbox
  // as a map of an integer (for the mail id) and a blocking queue from
  // strings (the latter is equal to a mail) which is thread safe
  private Map<String, Map<Integer, BlockingQueue<String>>> userMailboxes;
  // a generator for incrementing the ids of the mails of a specific user
  private Map<String, AtomicInteger> emailIdGenerators;

  /**
   * Creates a new server instance.
   *
   * @param componentId the id of the component that corresponds to the Config resource
   * @param config      the component config
   * @param in          the input stream to read console input from
   * @param out         the output stream to write console output to
   */
  public MailboxServer(String componentId, Config config, InputStream in, PrintStream out) {
    this.componentId = componentId;
    this.config = config;
    this.shell = new Shell(in, out);
    this.shell.register(this);
    this.socketSet = ConcurrentHashMap.newKeySet();
  }

  // Mailbox servers only
  //accept messages that contain known addresses of the domain they manage (see Section 2.5 for details).
  //In the case that the to field of the message contains an unknown recipient, the server should respond
  //with an error message
  @Override
  public void run() {
    this.domain = this.config.getString("domain");
    this.users = new Config(this.config.getString("users.config"));

    DOMAIN = this.config.getString("domain");
    USERS = new Config(this.config.getString("users.config"));

    // initialize empty storage for mailboxes and storage for id generators for each user
    this.userMailboxes = new ConcurrentHashMap<>();
    this.emailIdGenerators = new ConcurrentHashMap<>();

    Set<String> usersInMailbox = users.listKeys();
    System.out.println("[MBOX SERVER] Initialize users");
    for (String username : usersInMailbox) {
      // initialize an empty mailbox for the current user
      Map<Integer, BlockingQueue<String>> mails = new ConcurrentHashMap<>();
      this.userMailboxes.put(username, mails);

      // start incrementing the mail ids from id = 0
      this.emailIdGenerators.put(username, new AtomicInteger(0));
    }

    System.out.println("Starting [MBOX SERVER]...");
    try {
      // Prepare to bind to the specified port, create and start new TCP Server Socket
      dmtpListener = new ServerSocket(config.getInt("dmtp.tcp.port"));
      dmapListener = new ServerSocket(config.getInt("dmap.tcp.port"));

      new MailServerThread(dmtpListener, config, socketSet, userMailboxes, emailIdGenerators, domain, users).start();
      new UserMailServerThread(dmapListener, config, socketSet, userMailboxes, users).start();

    } catch (IOException e) {
      throw new UncheckedIOException("Error while creating server socket", e);
    }

    System.out.println("Starting [SHELL]...");
    shell.run();
  }

  @Override
  @Command
  public void shutdown() {

    if (dmtpListener != null && !dmtpListener.isClosed()) {
      try {
        this.dmtpListener.close();
        System.out.println("[MBOX DMTP SERVER] listener closed");
      } catch (IOException e) {
        System.err.println("Error while closing server socket: " + e.getMessage());
      }
    }
    if (dmapListener != null && !dmapListener.isClosed()) {
      try {
        this.dmapListener.close();
        System.out.println("[MBOX DMAP SERVER] listener closed...");
      } catch (IOException e) {
        System.err.println("Error while closing dmap server socket");
      }
    }

    // close all open socket connections
    for (Socket openSocket : socketSet) {
      try {
        openSocket.close();
      } catch (IOException e) {
        // cannot be handled
      }
    }

    // close shell
    throw new StopShellException();

  }


  public static boolean isKnownUser(String username) {
    //TODO: question, can we make it static?

    return USERS.containsKey(username);
  }

  public static void main(String[] args) throws Exception {
    IMailboxServer server = ComponentFactory.createMailboxServer(args[0], System.in, System.out);
    server.run();
  }
}
