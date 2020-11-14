/**
 * 
 * GUI Launcher for the car park simulation.
 * First agent to be created by Jade setup.
 * Launches a JavaFX application and passes through a reference to the container for future agents to be created.
 * 
 */

import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.basic.Action;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.JADEAgentManagement.JADEManagementOntology;
import jade.domain.JADEAgentManagement.ShutdownPlatform;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.wrapper.ContainerController;
import javafx.application.Application;

public class Gui extends Agent {
	// Flags
	public static boolean shutdown = false;
	
	// Agent Creation
	protected void setup() {
		// Get the container
		ContainerController container = getContainerController();
		
		// Statically pass the container to the JavaFX application
		JavaFXGui.container = container;
		
		// Behaviour to shutdown system
		addBehaviour(new ShutdownSystem());
		
		// Launch the JavaFX application
		Application.launch(JavaFXGui.class);
	}
	
	// Agent Deletion
	protected void takeDown() {
		
	}
	
	/**
	 * 
	 * Shutdown the system on window close.
	 * Credit: https://stackoverflow.com/a/7026974
	 * Accessed: 27/08/2020
	 *
	 */
	private class ShutdownSystem extends CyclicBehaviour {
		public void action() {
			// If shutdown flag is true, close down JADE
			if (shutdown == true) {
				// Create new codec
				Codec codec = new SLCodec(); 
				
				// Get the JADE management ontology
				Ontology jmo = JADEManagementOntology.getInstance();
				
				// Register codec and ontology with the content manager
				getContentManager().registerLanguage(codec);
				getContentManager().registerOntology(jmo);
				
				// Create a new request message
				ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
				
				// Setup message
				msg.addReceiver(getAMS());
				msg.setLanguage(codec.getName());
				msg.setOntology(jmo.getName());
				
				// Try to fill the message with shutdown request and send
				try {
				    getContentManager().fillContent(msg, new Action(getAID(), new ShutdownPlatform()));
				    send(msg);
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
        }
    }
}
