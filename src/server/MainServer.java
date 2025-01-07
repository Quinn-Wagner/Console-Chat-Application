package server;

public class MainServer {
    public static void main(String[] args) {
        ChatServer server = ChatServer.getInstance();
        server.startServer(8910);
    }
}