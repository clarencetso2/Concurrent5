package paxos;

import java.rmi.RemoteException;

import static paxos.State.Decided;
import static paxos.State.Pending;

public class PaxosRunnble implements Runnable{
    int seq;
    Object value;
    Paxos paxos;

    public PaxosRunnble(Paxos p, int s, Object v){
        this.seq = s;
        this.value = v;
        this.paxos = p;
    }



    @Override
    public void run() {
        //Your code here
        int localSeq = seq;
        Object localVal = value;

        paxos.n.put(localSeq, -1);
        paxos.states.put(localSeq, Pending);
        paxos.accept_n.put(localSeq, -1);
        paxos.accept_v.put(localSeq, -1);

        int count1 = 0;
        int count2 = 0;

        //Need to keep track of the state
        while(paxos.states.get(localSeq)!=Decided) {
            synchronized (this) {
                paxos.lamportClock = paxos.n.get(seq);
                paxos.lamportClock++;
                paxos.n.put(localSeq, paxos.lamportClock);
            }

            for (int i = 0; i < paxos.peers.length; i++) {
                Response prepResponse = paxos.Call("Prepare", new Request(localSeq, localVal, paxos.n.get(localSeq)), i);
                if (prepResponse != null && prepResponse.ack) {
                    count1++;
                    if (count1 > (paxos.peers.length / 2) + 1) {
                        if (prepResponse.acceptNum > paxos.n.get(localSeq)) {
                            paxos.n.put(localSeq, prepResponse.acceptNum);
                            localVal = prepResponse.value;

                        }
                        for(int j = 0; j < paxos.peers.length; j++) {
                            Response accResponse = paxos.Call("Accept", new Request(localSeq, localVal, paxos.n.get(localSeq)), i);
                            if (accResponse.ack) {
                                count2++;

                                if (count2 > (paxos.peers.length / 2) + 1) {
                                    printState(localSeq, paxos.states.get(localSeq));
                                    for(int k = 0; k < paxos.peers.length; k++){
                                        Response decideResponse = paxos.Call("Decide", new Request(localSeq, localVal, paxos.n.get(localSeq)), k);
                                    }
                                    i = j = paxos.peers.length;
                                }
                            }
                        }
                    }
                }
            }
        }
        //call Done(localSeq)


        //retStatus status = Status(localSeq);

    }

    public void printState(int s, State state){
        if(state == Decided){
            System.out.println(s + ": Decided");
        }
        if(state == Pending){
            System.out.println(s + ": Pending");
        }
    }

}
