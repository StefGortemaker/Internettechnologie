import java.util.ArrayList;

public class Group {

    private String name;
    private ArrayList<Client> clients;
    private Client groupLeader;

    Group(String name, Client groupLeader) {
        this.name = name;
        this.groupLeader = groupLeader;
        clients = new ArrayList<>();
        clients.add(groupLeader);
    }

    void addClient(Client client) {
        clients.add(client);
    }

    boolean isLeader(String userName) {
        return groupLeader.getUsername().equals(userName);
    }

    boolean isUserInGroup(Client client) {
        return clients.contains(client);
    }

    String getName() {
        return name;
    }

    ArrayList<Client> getClients() {
        return clients;
    }

    void removeClient(Client client) {
        clients.remove(client);
    }

    void sendMessage(String message) {
        for (Client client : clients) {
            client.print(message);
        }
    }

    void setGroupLeader(Client groupLeader) {
        this.groupLeader = groupLeader;
    }
}
