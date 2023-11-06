package dslab.transfer.tcp;

import dslab.protocol.DmapProtocol;
import dslab.protocol.DmtpClientProtocol;
import dslab.protocol.DmtpServerProtocol;
import dslab.protocol.IProtocol;
import dslab.util.Config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;

public class ClientHandlerThread implements Runnable {
  private Socket client;
  private Config config;
  private Config domains;
  private DmtpServerProtocol protocol;
  private Set<Socket> socketSet;
  private ExecutorService emailForwardingPool;
  private BlockingQueue<String> email;

  public ClientHandlerThread(Socket client, Config config, Config domains, DmtpServerProtocol protocol, Set<Socket> socketSet, ExecutorService emailForwardingPool) {
    this.client = client;
    this.config = config;
    this.domains = domains;
    this.protocol = protocol;
    this.socketSet = socketSet;
    this.emailForwardingPool = emailForwardingPool;
  }

  @Override
  public void run() {
    while (true) {
      // prepare the input reader for the socket
      // prepare the writer for responding to clients requests
      try (
          PrintWriter writer =
              new PrintWriter(client.getOutputStream(), true);
          BufferedReader reader = new BufferedReader(
              new InputStreamReader(client.getInputStream()));
      ) {

        String request, response;

        // Initiate conversation with client depending on whether a user or the transfer server  is the client

        response = protocol.processCommand(null);

        writer.println(response);

        // read client requests
        while ((request = reader.readLine()) != null) {

          //System.out.println("Client sent the following request: " + request);

          // Process the command and arguments depending on the type of client (user or transfer server)
          response = protocol.processCommand(request);

          // if the client is done writing the mail and the dmtp protcol accepts it, a forwarding thread handles the sending
          // to the mailbox
          if(request.startsWith("send") && response.equals("ok")) {
            for (String recipientDomain: protocol.getRecipientsDomain()) {
              this.emailForwardingPool.execute(new EmailForwardingThread(recipientDomain, config, domains, this.protocol.getEmail()));
            }
          }

          writer.println(response);
          if (response.equals("ok bye")) {
            break;
          }

          if (response.equals("error protocol error")) {
            break;
          }
        }

      } catch (SocketException e) {
        // when the socket is closed, the I/O methods of the Socket will throw a SocketException
        // almost all SocketException cases indicate that the socket was closed
        System.out.println("SocketException while handling socket: " + e.getMessage());
        break;
      } catch (IOException e) {
        System.err.println("IO Exception in ClientHandlerThread");
        System.err.println(Arrays.toString(e.getStackTrace()));
        // you should properly handle all other exceptions
        throw new UncheckedIOException(e);
      } finally {
        if (client != null && !client.isClosed()) {
          try {
            client.close();
          } catch (IOException e) {
            // Ignored because we cannot handle it
          }
        }
      }
    }
  }
}
