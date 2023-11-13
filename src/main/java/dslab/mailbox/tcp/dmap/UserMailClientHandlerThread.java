package dslab.mailbox.tcp.dmap;

import dslab.protocol.DmapProtocol;
import dslab.util.Config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

public class UserMailClientHandlerThread implements Runnable {
  private Socket client;
  private Config config;
  private DmapProtocol protocol;

  public UserMailClientHandlerThread(Socket client, Config config, DmapProtocol protocol) {
    this.client = client;
    this.config = config;
    this.protocol = protocol;
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

        // Initiate conversation with client which can be a normal user or the transfer server

        response = protocol.processCommand(null);

        writer.println(response);

        // read client requests
        while ((request = reader.readLine()) != null) {

          // Process the command and arguments depending on the type of client (user or transfer server)
          response = protocol.processCommand(request);

          // case when the user wants to see all mails in his mailbox and the protocol responded with okay
          String[] tokens = request.split("\\s", 2);
          if(tokens[0].equalsIgnoreCase("list") && response.equals("ok")) {
            Map<Integer, BlockingQueue<String>> mails = this.protocol.getCurrentMailbox();
            response = "";


            for (Map.Entry<Integer, BlockingQueue<String>> mail: mails.entrySet()) {
              BlockingQueue<String> lines = mail.getValue();
              String from = "";
              String subject = "";

              // process email and get the information for sender and subject
              for (String line: lines) {
                if(line.contains("from") && line.contains("@")) {
                  from = line.split("\\s")[1];
                } else if(line.contains("subject")) {
                  subject = line.split("\\s", 2)[1];
                }

                if(from != "" && subject != ""){
                  break;
                }
              }

              // add new line to the response so that the next email summary is printed on the next line
              if(response.length() > 0) {
                response += System.lineSeparator();
              }


              response += mail.getKey() + " " + from + " " + subject;
            }
          }

          if (tokens[0].equalsIgnoreCase("show") && response.equals("ok")) {
            BlockingQueue<String> mail = protocol.getEmailWithId(Integer.parseInt(tokens[1]));
            response = "";
            int count = 0;

            // process every line of the email and save it to the response variable
            for (String line: mail) {

              response += line;
              count++;


              // if we are not at the end of the mail, add a new line
              if(count < mail.size()){
                response += System.lineSeparator();
              }
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
