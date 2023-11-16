package dslab.mailbox.tcp.dmtp;

import dslab.protocol.DmtpClientProtocol;
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

// takes care of incoming TCP connections from Transfer Server, the protocol takes care for saving the messages in the corresponding
// mailbox if everything is correct
public class MailClientHandlerThread implements Runnable {
  private Socket client;
  private Config config;
  private DmtpClientProtocol protocol;
  private Set<Socket> socketSet;

  public MailClientHandlerThread(Socket client, Set<Socket> socketSet, Config config, DmtpClientProtocol protocol) {
    this.client = client;
    this.config = config;
    this.protocol = protocol;
    this.socketSet = socketSet;
  }

  @Override
  public void run() {

    // prepare the input reader for the socket
    // prepare the writer for responding to clients requests
    try (
        PrintWriter writer =
            new PrintWriter(this.client.getOutputStream(), true);
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(this.client.getInputStream()));
    ) {

      String request, response;

      // Initiate conversation with client which is the transfer server
      response = protocol.processCommand(null);

      writer.println(response);

      // read transfer server requests
      while ((request = reader.readLine()) != null) {
        response = protocol.processCommand(request);

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

    } catch (IOException e) {
      System.err.println("IO Exception in ClientHandlerThread");
      System.err.println(Arrays.toString(e.getStackTrace()));
      throw new UncheckedIOException(e);

    } finally {
      if (client != null && !client.isClosed()) {
        try {
          client.close();
          this.socketSet.remove(client);
        } catch (IOException e) {
          // Ignored because we cannot handle it
        }
      }
    }
  }
}
