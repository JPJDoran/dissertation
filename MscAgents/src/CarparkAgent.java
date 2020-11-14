/**
  * Carpark agent that manages the simulation.
  * Creates a carpark of given size and initiates the parking bay agents.
  * Creates a new car agent at a set interval.
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
import java.util.List;
import java.util.Random;

import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.*;
import jade.wrapper.*;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.*;

public class CarparkAgent extends Agent {
	
	// Instance Variables
	int length = 10;
	int width = 10;
	private AID[] parkingBayAgents;
	private ArrayList<AID> normalBays = new ArrayList<AID>();
	private ArrayList<AID> disabledBays = new ArrayList<AID>();
	private ArrayList<AID> electricBays = new ArrayList<AID>();
	private ArrayList<String> queue = new ArrayList<String>();
	private int modelSpeed = 1000;
	private ArrayList<String> numberPlateList = new ArrayList<String>();
	JavaFXGui gui;
	Analytics data;

	// Agent creation
	protected void setup() {
		// Get simulation variables
		Object[] parameters = getArguments();
		
		gui = (JavaFXGui) parameters[0];
		data = new Analytics(gui);
		
		// Setup simulation variables
		length = Integer.parseInt(parameters[1].toString());
		width = Integer.parseInt(parameters[2].toString());
		modelSpeed = (int) parameters[7];
		
		// Set total capacity of simulation
		data.setTotalCapacity(width, length);
		
		addBehaviour(new OneShotBehaviour(this) {
			public void action() {
				// Create empty object to store variables
				Object[] setup = new Object[3];
				
				// Generate an entrance position given the width and length of the car park
				int entrance[] = generateEntrance(width, length);
				
				// Loop through from 0 up to given length
				for (int y = 0; y < length; y++) {
					
					// Loop through from 0 up to given width
					for (int x = 0; x < width; x++) {
						// Calculate the distance the space is from the entrance
						int distance = calculateDistance(entrance, x, y);
						
						// Add variables to object
						setup[0] = distance; // distance the space is from the entrance
						setup[1] = data; // reference to the analytics instance
						setup[2] = (JavaFXGui) parameters[0]; // refence to the gui
	
						// Create reference to container
						ContainerController container = getContainerController();
						AgentController agent;
						
						// Try to create a new ParkingBayAgent
						try {
							agent = container.createNewAgent("ParkingBayAgent-" + (y+1) + ":" + (x+1), "ParkingBayAgent", setup);
							agent.start();
						} catch (StaleProxyException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}
				}
				
	            return;
	          } 
		});
		
		// On creation, create directory of parking bay agents
		addBehaviour(new OneShotBehaviour(this) {
            public void action() {
            	// Update array of parkingBayAgents
                DFAgentDescription template = new DFAgentDescription();
                ServiceDescription sd = new ServiceDescription();
                
                sd.setType("parkingBay");
                
                template.addServices(sd);
                
                // Try to add agents to array
                try {
                    DFAgentDescription[] result = DFService.search(myAgent, template);
                    
                    parkingBayAgents = new AID[result.length];
                    
                    for (int i = 0; i < result.length; ++i) {
                    	parkingBayAgents[i] = result[i].getName();
                    }
                } catch (FIPAException fe) {
                    fe.printStackTrace();
                }

                // Differentiate parking bays into types
                myAgent.addBehaviour(new ParkingBayTypes());
            }
        });
		
		// Every set interval, create a new car agent
		addBehaviour(new TickerBehaviour(this, modelSpeed) {
            protected void onTick() {
            	// Create new object for variables
            	Object[] setup = new Object[12];
            	
            	// Generate a new number plate
            	String numberPlate = newNumberPlate();
                
            	// Data to be passed to car agents
                setup[0] = numberPlate; // vehicle number plate
                setup[1] = data; // analytics class reference
                setup[2] = normalBays; // normal parking bays
                setup[3] = disabledBays; // disabled parking bays
                setup[4] = electricBays; // electric parking bays
                setup[5] = parameters[3]; // min duration 
                setup[6] = parameters[4]; // max duration
                setup[7] = parameters[5]; // ev percent
                setup[8] = parameters[6]; // disability percent
                setup[9] = queue; // queue of cars
                setup[10] = modelSpeed; // speed of the model
                setup[11] = parameters[8]; // the algorithm being used
                
                // Create reference to container
				ContainerController container = getContainerController();
				AgentController agent;
				
				// Try to create a new car agent
				try {
					agent = container.createNewAgent(numberPlate, "CarAgent", setup); 
					agent.start();
					
					queue.add(agent.getName());
				} catch (StaleProxyException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
        	}
        });
		
		// Every set interval, update the user interface ticks and graphs
		addBehaviour(new TickerBehaviour(this, 1000) {
            protected void onTick() {
            	// Set Model Ticks has to be set every second because it can't keep up with the number of requests at 0.05 a second
				data.setModelTicks(1000);
				data.setSystemTicks(1000);
				data.updateGraphs(queue);
            }
        } );
	}
    
	/**
	 * 
	 * Randomly generate the coordinates of an entrance to the car park
	 * Take in the given width and length of the car park to ensure entrance is within bounds
	 * 
	 * @param int width [The width of the carpark]
	 * @param int length [The length of the carpark]
	 * @return int[] entrance [The X and Y coordinates of the entrance to the carpark]
	 */
	protected int[] generateEntrance(int width, int length) {
		// Placeholder array for the entrance
		int entrance[] = new int[2];
		
		// Generate new random for random coordinates
		Random rand = new Random();
		
		// Generate random x value
		int dirx = rand.nextInt(width+1);
		
		// Generate random y value
		int diry = rand.nextInt(length+1);
		
		// Generate random value between 0-3 to determine the position of the entrance
		// 0 - North
		// 1 - East
		// 2 - South
		// 3 - West
		switch(rand.nextInt(4)) {
			// North
			case 0:
				entrance[0] = 0; // Because North is top, y value is 0
				entrance[1] = dirx; // Because North is top, x value is random
				break;
			// East
			case 1:
				entrance[0] = diry; // Because East is far right, y value is random
				entrance[1] = width; // Because East is far right, x value is max width
				break;
			
			// South
			case 2:
				entrance[0] = length; // Because South is bottom, y value is max length
				entrance[1] = dirx; // Because South is bottom, x value is random
				break;
				
			// West
			// If error default to west
			case 3:
			default:
				entrance[0] = diry; // Because West is far left, y value is random
				entrance[1] = 0; // Because West is far left, x value is 0
				break;
		} 
		
		// Update the user interface to show the carpark
		gui.setEntrance(entrance[0], entrance[1]);
		
		// Return the entrance coordinates
		return entrance;
	}
	
	/**
	 * 
	 * Calculate the distance a parking space is from the entrance
	 * Average statistics used for parking space sizes - source: https://www.theaa.com/breakdown-cover/advice/parking-space-size
	 * 
	 * @param int[] entrance [The X and Y coordinates of the entrance to the carpark]
	 * @param int x [The X coordinate of the parking bay]
	 * @param int y [The Y coordinate of the parking bay]
	 * @return int distance [How far the parking bay is from the entrance]
	 */
	protected static int calculateDistance(int entrance[], int x, int y) {
		// Placeholder for distance
		double distance = 0;
		
		// difference in y distance between entrance and grid position * by average length of parking space
		double difY = (entrance[0] - y) * 4.8;
		
		// difference in x distance between entrance and grid position * by average width of parking space
		double difX = (entrance[1] - x) * 2.8;
		
		// only straight line travel in the car park, no diagonals
		// Math.abs() used to ensure number is positive
		distance = Math.abs(difY) + Math.abs(difX); 
		
		// If the distance is 0, set it to 1 so that it has some arbitrary distance from the entrance 
		if (distance == 0) {
			distance = 1;
		}
		
		// Return rounded distance for ease of use
		return (int) Math.round(distance) * 10;
	}
	
	// Generate a new random valid number plate - source: https://assets.publishing.service.gov.uk/government/uploads/system/uploads/attachment_data/file/359317/INF104_160914.pdf
	// Format: DVlA Memory Tag (M), Age Identifier (A) and Random String (R)
	// e.g. MMAARRR
	protected String newNumberPlate() {
		Random rand = new Random();
		
		// Array of valid DVLA Memory Tags
		String[] memoryTags = { "AA", "AB", "AC", "AD", "AE", "AF", "AG", "AH", "AJ", "AK", "AL", "AM", "AN", "AO", "AP", "AR", "AS", "AT", "AU", "AV", "AW", "AX", "AY",
								"BA", "BB", "BC", "BD", "BE", "BF", "BG", "BH", "BJ", "BK", "BL", "BM", "BN", "BO", "BP", "BR", "BS", "BT", "BU", "BV", "BW", "BX", "BY",
								"CA", "CB", "CC", "CD", "CE", "CF", "CG", "CH", "CJ", "CK", "CL", "CM", "CN", "CO", "CP", "CR", "CS", "CT", "CU", "CV", "CW", "CX", "CY",
								"DA", "DB", "DC", "DD", "DE", "DF", "DG", "DH", "DJ", "DK", "DL", "DM", "DN", "DO", "DP", "DR", "DS", "DT", "DU", "DV", "DW", "DX", "DY",
								"EA", "EB", "EC", "ED", "EE", "EF", "EG", "EH", "EJ", "EK", "EL", "EM", "EN", "EO", "EP", "ER", "ES", "ET", "EU", "EV", "EW", "EX", "EY",
								"FA", "FB", "FC", "FD", "FE", "FF", "FG", "FH", "FJ", "FK", "FL", "FM", "FN", "FO", "FP", "FR", "FS", "FT", "FU", "FV", "FW", "FX", "FY",
								"GA", "GB", "GC", "GD", "GE", "GF", "GG", "GH", "GJ", "GK", "GL", "GM", "GN", "GO", "GP", "GR", "GS", "GT", "GU", "GV", "GW", "GX", "GY",
								"HA", "HB", "HC", "HD", "HE", "HF", "HG", "HH", "HJ", "HK", "HL", "HM", "HN", "HO", "HP", "HR", "HS", "HT", "HU", "HV", "HW", "HX", "HY",
								"KA", "KB", "KC", "KD", "KE", "KF", "KG", "KH", "KJ", "KK", "KL", "KM", "KN", "KO", "KP", "KR", "KS", "KT", "KU", "KV", "KW", "KX", "KY",
								"LA", "LB", "LC", "LD", "LE", "LF", "LG", "LH", "LJ", "LK", "LL", "LM", "LN", "LO", "LP", "LR", "LS", "LT", "LU", "LV", "LW", "LX", "LY",
								"MA", "MB", "MC", "MD", "ME", "MF", "MG", "MH", "MJ", "MK", "ML", "MM", "MN", "MO", "MP", "MR", "MS", "MT", "MU", "MV", "MW", "MX", "MY",
								"NA", "NB", "NC", "ND", "NE", "NF", "NG", "NH", "NJ", "NK", "NL", "NM", "NN", "NO", "NP", "NR", "NS", "NT", "NU", "NV", "NW", "NX", "NY",
								"OA", "OB", "OC", "OD", "OE", "OF", "OG", "OH", "OJ", "OK", "OL", "OM", "ON", "OO", "OP", "OR", "OS", "OT", "OU", "OV", "OW", "OX", "OY",
								"PA", "PB", "PC", "PD", "PE", "PF", "PG", "PH", "PJ", "PK", "PL", "PM", "PN", "PO", "PP", "PR", "PS", "PT", "PU", "PV", "PW", "PX", "PY",
								"RA", "RB", "RC", "RD", "RE", "RF", "RG", "RH", "RJ", "RK", "RL", "RM", "RN", "RO", "RP", "RR", "RS", "RT", "RU", "RV", "RW", "RX", "RY",
								"SA", "SB", "SC", "SD", "SE", "SF", "SG", "SH", "SJ", "SK", "SL", "SM", "SN", "SO", "SP", "SR", "SS", "ST", "SU", "SV", "SW", "SX", "SY",
								"VA", "VB", "VC", "VD", "VE", "VF", "VG", "VH", "VJ", "VK", "VL", "VM", "VN", "VO", "VP", "VR", "VS", "VT", "VU", "VV", "VW", "VX", "VY",
								"WA", "WB", "WC", "WD", "WE", "WF", "WG", "WH", "WJ", "WK", "WL", "WM", "WN", "WO", "WP", "WR", "WS", "WT", "WU", "WV", "WW", "WX", "WY",
								"YA", "YB", "YC", "YD", "YE", "YF", "YG", "YH", "YJ", "YK", "YL", "YM", "YN", "YO", "YP", "YR", "YS", "YT", "YU", "YV", "YW", "YX", "YY" };
		
		// Array of valid current DVLA Age Identifiers
		String[] ageIdentifier = { "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "51", "52", "53", "54", "55", "56", "57", "58", "59", "60", "61", "62", "63", "64", "65", "66", "67", "68", "69" };
		
		// Array of upper case alphabet used for random string
		String[] chars = { "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "0", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z" };
		
		// Valid random plate
		String plate = memoryTags[rand.nextInt(memoryTags.length)] + ageIdentifier[rand.nextInt(ageIdentifier.length)] + chars[rand.nextInt(chars.length)] + chars[rand.nextInt(chars.length)] + chars[rand.nextInt(chars.length)];
		
		// Ensure number plate is unique
		if (numberPlateList.size() > 0) {
			if (numberPlateList.contains(plate)) {
				plate = newNumberPlate();
			}
		}	
		
		// Add unique number plate to list of plates
		numberPlateList.add(plate);
		
		return plate;
	}
	
	// Agent termination
    protected void takeDown() {
        // Print analytics to console
        data.printResults();
    }
    
    /**
     * 
     * Organise the parking bay spaces
     * Step.1 - Contact all known parking bay agents with a request
     * Step.2 - Retrieve their space type and add to associated ArrayList
     * 
     */
    private class ParkingBayTypes extends Behaviour {
    	private MessageTemplate mt; // The template to receive replies
    	private int replies = 0; // The number of replies from ParkingBay agents
    	private int step = 0; // The current step of the sequence
    	
    	public void action() {
    		// Create a sequence of steps to represent conversation
    		switch (step) {
	            case 0:	            	
	                // Create request
	                ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
	                
	                // Send request to all known parking bay agents
	                for (int i = 0; i < parkingBayAgents.length; ++i) {
	                	request.addReceiver(parkingBayAgents[i]);
	                }
	                
	                // Set the request's content
	                request.setContent("");
	                
					// Set the request's conversation id
	                request.setConversationId("parking-bay-type");
	                
	                // Set the request's reply
	                request.setReplyWith("cfp" + System.currentTimeMillis());
	                
	                // Send the request
	                myAgent.send(request);
	                
	                // Prepare the template to get responses
	                mt = MessageTemplate.and(MessageTemplate.MatchConversationId("parking-bay-type"), MessageTemplate.MatchInReplyTo(request.getReplyWith()));
	                
	                // Proceed to the next step
	                step = 1;
	                
	                break;
	            case 1:
	                // Get all responses from parking bays
	                ACLMessage reply = myAgent.receive(mt);
	                
	                // If valid reply
	                if (reply != null) {
	                    // If reply is an inform
	                    if (reply.getPerformative() == ACLMessage.INFORM) {
	                        // Store message
	                        String parkingBayType = reply.getContent();
	                        
	                        // Depending on space type, add to associated ArrayList
	                        switch (parkingBayType) {
	                        	case "N":
	                        		normalBays.add(reply.getSender());
	                        		break;
	                        	case "D":
	                        		disabledBays.add(reply.getSender());
	                        		break;
	                        	case "E":
	                        		electricBays.add(reply.getSender());
	                        		break;
	                        }
	                    }
	                    
	                    // Increment reply count
	                    replies++;
	                    
	                    // If total replies is greater than or equal to the number of found parking bay agents, all replies have been received
	                    if (replies >= parkingBayAgents.length) {
	                        // Proceed to the next step
	                    	step = 2;
	                    }
	                } else {
	                	// Otherwise block response
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
