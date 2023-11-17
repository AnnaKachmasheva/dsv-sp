package sc.cvut.fel.dsv.sp.topology;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;


@Data
@Slf4j
public class Client {

    private Address address;

    public Client(Address address) {
        this.address = address;
    }

}
