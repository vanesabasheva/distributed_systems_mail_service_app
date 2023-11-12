package dslab.transfer.tcp;

import dslab.util.Config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;

public class EmailForwardingThread implements Runnable {

  private Config config;
  private Config domains;
  private String recipientDomain;
  private String sender;
  private final BlockingQueue<String> email;

  public EmailForwardingThread(String recipientDomain, String sender, Config config, Config domains, BlockingQueue<String> email) {
    this.recipientDomain = recipientDomain;
    this.sender = sender;
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
        System.out.println("[Email Forwarding Thread ERROR]: No such domain found");
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

        if (sendLine.contains("send")) {
          System.out.println("[Forwarding THREAD] message successfully forwarded");
        } else if (sendLine.equalsIgnoreCase("quit")) {
          System.out.println("[EMAIL Forwarding THREAD] message successfully forwarded");
          this.sendDatagramPacket(this.sender);
          break;
        }
      }

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
    String ip;
    try {
      ip = InetAddress.getLocalHost().getHostAddress();
    } catch (UnknownHostException e) {
      System.out.println("[ERROR] could not get ip address of transfer server");
      return;
    }

    String to = "to " + this.sender;
    String from = "from mailer@" + ip;
    String subject = "subject error";


    String[] senderMail = to.split("@");
    String[] senderIpAndPort = this.domainLookup(senderMail[1]);
    if (senderIpAndPort == null) {
      System.out.println("[Email Forwarding THREAD] Sender mail not found");
      return;
    }

    String senderIp = senderIpAndPort[0];
    int senderPort = Integer.parseInt(senderIpAndPort[1]);

    Socket socket = null;
    try {
      socket = new Socket(senderIp, senderPort);

      BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);

      String serverResponse = reader.readLine();
      System.out.println(" mailbox server responded: " + serverResponse);

      writer.println("begin");
      serverResponse = reader.readLine();
      System.out.println(" mbox server responded: " + serverResponse);

      writer.println(to);
      serverResponse = reader.readLine();
      System.out.println(" mbox server responded: " + serverResponse);

      writer.println(from);
      serverResponse = reader.readLine();
      System.out.println(" mbox server responded: " + serverResponse);

      writer.println(subject);
      serverResponse = reader.readLine();
      System.out.println(" mbox server responded: " + serverResponse);

      writer.println("data " + errorMessage);
      serverResponse = reader.readLine();
      System.out.println(" mbox server responded: " + serverResponse);

      writer.println("send");
      serverResponse = reader.readLine();
      System.out.println(" mbox server responded: " + serverResponse);

      writer.println("quit");


      reader.readLine();
      System.out.println("message successfully forwarded");
      this.sendDatagramPacket(sender);

    } catch (IOException e) {
      throw new RuntimeException(e);
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

  public void sendDatagramPacket(String sender) {
    String ip;
    try {
      ip = InetAddress.getLocalHost().getHostAddress();
    } catch (UnknownHostException e) {
      System.out.println("[ERROR] could not get ip address of transfer server");
      return;
    }

    int port = this.config.getInt("tcp.port");
    String message = ip + ":" + port + " " + sender;

    byte[] buffer;
    DatagramSocket socket = null;
    DatagramPacket packet;
    try {
      socket = new DatagramSocket();
      buffer = message.getBytes();
      packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(config.getString("monitoring.host")), config.getInt("monitoring.port"));
      socket.send(packet);

    } catch (SocketException e) {
      System.out.println("Socket exception while handling socket: " + e.getMessage());

    } catch (IOException e) {
      System.err.println("IO Exception in ClientHandlerThread");
      System.err.println(Arrays.toString(e.getStackTrace()));
      throw new UncheckedIOException(e);

    } finally {
      if (socket != null && !socket.isClosed()) {
        socket.close();
      }
    }
  }
}
