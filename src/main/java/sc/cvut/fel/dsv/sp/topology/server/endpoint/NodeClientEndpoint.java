package sc.cvut.fel.dsv.sp.topology.server.endpoint;

import lombok.extern.slf4j.Slf4j;
import sc.cvut.fel.dsv.sp.topology.server.listener.WebSocketEventManager;

import javax.websocket.*;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@ClientEndpoint
public class NodeClientEndpoint {

    private Session session;

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        log.info("WebSocket Connected: {}", session.getId());
    }

    @OnMessage
    public void onMessage(Session session, String message) {
        WebSocketEventManager.notifyOnMessageFromServer(message, session);
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
