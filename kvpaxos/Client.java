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

        return null;
    }

    public boolean Put(String key, Integer value){
        // Your code here
        //broadcast call?
        for(int i = 0; i < servers.length; i++) {
            Response getResponse = Call("Put", new Request(new Op("Put", < FIGURE THIS OUT >, key, value)), i);
        }

        // create a request, use Call() to contact server
        return false;
    }

}
