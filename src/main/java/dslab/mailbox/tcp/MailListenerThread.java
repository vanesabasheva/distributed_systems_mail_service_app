package dslab.mailbox.tcp;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MailListenerThread extends Thread {
  private ServerSocket serverSocket;
  private ArrayList<MailClientHandlerThread> clients;
  private ExecutorService pool = Executors.newCachedThreadPool();

  public MailListenerThread(ServerSocket serverSocket)  {
    this.serverSocket = serverSocket;
    this.clients = new ArrayList<>();
  }

  @Override
  public void run() {
    while (true) {
      Socket client = null;
      try {
        // [SERVER]: waits for a client to connect...
        client = serverSocket.accept();
        // [SERVER]: Connects to a client
        // handle incoming connections from client in a separate thread
        MailClientHandlerThread mailClientHandlerThread = new MailClientHandlerThread(client);
        clients.add(mailClientHandlerThread);

        // use the threads from the existing pool of threads
        pool.execute(mailClientHandlerThread);
      } catch (IOException e) {
        // you should properly handle all other exceptions
        throw new UncheckedIOException(e);
      }
    }
  }
}
