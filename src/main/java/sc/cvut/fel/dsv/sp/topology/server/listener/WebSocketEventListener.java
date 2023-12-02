package sc.cvut.fel.dsv.sp.topology.server.listener;

import javax.websocket.CloseReason;
import javax.websocket.Session;
import java.io.IOException;

public interface WebSocketEventListener {

    void onMessageFromServer(Session session, String message);

    void onMessageFromClient(Session session, String message) throws IOException;

    void onClose(Session session, CloseReason reason);

}