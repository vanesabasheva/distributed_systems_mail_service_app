package dslab.transfer.tcp;

import dslab.protocol.DmtpServerProtocol;
import dslab.util.Config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerThread extends Thread {
  private final ServerSocket serverSocket;
  private final Config config;
  private final Config domains;
  private Set<Socket> socketSet;
  private BlockingQueue<String> email;
  private final ExecutorService pool = Executors.newCachedThreadPool();
  private final ExecutorService emailForwardingPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() / 2);

  public ServerThread(ServerSocket serverSocket, Config config, Config domains, Set<Socket> socketSet) {
    this.serverSocket = serverSocket;
    this.domains = domains;
    this.config = config;
    this.socketSet = socketSet;
  }

  @Override
  public void run() {
    try {
      while (true) {
        // [SERVER]: waits for a client to connect...
        Socket client = serverSocket.accept();
        // [SERVER]: Connects to a client
        ClientHandlerThread clientHandlerThread = new ClientHandlerThread(client, config, domains, new DmtpServerProtocol(), socketSet, emailForwardingPool);
        socketSet.add(client);

        // handle incoming connections from client in a separate thread
        // use the threads from the existing pool of threads
        pool.execute(clientHandlerThread);
      }
    } catch (SocketException e) {
      // when the socket is closed, the I/O methods of the Socket will throw a SocketException
      // almost all SocketException cases indicate that the socket was closed
      System.out.println("SocketException while handling socket: " + e.getMessage());
    } catch (IOException e) {
      // you should properly handle all other exceptions
      throw new UncheckedIOException(e);
    } finally {
      if(pool != null) {
        pool.shutdownNow();
      }
      if(emailForwardingPool != null) {
        emailForwardingPool.shutdownNow();
      }
      System.out.println("[POOLS: closed]");
    }

    System.out.println("[SERVER THREAD: closed]");
  }

}
