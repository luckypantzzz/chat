package com.nikolaev.client;

import com.nikolaev.Connection;
import com.nikolaev.util.ConsoleHelper;
import com.nikolaev.Message;
import com.nikolaev.User;

import java.io.IOException;
import java.net.Socket;

public class Client {

    private Connection connection;
    private volatile boolean clientConnected = false;

    private String getServerAddress() {
        ConsoleHelper.writeMessage("Enter address");
        return ConsoleHelper.readString();
    }

    private int getServerPort() {
        ConsoleHelper.writeMessage("Enter port");
        return ConsoleHelper.readInt();
    }

    private String getLogin() {
        ConsoleHelper.writeMessage("Enter your login");
        return ConsoleHelper.readString();
    }

    private String getPassword() {
        ConsoleHelper.writeMessage("Enter your password");
        return ConsoleHelper.readString();
    }

    private int getRoomNumber() {
        ConsoleHelper.writeMessage("Choose a room");
        return ConsoleHelper.readInt();
    }

    public void run() {
        SocketThread socketThread = new SocketThread();
        socketThread.setDaemon(true);
        socketThread.start();

        try {
            synchronized (this) {
                this.wait();
            }
        } catch (InterruptedException e) {
            ConsoleHelper.writeMessage("Connection failed");
        }

        if (clientConnected)
            ConsoleHelper.writeMessage("Connection established. Print exit to quit");
        else
            ConsoleHelper.writeMessage("Connection failed");

        while (clientConnected) {
            String text = ConsoleHelper.readString();
            if (!text.equalsIgnoreCase("exit") && clientConnected) {
                if (text.matches("/block(\\s)+[\\w]+")){
                    sendBlockRequest(text);
                    continue;
                }
                sendMessage(text);
            } else
                break;
        }
    }

    private void sendMessage(String text) {
        connection.send(new Message(Message.MessageType.MESSAGE, text));
    }

    private void sendBlockRequest(String text) {
        connection.send(new Message(Message.MessageType.BLOCK_REQUEST, text));
    }

    private class SocketThread extends Thread {

        private void incomingMessage(String message) {
            ConsoleHelper.writeMessage(message);
        }

        private void userAddedNotification(String user) {
            ConsoleHelper.writeMessage(String.format("%s join to chat", user));
        }

        private void userRemovedNotification(String user) {
            ConsoleHelper.writeMessage(String.format("%s left the chat", user));
        }

        private void userBlockedNotification() {
            ConsoleHelper.writeMessage("You have been blocked!");
            connectionStatus(false);
        }

        @Override
        public void run() {
            String host = getServerAddress();
            int port = getServerPort();
            try {
                Socket socket = new Socket(host, port);
                connection = new Connection(socket);
                authorization();
                roomChoosing();
                clientLoop();
            } catch (IOException e) {
                connectionStatus(false);
            }
        }

        private void authorization() throws IOException {
            while (true) {
                Message message = connection.receive();
                if (message.getMessageType() == Message.MessageType.AUTHORIZATION) {
                    ConsoleHelper.writeMessage("Authorization Form");
                    String login = getLogin();
                    String password = getPassword();
                    connection.send(new Message(Message.MessageType.AUTHORIZATION, new User(login, password)));
                } else if (message.getMessageType() == Message.MessageType.REGISTRATION) {
                    ConsoleHelper.writeMessage("User with this login is missing");
                    registration();
                } else if (message.getMessageType() == Message.MessageType.USER_ACCEPTED) {
                    ConsoleHelper.writeMessage("Authorization passed");
//                    connectionStatus(true);
                    break;
                } else if (message.getMessageType() == Message.MessageType.USER_BANNED) {
                    ConsoleHelper.writeMessage("You have been banned!");
                    connectionStatus(false);
                    break;
                } else if (message.getMessageType() == Message.MessageType.USER_BLOCKED) {
                    ConsoleHelper.writeMessage(String.format("You have been blocked for 10 minutes. " +
                            "Time remaining %s", message.getData()));
                    connectionStatus(false);
                    break;
                } else throw new IOException("Unknown MessageType");
            }
        }

        private void roomChoosing() throws IOException {
            while (true) {
                Message message = connection.receive();
                if (message.getMessageType() == Message.MessageType.ROOM_CHOICE) {
                    int roomNumber = getRoomNumber();
                    connection.send(new Message(Message.MessageType.ROOM_CHOICE, roomNumber));
                } else if (message.getMessageType() == Message.MessageType.USER_ACCEPTED) {
                    connectionStatus(true);
                    break;
                }
            }
        }

        private void registration() throws IOException {
            ConsoleHelper.writeMessage("Registration Form");
            while (true) {
                ConsoleHelper.writeMessage("Login must contains only [a-zA-Z_0-9]");
                String login = getLogin();
                String password = getPassword();
                connection.send(new Message(Message.MessageType.REGISTRATION, new User(login, password)));
                Message message = connection.receive();
                if (message.getMessageType() == Message.MessageType.USER_ACCEPTED) {
                    ConsoleHelper.writeMessage("Registration successful");
                    break;
                }
            }
        }

        private void connectionStatus(boolean clientConnected) {
            Client.this.clientConnected = clientConnected;
            synchronized (Client.this) {
                Client.this.notify();
            }
        }

        private void clientLoop() throws IOException {
            while (true) {
                Message message = connection.receive();
                switch (message.getMessageType()) {
                    case MESSAGE:
                        incomingMessage(message.getData());
                        break;
                    case USER_ADDED:
                        userAddedNotification(message.getData());
                        break;
                    case USER_REMOVED:
                        userRemovedNotification(message.getData());
                        break;
                    case USER_BLOCKED:
                        userBlockedNotification();
                        break;
                    default:
                        throw new IOException("Unknown MessageType");
                }
            }
        }
    }

    public static void main(String[] args) {
        new Client().run();
    }
}
