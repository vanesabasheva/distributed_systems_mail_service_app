package dslab.transfer.tcp;

import dslab.util.Config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.BlockingQueue;

public class EmailForwardingThread implements Runnable {

  private Config config;
  private Config domains;
  private String recipientDomain;
  private final BlockingQueue<String> email;

  public EmailForwardingThread(String recipientDomain, Config config, Config domains, BlockingQueue<String> email) {
    this.recipientDomain = recipientDomain;
    this.config = config;
    this.domains = domains;
    this.email = email;

  }

  @Override
  public void run() {
    Socket socket = null;

    try {

      String[] receiverAddress = this.domainLookup(recipientDomain);
      if (receiverAddress == null) {
        System.out.println("[ERROR]: No such domain found");
        this.sendErrorEmail("domain not found for " + recipientDomain);
        return;
      }

      /*
       * create a new tcp socket at specified host and port - make sure
       * you specify them correctly in the domain properties file
       */
      String ip = receiverAddress[0];
      int port = Integer.parseInt(receiverAddress[1]);

      socket = new Socket(ip, port);
      // create a reader to retrieve messages send by the server
      BufferedReader serverReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      // create a writer to send messages to the server
      PrintWriter serverWriter = new PrintWriter(socket.getOutputStream(), true);

      String connectionEstablishedMessage = serverReader.readLine();
      if (!connectionEstablishedMessage.equals("ok DMTP")) {
        this.sendErrorEmail("error when contacting server");
        return;
      }

      //read email sent from user
      for (String sendLine : email) {

        if (sendLine == null) {
          sendLine = "quit";
        }

        //write provided user input to the socket
        serverWriter.println(sendLine);
        String serverResponse = serverReader.readLine();

        if (serverResponse.startsWith("error")) {
          this.sendErrorEmail("mailbox responded with an error message: " + serverResponse);
          return;
        }

        if (sendLine.equalsIgnoreCase("quit")) {
          break;
        }
      }
      System.out.println("[Forwarding THREAD] message successfully forwarded");

    } catch (UnknownHostException e) {
      System.out.println("Cannot connect to host: " + e.getMessage());
    } catch (SocketException e) {
      // when the socket is closed, the I/O methods of the Socket will throw a SocketException
      // almost all SocketException cases indicate that the socket was closed
      System.out.println("SocketException while handling socket: " + e.getMessage());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } finally {
      if (socket != null && !socket.isClosed()) {
        try {
          socket.close();
        } catch (IOException e) {
          // Ignored because we cannot handle it
        }
      }
    }
  }

  public String[] domainLookup(String domain) {
    if (!this.domains.containsKey(domain)) {
      return null;
    }

    String ipAndPort = this.domains.getString(domain);
    return ipAndPort.split(":", 2);
  }

  public void sendErrorEmail(String errorMessage) {
    // TODO: implement sending error message to the user

  }
}
