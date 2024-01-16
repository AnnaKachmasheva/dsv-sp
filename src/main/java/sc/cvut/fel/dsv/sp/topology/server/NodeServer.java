package sc.cvut.fel.dsv.sp.topology.server;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.javax.server.config.JavaxWebSocketServletContainerInitializer;
import sc.cvut.fel.dsv.sp.topology.model.Address;
import sc.cvut.fel.dsv.sp.topology.server.endpoint.NodeServerEndpoint;

@Slf4j
@Getter
public class NodeServer {

    private static final String CONTEXT_PATH = "/";

    private Address address;
    private final Server server;
    private final ServerConnector connector;
    private int countConnection;


    public NodeServer(Address address) {
        this.address = address;
        countConnection = 0;
        server = new Server(address.getPort());
        connector = new ServerConnector(server);
        connector.setHost(address.getHost());
        server.addConnector(connector);

        log.info(this.toString());

        // Set up the basic application "context"
        // This is also known as the handler tree (in jetty speak)
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath(CONTEXT_PATH);
        server.setHandler(context);

        // Initialize javax.websocket layer
        JavaxWebSocketServletContainerInitializer.configure(context, (servletContext, wsContainer) -> {
            // This lambda will be called at the appropriate place in the
            // ServletContext initialization phase where you can initialize
            // and configure your websocket container.

            // Configure defaults for container
            wsContainer.setDefaultMaxTextMessageBufferSize(65535);

            // Add WebSocket endpoint to javax.websocket layer
            wsContainer.addEndpoint(NodeServerEndpoint.class);
        });

    }

    public void run() {
        try {
            server.start();
        } catch (Exception e) {
            log.error("failed start server: {}. \n Message {}", this, e.getMessage());
        }
    }

    public void stop() {
        try {
            server.stop();
        } catch (Exception e) {
            log.error("failed stop server: {}. \n Message {}", this, e.getMessage());
        }
    }

    public void join() {
        try {
            server.join();
        } catch (InterruptedException e) {
            log.error("failed join server: {}. \n Message {}", this, e.getMessage());
        }
    }


    @Override
    public String toString() {
        return "NodeServer{" + '\n' +
                "       port = " + address.getPort() + '\n' +
                "       server = " + server + '\n' +
                "       connector = " + connector + '\n' +
                "           host = " + connector.getHost() + '\n' +
                "           uri = " + server.getURI() + '\n' +

                '}';
    }
}
