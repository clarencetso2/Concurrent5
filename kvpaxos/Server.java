package kvpaxos;
import paxos.Paxos;
import paxos.State;
// You are allowed to call Paxos.Status to check if agreement was made.

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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

    Map<String, Integer> logData;
    int seq;



    public Server(String[] servers, int[] ports, int me){
        this.me = me;
        this.servers = servers;
        this.ports = ports;
        this.mutex = new ReentrantLock();
        this.px = new Paxos(me, servers, ports);
        // Your initialization code here

        this.logData = new ConcurrentHashMap<String, Integer>();
        seq = 0;


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
        px.Start(seq, req.op);
        Op getResponse = wait(seq);
        while(!getResponse.key.equals(req.op.key)) {
            if (logData.containsKey(req.op.key)) {
                return new Response(req.op.key, logData.get(req.op.key), seq);
            } else {
                Update();
                seq++;
                px.Start(seq, req.op);
                getResponse = wait(seq);
            }
        }
        return new Response(getResponse.key, getResponse.value, seq);
    }

    public Response Put(Request req){
        // Your code here
        seq = px.Max() + 1;
        px.Start(seq, req.op);
        Op putResponse = wait(seq);
        while(!putResponse.key.equals(req.op.key)){
            seq++;
            px.Start(seq, req.op);
            putResponse = wait(seq);
        }
        logData.put(putResponse.key, putResponse.value);
        return new Response(putResponse.key, putResponse.value, seq);
    }

    public Op wait(int seq){
        int to = 10;
        while(true) {
            Paxos.retStatus ret = this.px.Status(seq);
            if(ret.state == State.Decided){
                return Op.class.cast(ret.v);
            }
            try {
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

    public void Update(){
        Set<Integer> decidedSeq = new HashSet<Integer>();
        for(int seqNum : px.states.keySet()){
            if(px.states.get(seqNum) == State.Decided)
                decidedSeq.add(seqNum);
        }
        for(int seqNum : decidedSeq) {
            Op retOp = (Op) px.decidedValues.get(seqNum);
            if (retOp.op.equals("Put"))
                logData.put(retOp.key, retOp.value);
            px.decidedValues.remove(seqNum);
            px.proposer_n.remove(seqNum);
            px.accept_v.remove(seqNum);
            px.accept_n.remove(seqNum);
            px.n.remove(seqNum);
            px.highestDone.remove(seqNum);
            px.states.put(seqNum, State.Forgotten);
            px.Done(seqNum);
        }
    }
}
