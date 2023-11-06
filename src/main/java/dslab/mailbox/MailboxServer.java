package dslab.mailbox;

import at.ac.tuwien.dsg.orvell.Shell;
import at.ac.tuwien.dsg.orvell.StopShellException;
import at.ac.tuwien.dsg.orvell.annotation.Command;
import dslab.ComponentFactory;
import dslab.mailbox.tcp.MailServerThread;
import dslab.protocol.DmapProtocol;
import dslab.protocol.DmtpClientProtocol;
import dslab.util.Config;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class MailboxServer implements IMailboxServer, Runnable {
  private static Map<String, String> usernameToPasswordMap;
  private static String USERS_FILE = "";
  private static String DOMAIN = "";
  private Config config;
  private String componentId;
  private InputStream in;
  private PrintStream out;
  private Shell shell;
  private ServerSocket dmap_listener;
  private ServerSocket dmtp_listener;

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
    this.in = in;
    this.out = out;
    this.shell = new Shell(in, out);
    this.shell.register(this);
  }

  // Mailbox servers only
  //accept messages that contain known addresses of the domain they manage (see Section 2.5 for details).
  //In the case that the to field of the message contains an unknown recipient, the server should respond
  //with an error message
  @Override
  public void run() {
    System.out.println("Starting [M SERVER]...");

    try {
      // Prepare to bind to the specified port, create and start new TCP Server Socket
      dmtp_listener = new ServerSocket(config.getInt("dmtp.tcp.port"));
      dmap_listener = new ServerSocket(config.getInt("dmap.tcp.port"));

      new MailServerThread(dmtp_listener, config, new DmtpClientProtocol()).start();
      new MailServerThread(dmap_listener, config, new DmapProtocol()).start();
      //new MailListenerThread(dmap_listener).start();

    } catch (IOException e) {
      throw new UncheckedIOException("Error while creating server socket", e);
    }

    while (true) {
      try (
          PrintWriter writer = new PrintWriter(out, true);
          BufferedReader reader = new BufferedReader(new InputStreamReader(in))
      ) {
        if (reader.readLine().equals("shutdown")) {
          this.shutdown();
          break;
        }
      } catch (IOException e) {
        // IOException from System.in is very very unlikely (or impossible)
        // and cannot be handled
        throw new RuntimeException(e);
      }
    }
  }

  @Override
  @Command
  public void shutdown() {
    try {
      in.close();
      out.close();
      dmtp_listener.close();
      dmap_listener.close();
    } catch (IOException e) {
      e.printStackTrace();
      System.err.println("Error while closing server socket: " + e.getMessage());
    }

    if (dmtp_listener != null && !dmtp_listener.isClosed()) {
      try {
        dmtp_listener.close();
      } catch (IOException e) {
        System.err.println("Error while closing server socket: " + e.getMessage());
      }
    }
    if (dmap_listener != null && !dmap_listener.isClosed()) {
      try {

        dmap_listener.close();
      } catch (IOException e) {
        System.err.println("Error while closing dmap server socket");
      }
    }

    // close shell
    throw new StopShellException();

  }

  // Checks if the username password pair is stored in this mailbox and returns a string
  // that notifies if the username/password is incorrect or if login is accepted
  public static String authenticateUser(String username, String password) {
    /*
    System.out.println("Debug mailbox nullpointer");
    System.out.println(usernameToPasswordMap);

     */
    if (!isKnownUser(username)) {
      return "error unknown user";
    } else if (!usernameToPasswordMap.get(username).equals(password)) {
      return "error wrong password";
    }
    return "ok";
  }

  public static String getDOMAIN() {
    return DOMAIN;
  }

  public static boolean isKnownUser(String username) {
    return usernameToPasswordMap.containsKey(username);
  }

  public static void main(String[] args) throws Exception {
    IMailboxServer server = ComponentFactory.createMailboxServer(args[0], System.in, System.out);
    server.run();
  }
}
