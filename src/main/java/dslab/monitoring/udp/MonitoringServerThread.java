package dslab.monitoring.udp;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Map;

public class MonitoringServerThread extends Thread {
  private DatagramSocket datagramSocket;
  private Map<String, Integer> addresses;
  private Map<String, Integer> servers;

  public MonitoringServerThread(DatagramSocket datagramSocket,
                                Map<String, Integer> addresses,
                                Map<String, Integer> servers) {
    this.datagramSocket = datagramSocket;
    this.addresses = addresses;
    this.servers = servers;
  }

  @Override
  public void run() {

    byte[] buffer;
    DatagramPacket packet;
    try {
      while (true) {
        buffer = new byte[1024];
        // create a datagram packet of specified length (buffer.length)
        /*
         * Keep in mind that, in UDP, packet delivery is not guaranteed,
         * and the order of the delivery/processing is also not guaranteed.
         */
        packet = new DatagramPacket(buffer, buffer.length);

        // wait for incoming packets from client
        datagramSocket.receive(packet);
        // get the data from the packet
        String request = new String(packet.getData());


        // check if request has the correct format:
        String[] parts = request.split("\\s");

        String response;
        if (parts.length != 2) {
          response = "error provided message does not fit the expected format: <host>:<port> <email-address>";
          System.out.println(response);
          continue;
        }

        String server = parts[0];
        String fromEmail = parts[1].trim();

        this.addresses.put(fromEmail, this.addresses.getOrDefault(fromEmail, 0) + 1);
        this.servers.put(server, this.servers.getOrDefault(server,0) + 1);

      }

    } catch (SocketException e) {
      // when the socket is closed, the send or receive methods of the DatagramSocket will throw a SocketException
      System.out.println("SocketException while waiting for/handling packets: " + e.getMessage());
      return;

    } catch (IOException e) {
      // other exceptions should be handled correctly in your implementation
      throw new UncheckedIOException(e);

    } finally {
      if (datagramSocket != null && !datagramSocket.isClosed()) {
        datagramSocket.close();
      }
    }

  }
}
