package sc.cvut.fel.dsv.sp.topology.server.endpoint;

import lombok.extern.slf4j.Slf4j;
import sc.cvut.fel.dsv.sp.topology.Node;
import sc.cvut.fel.dsv.sp.topology.server.listener.WebSocketEventManager;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static sc.cvut.fel.dsv.sp.topology.utils.Constants.CIP;

@Slf4j
@ServerEndpoint(value = "/server")
public class NodeServerEndpoint {

    private Session session;

//    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;

        log.info("WebSocket Connected: {}", session.getId());

        // sending my id, if I'm active to right neighbour
        // time is 1 sec
//        executorService.scheduleAtFixedRate(() -> {
//            try {
//                session.getBasicRemote().sendText(CIP + ":get");
//
////                if (node.getStateNode() == StateNode.ACTIVE &&
////                        node.getNeighbourRight() != null &&
////                        node.getNeighbourRight().getSession().getId().equals(session.getId())) {
////                    Message messageWithCIP = new Message(CIP, node.getAcnP());
////                    session.getBasicRemote().sendText(messageWithCIP.getMessage());
////                }
//            } catch (IOException e) {
//                log.error(e.getMessage());
//                log.error(session.getId());
//            }
//        }, 0, 1, TimeUnit.SECONDS);
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
        // Do error handling here
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
