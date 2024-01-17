package sc.cvut.fel.dsv.sp.topology.utils;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import sc.cvut.fel.dsv.sp.topology.Node;
import sc.cvut.fel.dsv.sp.topology.StateNode;
import sc.cvut.fel.dsv.sp.topology.model.Address;
import sc.cvut.fel.dsv.sp.topology.model.Message;
import sc.cvut.fel.dsv.sp.topology.server.Connection;
import sc.cvut.fel.dsv.sp.topology.server.listener.WebSocketEventListener;
import sc.cvut.fel.dsv.sp.topology.server.listener.WebSocketEventManager;

import javax.websocket.CloseReason;
import javax.websocket.Session;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static sc.cvut.fel.dsv.sp.topology.utils.Constants.*;

@Slf4j
@Setter
public class ConsoleHandler implements Runnable, WebSocketEventListener {

    private static final String IPV4_REGEX =
            "^(25[0-5]|2[0-4][0-9]|[0-1]?[0-9]?[0-9])(\\.(25[0-5]|2[0-4][0-9]|[0-1]?[0-9]?[0-9])){3}$";
    private static final Pattern IPV4_PATTERN = Pattern.compile(IPV4_REGEX);

    private static final String PORT_REGEX = "^([0-9][0-9][0-9][0-9])$";
    private static final Pattern PORT_PATTERN = Pattern.compile(PORT_REGEX);

    private Node node;
    private BufferedReader reader;

    private boolean reading = true;
    private boolean isStartServerCommand = false;
    private boolean isPort = false;
    private boolean isHost = false;
    private boolean isConnect = false;
    private String host;
    private int port;


    public ConsoleHandler(Node node) {
        this.node = node;
        reader = new BufferedReader(new InputStreamReader(System.in));
        WebSocketEventManager.registerListener(this);
    }

    @Override
    public void run() {
        String commandline;
        log.info("OPEN ConsoleHandler");

        while (reading) {
            if ((isConnect || isStartServerCommand) && !isHost) {
                System.out.print("  host: > ");
            } else if ((isStartServerCommand || isConnect) && !isPort)
                System.out.print("  port > ");
            else
                System.out.print("cmd > ");


            try {
                commandline = reader.readLine();
                parse_commandline(commandline);
            } catch (IOException | InterruptedException e) {
                reading = false;

                log.error("ConsoleHandler - error in reading console input. \n Message: {}", e.getMessage());
            }
        }

        log.info("CLOSE ConsoleHandler");
    }


    private void parse_commandline(String commandline) throws InterruptedException {

        if ((isConnect || isStartServerCommand) && !isHost) {
            if (isHostValid(commandline)) {
                host = commandline;
                isHost = true;
            } else {
                log.warn("Host like '127.0.0.100'");
            }
            return;
        }

        if ((isConnect || isStartServerCommand) && !isPort) {
            if (isPortValid(commandline)) {
                port = Integer.parseInt(commandline);
                isPort = true;
            } else {
                log.warn("Port like '8080'");
                return;
            }
        }

        if (isStartServerCommand && !isConnect) {
            isStartServerCommand = false;
            isHost = false;
            isPort = false;

            int port = Integer.parseInt(commandline);
            node.startServer(host, port);
            return;
        }

        if (isConnect) {
            isConnect = false;
            isHost = false;
            isPort = false;

            connect(host, port);

            return;
        }

        switch (commandline) {
            case "?" -> log.info("Possible commands:\n{}", getHelpMessage());
            case "info" -> {
                log.info("MY NODE {}", node);
            }
            case "start server" -> isStartServerCommand = true;
//            case "stop server" -> node.stopServer();
            case "connect" -> {
                // check if node has 2 active connection
                if (node.getNeighbourLeft() != null && node.getNeighbourRight() != null) {
                    log.error("Node already has 2 connection. Max 2 connection for 1 node.");
                } else
                    isConnect = true;
            }
            default -> log.info("Unknown command.\n For more information input '?'");
        }
    }

    private boolean isPortValid(String commandline) {
        Matcher matcher = PORT_PATTERN.matcher(commandline);
        return matcher.matches();
    }

    private boolean isHostValid(String commandline) {
        Matcher matcher = IPV4_PATTERN.matcher(commandline);
        return matcher.matches();
    }

    private String getHelpMessage() {
        StringBuilder builder = new StringBuilder();

        builder.append("info        - info about my node").append('\n')
                .append("connect     - connect with other node").append('\n')
                .append("start server").append('\n')
                .append("stop server ").append('\n');

        return builder.toString();
    }

    public void connect(String host, int port) {

        Address myAddress = node.getMyAddress();
        if (host.equals(myAddress.getHost()) && (port == myAddress.getPort())) {
            log.warn("Connection address{} is my. My address and the connection address are the same." +
                    " Enter another address", myAddress);
            isHost = false;
            isPort = false;
            return;
        }

        Address address = new Address(host, port);
        Connection connection = new Connection(node, address);
        connection.run();


        if (node.getNeighbourRight() == null) {
            node.setNeighbourRight(connection);
        }

        node.setNeighbourLeft(connection);

        // make ring topology
        //  get right neighbour
        Message messageGetRightNeighbour = new Message(GET_RIGHT_NEIGHBOUR, "");
        connection.sendMessage(messageGetRightNeighbour.getMessage());

        // connect with right neighbour for ring topology
        Message messageMyAddress = new Message(NEW_RIGHT_NEIGHBOUR, node.getMyAddress());
        connection.sendMessage(messageMyAddress.getMessage());

        // send repair message
        Message message = new Message(REPAIR, node.getMyAddress());
        node.getNeighbourLeft().sendMessage(message.getMessage());
        node.repairInit();
    }

    @Override
    public void onMessageFromServer(Session session, String messageFromServer) {
        if (!messageFromServer.equals(PING)) {
            log.info("Message from server: {}", messageFromServer);
        }

        Message message = new Message(messageFromServer);
        String title = message.getTitle();
        if (title == null)
            return;

        switch (title) {

            case MY_RIGHT_NEIGHBOUR -> {
                Address address = parseAddress(message.getBody());
                if (address != null) {

                    Connection newConnection = new Connection(node, address);
                    newConnection.run();

                    node.setNeighbourRight(newConnection);

                    Message messageNewRightN = new Message(NEW_LEFT_NEIGHBOUR, node.getMyAddress());
                    newConnection.sendMessage(messageNewRightN.getMessage());
                }
            }

            case NEW_LEFT_NEIGHBOUR -> {
                if (message.getBody().isEmpty())
                    return;

                Address newAddress = parseAddress(message.getBody());
                node.setNeighbourLeft(new Connection(node, newAddress, session));
            }

            case REPAIR -> repairMessageHandler(message);
            case CIP -> cipMessageHandler(message);
            case ACNP -> acnpMessageHandler(message);
            case WIN -> winMessageHandler(message);
            case NEW_ROUND_INIT -> newRoundHandler(message);
        }
    }

    @Override
    public void onMessageFromClient(Session session, String messageFromClient) {
        if (!messageFromClient.equals(PING)) {
            log.info("Message from client: {}", messageFromClient);
        }

        Message message = new Message(messageFromClient);
        String title = message.getTitle();
        if (title == null)
            return;

        switch (title) {
            case GET_RIGHT_NEIGHBOUR -> {
                Address address = node.getNeighbourRight() == null ? null : node.getNeighbourRight().getAddress();
                Message messageWithNeighbour = new Message(MY_RIGHT_NEIGHBOUR, address);
                sendMessage(session, messageWithNeighbour);
            }

            case NEW_RIGHT_NEIGHBOUR -> {
                Address newAddress = parseAddress(message.getBody());

                if (!newAddress.equals(node.getMyAddress())) {
                    Connection newConnection = new Connection(node, newAddress, session);

                    if (node.getNeighbourLeft() == null) {
                        node.setNeighbourLeft(newConnection);
                    }

                    node.setNeighbourRight(newConnection);
                }
            }
            case NEW_LEFT_NEIGHBOUR -> {
                Address newAddress = parseAddress(message.getBody());
                Connection newConnection = new Connection(node, newAddress, session);
                node.setNeighbourLeft(newConnection);

                if (node.getNeighbourRight() == null)
                    node.setNeighbourRight(newConnection);
            }
            case REPAIR -> repairMessageHandler(message);
            case CIP -> cipMessageHandler(message);
            case ACNP -> acnpMessageHandler(message);
            case WIN -> winMessageHandler(message);
            case NEW_ROUND_INIT -> newRoundHandler(message);
        }
    }

    private void repairMessageHandler(Message message) {
        Address address = parseAddress(message.getBody());

        if (node.getNeighbourLeft() == null) {
            Connection connection = new Connection(node, address);
            connection.run();
            node.setNeighbourLeft(connection);

            Message messageRN = new Message(NEW_RIGHT_NEIGHBOUR, node.getMyAddress());
            connection.sendMessage(messageRN.getMessage());
            node.repairInit();
        }
        if (!address.getHost().equals(node.getMyAddress().getHost()) &&
                !node.isStartRepair()) {
            Message messageRepair = new Message(REPAIR, address);
            node.getNeighbourLeft().sendMessage(messageRepair.getMessage());
            node.repairInit();
        } else {
            // start sending CIP after topology restoration
            if (node.isStartRepair()) {
                Message startAlgMessage = new Message(CIP, node.getId(), node.getId());
                node.getNeighbourRight().sendMessage(startAlgMessage.getMessage());
            }

            node.setStartRepair(false);
            node.setStartAlgorithm(true);
        }

        System.out.println(node);
    }

    private Address parseAddress(String messageBody) {
        if (messageBody.isEmpty())
            return null;

        String[] parts = messageBody.split("_");
        String host = parts[0];
        int port = Integer.parseInt(parts[1]);
        return new Address(host, port);
    }

    private void cipMessageHandler(Message message) {
        if (node.isStartRepair()) {
            node.setStartRepair(false);
        }
        node.setStartAlgorithm(true);

        long[] nums = parseMessageType2(message);
        if (nums.length == 0) {
            return;
        }
        long startId = nums[0];
        long cipM = nums[1];

        // only 2 nodes
        if (node.getNeighbourRight().getAddress().equals(node.getNeighbourLeft().getAddress())) {
            long max = Math.max(node.getId(), cipM);
            node.setWinP(max);

            if (max == node.getId()) {
                node.setStateNode(StateNode.LEADER);
            } else {
                node.setStateNode(StateNode.PASSIVE);
            }

            Message messagewin = new Message(WIN, max);
            node.getNeighbourRight().sendMessage(messagewin.getMessage());
            return;
        }

        // if not leader in topology
        if (node.getWinP() != null)
            return;

        node.setAcnP(cipM);

        if (node.getStateNode() == StateNode.PASSIVE) {
            // I'm passive pass data to right neighbour
            node.getNeighbourRight().sendMessage(message.getMessage());
        } else {
            // winner?
            long myId = node.getId();
            if (cipM == myId) {
                // send a message to the right one that I won
//                Message messagewin = new Message(WIN, node.getId());
//                node.getNeighbourRight().sendMessage(messagewin.getMessage());
//
//                log.info("passive. send message {}", messagewin.getMessage());
            }
            // pass data next
            if (startId != myId) {
                Message messagecip = new Message(CIP, startId, node.getCiP());
                node.getNeighbourRight().sendMessage(messagecip.getMessage());

                log.info("send message {}", messagecip.getMessage());
            } else {

                log.info("cancel send CIP. Start send ACNP");

                Message acnpMessage = new Message(ACNP, node.getId(), node.getAcnP());
                node.getNeighbourRight().sendMessage(acnpMessage.getMessage());

                log.info("send message {}", acnpMessage.getMessage());
            }
        }

        System.out.println(node);
    }

    private void acnpMessageHandler(Message message) {
        if (node.getWinP() != null)
            return;

        long[] nums = parseMessageType2(message);
        if (nums.length == 0) {
            return;
        }
        long startId = nums[0];
        long acnpM = nums[1];

        long id = node.getId();

        // send right neighbour to right
        if (node.getStateNode() == StateNode.PASSIVE) {
            // pass to right
            node.getNeighbourRight().sendMessage(message.getMessage());

            log.info("State is passive. Send message {} to next.", message.getMessage());
        } else {

            // set state
            long myAcnp = node.getAcnP();
            if (myAcnp > acnpM) {
                node.setStateNode(StateNode.PASSIVE);
            }

            if (id != startId) {
                Message acnpMessage = new Message(ACNP, startId, node.getAcnP());
                node.getNeighbourRight().sendMessage(acnpMessage.getMessage());

                log.info("send next {}", acnpMessage.getMessage());
            } else {
                log.info("cancel send ACNP. Init new round.");

                Message messageNewRound = new Message(NEW_ROUND_INIT, node.getId());
                node.getNeighbourRight().sendMessage(messageNewRound.getMessage());
            }
        }

        System.out.println(node);
    }

    private long[] parseMessageType2(Message message) {
        long[] nums = new long[2];

        String bodyM = message.getBody();
        String[] numsStr = bodyM.split(",");

        if (numsStr.length != 2) {
            log.error("Invalid message type 2 {}", message);
        } else {
            nums[0] = Long.parseLong(numsStr[0]);
            nums[1] = Long.parseLong(numsStr[1]);
        }

        return nums;
    }

    private void winMessageHandler(Message message) {
        if (node.getWinP() != null)
            return;

        long winId = Long.parseLong(message.getBody());
        long id = node.getId();

        if (id == winId) {
            node.setStateNode(StateNode.LEADER);
        } else {
            node.setStateNode(StateNode.PASSIVE);
            // pass info about winner next

            Message winMessage = new Message(WIN, winId);
            node.getNeighbourLeft().sendMessage(winMessage.getMessage());

            log.info("send message {}", winMessage.getMessage());
        }
        node.setWinP(winId);

        System.out.println(node);
    }

    private void newRoundHandler(Message message) {
        long startId = Long.parseLong(message.getBody());
        long myId = node.getId();

        log.info("Init new rond settings");
        node.setCiP(node.getAcnP());
        node.setAcnP(null);

        if (startId != myId) {
            node.getNeighbourRight().sendMessage(message.getMessage());
        } else {
            log.info("Cancel init new round. Start new round.");

//            // start new round
//            Message startRoundMessage;
//            if (node.getStateNode() == StateNode.ACTIVE) {
//                startRoundMessage = new Message(CIP, node.getId(), node.getCiP());
//            } else {
//                startRoundMessage = new Message(START_ROUND, node.getId());
//            }
//            node.getNeighbourRight().sendMessage(startRoundMessage.getMessage());
        }

        System.out.println(node);
    }

    private void sendMessage(Session session, Message message) {
        try {
            session.getBasicRemote().sendText(message.getMessage());
        } catch (IOException e) {
            log.error("failed send send message: {} in session: {}.\n Message: {}",
                    message, session.getId(), e.getMessage());
        }
    }


    @Override
    public void onClose(Session session, CloseReason reason) {
        String idCloseSession = session.getId();

        log.info(String.format("Session %s closed because of %s", idCloseSession, reason));

        // lost connection
        if (reason.getCloseCode() == CloseReason.CloseCodes.CLOSED_ABNORMALLY) {
            if (node.getNeighbourLeft().getSession().getId().equals(idCloseSession)) {
                node.setNeighbourLeft(null);

                log.info("Lost neighbour left");
            }
            if (node.getNeighbourRight().getSession().getId().equals(idCloseSession)) {
                node.setNeighbourRight(null);

                log.info("Lost neighbour right");
            }
        } else {
            if (idCloseSession.equals(node.getNeighbourRight().getSession().getId())) {
                node.getNeighbourRight().run();
            }

            if (idCloseSession.equals(node.getNeighbourLeft().getSession().getId())) {
                node.getNeighbourRight().run();
            }
        }
    }

}
