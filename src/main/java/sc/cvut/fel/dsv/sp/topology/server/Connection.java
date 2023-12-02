package sc.cvut.fel.dsv.sp.topology.server;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import sc.cvut.fel.dsv.sp.topology.model.Address;
import sc.cvut.fel.dsv.sp.topology.server.endpoint.NodeClientEndpoint;

import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import java.io.IOException;
import java.net.URI;

@Slf4j
@Getter
@Setter
public class Connection {

    private Address address;
    private Session session;

    public Connection(Address address) {
        this.address = address;
    }

    public Connection(Address address, Session session) {
        this.address = address;
        this.session = session;
    }

    public void run() {
        URI uri = getURI();
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();

        try {
            log.info("start connection. URI: {}", uri);
            // Create client side endpoint
            NodeClientEndpoint nodeClientEndpoint = new NodeClientEndpoint();

            // Attempt Connect
            container.setDefaultMaxSessionIdleTimeout(30000);
            session = container.connectToServer(nodeClientEndpoint, uri);
            session.setMaxIdleTimeout(30000);

            log.info("success start connection. session: {}", session.getId());

        } catch (DeploymentException | IOException e) {
            log.error("Failed start connection to host: {}  and port: {}.\n Message: {}",
                    address.getHost(), address.getPort(), e.getMessage());
        }
    }

    public void sendMessage(String message) {
        try {
            session.getBasicRemote().sendText(message);
        } catch (NullPointerException | IOException e) {
            log.error("Failed send message to host: {}  and port: {}. Message: {}\n Error: {}",
                    address.getHost(), address.getPort(), message, e.getMessage());
        }
    }

    public void close() {
        try {
            session.close();
        } catch (NullPointerException | IOException e) {
            log.error("Failed close connection to host: {}  and port: {}.\n Message: {}",
                    address.getHost(), address.getPort(), e.getMessage());
        }
    }


    public URI getURI() {
        StringBuilder builder = new StringBuilder();

        builder.append("ws://")
                .append(address.getHost())
                .append(":")
                .append(address.getPort())
                .append("/server");

        String uriStr = builder.toString();

        URI uri = null;
        try {
            uri = URI.create(uriStr);
        } catch (Exception e) {
            log.error("NOT VALID URI with host: {} and port:{}", address.getHost(), address.getPort());
        }

        return uri;
    }

    public boolean isActive() {
        return session != null;
    }

    @Override
    public String toString() {
        return "Connection{" +
                "address=" + address +
                ", session=" + session +
                '}';
    }

}
