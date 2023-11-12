package dslab.monitoring;

import at.ac.tuwien.dsg.orvell.Shell;
import at.ac.tuwien.dsg.orvell.StopShellException;
import at.ac.tuwien.dsg.orvell.annotation.Command;
import dslab.ComponentFactory;
import dslab.monitoring.udp.MonitoringServerThread;
import dslab.util.Config;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.DatagramSocket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


// The purpose of the monitoring server is to receive and display usage statistics of the outgoing traffic
//of transfers servers. Specifically, it records the amount of messages sent from specific servers and users.
//This is useful for analyzing message throughput of individual servers and users to, e.g., detect server
//abuse.
//The monitoring server receives usage statistics via a UDP socket. For each mail sent by a transfer server,
//it sends a UDP packet to the monitoring server containing the transfer server’s host and port, and the
//sender’s email address. The packet contains the information as plain text: see formatting in file
public class MonitoringServer implements IMonitoringServer {
  private String componentId;
  private Config config;
  private DatagramSocket datagramSocket;
  private Map<String, Integer> addresses;
  private Map<String, Integer> servers;
  private Shell shell;

  /**
   * Creates a new server instance.
   *
   * @param componentId the id of the component that corresponds to the Config resource
   * @param config      the component config
   * @param in          the input stream to read console input from
   * @param out         the output stream to write console output to
   */
  public MonitoringServer(String componentId, Config config, InputStream in, PrintStream out) {
    this.componentId = componentId;
    this.config = config;
    this.shell = new Shell(in, out);
    this.shell.register(this);
  }

  @Override
  public void run() {
    this.addresses = new ConcurrentHashMap<>();
    this.servers = new ConcurrentHashMap<>();

    try {
      // constructs a datagram socket and binds it to the specified port
      datagramSocket = new DatagramSocket(config.getInt("udp.port"));
      // create a new thread to listen for incoming packets
      new MonitoringServerThread(datagramSocket, addresses, servers).start();
    } catch (IOException e) {
      throw new RuntimeException("Cannot listen on UDP port.", e);
    }

    System.out.println("[MON SERVER] Starting Shell...");
    shell.run();

  }

  @Override
  @Command
  public void addresses() {
    for (Map.Entry<String, Integer> entry : this.addresses.entrySet()) {
      shell.out().println(entry.getKey() + " " + entry.getValue());
    }
  }

  @Override
  @Command
  public void servers() {
    for (Map.Entry<String, Integer> entry : this.servers.entrySet()) {
      shell.out().println(entry.getKey() + " " + entry.getValue());
    }
  }

  @Override
  @Command
  public void shutdown() {
    // close socket and listening thread
    /*
     * Note that closing the socket also triggers an exception in the
     * listening thread
     */
    if (datagramSocket != null) {
      datagramSocket.close();
    }

    // close shell
    throw new StopShellException();
  }

  public static void main(String[] args) throws Exception {
    IMonitoringServer server = ComponentFactory.createMonitoringServer(args[0], System.in, System.out);
    server.run();
  }

}
