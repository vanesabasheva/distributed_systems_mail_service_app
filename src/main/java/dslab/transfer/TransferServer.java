package dslab.transfer;

import at.ac.tuwien.dsg.orvell.Shell;
import at.ac.tuwien.dsg.orvell.StopShellException;
import at.ac.tuwien.dsg.orvell.annotation.Command;
import dslab.ComponentFactory;
import dslab.transfer.tcp.ServerThread;
import dslab.util.Config;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class TransferServer implements ITransferServer, Runnable {

  private Config config;
  private String componentId;
  private ServerSocket listener;
  private Shell shell;
  private Config domains;
  private Set<Socket> socketSet;

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
    this.shell = new Shell(in, out);
    this.shell.register(this);
    this.socketSet = ConcurrentHashMap.newKeySet();
  }

  @Override
  public void run() {
    this.domains = new Config("domains.properties");
    System.out.println("Starting [T SERVER]...");

    try {
      // Prepare to bind to the specified port, create and start new TCP Server Socket
      listener = new ServerSocket(config.getInt("tcp.port"));
      new ServerThread(listener, config, domains, socketSet).start();
    } catch (IOException e) {
      throw new UncheckedIOException("Error while creating server socket", e);
    }

    System.out.println("Starting [SHELL]...");
    shell.run();
  }

  @Override
  @Command
  public void shutdown() {
    if (this.listener != null && !this.listener.isClosed()) {
      try {
        this.listener.close();
        System.out.println("[T SERVER] listener closed ...");
      } catch (IOException e) {
        e.printStackTrace();
        System.err.println("Error while closing server socket: " + e.getMessage());
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

  public static void main(String[] args) throws Exception {
    ITransferServer server = ComponentFactory.createTransferServer(args[0], System.in, System.out);
    server.run();
  }


}
