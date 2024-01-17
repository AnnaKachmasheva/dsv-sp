package sc.cvut.fel.dsv.sp.topology.model;

import lombok.Getter;

@Getter
public class Message {

    private static final String DELIMITER_TITLE_BODY = ":";
    private static final String DELIMITER_IN_BODY = "_";

    private String title;
    private String body;

    public Message(String title, String body) {
        this.title = title;
        this.body = body;
    }

    public Message(String message) {
        String[] partMessage = message.split(DELIMITER_TITLE_BODY);

        if (partMessage.length > 0)
            this.title = partMessage[0];

        if (partMessage.length > 1)
            this.body = partMessage[1];
        else {
            this.body = "";
        }
    }

    public Message(String title, Address address) {
        this.title = title;
        this.body = address == null ? "" : address.getHost() + DELIMITER_IN_BODY + address.getPort();
    }

    public Message(String title, Long id) {
        this.title = title;
        this.body = String.valueOf(id);
    }

    // message type CIP, ACNP
    public Message(String title, Long id, Long num) {
        this.title = title;
        this.body = id + "," + num;
    }


    public String getMessage() {
        return title + DELIMITER_TITLE_BODY + body;
    }


}
