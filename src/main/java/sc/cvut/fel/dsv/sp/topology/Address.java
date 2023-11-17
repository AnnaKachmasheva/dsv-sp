package sc.cvut.fel.dsv.sp.topology;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

@Data
@Slf4j
@AllArgsConstructor
public class Address {

    String host;
    int port;

    public URI getURIStr() {
        StringBuilder builder = new StringBuilder();

        builder.append(host)
                .append(":")
                .append(port);

        String uriStr = builder.toString();

        URI uri = null;
        try {
            uri = new URI(uriStr);
        } catch (URISyntaxException e) {
            log.error("NOT VALID URI with host: {} and port:{}", host, port);
        }

        return uri;
    }

    @Override
    public String toString() {
        return "Address{" +
                "host = '" + host + '\'' +
                ", port = " + port +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Address address = (Address) o;
        return port == address.port && Objects.equals(host, address.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port);
    }
}
