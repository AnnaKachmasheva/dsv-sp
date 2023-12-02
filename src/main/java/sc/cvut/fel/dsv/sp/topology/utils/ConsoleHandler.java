package sc.cvut.fel.dsv.sp.topology.utils;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import sc.cvut.fel.dsv.sp.topology.Node;
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
                if (node.getNeighbourLeft() != null && node.getNeighbourRight() != null) {
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
                .append("stop server ").append('\n')
                .append("connect     - connect with other node");

        return builder.toString();
    }

    public void connect(String host, int port) {

        Address address = new Address(host, port);
        Connection connection = new Connection(address);
        connection.run();

        if (node.getNeighbourLeft() == null) {
            node.setNeighbourLeft(connection);
        }

        if (node.getNeighbourRight() == null) {
            node.setNeighbourRight(connection);
        }

        // get left neighbour
        Message messageGetRightNeighbour = new Message(GET_RIGHT_NEIGHBOUR, "");
        connection.sendMessage(messageGetRightNeighbour.getMessage());

        // connect with right neighbour for ring topology
        Message messageMyAddress = new Message(NEW_RIGHT_NEIGHBOUR, node.getMyAddress());
        connection.sendMessage(messageMyAddress.getMessage());

        // begin send <one, cip>
        Message messageWithCIP = new Message(CIP, node.getId());
        connection.sendMessage(messageWithCIP.getMessage());

    }

    @Override
    public void onMessageFromServer(Session session, String messageFromServer) {
        log.info("Message from server: {}", messageFromServer);

        Message message = new Message(messageFromServer);
        String title = message.getTitle();
        if (title == null)
            return;
        String body = message.getBody();


        switch (title) {

            case RIGHT_NEIGHBOUR -> {
                if (message.getBody() == null) {
                    node.setNeighbourRight(node.getNeighbourLeft());
                } else {
                    Address address = parseAddress(message.getBody());

                    Connection newConnection = new Connection(address);
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
                node.setNeighbourLeft(new Connection(newAddress, session));

                // creat connection with new neighbour
                if ((node.getNeighbourLeft().getAddress().getPort() != node.getNeighbourRight().getAddress().getPort()) &&
                        !(node.getNeighbourLeft().getAddress().getHost().equals(node.getNeighbourRight().getAddress().getHost()))) {
                    node.getNeighbourLeft().run();
                    Message messageWithNewRightNeighbour = new Message(NEW_RIGHT_NEIGHBOUR, node.getMyAddress());
                    node.getNeighbourLeft().sendMessage(messageWithNewRightNeighbour.getMessage());
                }
            }

            case Q -> {
                // receive <one, q> ; acnp:= q ;
                int one = Integer.parseInt(body);

                node.setAcnP(one);
                //if acnp = cip
                if (Objects.equals(node.getAcnP(), node.getId())) {
                    // begin send <smal, acnp>
                    // todo

                    //winp := acnp
                    node.setWinP(node.getAcnP());

                } else {
                    //begin send<two, acnp>
                    if (node.getNeighbourRight() != null) {
                        Message messageACNPTwo = new Message(ACNP, node.getAcnP());
                        node.getNeighbourRight().sendMessage(messageACNPTwo.getMessage());

                    }
                }
            }

            case SMALL -> {
                // receive <small, q> ; end
            }
        }
    }

    private Address parseAddress(String messageBody) {
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
                try {
                    session.getBasicRemote().sendText(messageWithRightNeighbour.getMessage());
                } catch (IOException e) {
                    log.error("failed send send message: {} to server in session: {}.\n Message: {}",
                            messageWithRightNeighbour, session.getId(), e.getMessage());
                }
            }

            case NEW_RIGHT_NEIGHBOUR -> {

                Address newAddress = parseAddress(message.getBody());

                if (!newAddress.equals(node.getMyAddress())) {
                    Connection newConnection = new Connection(newAddress, session);

                    if (node.getNeighbourRight() != null)
                        node.getNeighbourRight().close();

                    node.setNeighbourRight(newConnection);

                    if (node.getNeighbourLeft() == null)
                        node.setNeighbourLeft(newConnection);
                }
            }
            case NEW_LEFT_NEIGHBOUR -> {
                Address newAddress = parseAddress(message.getBody());
                Connection newConnection = new Connection(newAddress, session);
                node.setNeighbourLeft(newConnection);

                if (node.getNeighbourRight() == null)
                    node.setNeighbourRight(newConnection);
            }

        }
    }

    @Override
    public void onClose(Session session, CloseReason reason) {
        log.info(String.format("Session %s closed because of %s", session.getId(), reason));
    }


}
