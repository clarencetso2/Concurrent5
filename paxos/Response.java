package paxos;
import java.io.Serializable;

/**
 * Please fill in the data structure you use to represent the response message for each RMI call.
 * Hint: You may need a boolean variable to indicate ack of acceptors and also you may need proposal number and value.
 * Hint: Make it more generic such that you can use it for each RMI call.
 */
public class Response implements Serializable {
    static final long serialVersionUID=2L;
    // your data here
    boolean ack;
    int propNum;
    Object value;
    int acceptNum;
    boolean decided;
    

    // Your constructor and methods here
    public Response(boolean ack, int propNum, int acceptNum, Object value, boolean decided){
        this.ack = ack;
        this.propNum = propNum;
        this.value = value;
        this.acceptNum = acceptNum;
        this.decided = decided;
    }
}
