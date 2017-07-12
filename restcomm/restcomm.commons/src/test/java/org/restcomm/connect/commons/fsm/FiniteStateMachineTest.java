package org.restcomm.connect.commons.fsm;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import akka.actor.ActorRef;

public class FiniteStateMachineTest {

    @Test
    public void testFiniteStateMachine() {
        

        State uninitialized = new State("uninitialized", null, null);
        State idle = new State("idle", null, null);
        State active = new State("active", null, null);
        

        //try invalid state transition
        //uninit -> idle
        //idle -> uninit
        //idle -> active
        //active -> idle
        //invalid : uninit -> active
        //invalid : active -> uninit
        //?uninit -> ununit?
        //?idle -> idle?
        //?active -> active?
        final Set<Transition> transitions = new HashSet<Transition>();
        transitions.add(new Transition(uninitialized, idle));
        transitions.add(new Transition(idle, uninitialized));
        transitions.add(new Transition(idle, active));
        transitions.add(new Transition(active, idle));
        FiniteStateMachine fsm = new FiniteStateMachine(uninitialized, transitions);
        
        //what event??
        //current state 
        String event = new String("event1");
        //our current state=unint, -> idle, valid
        try{
            fsm.transition("event1", idle);
            fsm.transition("event2", uninitialized);

            fsm.transition("event3", idle);
            fsm.transition("event4", active);
            fsm.transition("event5", idle);
            fsm.transition("event6", uninitialized);
            fsm.transition("event7", idle);
            //??
            fsm.transition("event8", idle);
            //invalid
            fsm.transition("event9", active);
        }catch(Exception e){
            e.printStackTrace();
        }

    }
    private class AbstractAction implements Action {
        protected final ActorRef source;

        public AbstractAction(final ActorRef source) {
            super();
            this.source = source;
        }
        @Override
        public void execute(Object message) throws Exception {
            System.out.println(message.toString());

        }
    }

    @Test
    public void testState() {
        fail("Not yet implemented"); // TODO
    }

    @Test
    public void testTransition() {
        fail("Not yet implemented"); // TODO
    }

}
