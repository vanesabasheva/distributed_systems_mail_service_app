package dslab.mailbox.tcp;

import dslab.protocol.DMAPProtocol;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;

public class MailClientHandlerThread extends Thread {
  private Socket client;
  private User user;

  public MailClientHandlerThread(Socket client) {
    this.client = client;
  }

  @Override
  public void run() {
    // prepare the input reader for the socket
    // prepare the writer for responding to clients requests

    while (true) {
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
           PrintWriter writer = new PrintWriter(client.getOutputStream(), true);
      ) {

        String request, response;
        // Initiate conversation with client
        DMAPProtocol dmapProtocol = new DMAPProtocol();
        response = dmapProtocol.processCommand(null);
        writer.println(response);

        // read client requests
        while ((request = reader.readLine()) != null) {
          //System.out.println("Client sent the following request: " + request);

          // Process the command and arguments
          response = dmapProtocol.processCommand(request);
          writer.println(response);
          if (response.equals("ok bye")) {
            break;
          }

          //TODO: handle unknown or malicious commands
          if (response.equals("error protocol error")) {
            writer.println(response);
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
