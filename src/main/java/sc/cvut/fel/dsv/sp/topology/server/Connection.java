package sc.cvut.fel.dsv.sp.topology.server;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import sc.cvut.fel.dsv.sp.chat.endpoint.ChatClientEndpoint;
import sc.cvut.fel.dsv.sp.topology.Node;
import sc.cvut.fel.dsv.sp.topology.model.Address;
import sc.cvut.fel.dsv.sp.topology.server.endpoint.NodeClientEndpoint;

import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import java.io.IOException;
import java.net.URI;
import java.util.Objects;

@Slf4j
@Getter
@Setter
public class Connection {

    private Node node;
    private Address address;
    private Session session;
    private String pathURI;

    public Connection(Node node, Address address, String pathURI) {
        this.node = node;
        this.address = address;
        this.pathURI = pathURI;
    }

    public Connection(Node node, Address address, Session session, String pathURI) {
        this.node = node;
        this.address = address;
        this.session = session;
        this.pathURI = pathURI;
    }

    public void run() {
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();

        URI uri = getURI(pathURI);

        try {
            log.info("Start connection. URI: {}", uri);
            // Create client-side endpoint

            if (pathURI.equals("")) {
                NodeClientEndpoint nodeClientEndpoint = new NodeClientEndpoint();
                // Attempt Connect
                container.setDefaultMaxSessionIdleTimeout(Integer.MAX_VALUE);
                session = container.connectToServer(nodeClientEndpoint, uri);
            } else if (pathURI.equals("chat")) {
                ChatClientEndpoint chatClientEndpoint = new ChatClientEndpoint();
                container.setDefaultMaxSessionIdleTimeout(Integer.MAX_VALUE);
                session = container.connectToServer(chatClientEndpoint, uri);
            }

            session.setMaxIdleTimeout(30000);

            log.info("Success start connection. Session: {}", session.getId());

        } catch (DeploymentException | IOException e) {
            log.error("Failed start connection to host: {} and port: {}.\n Message: {}",
                    address.getHost(), address, e.getMessage());

            this.session = null;
        }
    }

    public void sendMessage(String message) {
        try {
//            log.error("Connection with session id {} close", session.getId());
            // open session if close
            if (!session.isOpen()) {
                run();
            }

            session.getBasicRemote().sendText(message);
        } catch (NullPointerException | IOException e) {
            session = null;

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


    public URI getURI(String path) {
        StringBuilder builder = new StringBuilder();

        builder.append("ws://")
                .append(address.getHost())
                .append(":")
                .append(address.getPort())
                .append("/server")
                .append(Objects.equals(path, "") ? path : '/' + path);

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
