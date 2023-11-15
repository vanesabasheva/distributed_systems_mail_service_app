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
import java.util.concurrent.LinkedBlockingQueue;

public class ServerThread extends Thread {
  private static final int NUM_FORWARDING_THREADS = Runtime.getRuntime().availableProcessors() / 2;
  private final ServerSocket serverSocket;
  private final Config config;
  private final Config domains;
  private Set<Socket> socketSet;

  // producer - consumer problem: save emails to be sent in a blocking queue
  private BlockingQueue<Email> emailsToBeSent = new LinkedBlockingQueue<>();
  private final ExecutorService pool = Executors.newCachedThreadPool();
  private final ExecutorService emailForwardingPool = Executors.newFixedThreadPool(NUM_FORWARDING_THREADS);

  public ServerThread(ServerSocket serverSocket, Config config, Config domains, Set<Socket> socketSet) {
    this.serverSocket = serverSocket;
    this.config = config;
    this.domains = domains;
    this.socketSet = socketSet;
  }

  @Override
  public void run() {
    try {
      while (true) {
        // [SERVER]: waits for a client to connect...
        Socket client = serverSocket.accept();
        // [SERVER]: Connects to a client
        ClientHandlerThread clientHandlerThread =
            new ClientHandlerThread(client, new DmtpServerProtocol(), socketSet, emailsToBeSent);
        socketSet.add(client);

        // handle incoming connections from client in a separate thread
        // use the threads from the existing pool of threads
        pool.execute(clientHandlerThread);
        for (int i = 0; i < NUM_FORWARDING_THREADS; i++) {
          EmailForwardingThread emailForwardingThread = new EmailForwardingThread(config, domains, emailsToBeSent);
          emailForwardingPool.execute(emailForwardingThread);
        }
      }
    } catch (SocketException e) {
      // when the socket is closed, the I/O methods of the Socket will throw a SocketException
      // almost all SocketException cases indicate that the socket was closed
      System.out.println("SocketException while handling socket: " + e.getMessage());

    } catch (IOException e) {
      throw new UncheckedIOException(e);

    } finally {
      if (pool != null) {
        pool.shutdownNow();
      }
      if (emailForwardingPool != null) {
        emailForwardingPool.shutdownNow();
      }
      System.out.println("[POOLS: closed]");
    }

    System.out.println("[SERVER THREAD: closed]");
  }

}
