package dslab.mailbox.tcp.dmap;

import dslab.protocol.DmapProtocol;
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

public class UserMailServerThread extends Thread {
  private ServerSocket serverSocket;
  private Set<Socket> socketSet;
  private Config users;
  private Map<String, Map<Integer, Email>> userMailboxes;
  private final ExecutorService pool = Executors.newCachedThreadPool();

  public UserMailServerThread(ServerSocket serverSocket, Set<Socket> socketSet,
                              Map<String, Map<Integer, Email>> userMailboxes,
                              Config users) {
    this.serverSocket = serverSocket;
    this.socketSet = socketSet;
    this.userMailboxes = userMailboxes;
    this.users = users;
  }

  @Override
  public void run() {
    try {

      while (true) {
        // [MAILBOX SERVER]: waits for a client to connect (can be a TransferServer or a normal user)...
        Socket client = serverSocket.accept();
        // [MAILBOX SERVER]: Connects to a client
        UserMailClientHandlerThread MailClientHandlerThread = new UserMailClientHandlerThread(client, socketSet, new DmapProtocol(userMailboxes, users));
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
