package dslab.mailbox.tcp;

import dslab.protocol.IProtocol;
import dslab.util.Config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MailServerThread extends Thread{
  private final ServerSocket serverSocket;
  private final Config config;
  private final IProtocol protocol;
  private final ExecutorService pool = Executors.newCachedThreadPool();

  //TODO: ADD SOCKETS AND THREADS TO ARRAY AND REMOVE THEM AFTER CLOSE
  private final ArrayList<MailClientHandlerThread> clients = new ArrayList<>();
  //private ArrayList<Socket> activeClientSockets = new ArrayList<>();


  public MailServerThread(ServerSocket serverSocket, Config config, IProtocol protocol) {
    this.serverSocket = serverSocket;
    this.config = config;
    this.protocol = protocol;
  }

  @Override
  public void run() {

    while (true) {
      try {
        // [MAILBOX SERVER]: waits for a client to connect (can be a TransferServer or a normal user)...
        // [MAILBOX SERVER]: Connects to a client
        MailClientHandlerThread mailClientHandlerThread = new MailClientHandlerThread(serverSocket.accept(), config, protocol);
        clients.add(mailClientHandlerThread);

        // handle incoming connections from client in a separate thread
        // use the threads from the existing pool of threads
        pool.execute(mailClientHandlerThread);
      } catch (SocketException e) {
        // when the socket is closed, the I/O methods of the Socket will throw a SocketException
        // almost all SocketException cases indicate that the socket was closed
        System.out.println("SocketException while handling socket: " + e.getMessage());
        break;
      } catch (IOException e) {
        // you should properly handle all other exceptions
        throw new UncheckedIOException(e);
      }
    }
  }
}
