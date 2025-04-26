import java.io.Serializable;

public class Message implements Serializable {
    static final long serialVersionUID = 42L;
    MessageType type;
    String message;
    String username;
    String recipient;
    int column;
    int[][] board;
    boolean isPlayerTurn;

    public Message(String message) {
        this.type = MessageType.SIMPLE;
        this.message = message;
    }

    public Message(MessageType type, String username) {
        this.type = type;
        this.username = username;
    }

    public Message(String username, int column) {
        this.type = MessageType.GAME_MOVE;
        this.username = username;
        this.column = column;
    }

    public Message(MessageType type, int[][] board, boolean isPlayerTurn) {
        this.type = type;
        this.board = board;
        this.isPlayerTurn = isPlayerTurn;
    }

    public Message(String sender, String recipient, String message) {
        this.type = MessageType.TEXT;
        this.username = sender;
        this.recipient = recipient;
        this.message = message;
    }

    public Message(MessageType type, String message, int[][] finalBoard) {
        this.type = type;
        this.message = message;
        this.board = finalBoard;
    }
}