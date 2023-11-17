package sc.cvut.fel.dsv.sp.topology;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.websocket.ClientEndpoint;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import java.io.IOException;
import java.net.URI;

@Data
@Slf4j
@ClientEndpoint
public class Communication {

    private Client client;
    private Server server;

    private Session session;

    private MessageListener listener;


    public Communication(Client client, Server server) {
        this.client = client;
        this.server = server;
    }

    // Send a message to the server
    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        new Thread().start();   // Start a new thread to listen for messages from the server
    }

    public void initConnection() {
        URI uri = server.address.getURIStr();
        javax.websocket.WebSocketContainer container = javax.websocket.ContainerProvider.getWebSocketContainer();
        try {
            session = container.connectToServer(Server.class, uri);

            log.info("WebSocket opened: {}", this);
        } catch (Exception e) {
            log.error("unsuccessful creation websocket session for server: {}", this);
        }
    }

    public void sendMessage(String message) {
        session.getAsyncRemote().sendText(message);
    }

    // Received from server
    @OnMessage
    public void onMessage(String message) throws IOException {
        log.info("Received from server: {}", message);

        // Notify listener
        listener.onMessageReceived(message, server);

    }

    public interface MessageListener {
        void onMessageReceived(String message, Server server) throws IOException;
    }

}
