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
import java.util.Objects;
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
            case "stop server" -> node.stopServer();
            case "connect" -> {
                // check if node has 2 active connection
                if (node.getNeighbourLeft() != null && node.getNeighbourRight() != null &&
                        !Objects.equals(node.getNeighbourLeft().getAddress().getHost(),
                                node.getNeighbourRight().getAddress().getHost())) {
                    log.error("Node already has 2 connection. Max count connection 2.");
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

        Address address = new Address(host, port);
        Connection connection = new Connection(node, address);
        connection.run();

        if (node.getNeighbourRight() == null) {
            node.setNeighbourRight(connection);
        }

        node.setNeighbourLeft(connection);

        // make ring topology
        //  get left neighbour
        Message messageGetRightNeighbour = new Message(GET_RIGHT_NEIGHBOUR, "");
        connection.sendMessage(messageGetRightNeighbour.getMessage());

        // connect with right neighbour for ring topology
        Message messageMyAddress = new Message(NEW_RIGHT_NEIGHBOUR, node.getMyAddress());
        connection.sendMessage(messageMyAddress.getMessage());
    }

    @Override
    public void onMessageFromServer(Session session, String messageFromServer) {
        log.info("Message from server: {}", messageFromServer);

        Message message = new Message(messageFromServer);
        String title = message.getTitle();
        if (title == null)
            return;

        switch (title) {

            case RIGHT_NEIGHBOUR -> {
                Address address = parseAddress(message.getBody());
                if (address != null) {

                    Connection newConnection = new Connection(node, address);
                    newConnection.run();

                    node.setNeighbourRight(newConnection);

                    Message messageNewRightN = new Message(NEW_RIGHT_NEIGHBOUR, node.getMyAddress());
                    newConnection.sendMessage(messageNewRightN.getMessage());
                }
            }

            case NEW_LEFT_NEIGHBOUR -> {
                if (message.getBody().isEmpty())
                    return;

                Address newAddress = parseAddress(message.getBody());
                node.setNeighbourLeft(new Connection(node, newAddress, session));

                // creat connection with new neighbour
                if ((node.getNeighbourLeft().getNode().getMyAddress().getPort() != node.getNeighbourRight().getNode().getMyAddress().getPort()) &&
                        !(node.getNeighbourLeft().getNode().getMyAddress().getHost().equals(node.getNeighbourRight().getNode().getMyAddress().getHost()))) {
                    node.getNeighbourLeft().run();
                    Message messageWithNewRightNeighbour = new Message(NEW_RIGHT_NEIGHBOUR, node.getMyAddress());
                    node.getNeighbourLeft().sendMessage(messageWithNewRightNeighbour.getMessage());
                }
            }


            case CIP -> {
                cipMessageHandler(message);
            }
            case ACNP -> {
                acnpMessageHandler(message, session);
            }
            case SMALL -> {
                // todo ?
                smallMessageHandler(message, session);
            }
            case WIN -> {
                long win = Integer.parseInt(message.getBody());
                node.setWinP(win);
            }
        }

    }

    private Address parseAddress(String messageBody) {
        if (messageBody.isEmpty())
            return null;

        String[] parts = messageBody.split("_");
        String host = parts[0];
        int port = Integer.parseInt(parts[1]);
        return new Address(host, port);
    }

    @Override
    public void onMessageFromClient(Session session, String messageFromClient) {
        log.info("Message from client: {}", messageFromClient);

        Message message = new Message(messageFromClient);
        String title = message.getTitle();
        if (title == null)
            return;

        switch (title) {
            case GET_RIGHT_NEIGHBOUR -> {
                Address address = node.getNeighbourRight() == null ? null : node.getNeighbourRight().getAddress();
                Message messageWithRightNeighbour = new Message(RIGHT_NEIGHBOUR, address);
                sendMessage(session, messageWithRightNeighbour);
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
            case CIP -> {
                cipMessageHandler(message);
            }
            case ACNP -> {
                acnpMessageHandler(message, session);
            }
            case SMALL -> {
                smallMessageHandler(message, session);
            }
            case WIN -> {
                long win = Integer.parseInt(message.getBody());
                node.setWinP(win);
            }

        }
    }

    private void cipMessageHandler(Message message) {
        if (node.getStateNode() == StateNode.PASSIVE) {
            node.getNeighbourRight().sendMessage(message.getMessage()); // I'm passive pass data to right neighbour
        } else {
            long cip = Integer.parseInt(message.getBody());
            node.setAcnP(cip);
        }

    }

    private void acnpMessageHandler(Message message, Session session) {
        int acnp = Integer.parseInt(message.getBody());

        // only 2 active nodes in topology
        if (acnp == node.getId()) {
            long small = Math.min(node.getAcnP(), node.getId());
            node.setWinP(small);
            if (small == node.getId()) {
                node.setStateNode(StateNode.LEADER);
            }

            // send small neighbour
            Message messageWithSmall = new Message(WIN, small);
            sendMessage(session, messageWithSmall);
        } else if (acnp < node.getId()) {
            node.setStateNode(StateNode.PASSIVE);
        } else {
            // todo
        }


    }

    private void smallMessageHandler(Message message, Session session) {
        long small = Integer.parseInt(message.getBody());

        if (small == node.getId()) {
            node.setWinP(small);
            node.setStateNode(StateNode.LEADER);
        }
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
        log.info(String.format("Session %s closed because of %s", session.getId(), reason));

    }

}
