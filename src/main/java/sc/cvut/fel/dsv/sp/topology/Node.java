package sc.cvut.fel.dsv.sp.topology;


import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import sc.cvut.fel.dsv.sp.topology.model.Address;
import sc.cvut.fel.dsv.sp.topology.server.Connection;
import sc.cvut.fel.dsv.sp.topology.server.NodeServer;
import sc.cvut.fel.dsv.sp.topology.utils.ConsoleHandler;

import java.util.Objects;
import java.util.UUID;

@Slf4j
@Getter
@Setter
public class Node implements Runnable {

    Integer id;    // unique identification
    Integer winP;  // unique win_id in this ring
    Integer acnP;

    Address myAddress;

    private StateNode stateNode;

    private ConsoleHandler consoleHandler;
    private NodeServer server;

    private Connection neighbourLeft;
    private Connection neighbourRight;

    private boolean running = true;



    public void init() {
        stateNode = StateNode.ACTIVE;                       // initiator
        consoleHandler = new ConsoleHandler(this);
    }

    private Integer generateId() {
        UUID uuid = UUID.randomUUID();
        String host = server.getAddress().getHost();
        int port = server.getAddress().getPort();

        String str = host + ":" + port + uuid;

        return str.hashCode();
    }

    public void run() {
        new Thread(consoleHandler).run();

        while (running) {
//            if (Objects.equals(id, winP))
//                stateNode = StateNode.LEADER;
//            else
//                stateNode = StateNode.LOST;
        }
    }

    public void startServer(String host, int port) {
        myAddress = new Address(host, port);
        server = new NodeServer(myAddress);
        server.run();

        // begin if P is initiator then statP := active else stateP := passive;
        if (isStateActive()) {
            id = generateId();
        } else {
            stateNode = StateNode.PASSIVE;
        }
    }

    public void stopServer() {
        // disable server
        server.stop();

        // close connection with neighbours
        if (neighbourRight != null && neighbourRight.isActive()) {
            neighbourRight.close();
            neighbourRight = null;
        }

        if (neighbourLeft != null && neighbourLeft.isActive()) {
            neighbourLeft.close();
            neighbourLeft = null;
        }
    }

    private boolean isStateActive() {
        return stateNode.equals(StateNode.ACTIVE);
    }

    @Override
    public String toString() {
        return "Node {" + '\n' +
                "   id = " + id + '\n' +
                "   winP = " + winP + '\n' +
                "   state = " + stateNode + '\n' +
                "   server = " + server + '\n' +
                "   neighbourLeft = " + neighbourLeft + '\n' +
                "   neighbourRight = " + neighbourRight + '\n' +
                '}';
    }

}
