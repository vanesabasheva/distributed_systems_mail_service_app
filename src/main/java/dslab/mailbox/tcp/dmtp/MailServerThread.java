package dslab.mailbox.tcp.dmtp;

import dslab.protocol.DmtpClientProtocol;
import dslab.transfer.tcp.Email;
import dslab.util.Config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class MailServerThread extends Thread {
  private final ServerSocket serverSocket;
  private final Config config;
  private Set<Socket> socketSet;
  private Map<String, Map<Integer, Email>> userMailboxes;
  private Map<String, AtomicInteger> emailIdGenerators;
  private String domain;
  private Config users;
  private final ExecutorService pool = Executors.newCachedThreadPool();

  public MailServerThread(ServerSocket serverSocket, Config config, Set<Socket> socketSet,
                          Map<String, Map<Integer, Email>> userMailboxes,
                          Map<String, AtomicInteger> emailIdGenerators,
                          String domain, Config users) {
    this.serverSocket = serverSocket;
    this.config = config;
    this.socketSet = socketSet;
    this.userMailboxes = userMailboxes;
    this.emailIdGenerators = emailIdGenerators;
    this.domain = domain;
    this.users = users;
  }

  @Override
  public void run() {
    try {

      while (true) {
        // [MAILBOX SERVER]: waits for a client to connect (can be a TransferServer or a normal user)...
        Socket client = serverSocket.accept();
        // [MAILBOX SERVER]: Connects to a client
        MailClientHandlerThread MailClientHandlerThread = new MailClientHandlerThread(client, socketSet, config, new DmtpClientProtocol(userMailboxes, emailIdGenerators, domain, users));
        socketSet.add(client);

        // handle incoming connections from client in a separate thread
        // use the threads from the existing pool of threads
        pool.execute(MailClientHandlerThread);
      }
    } catch (SocketException e) {
      // when the socket is closed, the I/O methods of the Socket will throw a SocketException
      // almost all SocketException cases indicate that the socket was closed
      System.out.println("SocketException while handling socket: " + e.getMessage());
    } catch (IOException e) {
      // you should properly handle all other exceptions
      throw new UncheckedIOException(e);
    } finally {
      if (pool != null) {
        pool.shutdownNow();
      }
    }
  }
}
