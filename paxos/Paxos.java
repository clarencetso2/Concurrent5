package paxos;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.Registry;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import static paxos.State.Decided;
import static paxos.State.Pending;

/**
 * This class is the main class you need to implement paxos instances.
 */
public class Paxos implements PaxosRMI, Runnable{

    ReentrantLock mutex = new ReentrantLock();
    Condition condition = mutex.newCondition();
    String[] peers; // hostname
    int[] ports; // host port
    int me; // index into peers[]

    Registry registry;
    PaxosRMI stub;

    AtomicBoolean dead;// for testing
    AtomicBoolean unreliable;// for testing

    ReentrantLock lamportLock;
    int lamportClock = 0;

    // Your data here
    int seq;
    Object value;

    int highestPrepare = -1;
    int highestAccept = -1;

    int max;
    int min;

    Object highestValueAcceptor = null;
    Object highestValueProposer = null;

    Map<Integer, Object> decidedValues;
    Map<Integer, Integer> proposer_n;
    Map<Integer, State> states;
    Map<Integer, Integer> accept_n;
    Map<Integer, Object> accept_v;
    Map<Integer, Integer> n;
    Map<Integer, Boolean> proposing;
    Map<Integer, Boolean> broadcasted;


    /**
     * Call the constructor to create a Paxos peer.
     * The hostnames of all the Paxos peers (including this one)
     * are in peers[]. The ports are in ports[].
     */
    public Paxos(int me, String[] peers, int[] ports){

        this.me = me;
        this.peers = peers;
        this.ports = ports;
        this.mutex = new ReentrantLock();
        this.dead = new AtomicBoolean(false);
        this.unreliable = new AtomicBoolean(false);

        // Your initialization code here
        this.lamportClock = 0;

        this.decidedValues = new ConcurrentHashMap<Integer,Object>();
        this.proposer_n = new ConcurrentHashMap<Integer, Integer>();
        this.states = new ConcurrentHashMap<Integer, State>();
        this.accept_n = new ConcurrentHashMap<Integer, Integer>();
        this.accept_v = new ConcurrentHashMap<Integer, Object>();
        this.n = new ConcurrentHashMap<Integer, Integer>();
        this.proposing = new ConcurrentHashMap<Integer, Boolean>();
        this.broadcasted = new ConcurrentHashMap<Integer, Boolean>();



        this.max = -1;
        this.min = -1;

        // register peers, do not modify this part
        try{
            System.setProperty("java.rmi.server.hostname", this.peers[this.me]);
            registry = LocateRegistry.createRegistry(this.ports[this.me]);
            stub = (PaxosRMI) UnicastRemoteObject.exportObject(this, this.ports[this.me]);
            registry.rebind("Paxos", stub);
        } catch(Exception e){
            e.printStackTrace();
        }
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
        //System.out.println("id: " + id);
        Response callReply = null;
        PaxosRMI stub;
        try{
            Registry registry=LocateRegistry.getRegistry(this.ports[id]);
            stub=(PaxosRMI) registry.lookup("Paxos");
            if(rmi.equals("Prepare")) {
                //System.out.println("prepare " + id);
                callReply = stub.Prepare(req);
            }
            else if(rmi.equals("Accept")){
                //System.out.println("accept " + id);
                callReply = stub.Accept(req);

            }
            else if(rmi.equals("Decide")) {
                //System.out.println("decide " + id);

                callReply = stub.Decide(req);
            }
            else
                System.out.println("Wrong parameters!");
        } catch(Exception e){
            //System.out.println("EXCEPTION: " + e);
            return null;
        }
        return callReply;
    }


    /**
     * The application wants Paxos to start agreement on instance seq,
     * with proposed value v. Start() should start a new thread to run
     * Paxos on instance seq. Multiple instances can be run concurrently.
     *
     * Hint: You may start a thread using the runnable interface of
     * Paxos object. One Paxos object may have multiple instances, each
     * instance corresponds to one proposed value/command. Java does not
     * support passing arguments to a thread, so you may reset seq and v
     * in Paxos object before starting a new thread. There is one issue
     * that variable may change before the new thread actually reads it.
     * Test won't fail in this case.
     *
     * Start() just starts a new thread to initialize the agreement.
     * The application will call Status() to find out if/when agreement
     * is reached.
     */
    public void Start(int seq, Object value){

        // Your code here

        this.seq = seq;
        this.value = value;
        PaxosRunnble paxosRunnble = new PaxosRunnble(this, seq, value, me);
        Thread t = new Thread(paxosRunnble);
        t.start();
    }

    @Override
    public void run(){

    }

    // RMI handler
    public Response Prepare(Request req){
        //might need to break ties with pid
//    	if(proposing.get(req.seq) == null && this.me != req.me && this.me != 0){
//    		proposing.put(req.seq, true);
//    		Start(req.seq, req.value);
//    	}

        if(this.Status(req.seq).state == Decided){
            return new Response(true, req.propNum, accept_n.get(req.seq), accept_v.get(req.seq), true);
        }
    	
        if(accept_n.get(req.seq) == null)
            accept_n.put(req.seq, -1);
        if(proposer_n.get(req.seq) == null){
            proposer_n.put(req.seq, req.propNum);
//            System.out.println("propNum: " + req.propNum + " acc_n: " + accept_n.get(req.seq) + " acc_v: " + accept_v.get(req.seq));
            Response response =  new Response(true, req.propNum, accept_n.get(req.seq), accept_v.get(req.seq),false);
            return response;
        }
        if(req.propNum > proposer_n.get(req.seq)) {
            proposer_n.put(req.seq, req.propNum);
            Response response = new Response(true, req.propNum, accept_n.get(req.seq), accept_v.get(req.seq),false);

            return response;
        }

        else{

            return new Response(false, req.propNum, accept_n.get(req.seq), accept_v.get(req.seq),false);

        }
    }

    public Response Accept(Request req){
        // your code here
//    	if(proposing.get(req.seq) == null){
//    		proposing.put(req.seq, true);
//    		Start(req.seq, req.value);
//    	}
        if(req.propNum >= proposer_n.get(req.seq)) {
            accept_n.put(req.seq, req.propNum);
            proposer_n.put(req.seq, req.propNum);
            accept_v.put(req.seq, req.value);
            Response response = new Response(true, req.propNum, accept_n.get(req.seq), accept_v.get(req.seq),false);
            return response;
        }

        else{
            return new Response(false, req.propNum, accept_n.get(req.seq), accept_v.get(req.seq), false);
        }
    }

    public Response Decide(Request req){
    	
        //System.out.println("Decide call: " + req.seq);
        
        states.put(req.seq, Decided);
        decidedValues.put(req.seq, req.value);
        return null;
    }

    /**
     * The application on this machine is done with
     * all instances <= seq.
     *
     * see the comments for Min() for more explanation.
     */
    public void Done(int seq) {
        // Your code here

    }


    /**
     * The application wants to know the
     * highest instance sequence known to
     * this peer.
     */
    public int Max(){
        // Your code here
        return 0;
    }

    /**
     * Min() should return one more than the minimum among z_i,
     * where z_i is the highest number ever passed
     * to Done() on peer i. A peers z_i is -1 if it has
     * never called Done().

     * Paxos is required to have forgotten all information
     * about any instances it knows that are < Min().
     * The point is to free up memory in long-running
     * Paxos-based servers.

     * Paxos peers need to exchange their highest Done()
     * arguments in order to implement Min(). These
     * exchanges can be piggybacked on ordinary Paxos
     * agreement protocol messages, so it is OK if one
     * peers Min does not reflect another Peers Done()
     * until after the next instance is agreed to.

     * The fact that Min() is defined as a minimum over
     * all Paxos peers means that Min() cannot increase until
     * all peers have been heard from. So if a peer is dead
     * or unreachable, other peers Min()s will not increase
     * even if all reachable peers call Done. The reason for
     * this is that when the unreachable peer comes back to
     * life, it will need to catch up on instances that it
     * missed -- the other peers therefore cannot forget these
     * instances.
     */
    public int Min(){
        // Your code here

        return 0;
    }



    /**
     * the application wants to know whether this
     * peer thinks an instance has been decided,
     * and if so what the agreed value is. Status()
     * should just inspect the local peer state;
     * it should not contact other Paxos peers.
     */
    public retStatus Status(int seq){
        // Your code here
        return new retStatus(states.get(seq), decidedValues.get(seq));
    }

    /**
     * helper class for Status() return
     */
    public class retStatus{
        public State state;
        public Object v;

        public retStatus(State state, Object v){
            this.state = state;
            this.v = v;
        }
    }

    /**
     * Tell the peer to shut itself down.
     * For testing.
     * Please don't change these four functions.
     */
    public void Kill(){
        this.dead.getAndSet(true);
        if(this.registry != null){
            try {
                UnicastRemoteObject.unexportObject(this.registry, true);
            } catch(Exception e){
                System.out.println("None reference");
            }
        }
    }

    public boolean isDead(){
        return this.dead.get();
    }

    public void setUnreliable(){
        this.unreliable.getAndSet(true);
    }

    public boolean isunreliable(){
        return this.unreliable.get();
    }


}
