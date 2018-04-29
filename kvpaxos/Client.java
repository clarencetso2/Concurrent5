package kvpaxos;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class Client {
    String[] servers;
    int[] ports;

    // Your data here

    static int id = -1;
    int clientSeq =-1;



    public Client(String[] servers, int[] ports){
        this.servers = servers;
        this.ports = ports;
        // Your initialization code here
        this.id = id + 1;
    }

    /**
     * Call() sends an RMI to the RMI handler on server with
     * arguments rmi name, request message, and server id. It
     * waits for the reply and return a response message if
     * the server responded, and return null if Call() was not
     * be able to contact the server.
     *
     * You should assume that Call() will time out and return
     * null after a while if it doesn't get a reply from the server.
     *
     * Please use Call() to send all RMIs and please don't change
     * this function.
     */
    public Response Call(String rmi, Request req, int id){
        Response callReply = null;
        KVPaxosRMI stub;
        try{
            Registry registry= LocateRegistry.getRegistry(this.ports[id]);
            stub=(KVPaxosRMI) registry.lookup("KVPaxos");
            if(rmi.equals("Get"))
                callReply = stub.Get(req);
            else if(rmi.equals("Put")){
                callReply = stub.Put(req);}
            else
                System.out.println("Wrong parameters!");
        } catch(Exception e){
            return null;
        }
        return callReply;
    }

    // RMI handlers CALLERS
    public Integer Get(String key){
        // Your code here
        Op op = new Op("Get", clientSeq, key, 0);
        Response getResponse=null;

        for(int i = 0; getResponse == null; i= (i+1)%servers.length) {
            getResponse = Call("Get", new Request(op), i);
        }
        return getResponse.value;
    }

    public boolean Put(String key, Integer value){
        // Your code here
        Response putResponse=null;
        Op op = new Op("Put", id, key, value);
        for(int i = 0; putResponse == null; i= (i+1)%servers.length) {
            //System.out.println(i);
            putResponse = Call("Put", new Request(op), i);
        }

        return true;
    }

}
