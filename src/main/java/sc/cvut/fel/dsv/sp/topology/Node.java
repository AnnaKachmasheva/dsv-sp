package sc.cvut.fel.dsv.sp.topology;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import sc.cvut.fel.dsv.sp.util.ConsoleHandler;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Data
public class Node implements Runnable, Communication.MessageListener {

    Integer id; // unique
    Integer winp; // unique

    Address address;

    Server server; // winp
    Client client;

    StateNode stateNode;

    Server neighbour1;
    Server neighbour2;

    ConsoleHandler myConsoleHandler;

    Communication communication1;
    Communication communication2;

    Integer acn_p;
    Integer q2;

    public Node(Address address) {
        this.address = address;

        this.stateNode = StateNode.ACTIVE;
    }

    public void init() {
        id = generateId();
        myConsoleHandler = new ConsoleHandler(this);
    }


    //todo change it
    private int generateId() {
        UUID uuid = UUID.randomUUID();
        String str = address.host + ":" + address.port + uuid;

        return str.hashCode();
    }

    public void run() {
        client = new Client(address);


        if (communication1 == null && communication2 == null) {
            winp = id;
            // jsem server
            server = new Server(address);
        } else if (communication1 != null) {
            // client - server communication
            communication1.sendMessage(id.toString());
            if (acn_p == null)
                return;

            if (acn_p < id) {
                winp = acn_p;
                stateNode = StateNode.PASSIVE;
            }

        } else if (stateNode.equals(StateNode.ACTIVE)) {  // Peterson/DKR algorithm

            // send my_id addressNeighbour1, get response with min_id
            communication1.sendMessage(id.toString());

            if (acn_p == null)
                return;

            if (acn_p.equals(id)) {
                // I'm winner
                winp = id;
            } else {

                // send acn_p addressNeighbour2, get response with min_id
                communication2.sendMessage(acn_p.toString());

                if (q2 == null)
                    return;

                if (acn_p < id && acn_p < q2) {
                    id = acn_p;
                } else {
                    stateNode = StateNode.PASSIVE;
                }
            }
        } else {
            // todo
            // prijima zpravy od sousedu
            // calculate min = min(my_id, request_min_id)
            // odesila min
        }

        if (id.equals(winp))
            stateNode = StateNode.LEADER;

        if (winp == null)
            stateNode = StateNode.LOST;

        new Thread(myConsoleHandler).run();
    }

    public void connect(String host, Integer port) {
        Server server = new Server(new Address(host, port));
        Communication communication = new Communication(client, server);

        if (neighbour1 == null) {
            neighbour1 = server;
            communication1 = communication;
            communication1.initConnection();
        } else if(neighbour2 == null) {
            neighbour2 = server;
            communication2 = communication;
            communication2.initConnection();
        } else {
            log.warn("My node already has 2 neighbours");
        }

    }


    @Override
    public String toString() {
        return "\n Node{" + '\n' +
                "   id = " + id + '\n' +
                "   address = " + address + '\n' +
                "   server = " + server + '\n' +
                "   client = " + client + '\n' +
                "   stateNode = " + stateNode + '\n' +
                "   neighbour1 = " + neighbour1 + '\n' +
                "   neighbour2 = " + neighbour2 + '\n' +
                '}';
    }

    @Override
    public void onMessageReceived(String message, Server serverMessage) throws IOException {
        int idMessage = Integer.parseInt(message);

        int minId = Math.min(idMessage, id);
        if(idMessage == minId) {
            winp = idMessage;
            server = serverMessage;

            if (stateNode.equals(StateNode.LEADER) || stateNode.equals(StateNode.ACTIVE))
                stateNode = StateNode.PASSIVE;
        } else {
            winp = id;
            stateNode = StateNode.LEADER;
        }

        String messageForServer = Integer.toString(winp);
        server.onMessage(messageForServer);

    }

}
