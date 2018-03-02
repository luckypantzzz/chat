package com.nikolaev;

public class Message {

    private MessageType messageType;
    private User user;
    private String data;
    private int roomNumber;

    public Message(MessageType messageType, User user) {
        this.messageType = messageType;
        this.user = user;
    }

    public Message(MessageType messageType, int roomNumber) {
        this.messageType = messageType;
        this.roomNumber = roomNumber;
    }

    public Message(MessageType messageType) {
        this.messageType = messageType;
    }

    public Message(MessageType messageType, String data, int roomNumber) {
        this.messageType = messageType;
        this.data = data;
        this.roomNumber = roomNumber;
    }

    public Message(MessageType messageType, String data) {
        this.messageType = messageType;
        this.data = data;
    }

    public int getRoomNumber() {
        return roomNumber;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public User getUser() {
        return user;
    }

    public String getData() {
        return data;
    }

    public enum MessageType {
        AUTHORIZATION,
        REGISTRATION,
        USER_ACCEPTED,
        MESSAGE,
        USER_ADDED,
        USER_REMOVED,
        USER_BANNED,
        USER_BLOCKED,
        BLOCK_REQUEST,
        ROOM_CHOICE
    }
}
