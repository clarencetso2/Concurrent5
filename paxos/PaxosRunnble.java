package paxos;

import java.rmi.RemoteException;

import paxos.Paxos.retStatus;

import static paxos.State.Decided;
import static paxos.State.Pending;

public class PaxosRunnble implements Runnable{
    int seq;
    int me;
    Object value;
    Paxos paxos;
    int max_na =-1;
    
    public PaxosRunnble(Paxos p, int s, Object v, int m){
        this.seq = s;
        this.value = v;
        this.paxos = p;
        this.me = m;
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
        paxos.proposer_n.put(localSeq, -1);

        int count1 = 0;
        int count2 = 0;
        
        int received_n = -1;
        Object received_v = null;

        //Need to keep track of the state
        while(paxos.states.get(localSeq)!=Decided) {
                paxos.lamportClock = paxos.proposer_n.get(seq);
                paxos.lamportClock++;
                paxos.n.put(localSeq, paxos.lamportClock);
        //      paxos.proposing.put(localSeq, true);



            // recieved n , received value, update it
            
            ////////////////////////////////Phase 1///////////////////////////////////////////////////////
            for (int i = 0; i < paxos.peers.length; i++) {
            	Response prepResponse;
            	if(i == me){
            		 prepResponse = paxos.Prepare( new Request(localSeq, localVal, paxos.n.get(localSeq), me, paxos.highestDone.get(me)));
            	}
            	else{
            		 prepResponse = paxos.Call("Prepare", new Request(localSeq, localVal, paxos.n.get(localSeq),me,paxos.highestDone.get(me)), i);
            	}
            	
            	if (prepResponse != null && prepResponse.acceptNum >= received_n) {
                      received_v = prepResponse.value;
                      received_n = prepResponse.acceptNum;
                  }

                if (prepResponse != null && prepResponse.ack) {
                    count1++;
                }
            }
                    
                
            if (count1 >= (paxos.peers.length / 2) + 1) {
            	if (received_n != -1 && received_v != null) {
            		localVal = received_v;
                    max_na = received_n;
                }
                                
                for(int j = 0; j < paxos.peers.length; j++) {
                	Response accResponse; 
                   	if(j == me){
                   		accResponse = paxos.Accept(new Request(localSeq, localVal, paxos.n.get(localSeq),me, paxos.highestDone.get(me)));
                        		
                    }
                    else{
                        accResponse = paxos.Call("Accept", new Request(localSeq, localVal, paxos.n.get(localSeq), me,paxos.highestDone.get(me)), j);

                    }
                    if (accResponse != null && accResponse.ack) {
                        count2++;
                    }
                }

                if (count2 >= (paxos.peers.length / 2) + 1) {
                    //printState(localSeq, paxos.states.get(localSeq));
                    for(int k = 0; k < paxos.peers.length; k++){
                    	Response decideResponse;
                    	if(k==me){
                    		decideResponse = paxos.Decide(new Request(localSeq, localVal, paxos.n.get(localSeq), me, paxos.highestDone.get(me)));
                    	}
                    	else{
                    		decideResponse = paxos.Call("Decide", new Request(localSeq, localVal, paxos.n.get(localSeq), me,paxos.highestDone.get(me)), k);
                    	}
                    }

                }
            }
        }
    }


                        
                        
                        
                        
                        
                            
        
       //call Done(localSeq)


      //retStatus status = paxos.Status(localSeq);

    

    public void printState(int s, State state){
        if(state == Decided){
            System.out.println(s + ": Decided");
        }
        if(state == Pending){
            System.out.println(s + ": Pending");
        }
    }

}
