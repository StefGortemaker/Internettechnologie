public class ServerMessage {

    private ServerMessage.MessageType type;
    private String line;

    ServerMessage(ServerMessage.MessageType type, String line) {
        this.type = type;
        this.line = line;
    }

    public String toString() {
        return this.type + " " + this.line;
    }

    public enum MessageType {
        BCST, DSCN, GRP_SEND, GRP_JOIN, GRP_KICK, GRP_LEAVE, HELO, PM, PING, QUIT;

        MessageType() {
        }
    }
}
