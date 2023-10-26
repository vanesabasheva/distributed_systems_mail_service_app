package dslab.mailbox;

import dslab.ComponentFactory;
import dslab.protocol.DmapProtocol;
import dslab.protocol.DmtpClientProtocol;
import dslab.tcp.ServerThread;
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
    USERS_FILE = "src\\main\\resources\\" + this.config.getString("users.config");
    DOMAIN = this.config.getString("domain");
  }

  // Mailbox servers only
  //accept messages that contain known addresses of the domain they manage (see Section 2.5 for details).
  //In the case that the to field of the message contains an unknown recipient, the server should respond
  //with an error message
  @Override
  public void run() {
    try {
      // Prepare to bind to the specified port, create and start new TCP Server Socket
      dmtp_listener = new ServerSocket(config.getInt("dmtp.tcp.port"));
      dmap_listener = new ServerSocket(config.getInt("dmap.tcp.port"));

      new ServerThread(dmtp_listener, config, new DmtpClientProtocol()).start();
      new ServerThread(dmap_listener, config, new DmapProtocol()).start();
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

    // Set users of the current mailbox, parse their username and password and store them in the map
    usernameToPasswordMap = new HashMap<>();
    System.out.println(USERS_FILE);
    Properties users = new Properties();
    users.load(new FileReader(USERS_FILE));
    for (String key : users.stringPropertyNames()) {
      String value = users.getProperty(key);
      usernameToPasswordMap.put(key, value);
    }

    server.run();
  }
}
