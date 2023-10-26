package dslab.tcp;

import dslab.protocol.IProtocol;
import dslab.tcp.ClientHandlerThread;
import dslab.util.Config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerThread extends Thread {
  private final ServerSocket serverSocket;
  private final Config config;
  private final IProtocol protocol;
  private final ExecutorService pool = Executors.newCachedThreadPool();

  //TODO: ADD SOCKETS AND THREADS TO ARRAY AND REMOVE THEM AFTER CLOSE
  private final ArrayList<ClientHandlerThread> clients = new ArrayList<>();
  //private ArrayList<Socket> activeClientSockets = new ArrayList<>();


  public ServerThread(ServerSocket serverSocket, Config config, IProtocol protocol) {
    this.serverSocket = serverSocket;
    this.config = config;
    this.protocol = protocol;
  }

  @Override
  public void run() {
    while (true) {
      try {
        // [SERVER]: waits for a client to connect...
        // [SERVER]: Connects to a client
        ClientHandlerThread clientHandlerThread = new ClientHandlerThread(serverSocket.accept(), config, protocol);
        clients.add(clientHandlerThread);

        // handle incoming connections from client in a separate thread
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
