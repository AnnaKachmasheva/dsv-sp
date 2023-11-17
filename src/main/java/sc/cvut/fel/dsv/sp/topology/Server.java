package sc.cvut.fel.dsv.sp.topology;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Objects;

@Data
@Slf4j
@ServerEndpoint("/server")
public class Server {

    Address address;
    private Session session;

    public Server(Address address) {
        this.address = address;
    }

    @OnMessage
    public void onMessage(String message) throws IOException {
        log.info("Received message: {}", message);

        //  the received message back to the client
        session.getBasicRemote().sendText(message);
    }

    @OnClose
    public void onClose(Session session) {
        log.info("WebSocket closed: {}", session.getId());

        try {
            session.close();
        } catch (Exception e) {
            log.error("unsuccessful close websocket session for server: {}", this);
        }
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        log.error("Server: {} '\n' WebSocket: {}", this, throwable.getMessage());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Server server = (Server) o;
        return Objects.equals(address, server.address) && Objects.equals(session, server.session);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, session);
    }
}
