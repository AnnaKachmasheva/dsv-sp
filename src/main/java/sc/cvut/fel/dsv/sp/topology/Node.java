package sc.cvut.fel.dsv.sp.topology;


import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import sc.cvut.fel.dsv.sp.topology.model.Address;
import sc.cvut.fel.dsv.sp.topology.model.Message;
import sc.cvut.fel.dsv.sp.topology.server.Connection;
import sc.cvut.fel.dsv.sp.topology.server.NodeServer;
import sc.cvut.fel.dsv.sp.topology.utils.ConsoleHandler;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static sc.cvut.fel.dsv.sp.topology.utils.Constants.*;

@Slf4j
@Getter
@Setter
public class Node implements Runnable {

    Long id;    // unique identification
    Long winP;  // unique win_id in this ring
    Long acnP;  // current id of neighbour
    Long ciP;  // current id of neighbour

    Address myAddress;

    private StateNode stateNode;

    private ConsoleHandler consoleHandler;
    private NodeServer server; // my node

    private Connection neighbourLeft; // <-
    private Connection neighbourRight; // ->

    private boolean running = true; // if node is running

    private boolean startRepair = false;
    private boolean startAlgorithm = false;

    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

    public void init() {
        stateNode = StateNode.ACTIVE;                       // because initiator
        consoleHandler = new ConsoleHandler(this);
    }

    private Long generateId() {
        String host = server.getAddress().getHost().replaceAll("\\.", "");
        int port = server.getAddress().getPort();
        String str = host + port;
        return Long.parseLong(str);
    }

    public void run() {
        // regular events each 2 sec
        executorService.scheduleAtFixedRate(() -> {

            // connections ping
            ping(neighbourLeft);
            ping(neighbourRight);

            // check sessions
            if (neighbourLeft != null && neighbourLeft.getSession() == null) {
                setNeighbourLeft(null);
            }
            if (neighbourRight != null && neighbourRight.getSession() == null) {
                setNeighbourRight(null);
            }

            // check if topology need repair
            if (neighbourRight == null && neighbourLeft != null && !startRepair) {
                Message message = new Message(REPAIR, myAddress);
                neighbourLeft.sendMessage(message.getMessage());
                repairInit();
            }

            // lost node
            if (neighbourLeft == null && neighbourRight == null && stateNode == StateNode.PASSIVE) {
                setStateNode(StateNode.LOST);
            }


            if (startRepair) {
                this.startAlgorithm = false;
            }

//            if (startAlgorithm && stateNode == StateNode.ACTIVE) {
//                // if I have active state, send my id
//                Message messageCIP = new Message(CIP, id);
//                neighbourRight.sendMessage(messageCIP.getMessage());
//
//                // send my acnp
//                if (acnP != null) {
//                    Message messageACNP = new Message(ACNP, acnP);
//                    neighbourRight.sendMessage(messageACNP.getMessage());
//                }
//            }

        }, 0, 1, TimeUnit.SECONDS);

        new Thread(consoleHandler).run();
    }

    public void startServer(String host, int port) {
        myAddress = new Address(host, port);
        server = new NodeServer(myAddress);
        server.run();

        // begin if P is initiator then statP := active else stateP := passive;
        if (isStateActive()) {
            id = generateId();
            ciP = id;
        } else {
            stateNode = StateNode.PASSIVE;
        }
    }

    private void ping(Connection connection) {
        if (connection != null && connection.getSession() != null) {
            connection.sendMessage(PING);
        }
    }

    public void repairInit() {
        this.winP = null;
        this.acnP = null;
        this.startRepair =  true;
        this.stateNode = StateNode.ACTIVE;
    }

    private boolean isStateActive() {
        return stateNode.equals(StateNode.ACTIVE);
    }

    @Override
    public String toString() {
        return "Node{" + '\n' +
                "   id = " + id + '\n' +
                "   ciP = " + ciP + '\n' +
                "   winP = " + winP + '\n' +
                "   acnP = " + acnP + '\n' +
                "   myAddress = " + myAddress + '\n' +
                "   stateNode = " + stateNode + '\n' +
//                ", consoleHandler=" + consoleHandler + '\n' +
                "   server = " + server + '\n' +
                "   neighbourLeft = " + neighbourLeft + '\n' +
                "   neighbourRight = " + neighbourRight + '\n' +
                "   running = " + running + '\n' +
                "   startRepair = " + startRepair + '\n' +
                "   startAlgorithm = " + startAlgorithm + '\n' +
//                "   executorService = " + executorService + '\n' +
                '}';
    }
}
