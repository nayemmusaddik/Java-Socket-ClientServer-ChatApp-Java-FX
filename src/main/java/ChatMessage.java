import java.io.Serializable;


public class ChatMessage implements Serializable {

    public static final int WHOISIN = 0;
    public static final int MESSAGE = 1;
    public static final int LOGOUT = 2;
    public static final int LOGIN = 3;
    protected static final long serialVersionUID = 1112122200L;
    private int type;
    private String message;


    public ChatMessage(int type, String message) {
        this.type = type;
        this.message = message;
    }

    public int getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }
}