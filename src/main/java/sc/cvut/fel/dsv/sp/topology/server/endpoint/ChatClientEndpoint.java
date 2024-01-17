package sc.cvut.fel.dsv.sp.topology.server.endpoint;

import lombok.extern.slf4j.Slf4j;

import javax.websocket.*;

@Slf4j
@ClientEndpoint
public class ChatClientEndpoint {

    @OnOpen
    public void onOpen(Session session) {
        log.info("Chat webSocket Connected: {}", session.getId());
    }

    @OnMessage
    public void onMessage(Session session, String message) {
        if (!message.equals("ping"))
            log.info(message);
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        // no implementation
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        // no implementation
    }

}