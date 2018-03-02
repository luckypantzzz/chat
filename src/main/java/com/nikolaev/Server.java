package com.nikolaev;

import com.nikolaev.util.ConsoleHelper;

import java.io.IOException;
import java.net.*;
import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

public class Server {

    private static Map<Integer, Map<String, Connection>> connectionMap = new ConcurrentHashMap<>();
    private static Map<String, String> userMap = new ConcurrentHashMap<>();
    static {
        userMap.put("admin", String.valueOf("admin".hashCode()));
        userMap.put("test", String.valueOf("test".hashCode()));
    }

    private static Set<String> bannedUsers = new ConcurrentSkipListSet<>();
//    static {
//        bannedUsers.add("127.0.0.1");
//    }

    private static Map<String, LocalTime> blockedMap = new ConcurrentHashMap<>();

    private static void sendMessageToAllInRoom(Message message) {
        for (Connection connection : connectionMap.get(message.getRoomNumber()).values()) {
            connection.send(message);
        }
    }

    private static class Handler extends Thread {

        private Socket socket;

        public Handler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            SocketAddress remoteSocketAddress = null;
            String user = null;
            Integer roomNumber = null;
            try (Connection connection = new Connection(socket)) {
                remoteSocketAddress = connection.getRemoteSocketAddress();
                ConsoleHelper.writeMessage("A new connection from remote location " + remoteSocketAddress);
                user = authorization(connection);
                roomNumber = roomChoosing(connection);
                addUserToRoom(connection, user, roomNumber);
                sendMessageToAllInRoom(new Message(Message.MessageType.USER_ADDED, user, roomNumber));
                sendListOfUsers(user, connection, roomNumber);
                serverLoop(connection, user, roomNumber);
            } catch (IOException e) {
                ConsoleHelper.writeMessage("Error communicating with remote address");
            } finally {
                if (user != null && roomNumber != null) {
                    connectionMap.get(roomNumber).remove(user);
                    sendMessageToAllInRoom(new Message(Message.MessageType.USER_REMOVED, user, roomNumber));
                }
            }
            ConsoleHelper.writeMessage("Closed connection to remote address " + remoteSocketAddress);
        }

        private void sendListOfUsers(String user, Connection connection, Integer roomNumber) {
            List<String> users = connectionMap.get(roomNumber).keySet().stream()
                    .filter(s -> !s.equals(user))
                    .collect(Collectors.toList());
            connection.send(new Message(Message.MessageType.MESSAGE,
                    "Greetings!\n" +
                    "In chat available next commands:\n" +
                    "/block <userName>\n" +
                    "List of current users " + users.toString()));
        }

        private String authorization(Connection connection) throws IOException {
            int count = 1;
            while (true) {
                if (bannedUsers.contains(connection.getIP()))
                    connection.send(new Message(Message.MessageType.USER_BANNED));

                connection.send(new Message(Message.MessageType.AUTHORIZATION));
                Message message = connection.receive();
                String login = message.getUser().getLogin();
                String password = message.getUser().getPassword();

                if (blockedMap.containsKey(login)) {
                    LocalTime blockedTime = blockedMap.get(login);
                    long duration = Duration.between(blockedTime, LocalTime.now()).toMinutes();
                    if (duration < 10)
                        connection.send(new Message(Message.MessageType.USER_BLOCKED, String.valueOf(10 - duration)));
                    else blockedMap.remove(login);
                }

                if (message.getMessageType() == Message.MessageType.AUTHORIZATION &&
                        message.getUser() != null && !userMap.containsKey(login)) {
                    registration(connection);
                    continue;
                }

                if (!userMap.get(login).equals(password)) {
                    if (count == 5) {
                        bannedUsers.add(connection.getIP());
                    }
                    count++;
                    continue;
                }

//                connectionMap.put(login, connection);
                connection.send(new Message(Message.MessageType.USER_ACCEPTED));
                return login;
            }
        }

        private int roomChoosing(Connection connection) throws IOException {
            connection.send(new Message(Message.MessageType.ROOM_CHOICE));
            Message message = connection.receive();
            return message.getRoomNumber();
        }

        private void addUserToRoom(Connection connection, String user, Integer roomNumber) {
            if (!connectionMap.containsKey(roomNumber)) {
                connectionMap.put(roomNumber, new ConcurrentHashMap<>());
            }
            Map<String, Connection> map = connectionMap.get(roomNumber);
            map.put(user, connection);
            connectionMap.put(roomNumber, map);
            connection.send(new Message(Message.MessageType.USER_ACCEPTED));
        }

        private void registration(Connection connection) throws IOException {
            while (true) {
                connection.send(new Message(Message.MessageType.REGISTRATION));
                Message message = connection.receive();
                String login = message.getUser().getLogin();
                String password = message.getUser().getPassword();
                if (message.getMessageType() == Message.MessageType.REGISTRATION &&
                        login != null && !login.equals("") && login.matches("[\\w]+") &&
                        password != null && !password.equals("") ) {
                    userMap.put(login, password);
                    connection.send(new Message(Message.MessageType.USER_ACCEPTED));
                    break;
                }
            }
        }

        private void serverLoop(Connection connection, String user , Integer roomNumber) throws IOException {
            while (true) {
                Message message = connection.receive();
                if (message.getMessageType() == Message.MessageType.MESSAGE) {
                    String text = String.format("%s: %s", user, message.getData());
                    sendMessageToAllInRoom(new Message(Message.MessageType.MESSAGE, text, roomNumber));
                } else if (message.getMessageType() == Message.MessageType.BLOCK_REQUEST) {
                    String login = message.getData().split("[\\s]")[1];
                    if (!connectionMap.get(roomNumber).containsKey(login)) {
                        String text = String.format("User with name %s not found", login);
                        connection.send(new Message(Message.MessageType.MESSAGE, text));
                        continue;
                    }
                    blockedMap.put(login, LocalTime.now());
                    connectionMap.get(roomNumber).get(login).send(new Message(Message.MessageType.USER_BLOCKED));
                }
            }
        }
    }

    public static void main(String[] args) {
        ConsoleHelper.writeMessage("Enter server port");
        try {
            ServerSocket serverSocket = new ServerSocket(ConsoleHelper.readInt());
            ConsoleHelper.writeMessage("Server Up");
            while (true) {
                new Handler(serverSocket.accept()).start();
            }
        } catch (IOException e) {
            ConsoleHelper.writeMessage("Server connection error");
        }
    }
}
