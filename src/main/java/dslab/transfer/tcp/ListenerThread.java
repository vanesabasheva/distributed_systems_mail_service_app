package dslab.transfer.tcp;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ListenerThread extends Thread {
  private ServerSocket serverSocket;
  private ArrayList<ClientHandlerThread> clients;
  private ArrayList<Socket> activeClientSockets;
  private ExecutorService pool = Executors.newCachedThreadPool();


  public ListenerThread(ServerSocket serverSocket) {
    this.serverSocket = serverSocket;
    this.clients = new ArrayList<>();
    this.activeClientSockets = new ArrayList<>();
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
        ClientHandlerThread clientHandlerThread = new ClientHandlerThread(client);
        clients.add(clientHandlerThread);

        // use the threads from the existing pool of threads
        pool.execute(clientHandlerThread);
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
