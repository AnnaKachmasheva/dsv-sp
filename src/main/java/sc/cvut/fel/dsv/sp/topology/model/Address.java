package sc.cvut.fel.dsv.sp.topology.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
@AllArgsConstructor
public class Address {

    private String host;
    private int port;

}