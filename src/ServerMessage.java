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
        BCST, DSCN, HELO, PM, PING, QUIT;

        MessageType() {
        }
    }
}

