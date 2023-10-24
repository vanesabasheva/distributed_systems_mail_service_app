package dslab.transfer.tcp;

import dslab.protocol.DMTPProtocol;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;

public class ClientHandlerThread implements Runnable {
  private Socket client;
  private Email email;

  public ClientHandlerThread(Socket client) {
    this.client = client;
  }

  @Override
  public void run() {
    email = new Email(null, null, null, null);
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
        // Initiate conversation with client
        DMTPProtocol dmtpProtocol = new DMTPProtocol();
        response = dmtpProtocol.processCommand(null);
        writer.println(response);

        // read client requests
        while ((request = reader.readLine()) != null) {

          //System.out.println("Client sent the following request: " + request);

          // Process the command and arguments
          response = dmtpProtocol.processCommand(request);
          writer.println(response);
          if(response.equals("ok bye")){
            break;
          }

          //TODO: handle unknown or malicious commands
          if(response.equals("error unknown command")) {
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
