package paxos;
import java.io.Serializable;

/**
 * Please fill in the data structure you use to represent the request message for each RMI call.
 * Hint: You may need the sequence number for each paxos instance and also you may need proposal number and value.
 * Hint: Make it more generic such that you can use it for each RMI call.
 * Hint: Easier to make each variable public
 */
public class Request implements Serializable {
    static final long serialVersionUID=1L;
    // Your data here
    int seq;
    int propNum;
    Object value;


    // Your constructor and methods here
    public Request(int seq, Object value, int propNum){
        this.seq = seq;
        this.propNum = propNum;
        this.value = value;
    }
}
