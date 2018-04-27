package kvpaxos;
import paxos.Paxos;
import paxos.State;
// You are allowed to call Paxos.Status to check if agreement was made.

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;


public class Server implements KVPaxosRMI {

    ReentrantLock mutex;
    Registry registry;
    Paxos px;
    int me;

    String[] servers;
    int[] ports;
    KVPaxosRMI stub;

    // Your definitions here

    Map<Integer, Object> log;
    int seq;



    public Server(String[] servers, int[] ports, int me){
        this.me = me;
        this.servers = servers;
        this.ports = ports;
        this.mutex = new ReentrantLock();
        this.px = new Paxos(me, servers, ports);
        // Your initialization code here

        this.log = new ConcurrentHashMap<Integer, Object>();


        try{
            System.setProperty("java.rmi.server.hostname", this.servers[this.me]);
            registry = LocateRegistry.getRegistry(this.ports[this.me]);
            stub = (KVPaxosRMI) UnicastRemoteObject.exportObject(this, this.ports[this.me]);
            registry.rebind("KVPaxos", stub);
        } catch(Exception e){
            e.printStackTrace();
        }
    }


    // RMI handlers
    public Response Get(Request req){
        // Your code here
        //enter a Get Op to the log

        return null;
    }

    public Response Put(Request req){
        // Your code here
        //paxos log???
        //add a Put Op to the log
        //Paxos.Start(req.seq, req.op);

        return null;
    }

    public Op wait(int seq){
        int to = 10;
        while(true)
        {
            Paxos.retStatus ret = this.px.Status(seq);
            if(ret.state == State.Decided){
                return Op.class.cast(ret.v);
            }
            try
            {
                try {
                    Thread.sleep(to);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            catch(Exception e){
                e.printStackTrace();
            }
            if(to < 1000){
                to *= 2;
            }
        }
    }

}
