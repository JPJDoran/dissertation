/**
  * Parking bay agent that represents a single parking bay in the carpark.
  * Communicates with car agents to negotiate parking.
  * Has a random type and a set distance from the entrance.
  * 
  * Amended from JADE tutorial examples:
  *	Accessed 17/08/2020
  *	https://jade.tilab.com/doc/tutorials/JADEProgramming-Tutorial-for-beginners.pdf
  *	https://github.com/jason-lang/jason/tree/master/doc/tutorials/jason-jade/jade-example/examples/bookTrading
  *
**/

import jade.core.Agent;

import java.util.List;
import java.util.Random;

import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.*;
import jade.wrapper.*;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.*;

public class ParkingBayAgent extends Agent {
	// Instance Variables
	int distanceFromEntrance; // how far the space is from the entrance in metres
	String type;
	String occupiedBy; // which vehicle is occupying the space
	JavaFXGui gui;
	int x; // The x coordinate of the parking bay
	int y; // The y coordinate of the parking bay
	
	// Agent creation
	protected void setup() {
		// Get the variables passed to the agent on creation
		Object[] parameters = getArguments();
		
		// Setup parking bay agent
		int distance =(int) parameters[0];
		Analytics data = (Analytics) parameters[1];
		gui = (JavaFXGui) parameters[2];
		distanceFromEntrance = distance;
		type = isDisabledSpace(); // 5% chance of being a disabled space - source: http://evacuation-chair.co.uk/disabled-parking-spaces-legal-issues-obligations/
		
		// If the type is normal after disabled chance, check electric chance
		if (type.equals("N")) {
			type = evCharger(); // 20% chance of being an EV space - source: https://www.addleshawgoddard.com/en/insights/insights-briefings/2019/real-estate/electric-vehicle-charging-points/#:~:text=A%20respective%20building%20with%2020,least%20one%20EV%20charge%20point.
		}
		
		occupiedBy = null; // the parking bay always starts unoccupied 
		
		// Register the parking bay in the yellow pages
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        
        ServiceDescription sd = new ServiceDescription();
        sd.setType("parkingBay");
        sd.setName("MAS-Carpark");
        
        dfd.addServices(sd);
        
        // Attempt to register the parking bay
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        
        // Set the relevant space type in the display
        addBehaviour( new OneShotBehaviour(this) {
			public void action() {
				// Agent Name is in format - ParkingBay-Y:X@...
				String name[] = myAgent.getName().split("@");
				name = name[0].split("-");
				name = name[1].split(":");
				
				// Set the coordinates of the parking bay
				y = Integer.parseInt(name[0]) - 1;
				x = Integer.parseInt(name[1]) - 1;
		  	  
				// Update the given UI element that matches the coordinates of the parking bay
				gui.setSpaceType(type, y, x);
			}
        });
        
        // Behaviour to respond to queries asking for parking type
        addBehaviour(new ParkingType());
        
        // Behaviour to respond to queries from parking
        addBehaviour(new ParkingAvailability());

        // Behaviour to park a car
        addBehaviour(new ParkCar(data));
        
        // Behaviour to free up a parking space
        addBehaviour(new MakeAvailable());
        
	}
	
	// generate whether or not the space is disabled parking
	private String isDisabledSpace() {
		// Create new random
		Random rand = new Random();
		
		boolean disabledSpace = false;
		
		// 5% chance of being a disabled space
		if (rand.nextDouble() <= 0.05) {
			disabledSpace = true;
		}
		
		return disabledSpace ? "D" : "N";
	}
	
	// generate whether or not the space is for an ev
	private String evCharger() {
		Random rand = new Random();
		
		boolean charger = false;
		
		// 20% chance of being an ev space
		if (rand.nextDouble() <= 0.2) {
			charger = true;
		}
		
		return charger? "E" : "N";
	}
	
	// Agent termination
    protected void takeDown() {
        
    }
    
    /**
     * 
     * Cyclic Behaviour to continuously respond to request messages.
     * Behaviour responds with the type of the parking bay.
     *
     */
    private class ParkingType extends CyclicBehaviour {
    	public void action() {
    		// Get the message template that matches the performative REQUEST
    		MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
            
    		// Strip out the message
    		ACLMessage msg = myAgent.receive(mt);
            
    		// If the message is set
            if (msg != null) {
            	 // CFP Message received. Process it
                ACLMessage reply = msg.createReply();
                
            	// Reply with the type of the parking bay
                reply.setPerformative(ACLMessage.INFORM);
                reply.setContent(type);
                myAgent.send(reply);
            }
    	}
    }
    
    /**
     * 
     * Cyclic Behaviour to continuously respond to call for proposal messages.
     * Behaviour responds with the availability of the parking bay.
     *
     */
    private class ParkingAvailability extends CyclicBehaviour {
        public void action() {
        	// Get the message template that matches the performative CFP
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            
            // Strip out the message
            ACLMessage msg = myAgent.receive(mt);
            
            // If the message is set
            if (msg != null) {
            	// Try to get the content object
            	try {
                    Object content = msg.getContentObject();
                    
                    // Get the car specification from the content
                    CarSpecification car = (CarSpecification) content;
                    
                    // Get the car type
                    String carType = car.getType();
                    
                    // CFP Message received. Process it
                    ACLMessage reply = msg.createReply();
                    
                    // The parking bay is available.
                    if (occupiedBy == null) {
	                    // Reply with the distance from entrance
	                    reply.setPerformative(ACLMessage.PROPOSE);
	                    reply.setContent(String.valueOf(distanceFromEntrance));
                    } else {
                        // The parking bay is occupied.
                        reply.setPerformative(ACLMessage.REFUSE);
                        reply.setContent("not-available");
                    }
                    
                    myAgent.send(reply);
            	} catch (Exception ex) { 
            		ex.printStackTrace(); 
            	}
            } else {
                block();
            }
        }
    }
    
    /**
     * 
     * Cyclic Behaviour to continuously respond to accept proposal messages.
     * Behaviour responds with the acceptance of the proposal.
     *
     */
    private class ParkCar extends CyclicBehaviour {
    	// Reference to the analytics instance to be able to update the data
    	Analytics data;
    	
    	// Constructor
        public ParkCar(Analytics data) {
			this.data = data;
		}
        
        public void action() {
        	// Get the message template that matches the performative ACCEPT_PROPOSAL
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
            
            // Strip out the message
            ACLMessage msg = myAgent.receive(mt);
            
            // If message is set
            if (msg != null) {
                // ACCEPT_PROPOSAL Message received. Process it
                ACLMessage reply = msg.createReply();
                
                // Try to get the content object
                try {
                    Object content = msg.getContentObject();
                    CarSpecification car = (CarSpecification) content;
                    
                    // If the parking bay is still available
	                if (occupiedBy == null) {
	                	// Inform the car the parking bay is still available
	                    reply.setPerformative(ACLMessage.INFORM);
	                    
	                    // Set car as occupying the space
	                    occupiedBy = car.getNumberPlate();
	                    
	                    // Update GUI
	                    gui.updateSpace(type, y, x, false);
	                    
	                    // Increase cars parked count
	                    data.increaseCarsParked();
	                    
	                    // If car is not an electric car, then calculate the cost of parking
	                    if (!car.getType().equals("E")) {
	                    	data.calculateParkingCost(distanceFromEntrance, car);
	                    }
	                } else {
	                    // The parking bay is no longer available.
	                    reply.setPerformative(ACLMessage.FAILURE);
	                    reply.setContent("not-available");
	                }
	                
	                myAgent.send(reply);
                } catch (Exception ex) { 
            		ex.printStackTrace(); 
            	}
            } else {
                block();
            }
        }
    }
    
    /**
     * 
     * Cyclic Behaviour to continuously respond to inform messages.
     * Behaviour responds with the success of freeing up the parking bay.
     *
     */
    private class MakeAvailable extends CyclicBehaviour {
		public void action() {
			// Get the message template that matches the performative INFORM
        	MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
        	
        	// Strip out the message
            ACLMessage msg = myAgent.receive(mt);
            
            // If message is set
            if (msg != null) {
            	ACLMessage reply = msg.createReply();
            	 
            	// If parking bay is occupied
            	if (occupiedBy != null) {
            		// Inform the agent that the parking bay is still occupied
	                reply.setPerformative(ACLMessage.INFORM);
	                reply.setContent("occupied");
	                
	                // Free up the parking bay
	                occupiedBy = null;
	                
	                // Update the user interface to reflect changes in availability
	                gui.updateSpace(type, y, x, true);
            	} else {
            		// The parking bay is already available.
                    reply.setPerformative(ACLMessage.FAILURE);
                    reply.setContent("available");
            	}
            	 
            	myAgent.send(reply);
            } else {
                block();
            }
       }
   }
}
