package sc.cvut.fel.dsv.sp.topology.server.endpoint;

import lombok.extern.slf4j.Slf4j;
import sc.cvut.fel.dsv.sp.topology.server.listener.WebSocketEventManager;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;

@Slf4j
@ServerEndpoint(value = "/server")
public class NodeServerEndpoint {


    public NodeServerEndpoint() {
    }

    @OnOpen
    public void onOpen(Session session) {
        log.info("WebSocket Connected: {}", session.getId());
    }

    @OnMessage
    public void onMessage(Session session, String message) throws IOException {
        WebSocketEventManager.notifyOnMessageFromClient(message, session);
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        WebSocketEventManager.notifyOnClose(session, closeReason);
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        // Do error handling here
    }

}
