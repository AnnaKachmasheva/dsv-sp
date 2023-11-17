package sc.cvut.fel.dsv.sp.chat;

import sc.cvut.fel.dsv.sp.chat.enums.MessageType;

import java.time.LocalDateTime;

public record Message(String description,
                      LocalDateTime dateTime,
                      MessageType messageType,
                      User sender) {
}