import java.util.ArrayList;

/**
 * 
 * Class to hold all the analytics data.
 * Calculates all the necessary data and graph points.
 *
 */

public class Analytics {
	
	private double fuel = 0;
	private double carbonEmissions = 0;
	private int carsParked = 0;
	private double totalCapacity = 0;
	private double usedCapacity = 0;
	private double utilisation = 0;
	private JavaFXGui gui;
	private int queueTime = 0;
	private int carsCreated = 0;
	public int modelTicks = 0;
	public int systemTicks = 0;
	
	/**
	 * 
	 * Class Constructor 
	 * 
	 * @param JavaFXGui javaFXGui [A reference to user interface]
	 */
	Analytics(JavaFXGui javaFXGui) {
		this.gui = javaFXGui;
	}
	
	/**
	 * 
	 * Update the model ticks
	 * 
	 * @param int ticks [The number of microseconds the simulation has been running for]
	 */
	public void setModelTicks(int ticks) {
		modelTicks = modelTicks + ticks;
		gui.updateTicksCount(modelTicks);
	}
	
	/**
	 * 
	 * Update the system ticks
	 * 
	 * @param int ticks [The number of microseconds the graphs have been running for]
	 */
	public void setSystemTicks(int ticks) {
		systemTicks = systemTicks + ticks;
	}
	
	/**
	 * 
	 * Update all the graphs
	 * 
	 * @param ArrayList<String> queue [The queue of cars awaiting entry to the carpark]
	 */
	public void updateGraphs(ArrayList<String> queue) {
		gui.updateChart("carpark-utilisation", "" + systemTicks, this.utilisation);
		gui.updateChart("queue-duration", "" + systemTicks, (queueTime)/carsCreated);
		gui.updateChart("queue-length", "" + systemTicks, queue.size());
	}
	
	// Increment the number of cars created by the simulation
	public void updateCarsCreated() {
		carsCreated++;
	}
	
	/**
	 * 
	 * Update the average duration a car spends in the queue
	 * 
	 * @param int duration [How long a car spent in the queue in microseconds]
	 */
	public void updateQueueDuration(int duration) {
		queueTime = queueTime + duration;
	}
	
	/**
	 * 
	 * Update the total amount of fuel used by the simulation.
	 * 
	 * @param double used [The amount of fuel used]
	 */
	public void addFuelUsage(double used) {
		this.fuel = this.fuel + used;
		gui.updateFuelUsed(this.fuel);
	}
	
	/**
	 * 
	 * Update the total amount of carbon emissions produced by the simulation.
	 * 
	 * @param double produced [The amount of carbon emissions produced]
	 */
	public void addCarbonEmissions(double produced) {
		this.carbonEmissions = this.carbonEmissions + produced;
		gui.updateCO2Produced(this.carbonEmissions/1000);
	}
	
	// Increment the number of cars parked by the simulation
	public void increaseCarsParked() {
		// Increment cars parked
		this.carsParked = this.carsParked + 1;
		
		// Update the used capacity
		updateUsedCapacity(false);
		
		// Update the user interface
		gui.updateCarsParkedCount(this.carsParked);
	}
	
	/**
	 * 
	 * Set the maximum capacity of the carpark
	 * 
	 * @param int x [The width of the carpark]
	 * @param int y [The length of the carpark]
	 */
	public void setTotalCapacity(int x, int y) {
		this.totalCapacity = x * y;
	}
	
	/**
	 * 
	 * Update the number of parking bays currently in use
	 * 
	 * @param boolean leaving [Whether or not the update is triggered by a car leaving or entering the carpark]
	 */
	public void updateUsedCapacity(boolean leaving) {
		// If the car is leaving the carpark reduce the used capacity, otherwise increase it
		if (leaving) {
			this.usedCapacity = this.usedCapacity - 1;
		} else {
			this.usedCapacity = this.usedCapacity + 1;
		}
		
		// Update the current utilisation
		setUtilisation();
	}
	
	// Update the current utilisation of the carpark as a percentage
	public void setUtilisation() {
		this.utilisation = (this.usedCapacity / this.totalCapacity) * 100;
	}
	
	/**
	 * 
	 * Calculate the parking cost to park a car in its desired space
	 * 
	 * metres into kilometres = (m / 1000)
	 * km into miles = (km / 1.609)
	 * 4.546 litres to a gallon of fuel
	 * 
	 * @param int distance [The distance the car travelled]
	 * @param CarSpecification car [The details about the car]
	 */
	public void calculateParkingCost(int distance, CarSpecification car) {
		// Setup placeholders
		double fuel;
		double co2;
		
		// Workout fuel used
		// Step.1 - convert metres into kilometres (m / 1000)
		// Step.2 - convert kilometres into miles (km / 1.609)
		// Step.3 - convert into % of the car's fuel economy
		// Step.4 - multiply by 4.546 litres (a gallon of fuel)
		// Step.5 - multiply by 2 for fuel used to the space and from the space
		fuel = (((((double)distance / 1000) / 1.609) / car.getMpg()) * 4.546) * 2;
		
		// Workout co2 produced
		// Step.1 - convert metres into kilometres (m / 1000)
		// Step.2 - multiply by the car's carbon emissions produced per kilometre
		// Step.3 - multiply by 2 for emissions produced to the space and from the space
		co2 = (((double) distance / 1000) * car.getCarbonEmissions()) * 2;
		
		// Update total fuel used
		addFuelUsage(fuel);
		
		// Update total emissions produced
		addCarbonEmissions(co2);
	}
	
	// Print out the analytics to console
	public void printResults() {
		System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
		System.out.println(carsParked + " cars parked.");
		System.out.println(Double.toString(fuel) + " (L) of fuel");
		System.out.println(Double.toString((carbonEmissions)/1000) + " (kg/KM) of CO2");
		System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
	}
	
}
