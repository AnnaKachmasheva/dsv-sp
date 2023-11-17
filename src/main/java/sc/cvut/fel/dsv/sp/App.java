package sc.cvut.fel.dsv.sp;

import sc.cvut.fel.dsv.sp.topology.Address;
import sc.cvut.fel.dsv.sp.topology.Node;

public class App {

    public static void main(String[] args) {
        // start new node and set neighbours
        // todo  set paramets from cmd
        String myHost = "127.0.0.1";
        int myPort = 8080;
        Address address = new Address(myHost, myPort);

//        // 1st neighbour
//        String neighbour1Host = "127.0.0.100";
//        int neighbour1Port = 8080;
//        Address neighbour1Address = new Address(neighbour1Host, neighbour1Port);
//
//        // 2nd neighbour
//        String neighbour2Host = "127.0.0.200";
//        int neighbour2Port = 8080;
//        Address neighbour2Address = new Address(neighbour2Host, neighbour2Port);

        Node node = new Node(address);
        node.init();
        node.run();

    }

}