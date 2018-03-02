package com.nikolaev;

import com.google.gson.Gson;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

public class Connection implements Closeable {

    private final Socket socket;
    private final PrintWriter writer;
    private final BufferedReader reader;
    private final Gson gson = new Gson();

    public Connection(Socket socket) throws IOException {
        this.socket = socket;
        this.writer = new PrintWriter(socket.getOutputStream());
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public void send(Message message) {
        synchronized (writer) {
            writer.println(gson.toJson(message));
            writer.flush();
        }
    }

    public Message receive() throws IOException {
        synchronized (reader) {
            return gson.fromJson(reader.readLine(), Message.class);
        }
    }

    public SocketAddress getRemoteSocketAddress() {
        return socket.getRemoteSocketAddress();
    }

    public String getIP() {
        return socket.getInetAddress().getHostAddress();
    }

    public void close() throws IOException {
        socket.close();
        writer.close();
        reader.close();
    }
}
