package dslab.transfer.tcp;

import dslab.protocol.DmtpServerProtocol;

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

public class ClientHandlerThread implements Runnable {
  private Socket client;
  private DmtpServerProtocol protocol;
  private Set<Socket> socketSet;
  private BlockingQueue<Email> emailsToBeSent;

  public ClientHandlerThread(Socket client, DmtpServerProtocol protocol, Set<Socket> socketSet,
                             BlockingQueue<Email> emailsToBeSent) {
    this.client = client;
    this.protocol = protocol;
    this.socketSet = socketSet;
    this.emailsToBeSent = emailsToBeSent;
  }

  @Override
  public void run() {

    // prepare the input reader for the socket
    // prepare the writer for responding to clients requests
    try (
        PrintWriter writer =
            new PrintWriter(client.getOutputStream(), true);
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(client.getInputStream()))
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

        if (request.startsWith("send") && response.equals("ok")) {
          this.emailsToBeSent.add(this.protocol.getEmail());
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
          System.out.println("[ClientHandlerThread] Socket is closed ");

        } catch (IOException e) {
          // Ignored because we cannot handle it
        }
      }
    }
  }
}
