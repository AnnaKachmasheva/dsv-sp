package sc.cvut.fel.dsv.sp.util;

import lombok.extern.slf4j.Slf4j;
import sc.cvut.fel.dsv.sp.topology.Node;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class ConsoleHandler implements Runnable {

    private static final String IPV4_REGEX =
            "^(25[0-5]|2[0-4][0-9]|[0-1]?[0-9]?[0-9])(\\.(25[0-5]|2[0-4][0-9]|[0-1]?[0-9]?[0-9])){3}$";
    private static final Pattern IPV4_PATTERN = Pattern.compile(IPV4_REGEX);


    private static final String PORT_REGEX = "^([0-9][0-9][0-9][0-9])$";
    private static final Pattern PORT_PATTERN = Pattern.compile(PORT_REGEX);


    private boolean reading = true;
    private BufferedReader reader;
    private Node myNode;

    private boolean isConnect = false;
    private boolean isPort = false;
    private boolean isHost = false;
    private Integer port;
    private String host;


    public ConsoleHandler(Node myNode) {
        this.myNode = myNode;
        reader = new BufferedReader(new InputStreamReader(System.in));
    }


    private void parse_commandline(String commandline) {

        if (isConnect && !isHost) {
            boolean isHostValid = validHost(commandline);

            if (isHostValid) {
                host = commandline;
                isHost = true;
            } else {
                log.warn("Host must has format '127.0.0.100'");
            }

            return;
        } else if (isConnect && !isPort) {
            boolean isPortValid = validPort(commandline);

            if (isPortValid) {
                port = Integer.parseInt(commandline);
                isPort = true;
            } else {
                log.warn("Port must has 4 numbers format");
            }

            return;
        }

        if (isConnect) {
            isConnect = false;
            isHost = false;
            isPort = false;

            myNode.connect(host, port);
            return;
        }

        switch (commandline) {
            case "?" -> log.info("Possible commands: {} \n", helpMessage());
            case "info" -> log.info("MY NODE {}", myNode);
            case "on" -> {
                myNode.run();
                log.info("ON MY NODE");
            }
            case "off" -> {
                log.info("OFF MY NODE");
            }
            case "connect" -> {
                isConnect = true;
            }

            default -> log.info("Unknown command. For more information input '?'");
        }

    }


    private boolean validHost(String commandline) {
        Matcher matcher = IPV4_PATTERN.matcher(commandline);
        return matcher.matches();
    }

    private boolean validPort(String commandline) {
        Matcher matcher = PORT_PATTERN.matcher(commandline);
        return matcher.matches();
    }

    private String helpMessage() {
        StringBuilder builder = new StringBuilder();

        builder.append("info - info about my node").append('\n')
                .append("on - ON my node").append('\n')
                .append("off - OFF my node").append('\n')
                .append("connect - connect with node");

        return builder.toString();
    }


    @Override
    public void run() {
        String commandline;
        log.info("Opening ConsoleHandler.");

        while (reading) {
            if (!isConnect)
                System.out.print("\ncmd > ");
            else if (!isHost) {
                System.out.print("  host > ");
            } else if (!isPort) {
                System.out.print("  port > ");
            }

            try {
                commandline = reader.readLine();
                parse_commandline(commandline);
            } catch (IOException e) {
                log.error("ConsoleHandler - error in reading console input.");

                reading = false;
            }
        }

        log.info("Closing ConsoleHandler.");
    }


}
