package client;

public class MainClient {
    public static void main(String[] args) {
        ChatClient client = new ChatClient();
        client.connect("localhost", 8910);
    }
}