package sc.cvut.fel.dsv.sp.topology.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

import java.util.Objects;

@Data
@ToString
@AllArgsConstructor
public class Address {

    private String host;
    private int port;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Address address = (Address) o;
        return port == address.port &&
                Objects.equals(host, address.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port);
    }

}