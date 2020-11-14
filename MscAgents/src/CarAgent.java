/**
  * Car agent that represents a car and driver in the simulation.
  * Communicates with parking bay agents in order to negotiate parking.
  * Has a random type and car specification based on representative real world data.
  * 
  * Amended from JADE tutorial examples:
  *	Accessed 17/08/2020
  *	https://jade.tilab.com/doc/tutorials/JADEProgramming-Tutorial-for-beginners.pdf
  *	https://github.com/jason-lang/jason/tree/master/doc/tutorials/jason-jade/jade-example/examples/bookTrading
  *
**/

import jade.core.Agent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.*;
import jade.wrapper.*;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.*;

public class CarAgent extends Agent {
	
	// Instance Variables
	String numberPlate; // identifier of the vehicle
	int duration; // duration the car is parked for in min
	String makeModel = ""; // the make and model of the car
	String type = "N"; // type of car
	double fuelEconomy; // miles per gallon of the car
	double carbonDioxideEmissions; // the co2 emissions produced grams per km (g/km)
	private AID[] parkingBayAgents; // The list of known ParkingBay agents
	private ArrayList<AID> normalBays;
	private ArrayList<AID> disabledBays;
	private ArrayList<AID> electricBays;
	private AID currentSpace;
	boolean parked = false;
	private Analytics data;
	private ArrayList<String> queue;
	private int durationTick = 1000;
	private int modelSpeed;
	private String name;
	private String algorithm;
	
	// Agent creation
	protected void setup() {
		// Get all variables passed through on creation
		Object[] parameters = getArguments();
		
		// Setup the car agent
		numberPlate = parameters[0].toString();
		data = (Analytics) parameters[1];
		normalBays = (ArrayList<AID>) parameters[2];
		disabledBays = (ArrayList<AID>) parameters[3];
		electricBays = (ArrayList<AID>) parameters[4];
		duration = newDuration(Integer.parseInt(parameters[5].toString()), Integer.parseInt(parameters[6].toString()));
		type = isDisabled(Double.parseDouble(parameters[8].toString()));
		
		// If type is still normal after disability chance, check electric chance
		if (type == "N") {
			type = isEv(Double.parseDouble(parameters[7].toString())); 
		}
			
		makeModel = newMakeModel(this.type);
		fuelEconomy = generateFuelEconomy(this.makeModel); 
		carbonDioxideEmissions = generateCarbonDioxideEmissions(this.makeModel);
		queue = (ArrayList<String>) parameters[9];
		modelSpeed = (int) parameters[10];
		algorithm = (String) parameters[11];
		
		// Update the total number of cars created
		data.updateCarsCreated();
		
		// On creation, create directory of parking bay agents and attempt to park
		addBehaviour(new OneShotBehaviour(this) {
            public void action() {
            	name = myAgent.getName();
            	
            	// Update array of parkingBayAgents
                DFAgentDescription template = new DFAgentDescription();
                ServiceDescription sd = new ServiceDescription();
                
                sd.setType("parkingBay");
                
                template.addServices(sd);
                
                // Try to add parking bay agents to array
                try {
                    DFAgentDescription[] result = DFService.search(myAgent, template);
                    
                    parkingBayAgents = new AID[result.length];
                    
                    for (int i = 0; i < result.length; ++i) {
                    	parkingBayAgents[i] = result[i].getName();
                    }
                } catch (FIPAException fe) {
                    fe.printStackTrace();
                }
            }
        });
		
		// Every set interval, if the car isn't parked and it's at the front of the queue attempt to park
		addBehaviour(new TickerBehaviour(this, modelSpeed) {
			protected void onTick() {
				if (!parked) {
					if (queue.size() > 0) {
						if (name.equals(queue.get(0))) {
			                // Attempt to park
			                myAgent.addBehaviour(new AttemptToPark());
						} else {
							// If not at the front of the queue, update average time in queue
							data.updateQueueDuration(modelSpeed);
						}
					}
				}
			}
		});
		
		// On creation, tick through duration if parked and kill agent if duration = 0
		addBehaviour(new TickerBehaviour(this, durationTick) {
			protected void onTick() {
				// Agent is parked so reduce duration
				if (parked) {
					// Duration reduced by 10 each tick
					duration = duration - 10;
				}
				
				// Duration is up so kill agent
				if (duration == 0) {
					// Tell the agent to leave the carpark
					myAgent.addBehaviour(new LeaveCarpark());
				}
			}
		});
	}
	
	/**
	 * 
	 * Generate new duration between given min and max duration
	 * 
	 * @param int min [The minimum duration the car can be parked for]
	 * @param int max [The maximum duration the car can be parked for]
	 * @return int duration [The duration the car is looking to park for]
	 */
	protected int newDuration(int min, int max) {
		Random rand = new Random();
		
		// Divide by 10 to ensure the number will be a multiple of 10
		min = min / 10;
		max = max / 10;
		
		// Duration is a new random number between min and max multiple by 10
		int duration = (rand.nextInt(max)+min)*10;
		
		return duration;
	}
	
	/**
	 * 
	 * Generate whether or not the agent requires disabled parking
	 * 
	 * @param double percent [The likelihood the driver requires disabled parking]
	 * @return String disabled [The character representation of the vehicle type i.e. D or N]
	 */
	protected String isDisabled(Double percent) {
		// Create new random
		Random rand = new Random();
		
		boolean disabled = false;
		
		// Divide by 100 to get a number between 0 and 1
		percent = percent / 100;
		
		// If the next random number between 0 and 1 is less than the percent likelihood of being disabled, the driver requires disabled parking
		if (rand.nextDouble() <= percent) {
			disabled = true;
		}
		
		return disabled ? "D" : "N";
	}
	
	/**
	 * 
	 * Generate whether or not the agent is an electric vehicle
	 * 
	 * @param double percent [The likelihood the driver requires electric parking]
	 * @return String ev [The character representation of the vehicle type i.e. E or N]
	 */
	protected String isEv(Double percent) {
		// Create new random
		Random rand = new Random();
		
		boolean ev = false;
		
		// Divide by 100 to get a number between 0 and 1
		percent = percent / 100;
		
		// If the next random number between 0 and 1 is less than the percent likelihood of being electric, the driver requires electric parking
		if (rand.nextDouble() <= percent) {
			ev = true;
		}
		
		return ev ? "E" : "N";
	}
	
	/**
	 * 
	 * Generate the make and model of the agent 
	 * Likelihoods adapted from data by Statista: https://www.statista.com/statistics/299018/car-models-which-sold-the-most-in-the-united-kingdom/
	 * 
	 * @param String type [The vehicle type]
	 * @return String [The make and model of the vehicle]
	 */
	protected String newMakeModel(String type) {
		// If the vehicle is electric, return early
		if (type == "E") {
			return "EV";
		}
		
		// Create new random
		Random rand = new Random();
		
		// Get new random number between 0 and 1
		double percent = rand.nextDouble();
		
		// 15.3% chance of being a Ford Fiesta
		if (percent <= 0.153) {
			return "Ford Fiesta";
		}
		
		// 11.6% chance of being a Volkswagen Golf
		if (percent <= 0.269) {
			return "Volkswagen Golf";
		}
		
		// 11.13% chance of being a Ford Focus
		if (percent <= 0.3803) {
			return "Ford Focus";
		}
		
		// 10.66% chance of being a Vauxhall Corsa
		if (percent <= 0.4869) {
			return "Vauxhall Corsa";
		}
		
		// 10.56% chance of being a Mercedes A-Class
		if (percent <= 0.5925) {
			return "Mercedes A-Class";
		}
		
		// 10.33% chance of being a Nissan Qashqai
		if (percent <= 0.6958) {
			return "Nissan Qashqai";
		}
		
		// 8.19% chance of being a Ford Kuga
		if (percent <= 0.7777) {
			return "Ford Kuga";
		}
		
		// 8.1% chance of being a Mini
		if (percent <= 0.8587) {
			return "MINI";
		}
		
		// 7.36% chance of being a Volkswagen Polo
		if (percent <= 0.9323) {
			return "Volkswagen Polo";
		}
		
		// ~6.78% chance of being a Kia Sportage
		if (percent <= 1.0) {
			return "Kia Sportage";
		}
		
		return "EV";
	}
 
	/**
	 * 
	 * Generate random fuel economy using real life mpg statistics provided by Fleet News - source: https://www.fleetnews.co.uk/cars/Car-CO2-and-fuel-economy-mpg-figures
	 * Random double between doubles adapted from: https://stackoverflow.com/questions/28786856/java-get-random-double-between-two-doubles/28786888
	 * Accessed: 22/07/2020
	 * 
	 * @param String makeModel [The make and model of the vehicle]
	 * @return int mpg [The miles per gallon of the vehicle]
	 */
	protected double generateFuelEconomy(String makeModel) {
		// Create new random
		Random rand = new Random();
		
		// Create variables
		double mpg = 0.0;
		double minMpg = 0.0;
		double maxMpg = 0.0;
		
		// Switch between the possible make and models
		switch(makeModel) {
			case "Ford Fiesta":
				minMpg = 40.4;
				maxMpg = 65.7;
				mpg = minMpg + rand.nextDouble() * (maxMpg - minMpg);
				break;
			case "Volkswagen Golf":
				minMpg = 32.8;
				maxMpg = 68.9;
				mpg = minMpg + rand.nextDouble() * (maxMpg - minMpg);
				break;
			case "Ford Focus":
				minMpg = 34.4;
				maxMpg = 62.8;
				mpg = minMpg + rand.nextDouble() * (maxMpg - minMpg);
				break;
			case "Vauxhall Corsa":
				minMpg = 48.7;
				maxMpg = 70.6;
				mpg = minMpg + rand.nextDouble() * (maxMpg - minMpg);
				break;
			case "Mercedes A-Class":
				minMpg = 31.4;
				maxMpg = 62.8;
				mpg = minMpg + rand.nextDouble() * (maxMpg - minMpg);
				break;
			case "Nissan Qashqai":
				minMpg = 39.8;
				maxMpg = 53.3;
				mpg = minMpg + rand.nextDouble() * (maxMpg - minMpg);
				break;
			case "Ford Kuga":
				minMpg = 41.5;
				maxMpg = 56.5;
				mpg = minMpg + rand.nextDouble() * (maxMpg - minMpg);
				break;
			case "MINI":
				minMpg = 34.0;
				maxMpg = 48.7;
				mpg = minMpg + rand.nextDouble() * (maxMpg - minMpg);
				break;
			case "Volkswagen Polo":
				minMpg = 39.8;
				maxMpg = 57.6;
				mpg = minMpg + rand.nextDouble() * (maxMpg - minMpg);
				break;
			case "Kia Sportage":
				minMpg = 32.1;
				maxMpg = 53.3;
				mpg = minMpg + rand.nextDouble() * (maxMpg - minMpg);
				break;
			default:
				// hit if vehicle is electric at which point mpg is 0
				break;
		}
		
		// Return rounded number for ease
		return (int) Math.round(mpg);
	}
	
	/**
	 * 
	 * Generate random CO2 emissions using real life CO2 statistics provided by Fleet News - source: https://www.fleetnews.co.uk/cars/Car-CO2-and-fuel-economy-mpg-figures
	 * Random double between doubles adapted from: https://stackoverflow.com/questions/28786856/java-get-random-double-between-two-doubles/28786888
	 * Accessed: 22/07/2020
	 * 
	 * @param String makeModel [The make and model of the vehicle]
	 * @return int CO2 [The carbon emissions the vehicle produces per kilometre]
	 */
	protected double generateCarbonDioxideEmissions(String makeModel) {
		// Create new random
		Random rand = new Random();

		// Create variables
		double CO2 = 0.0;
		double minCO2 = 0.0;
		double maxCO2 = 0.0;
		
		// Switch between the make and model
		switch(makeModel) {
			case "Ford Fiesta":
				minCO2 = 112.0;
				maxCO2 = 158.0;
				CO2 = minCO2 + rand.nextDouble() * (maxCO2 - minCO2);
				break;
			case "Volkswagen Golf":
				minCO2 = 107.0;
				maxCO2 = 195.0;
				CO2 = minCO2 + rand.nextDouble() * (maxCO2 - minCO2);
				break;
			case "Ford Focus":
				minCO2 = 117.0;
				maxCO2 = 188.0;
				CO2 = minCO2 + rand.nextDouble() * (maxCO2 - minCO2);
				break;
			case "Vauxhall Corsa":
				minCO2 = 108.0;
				maxCO2 = 134.0;
				CO2 = minCO2 + rand.nextDouble() * (maxCO2 - minCO2);
				break;
			case "Mercedes A-Class":
				minCO2 = 117.0;
				maxCO2 = 207.0;
				CO2 = minCO2 + rand.nextDouble() * (maxCO2 - minCO2);
				break;
			case "Nissan Qashqai":
				minCO2 = 131.0;
				maxCO2 = 182.0;
				CO2 = minCO2 + rand.nextDouble() * (maxCO2 - minCO2);
				break;
			case "Ford Kuga":
				minCO2 = 132.0;
				maxCO2 = 161.0;
				CO2 = minCO2 + rand.nextDouble() * (maxCO2 - minCO2);
				break;
			case "MINI":
				minCO2 = 131.0;
				maxCO2 = 189.0;
				CO2 = minCO2 + rand.nextDouble() * (maxCO2 - minCO2);
				break;
			case "Volkswagen Polo":
				minCO2 = 124.0;
				maxCO2 = 160.0;
				CO2 = minCO2 + rand.nextDouble() * (maxCO2 - minCO2);
				break;
			case "Kia Sportage":
				minCO2 = 138.0;
				maxCO2 = 201.0;
				CO2 = minCO2 + rand.nextDouble() * (maxCO2 - minCO2);
				break;
			default:
				// hit if vehicle is electric at which point CO2 emissions are 0
				break;
		}
		
		// Return rounded number for ease
		return (int) Math.round(CO2);
	}
	
	// Agent termination
    protected void takeDown() {
    	// Decrement the used capacity of the carpark
        data.updateUsedCapacity(true);
    }
    
    /**
     * 
     * Attempt to park the newly create agent
     * Step.1 - Contact all known parking bay agents with a proposal
     * Step.2 - Find the best space
     * Step.3 - Propose parking
     * Step.4 - Park / Repeat if space not available
     * 
     */
    private class AttemptToPark extends Behaviour {
    	private AID space; // The space offered
        private int distance;  // The distance offered
        private int replies = 0; // The number of replies from ParkingBay agents
        private MessageTemplate mt; // The template to receive replies
        private int step = 0;
        private boolean typeFlag = true;
        private int length = 0;

		public void action() {
            // Create content object parameters
            CarSpecification params = new CarSpecification();
            params.setNumberPlate(numberPlate);
            params.setType(type);
            params.setMpg(fuelEconomy);
            params.setCarbonEmissions(carbonDioxideEmissions);
            
            // Create a sequence of steps to represent communication
            switch (step) {
	            case 0:	            	
	                // Create call for proposal
	                ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
	                
	                // If we are looking to match car type
	                if (typeFlag) {
	                	if (type.equals("D")) {
		                	for (int i = 0; i < disabledBays.size(); ++i) {
			                    cfp.addReceiver(disabledBays.get(i));
			                }
		                	
		                	length = disabledBays.size();
	                	} else if (type.equals("E")) {
	                		for (int i = 0; i < electricBays.size(); ++i) {
			                    cfp.addReceiver(electricBays.get(i));
			                }
	                		
	                		length = electricBays.size();
	                	} else {
	                		for (int i = 0; i < normalBays.size(); ++i) {
			                    cfp.addReceiver(normalBays.get(i));
			                }
	                		
	                		length = normalBays.size();
	                	}
	                } else {
	                	// No parking bays match our desired type
		                // So send call for proposal to all normal parking bay agents
		                for (int i = 0; i < normalBays.size(); ++i) {
		                    cfp.addReceiver(normalBays.get(i));
		                }
		                
		                length = normalBays.size();
	                }
	                
	                // Set the proposal's content
	                cfp.setContent(numberPlate);
	                
	                // Attempt to set the proposal's content object
					try {
						cfp.setContentObject(params);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					// Set the proposal's conversation id
	                cfp.setConversationId("request-park-car");
	                
	                // Set the proposal's reply
	                cfp.setReplyWith("cfp" + System.currentTimeMillis());
	                
	                // Send the proposal
	                myAgent.send(cfp);
	                
	                // Prepare the template to get proposals
	                mt = MessageTemplate.and(MessageTemplate.MatchConversationId("request-park-car"), MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
	                
	                // Proceed to the next step
	                step = 1;
	                
	                break;
	            case 1:
	                // Get all responses from parking bays
	                ACLMessage reply = myAgent.receive(mt);
	                
	                // If valid reply
	                if (reply != null) {
	                    // If reply is a proposal
	                    if (reply.getPerformative() == ACLMessage.PROPOSE) {
	                        // Store proposed distance
	                        int value = Integer.parseInt(reply.getContent());
	                        
	                        // Switch between the possible algorithms
	                        switch (algorithm) {
	                        	// Optimised efficiency - aims to reduce total emissions and fuel usage
	                        	case "Optimised Efficiency":
			                        // If car is electric or has high fuel economy and low emissions, go for farthest distance, else go for closest
			                        if (type == "E" || (fuelEconomy >= 60 && carbonDioxideEmissions <= 150)) {
			                        	// If best offer is null or proposed distance is greater than best distance, update placeholders
				                        if (space == null || value > distance) {
				                        	distance = value;
				                            space = reply.getSender();
				                        }
			                        } else {
				                        // If best offer is null or proposed distance is less than best distance, update placeholders
				                        if (space == null || value < distance) {
				                        	distance = value;
				                            space = reply.getSender();
				                        }
			                        }
			                        
			                        break;
			                    // First Come First Served - aims to park all cars as close as possible to entrance in their desired space type
	                        	case "First Come First Served":
	                        		// If best offer is null or proposed distance is less than best distance, update placeholders
	                        		if (space == null || value < distance) {
	                        			distance = value;
			                            space = reply.getSender();
			                        }
	                        		
	                        		break;
	                        }
	                    }
	                    
	                    // Increment reply count
	                    replies++;
	                    
	                    // If total replies is greater than or equal to the number of associated parking bay agents, all replies have been received
	                    if (replies >= length) {
	                    	// If no space was found and type flag was passed, start again with no space preference
	                    	if (typeFlag == true && space == null) {
	                    		typeFlag = false;
	                    		step = 0;
	                    	} else {
	                    		// Proceed to the next step
	                    		step = 2;
	                    	}
	                    }
	                } else {
	                	// Otherwise block response
	                    block();
	                }
	                
	                break;
	            case 2:
	                // Send the parking request to the parking bay that provided the best offer
	                ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
	                
	                order.addReceiver(space);
	                order.setContent(numberPlate);
	                order.setConversationId("park-car");
	                order.setReplyWith("order"+System.currentTimeMillis());
	                
	                // Attempt to set the proposal's content object
					try {
						order.setContentObject(params);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
	                
	                myAgent.send(order);
	                
	                // Prepare the template to get the parking bay reply
	                mt = MessageTemplate.and(MessageTemplate.MatchConversationId("park-car"), MessageTemplate.MatchInReplyTo(order.getReplyWith()));
	                
	                step = 3;
	                
	                break;
	            case 3:
	                // Receive the parking bay reply
	                reply = myAgent.receive(mt);
	                
	                if (reply != null) {
	                    // Parking bay reply received
	                    if (reply.getPerformative() == ACLMessage.INFORM) {
	                        parked = true;
	                        queue.remove(name);
	                        currentSpace = space;
	                    }
	
	                    step = 4;
	                } else {
	                    block();
	                }
	                
	                break;
	           }
        }
        
		// Check whether or not sequence is complete
        public boolean done() {  
        	// If step = 2 and space is null, don't progress
        	// If step = 4 car is parked
        	return ((step == 2 && space == null) || step == 4);
        }
    }
    
    /**
     * 
     * Called when parking duration is up.
     * Attempts to free up the space the agent was occupying.
     * 
     */
    private class LeaveCarpark extends Behaviour {
		private MessageTemplate mt; // The template to receive replies
		private int step = 0;
		private ACLMessage reply;
		
    	public void action() {	
    		// Create a sequence of steps to represent communication
    		switch (step) {
    			case 0:
    				// Send the request to the parking bay
	                ACLMessage inform = new ACLMessage(ACLMessage.INFORM);
	                
	                inform.addReceiver(currentSpace);
	                inform.setContent(numberPlate);
	                inform.setConversationId("free-up-space");
	                inform.setReplyWith("inform"+System.currentTimeMillis());
	                
	                myAgent.send(inform);
	                
	                // Prepare the template to get the parking bay reply
	                mt = MessageTemplate.and(MessageTemplate.MatchConversationId("free-up-space"), MessageTemplate.MatchInReplyTo(inform.getReplyWith()));
	                
	                step = 1;
	                
    				break;
    			case 1:
    				// Receive the reply
					reply = myAgent.receive(mt);
	                
					// If reply has been received
	                if (reply != null) {
	                	// Delete agent
	                    myAgent.doDelete();

	                    step = 2;
	                } else {
	                    block();
	                }
	                
	                break;
    		}
    	}

    	// Check whether or not sequence is complete
		public boolean done() {
			if (step == 2) {
				return true;
			}
			
			return false;
		}
    }
}