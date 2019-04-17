import java.util.ArrayList;

/**
 * The group class is the class that contains all the information of a group
 */

class Group {

    private String name;
    private ArrayList<Client> clients;
    private Client groupLeader;

    Group(String name, Client groupLeader) {
        this.name = name;
        this.groupLeader = groupLeader;
        clients = new ArrayList<>();
        clients.add(groupLeader);
    }

    /**
     * The addClient adds a client to the group
     *
     * @param client The client that needs to be added to teh group
     */
    void addClient(Client client) {
        clients.add(client);
    }

    /**
     * Check if the user with a certain username is the leader of the group
     *
     * @param userName the username of the user you want to check if it is the leader of the group
     * @return Returns true if the user is the leader of the group, else returns false
     */
    boolean isLeader(String userName) {
        return groupLeader.getUsername().equals(userName);
    }

    /**
     * Checks if a certain user is in the group
     *
     * @param client The client you want ot check if it is part of the group
     * @return Returns true if the client is part of the group, else returns false
     */
    boolean isUserInGroup(Client client) {
        return clients.contains(client);
    }

    /**
     * Gets the name of the group
     *
     * @return Returns the name of the group
     */
    String getName() {
        return name;
    }

    /**
     * Gets the list of clients that are in the group
     *
     * @return Returns the list of clients
     */
    ArrayList<Client> getClients() {
        return clients;
    }

    /**
     * Removes a client from teh group
     *
     * @param client The client that needs to be removed
     */
    void removeClient(Client client) {
        clients.remove(client);
    }

    /**
     * Sends a message to all the clients within the group
     *
     * @param message The message that needs to be send to the clients within the group
     */
    void sendMessage(String message) {
        for (Client client : clients) {
            client.print(message);
        }
    }

    /**
     * set a new leader of the group
     *
     * @param groupLeader The client that becomes the group leader
     */
    void setGroupLeader(Client groupLeader) {
        this.groupLeader = groupLeader;
    }
}
