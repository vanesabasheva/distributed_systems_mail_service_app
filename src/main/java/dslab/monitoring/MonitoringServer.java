package dslab.monitoring;

import java.io.InputStream;
import java.io.PrintStream;

import dslab.ComponentFactory;
import dslab.util.Config;


// The purpose of the monitoring server is to receive and display usage statistics of the outgoing traffic
//of transfers servers. Specifically, it records the amount of messages sent from specific servers and users.
//This is useful for analyzing message throughput of individual servers and users to, e.g., detect server
//abuse.
//The monitoring server receives usage statistics via a UDP socket. For each mail sent by a transfer server,
//it sends a UDP packet to the monitoring server containing the transfer server’s host and port, and the
//sender’s email address. The packet contains the information as plain text: see formatting in file
public class MonitoringServer implements IMonitoringServer {

    /**
     * Creates a new server instance.
     *
     * @param componentId the id of the component that corresponds to the Config resource
     * @param config the component config
     * @param in the input stream to read console input from
     * @param out the output stream to write console output to
     */
    public MonitoringServer(String componentId, Config config, InputStream in, PrintStream out) {
        // TODO
    }

    @Override
    public void run() {
        // TODO
    }

    @Override
    public void addresses() {
        // TODO
    }

    @Override
    public void servers() {
        // TODO
    }

    @Override
    public void shutdown() {
        // TODO
    }

    public static void main(String[] args) throws Exception {
        IMonitoringServer server = ComponentFactory.createMonitoringServer(args[0], System.in, System.out);
        server.run();
    }

}
