package dslab.transfer;

import dslab.ComponentFactory;
import dslab.transfer.tcp.ListenerThread;
import dslab.util.Config;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// When a client wants to send a message, it has to connect to a server that speaks DMTP via TCP, and
// send the instructions over the socket. A server that accepts DMTP instructions will immediately respond to each instruction with a specific
// answer. This communication pattern is known as synchronous request–response. Each time a valid
// instruction is received, the server responds with ok. When a client connects, the server initially sends the
// string: ok DMTP thereby telling the client that the server is ready and speaks DMTP. If the instruction
// caused an error, the server responds with error <explanation>. Repeated commands (e.g., setting the
// subject twice) will overwrite the previous value. If the server receives an instruction that is undefined,
// then it may immediately terminate the connection.

// It is both a DMTP server and a DMTP client. When a user connects to a transfer server, the
//server acts as a DMTP server. When the transfer server connects to a mailbox server, the transfer server
//takes the role of a DMTP client. Unlike mailbox servers, transfer servers accept messages from any
//recipient domain.

// The mailbox server is responsible for receiving and storing mails, and providing means for users to
// access them via the DMAP server protocol. Each mailbox server is associated with exactly one domain,
// e.g., example.com or univer.ze and manages email addresses ending with these domains. Only mails to
// recipients ending with the managed domain are stored.
// A mailbox server receives emails by also providing the DMTP server protocol. However, unlike transfer
// servers, mailbox servers do not forward messages to other mailbox DMTP servers, as they do not have
// the capability to lookup mail domains. Mailbox servers should be able to handle multiple DMTP as well
// as DMAP connections at once. An important feature of the mailbox servers is to store messages for users
// even if they are not currently logged in.

public class TransferServer implements ITransferServer, Runnable {

  private Config config;
  private String componentId;
  private InputStream in;
  private PrintStream out;
  private ServerSocket listener;
  private static final String PROPERTIES_FILE = "src\\main\\resources\\domains.properties";
  private static Map<String, String> domainToSocketAddressMap;
  private static Properties properties;

  /**
   * Creates a new server instance.
   *
   * @param componentId the id of the component that corresponds to the Config resource
   * @param config      the component config
   * @param in          the input stream to read console input from
   * @param out         the output stream to write console output to
   */
  public TransferServer(String componentId, Config config, InputStream in, PrintStream out) {
    this.componentId = componentId;
    this.config = config;
    this.in = in;
    this.out = out;

  }

  @Override
  public void run() {
    try {
      // Prepare to bind to the specified port, create and start new TCP Server Socket
      listener = new ServerSocket(config.getInt("tcp.port"));
      new ListenerThread(listener).start();

    } catch (IOException e) {
      throw new UncheckedIOException("Error while creating server socket", e);
    }
    // System.out.println("ok DMTP");
    // create a reader to read from the console
    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

    try {
      // read commands from the console
      reader.readLine();
    } catch (IOException e) {
      // IOException from System.in is very very unlikely (or impossible)
      // and cannot be handled
    }
    // close socket and listening thread
    //shutdown();
  }

  @Override
  public void shutdown() {
    if (listener != null && !listener.isClosed()) {
      try {
        in.close();
        out.close();
        listener.close();
      } catch (IOException e) {
        e.printStackTrace();
        System.err.println("Error while closing server socket: " + e.getMessage());
      }
    }
  }

  public static void main(String[] args) throws Exception {
    ITransferServer server = ComponentFactory.createTransferServer(args[0], System.in, System.out);
    domainToSocketAddressMap = new HashMap<>();
    properties = new Properties();
    properties.load(new FileReader(PROPERTIES_FILE));
    for (String key: properties.stringPropertyNames()) {
      String value = properties.getProperty(key);
      domainToSocketAddressMap.put(key, value);
    }
    server.run();
  }

  public static String getSocketAddressForDomain(String domain) {
      return domainToSocketAddressMap.get(domain);
  }

}
