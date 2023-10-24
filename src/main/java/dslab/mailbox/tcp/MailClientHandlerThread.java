package dslab.mailbox.tcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Objects;

public class MailClientHandlerThread extends Thread {
  private Socket client;
  private BufferedReader reader;
  private PrintWriter writer;
  private User user;

  public MailClientHandlerThread(Socket client) {
    this.client = client;
  }

  @Override
  public void run() {
    while (true) {
      try {
        // prepare the input reader for the socket
        reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
        // prepare the writer for responding to clients requests
        writer = new PrintWriter(client.getOutputStream());

        writer.println("ok DMTP");
        writer.flush();

        String request;
        // read client requests
        while ((request = reader.readLine()) != null) {
          System.out.println("Client sent the following request: " + request);

          String[] tokens = request.split(" ", 2); // Split the message into command and arguments
          String command = tokens[0].toLowerCase();
          String arguments = (tokens.length > 1) ? tokens[1] : null;

          // Process the command and arguments
          String response = processCommand(command, arguments);

          //TODO: handle unknown or malicious commands
          if (response.equals("error unknown command")) {
            writer.println(response);
            break;
          }
          // print request
          writer.println(response);
          writer.flush();

          if (command.equals("quit")) {
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
            writer.close();
            reader.close();
            client.close();
          } catch (IOException e) {
            // Ignored because we cannot handle it
          }
        }
      }
    }

  }

  private String processCommand(String command, String arguments) {
    switch (command) {

      case "login":
        if (arguments == null || Objects.equals(arguments, "")) {
          return "error usage: 'login <username> <password'";
        }
        String[] user = arguments.split("\\s");
        if (user.length > 2 || user.length < 1) {
          return "error usage: 'login <username> <password'";
        }

        this.user.setUsername(user[0]);
        this.user.setPassword(user[1]);
        return "ok ";

      case "list":
        if(this.user == null) {
          return "error not logged in";
        }
        return "ok";

      case "show":
        if(this.user == null) {
          return "error not logged in";
        }
        return "ok";

      case "data":
        return "ok";

      case "send":
        return "ok";

      case "quit":
        // Handle 'quit' command
        // ...
        return "ok bye";

      default:
        // Handle undefined commands
        return "error Unknown command";
    }
  }

}
