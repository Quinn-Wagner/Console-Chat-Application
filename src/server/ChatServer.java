package server;

import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {
    private ServerSocket serverSocket;
    private volatile boolean running = true;
    private final List<ClientHandler> clients = Collections.synchronizedList(new ArrayList<>());

    private ChatServer() {}

    private static class SingletonHolder {
        private static final ChatServer INSTANCE = new ChatServer();
    }

    public static ChatServer getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public void stopServer() {
        running = false;
        try {
            synchronized (clients) {
                for (ClientHandler client : clients) {
                    client.sendMessage("The server is shutting down.");
                    client.closeConnection();
                }
            }

            serverSocket.close();
            System.out.println("Server stopped successfully.");
        } catch (IOException e) {
            System.err.println("Error closing server socket: " + e.getMessage());
        }
    }

    public void startServer(int port) {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server started on port " + port);

            new Thread(this::listenForAdminCommands).start();

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    ClientHandler handler = new ClientHandler(clientSocket, this);
                    clients.add(handler);
                    new Thread(handler).start();
                } catch (IOException e) {
                    if (running) {
                        System.err.println("Error accepting client connection: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error starting server: " + e.getMessage());
        }
    }

    private void listenForAdminCommands() {
        try (BufferedReader consoleInput = new BufferedReader(new InputStreamReader(System.in))) {
            String command;
            while ((command = consoleInput.readLine()) != null) {
                if ("exit".equalsIgnoreCase(command)) {
                    stopServer();
                    break;
                } else if (command.startsWith("kick ")) {
                    String clientName = command.substring(5).trim();
                    kickClient(clientName);
                } else if ("list".equalsIgnoreCase(command)) {
                    listClients();
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading admin command: " + e.getMessage());
        }
    }

    public void kickClient(String clientName) {
        synchronized (clients) {
            for (ClientHandler client : clients) {
                if (client.getClientName().equals(clientName)) {
                    client.sendMessage("You have been kicked from the server.");
                    client.closeConnection();
                    clients.remove(client);
                    System.out.println(clientName + " has been kicked from the server.");
                    return;
                }
            }
        }
        System.out.println("No client found with the name: " + clientName);
    }

    public void listClients() {
        synchronized (clients) {
            if (clients.isEmpty()) {
                System.out.println("No clients connected.");
            } else {
                System.out.println("Connected clients:");
                for (ClientHandler client : clients) {
                    System.out.println(client.getClientName());
                }
            }
        }
    }

    public void broadcast(String message, ClientHandler sender) {
        synchronized (clients) {
            for (ClientHandler client : clients) {
                if (client != sender) {
                    client.sendMessage(message);
                }
            }
        }
    }

    public void removeClient(ClientHandler client) {
        clients.remove(client);
    }
}