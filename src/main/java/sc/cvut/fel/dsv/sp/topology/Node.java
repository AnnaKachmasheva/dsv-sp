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

import static sc.cvut.fel.dsv.sp.topology.utils.Constants.PING;
import static sc.cvut.fel.dsv.sp.topology.utils.Constants.REPAIR;

@Slf4j
@Getter
@Setter
public class Node implements Runnable {

    private Long id;    // unique identification
    private Long winP;  // unique win_id in this ring
    private Long acnP;  // current id of neighbour
    private Long ciP;   // current id of neighbour

    private Address myAddress;

    private StateNode stateNode;

    private ConsoleHandler consoleHandler;
    private NodeServer server; // my node

    private Connection neighbourLeft; // <-
    private Connection neighbourRight; // ->
    private Connection chatConnection;

    private boolean running = true; // if node is running

    private boolean startRepair = false;
    private boolean startAlgorithm = false;

    // for chat
    private boolean runningChat = false; // is chat running

    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

    public void init() {
        stateNode = StateNode.ACTIVE;
        consoleHandler = new ConsoleHandler(this);
    }

    private Long generateId() {
        String host = server.getAddress().getHost();
        int port = server.getAddress().getPort();

        // Split the host into octets
        String[] octets = host.split("\\.");
        StringBuilder hostBuilder = new StringBuilder();

        // Ensure each octet has three digits with leading zeros
        for (String octet : octets) {
            hostBuilder.append(String.format("%03d", Integer.parseInt(octet)));
        }

        String str = hostBuilder.toString() + port;
        return Long.parseLong(str);
    }

    public Address convertToAddress() {
        if (winP == null)
            return null;

        String address = winP.toString();

        // Extract host and port from the input string
        String hostPart = address.substring(0, 12);
        String portPart = address.substring(12);

        // Remove leading zeros from each octet in the host part
        String[] octets = hostPart.split("(?<=\\G...)");
        StringBuilder hostBuilder = new StringBuilder();
        for (String octet : octets) {
            hostBuilder.append(Integer.parseInt(octet)).append(".");
        }
        String host = hostBuilder.deleteCharAt(hostBuilder.length() - 1).toString();

        // Convert port part to an integer
        int port = Integer.parseInt(portPart);

        return new Address(host, port);
    }

    public void run() {
        // regular events each 2 sec
        executorService.scheduleAtFixedRate(() -> {

            // connections ping
            ping(neighbourLeft);
            ping(neighbourRight);
            ping(chatConnection);

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
                neighbourLeft.sendMessage(message.toStringM());
                repairInit();
            }

            // lost node
            if (neighbourLeft == null && neighbourRight == null && stateNode == StateNode.PASSIVE) {
                setStateNode(StateNode.LOST);
            }

        }, 0, 1, TimeUnit.SECONDS);

        new Thread(consoleHandler).run();
    }

    public void startServer(String host, int port) {
        myAddress = new Address(host, port);
        server = new NodeServer(myAddress);
        server.run();

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
        this.startRepair = true;
        this.chatConnection = null;
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
                "   chat = " + chatConnection + '\n' +
                "   running = " + running + '\n' +
                "   startRepair = " + startRepair + '\n' +
                "   startAlgorithm = " + startAlgorithm + '\n' +
//                "   executorService = " + executorService + '\n' +
                '}';
    }
}