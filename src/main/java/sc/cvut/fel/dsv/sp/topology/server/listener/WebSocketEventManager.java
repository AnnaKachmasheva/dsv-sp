package sc.cvut.fel.dsv.sp.topology.server.listener;

import javax.websocket.CloseReason;
import javax.websocket.Session;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WebSocketEventManager {

    private static final List<WebSocketEventListener> listeners = new ArrayList<>();

    public static void registerListener(WebSocketEventListener listener) {
        listeners.add(listener);
    }

    public static void notifyOnMessageFromServer(String message, Session session) {
        for (WebSocketEventListener listener : listeners) {
            listener.onMessageFromServer(session, message);
        }
    }

    public static void notifyOnMessageFromClient(String message, Session session) throws IOException {
        for (WebSocketEventListener listener : listeners) {
            listener.onMessageFromClient(session, message);
        }
    }

    public static void notifyOnClose(Session session, CloseReason reason) {
        for (WebSocketEventListener listener : listeners) {
            listener.onClose(session, reason);
        }
    }
}