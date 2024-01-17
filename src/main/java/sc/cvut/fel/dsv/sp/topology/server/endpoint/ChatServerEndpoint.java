package sc.cvut.fel.dsv.sp.topology.server.endpoint;

import lombok.extern.slf4j.Slf4j;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@ServerEndpoint(value = "/server/chat")
public class ChatServerEndpoint {

    // Store sessions of clients
    private static Set<Session> sessions = Collections.synchronizedSet(new HashSet<>());

    @OnOpen
    public void onOpen(Session session) {
        sessions.add(session);

        log.info("CHAT:WebSocket Connection opened: {}", session.getId());
    }

    @OnMessage
    public void onMessage(Session session, String message) throws IOException {
        if (!message.equals("ping")) {
            log.info(message);

            // Broadcast the message to all clients
            broadcast(message);
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        sessions.remove(session);

        log.info("CHAT:WebSocket Connection closed: {}", session.getId());
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        log.error("CHAT:WebSocket Error occurred for session {}: {}", session.getId(), throwable.getMessage());
    }
    private static void broadcast(String message) {
        for (Session session : sessions) {
            try {
                session.getBasicRemote().sendText(message);
            } catch (IOException e) {
                log.error("CHAT:Error broadcasting message to session {}: {}", session.getId(), e.getMessage());
            }
        }
    }
}
