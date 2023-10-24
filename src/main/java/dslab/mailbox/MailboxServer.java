package dslab.mailbox;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import dslab.ComponentFactory;
import dslab.transfer.tcp.ListenerThread;
import dslab.util.Config;

public class MailboxServer implements IMailboxServer, Runnable {

    private Config config;
    private String componentId;
    private InputStream in;
    private PrintStream out;
    private static Map <String, String> usernameToPasswordMap;
    private static Properties users;
    private static String USERS_FILE = "";
    private ServerSocket dmap_listener;
    private ServerSocket dmtp_listener;

    /**
     * Creates a new server instance.
     *
     * @param componentId the id of the component that corresponds to the Config resource
     * @param config the component config
     * @param in the input stream to read console input from
     * @param out the output stream to write console output to
     */
    public MailboxServer(String componentId, Config config, InputStream in, PrintStream out) {
        this.componentId = componentId;
        this.config = config;
        this.in = in;
        this.out = out;
        USERS_FILE = "src\\main\\resources\\" + this.config.getString("users.config" );
    }

    // Mailbox servers only
    //accept messages that contain known addresses of the domain they manage (see Section 2.5 for details).
    //In the case that the to field of the message contains an unknown recipient, the server should respond
    //with an error message
    @Override
    public void run() {
        try {
            // Prepare to bind to the specified port, create and start new TCP Server Socket
            dmtp_listener = new ServerSocket(config.getInt("dmtp.tcp.port"));
            dmap_listener = new ServerSocket(config.getInt("dmap.tcp.port"));

            new ListenerThread(dmtp_listener).start();
            new ListenerThread(dmap_listener).start();

        } catch (IOException e) {
            throw new UncheckedIOException("Error while creating server socket", e);
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        try {
            // read commands from the console
            reader.readLine();
        } catch (IOException e) {
            // IOException from System.in is very very unlikely (or impossible)
            // and cannot be handled
        }

    }

    @Override
    public void shutdown() {
        if (dmap_listener != null) {
            try {
                dmap_listener.close();
                //TODO: You should then proceed to terminate all open Socket connections.
                //Close any other I/O resources you may be using and
                //shut down all your thread pools.
            } catch (IOException e) {
                System.err.println("Error while closing server socket: " + e.getMessage());
            }
        }
        if(dmtp_listener != null) {
            try {
                dmtp_listener.close();
            } catch (IOException e) {
                System.err.println("Error while closing server socket: " + e.getMessage());
            }
        }

    }

    // Checks if the username password pair is stored in this mailbox and returns a string
    // that notifies if the username/password is incorrect or if login is accepted
    public static String loginUser(String username, String password) {
        if(!usernameToPasswordMap.containsKey(username)) {
            return "error unknown user";
        } else if (!usernameToPasswordMap.get(username).equals(password)) {
            return "error wrong password";
        } return "ok";
    }

    public static void main(String[] args) throws Exception {
        IMailboxServer server = ComponentFactory.createMailboxServer(args[0], System.in, System.out);

        // Set users of the current mailbox, parse their username and password and store them in the map
        usernameToPasswordMap = new HashMap<>();
        users = new Properties();
        users.load(new FileReader(USERS_FILE));
        for (String key: users.stringPropertyNames()) {
            String value = users.getProperty(key);
            usernameToPasswordMap.put(key, value);
        }

        server.run();
    }
}
