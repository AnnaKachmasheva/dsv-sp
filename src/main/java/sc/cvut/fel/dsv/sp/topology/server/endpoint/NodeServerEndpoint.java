package sc.cvut.fel.dsv.sp.topology.server.endpoint;

import lombok.extern.slf4j.Slf4j;
import sc.cvut.fel.dsv.sp.topology.server.listener.WebSocketEventManager;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.TimerTask;

@Slf4j
@ServerEndpoint(value = "/server")
public class NodeServerEndpoint {

    private Session session;

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;

        log.info("WebSocket Connected: {}", session.getId());
    }

    @OnMessage
    public void onMessage(Session session, String message) throws IOException {
        if (!"Pong".equals(message)) {
            WebSocketEventManager.notifyOnMessageFromClient(message, session);
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        WebSocketEventManager.notifyOnClose(session, closeReason);
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        // no implementation
    }

    private class CheckConnectionTask extends TimerTask {
        @Override
        public void run() {
            if (session != null && session.isOpen()) {
                try {
                    session.getBasicRemote().sendPing(ByteBuffer.wrap("Ping".getBytes()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
