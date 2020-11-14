/**
 * 
 * The front-end for the simulation.
 * Created by the Gui agent on launch.
 * Creates the user interface as well as updates the labels, display and graphs.
 * Utilises JavaFX for advanced UI functionality.
 * 
 */

import java.math.BigDecimal;
import java.math.RoundingMode;

import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Slider;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.scene.control.TextField; 
import javafx.stage.Stage;  

public class JavaFXGui extends Application {
	static ContainerController container;
	static AgentController agent;
	
	// Display
	Button carpark[][];
	
	// Flags
	boolean running = false;
	
	// Counters
	int countx = 1;
	int county = 1;
	
	// Create Labels
	Text carsParked = new Text("Cars Parked: 0");
	Text simTicks = new Text("Ticks: 0"); 
	Text fuelUsed = new Text("Fuel Used (L): 0");
	Text co2Produced = new Text("CO2 Produced (kg/km): 0");
	Text errorMessage = new Text("");
				
	// Create TextFields
	TextField lengthInput = new TextField("10"); // Default carpark length
	TextField widthInput = new TextField("10"); // Default carpark width
	TextField maxDurationInput = new TextField("100"); // Default car max duration
	TextField minDurationInput = new TextField("10"); // Default car min duration
	TextField evPercentInput = new TextField("6.1"); // EVs currently make up 6.1% of the market - source: https://www.drivingelectric.com/news/678/electric-car-sales-uk-tesla-model-3-remains-top-10-june#:~:text=Electrified%20car%20sales%20as%20a,increase%20compared%20to%20June%202019.
	TextField disabilityPercentInput = new TextField("4.1"); // Blue badge holders make up 4.1% of the population - source: https://assets.publishing.service.gov.uk/government/uploads/system/uploads/attachment_data/file/850086/blue-badge-scheme-statistics-2019.pdf
	
	// Create Sliders
	Slider speedControl = new Slider(0, 100, 100); // Slider range from 0-100, starting at 100
	
	// Create Combo Box
	ComboBox algorithmChoiceInput;
	
	// Create Buttons
	Button go = new Button("GO");
	Button end = new Button("END");
	
	// Create Chart Data
	XYChart.Series<String, Number> queueLengthSeries = new XYChart.Series<>();
	XYChart.Series<String, Number> queueTimeSeries = new XYChart.Series<>();
	XYChart.Series<String, Number> carparkUtilisationSeries = new XYChart.Series<>();
	
	// Create Graphs
	LineChart queueLengthGraph = createChart("Ticks", "Cars In Queue", queueLengthSeries);
	LineChart queueTimeGraph = createChart("Ticks", "Queue Duration", queueTimeSeries);
	LineChart carparkUtilisationGraph = createChart("Ticks", "Utilisation %", carparkUtilisationSeries);
	
	// On launch of application
	public void start(Stage stage) {
		JavaFXGui gui = this;
		
		// Create Border
		BorderPane border = new BorderPane();
		
		// Setup Border
		border.setMinSize(1000, 800); 
		
		// Add Border Sections
		border.setTop(simulationHeader());
		border.setLeft(simulationOptions());
		border.setBottom(simulationGraphs());
       
      	// Create Scene
      	Scene scene = new Scene(border); 
       
      	// Setup Stage
      	stage.setTitle("Smart Car Parking Allocation"); 
      	stage.setScene(scene);
      	stage.show(); 
      	
      	// On click of GO, start simulation
      	go.setOnAction(new EventHandler() {
			public void handle(Event arg0) {		
				// If the simulation isn't running
				if (running == false) {
					// Check if the inputs are valid
					boolean valid = validateForm();
					
					// If the inputs are valid
					if (valid) {
						// Hide any showing error message
						setErrorMessage("");
						
						// Set running to true to prevent multiple launches
						running = true;
						
						// Add the display to the UI
						border.setCenter(simulationDisplay());
						
						// Create object of simulation options to be passed to agents
						Object[] setup = getOptions(gui);
						
						// Try to create a new parent carpark agent
						try {
							agent = container.createNewAgent("Carpark", "CarparkAgent", setup);
							agent.start();
						} catch (StaleProxyException e) {
							e.printStackTrace();
						}
					}
				}
			}
      	});
      	
      	// On click of END, close simulation
      	end.setOnAction(new EventHandler() {
			public void handle(Event arg0) {
				// If simulation is running
				if (running == true) {
					// Set running to false
					running = false;
					
					// End the simulation
					// Adapted from: https://stackoverflow.com/a/25038465
					// Accessed: 27/08/2020
					Stage stage = (Stage) end.getScene().getWindow();
				    stage.close();
				}
			}
      	});
	} 
	
	// On end of the simulation
	public void stop() {
		// Tell the Gui Agent to shutdown the system
		Gui.shutdown = true;
	}
	
	/**
	 * 
	 * Do simulation option validation to ensure that all inputs are valid.
	 * Return false and set error message if input is invalid.
	 * 
	 * Number assertion check adapted from: https://www.baeldung.com/java-check-string-number
	 * Accessed: 29/08/20
	 * 
	 */
	public boolean validateForm() {
		int validNum;
		double validDouble;
		String lengthVal = lengthInput.getText();
		String widthVal = widthInput.getText();
		String minDurationVal = minDurationInput.getText();
		String maxDurationVal = maxDurationInput.getText();
		String evPercentVal = evPercentInput.getText();
		String disabilityPercentVal = disabilityPercentInput.getText();
		
		/**
		 * Step.1 - Ensure all inputs are numbers
		 */
		
		// Try to parse length as int
		try {
			validNum = Integer.parseInt(lengthVal);
	    } catch (NumberFormatException e) {
	    	setErrorMessage("Length must be an integer");
	        return false;
	    }
		
		// Try to parse width as int
		try {
			validNum = Integer.parseInt(widthVal);
	    } catch (NumberFormatException e) {
	    	setErrorMessage("Width must be an integer");
	        return false;
	    }
		
		// Try to parse width as int
		try {
			validNum = Integer.parseInt(minDurationVal);
	    } catch (NumberFormatException e) {
	    	setErrorMessage("Min Duration must be an integer");
	        return false;
	    }
		
		// Try to parse width as int
		try {
			validNum = Integer.parseInt(maxDurationVal);
	    } catch (NumberFormatException e) {
	    	setErrorMessage("Max Duration must be an integer");
	        return false;
	    }
		
		// Try to parse width as int
		try {
			validDouble = Double.parseDouble(evPercentVal);
	    } catch (NumberFormatException e) {
	    	setErrorMessage("EV % must be a number");
	        return false;
	    }
		
		// Try to parse width as int
		try {
			validDouble = Double.parseDouble(disabilityPercentVal);
	    } catch (NumberFormatException e) {
	    	setErrorMessage("Disability % must be a number");
	        return false;
	    }
		
		/**
		 * Step.2 - Ensure all needed inputs are non-zero
		 */
		
		validNum = Integer.parseInt(lengthVal);
		if (validNum <= 0) {
			setErrorMessage("Length must be greater than 0");
			return false;
		}
		
		validNum = Integer.parseInt(widthVal);
		if (validNum <= 0) {
			setErrorMessage("Width must be greater than 0");
			return false;
		}
		
		validNum = Integer.parseInt(minDurationVal);
		if (validNum < 10) {
			setErrorMessage("Min Duration must be at least 10");
			return false;
		}
		
		validNum = Integer.parseInt(maxDurationVal);
		if (validNum < 10) {
			setErrorMessage("Max Duration must be at least 10");
			return false;
		}
		
		validDouble = Double.parseDouble(evPercentVal);
		if (validDouble < 0) {
			setErrorMessage("EV % can't be less than 0");
			return false;
		}
		
		validDouble = Double.parseDouble(disabilityPercentVal);
		if (validDouble < 0) {
			setErrorMessage("Disability % can't be less than 0");
			return false;
		}
		
		/**
		 * Step.3 - Ensure Max Duration can't be smaller than Min Duration
		 */
		
		if (Integer.parseInt(minDurationVal) > Integer.parseInt(maxDurationVal)) {
			setErrorMessage("Max Duration can't be less than Min Duration");
			return false;
		}
		
		/**
		 * Step.4 - Ensure values aren't too large for the system to handle
		 */
		
		if (Integer.parseInt(lengthVal) > 15) {
			setErrorMessage("Carpark length can't be greater than 15 spaces");
			return false;
		}

		if (Integer.parseInt(widthVal) > 25) {
			setErrorMessage("Carpark width can't be greater than 25 spaces");
			return false;
		}
		if (Integer.parseInt(minDurationVal) > 150) {
			setErrorMessage("Min Duration can't be greater than 150");
			return false;
		}
		
		if (Integer.parseInt(maxDurationVal) > 300) {
			setErrorMessage("Max Duration can't be greater than 300");
			return false;
		}
		
		if (Double.parseDouble(evPercentVal) > 100) {
			setErrorMessage("EV % can't be greater than 100");
			return false;
		}
		
		if (Double.parseDouble(disabilityPercentVal) > 100) {
			setErrorMessage("Disability % can't be greater than 100");
			return false;
		}
		
		if (Integer.parseInt(minDurationVal) > 150) {
			setErrorMessage("Min Duration can't be greater than 150");
			return false;
		}
		
		return true;
	}
	
	/**
	 * 
	 * Set the error message label to the given message
	 * 
	 * @param String msg [The message to be displayed]
	 */
	public void setErrorMessage(String msg) {
		errorMessage.setText(msg);
	}
	
	/**
	 * 
	 * Create an object of simulation variables to be passed to agents
	 * 
	 * @param JavaFXGui gui [Reference to the user interface]
	 * @return Object setup [The object to be returned]
	 */
	public Object[] getOptions(JavaFXGui gui) {
		// Create new object for setup variables
		Object[] setup = new Object[9];
		
		// Add setup variables
		setup[0] = gui;
		setup[1] = lengthInput.getText();
		setup[2] = widthInput.getText();
		setup[3] = minDurationInput.getText();
		setup[4] = maxDurationInput.getText();
		setup[5] = evPercentInput.getText();
		setup[6] = disabilityPercentInput.getText();
		setup[7] = getModelSpeed();
		setup[8] = algorithmChoiceInput.getValue();
		
		return setup;
	}
	
	// Quantify model speed
	public int getModelSpeed() {
		// Get the model speed value from 0-100
		int modelSpeed = (int) speedControl.getValue();
		
		// Set model speed in microseconds depending on the slider value
		switch (modelSpeed) {
			// 0 = Slowest
			case 0:
				modelSpeed = 500;
				break;
			case 10:
				modelSpeed = 500;
				break;
			case 20:
				modelSpeed = 450;
				break;
			case 30:
				modelSpeed = 400;
				break;
			case 40:
				modelSpeed = 350;
				break;
			case 50:
				modelSpeed = 300;
				break;
			case 60:
				modelSpeed = 250;
				break;
			case 70:
				modelSpeed = 200;
				break;
			case 80:
				modelSpeed = 150;
				break;
			case 90:
				modelSpeed = 100;
				break;
			// 100 = Fastest
			case 100:
				modelSpeed = 50;
				break;
		}
		
		return modelSpeed;
	}
	
	/**
	 * 
	 * Update the number of cars parked
	 * 
	 * @param int count [The number of cars parked by the simulation]
	 */
	public void updateCarsParkedCount(int count) {
		carsParked.setText("Cars Parked: " + Integer.toString(count));
	}
	
	/**
	 * 
	 * Update the number of ticks passed
	 * 
	 * @param int ticks [How long the model has been running for]
	 */
	public void updateTicksCount(int ticks) {
		simTicks.setText("Ticks: " + ticks);
	}
	
	/**
	 * 
	 * Update the amount of fuel used
	 * 
	 * @param int fuel [The amount of fuel used]
	 */
	public void updateFuelUsed(double fuel) {
		fuelUsed.setText("Fuel Used (L): " + BigDecimal.valueOf(fuel).setScale(2, RoundingMode.HALF_UP));
		
	}
	
	/**
	 * 
	 * Update the amount of co2 produced
	 * 
	 * @param double co2 [The amount of CO2 produced]
	 */
	public void updateCO2Produced(double co2) {
		co2Produced.setText("CO2 Produced (kg/km): " + BigDecimal.valueOf(co2).setScale(2, RoundingMode.HALF_UP));
	}
	
	// Create the simulation header pane
	public GridPane simulationHeader() {
		// Create Title
		HBox title = simulationTitle();
		
		// Create Slider Title
		HBox sliderTitle = simulationControlTitle();
		
		// Create Slider
		HBox slider = simulationControl();
		
		// Create Tick Counter
		HBox tickCount = simulationTicks();
		
		// Create Error Placeholder
		HBox errorMessage = errorMessage();
		
		// Setup Grid
		GridPane header = new GridPane();
		header.setMinSize(800, 200); 
		header.setVgap(10); 
		header.setHgap(10);       
		header.setAlignment(Pos.CENTER); 
		
		// Add Components To Grid
		header.add(title, 0, 0); 
		header.add(sliderTitle, 0, 1);
		header.add(slider, 0, 2); 
		header.add(tickCount, 0, 3); 
		header.add(errorMessage, 0, 4);
		
		return header;
	}
	
	// Create the simulation title HBox
	public HBox simulationTitle() {
		// Create Heading
		Text heading = new Text("Smart Car Parking Allocation");
		
		// Setup Heading
		// Accessed 26/08/2020
		// Adapted from: https://www.tutorialspoint.com/javafx/javafx_text.htm#:~:text=You%20can%20change%20the%20font,scene.
		heading.setFont(Font.font("verdana", FontWeight.BOLD, FontPosture.REGULAR, 15));
		
		// Setup HBox
		HBox title = new HBox();
		title.setPadding(new Insets(15, 0, 10, 0)); 
		title.setAlignment(Pos.CENTER); 
		
		// Add Heading To HBox
		title.getChildren().add(heading);
		
		return title;
	}
	
	// Create the simulation control title HBox
	public HBox simulationControlTitle() {
		// Create Heading
		Text heading = new Text("Model Speed");
		
		// Setup Heading
		// Accessed 26/08/2020
		// Adapted from: https://www.tutorialspoint.com/javafx/javafx_text.htm#:~:text=You%20can%20change%20the%20font,scene.
		heading.setFont(Font.font("verdana", FontWeight.NORMAL, FontPosture.REGULAR, 11));
		
		// Setup HBox
		HBox controlTitle = new HBox(); 
		controlTitle.setAlignment(Pos.CENTER); 
		
		// Add Heading To HBox
		controlTitle.getChildren().add(heading);
		
		return controlTitle;
	}
	
	// Create the simulation control HBox
	public HBox simulationControl() {		
		// Setup Slider
		speedControl.setPrefWidth(800);
		speedControl.setBlockIncrement(10);
		speedControl.setMajorTickUnit(10);
		speedControl.setMinorTickCount(0);
		speedControl.setSnapToTicks(true);
		
		// Setup HBox
		HBox control = new HBox(); 
		control.setAlignment(Pos.CENTER); 
		
		// Add Slider To HBox
		control.getChildren().add(speedControl);
		
		return control;
	}
	
	// Create the simulation ticks HBox
	public HBox simulationTicks() { 
		// Setup Ticks
		// Accessed 26/08/2020
		// Adapted from: https://www.tutorialspoint.com/javafx/javafx_text.htm#:~:text=You%20can%20change%20the%20font,scene.
		simTicks.setFont(Font.font("verdana", FontWeight.NORMAL, FontPosture.REGULAR, 11));
		
		// Setup HBox
		HBox ticks = new HBox(); 
		ticks.setAlignment(Pos.CENTER); 
		
		// Add Slider To HBox
		ticks.getChildren().add(simTicks);
		
		return ticks;
	}
	
	// Create the error message HBox
	public HBox errorMessage() {
		// Setup Error Message
		errorMessage.setFill(Color.RED);
		
		// Setup HBox
		HBox error = new HBox();
		error.setAlignment(Pos.CENTER); 
		
		// Add Error Message
		error.getChildren().add(errorMessage);
		
		return error;
	}
	
	// Create the analytics GridPane
	public GridPane simulationAnalytics() {
		// Create Grid
		GridPane analytics = new GridPane();  
				
		// Setup Cars Parked
		// Accessed 26/08/2020
		// Adapted from: https://www.tutorialspoint.com/javafx/javafx_text.htm#:~:text=You%20can%20change%20the%20font,scene.
		carsParked.setFont(Font.font("verdana", FontWeight.BOLD, FontPosture.REGULAR, 13));
		fuelUsed.setFont(Font.font("verdana", FontWeight.BOLD, FontPosture.REGULAR, 13));
		co2Produced.setFont(Font.font("verdana", FontWeight.BOLD, FontPosture.REGULAR, 13));
		
		// Add Components
		analytics.add(carsParked, 0, 0);
		analytics.add(fuelUsed, 0, 1);
		analytics.add(co2Produced, 0, 2);
		
		return analytics;
	}
	
	// Create the simulation options grid pane
	public GridPane simulationOptions() {
		// Create Labels
		Text length = new Text("Length");
		Text width = new Text("Width");
		Text maxDuration = new Text("Max Duration");
		Text minDuration = new Text("Min Duration");
		Text evPercent = new Text("EV %");
		Text disabilityPercent = new Text("Disability %");
		Text algorithmChoice = new Text("Algorithm");
		
		// Create Combo Box
		ObservableList<String> algorithms = 
		    FXCollections.observableArrayList(
		        "Optimised Efficiency",
		        "First Come First Served"
		    );
		
		algorithmChoiceInput = new ComboBox(algorithms);
		
		// Setup Combo Box
		algorithmChoiceInput.setValue("Optimised Efficiency");
		
		// Create Grid
		GridPane controls = new GridPane();    
		
		// Setup Grid
		controls.setMinSize(300, 0); 
		controls.setMaxSize(300, 300); 
		controls.setPadding(new Insets(0, 15, 0, 15)); 
		controls.setVgap(10); 
		controls.setHgap(10);       
		controls.setAlignment(Pos.CENTER); 
		
		// Setup Go Button
		go.setMaxWidth(300);
		end.setMaxWidth(300);
       
	    // Add Components To Grid
		controls.add(simulationAnalytics(), 0, 0, 2, 1);
		controls.add(length, 0, 1, 1, 1); 
		controls.add(width, 1, 1, 1, 1); 
		controls.add(lengthInput, 0, 2, 1, 1);       
		controls.add(widthInput, 1, 2, 1, 1); 
      	controls.add(minDuration, 0, 3, 1, 1); 
      	controls.add(maxDuration, 1, 3, 1, 1); 
      	controls.add(minDurationInput, 0, 4, 1, 1); 
      	controls.add(maxDurationInput, 1, 4, 1, 1);
      	controls.add(evPercent, 0, 5, 1, 1); 
      	controls.add(disabilityPercent, 1, 5, 1, 1); 
      	controls.add(evPercentInput, 0, 6, 1, 1); 
      	controls.add(disabilityPercentInput, 1, 6, 1, 1); 
      	controls.add(algorithmChoice, 0, 7, 1, 1);
      	controls.add(algorithmChoiceInput, 0, 8, 2, 1);
      	controls.add(go, 0, 9, 1, 1); 
      	controls.add(end, 1, 9, 1, 1); 
		
		return controls;
	}
	
	public GridPane simulationDisplay() {
		// Counts
		int length = Integer.parseInt(lengthInput.getText());
		int width = Integer.parseInt(widthInput.getText());
		
		// Setup Carpark
		carpark = new Button[length][width];
		
		// Create GridPane for display
		GridPane display = new GridPane();
		
		// Setup Pane
		display.setMinSize(700, 0); 
		display.setMaxSize(700, 300); 
		display.setAlignment(Pos.CENTER); 
		
		// Add Components to pane
		for (int y = 0; y < length; y++) {
			for (int x = 0; x < width; x++) {
				Button space = new Button("");
				carpark[y][x] = space;
				
				display.add(space, x, y);
			}
		}
		
		return display;
	}
	
	/**
	 * 
	 * Set the entrance to the carpark to be displayed
	 * 
	 * @param int yCoord [The Y coordinate of the entrance]
	 * @param int xCoord [The X coordinate of the entrance]
	 */
	public void setEntrance(int yCoord, int xCoord) {
		// Prevent out of bounds exception
		if (yCoord >= Integer.parseInt(lengthInput.getText())) {
			yCoord--;
		}
		
		// Prevent out of bounds exception
		if (xCoord >= Integer.parseInt(widthInput.getText())) {
			xCoord--;
		}
		
		// Get Space
		Button space = carpark[yCoord][xCoord];
		
		// Platform.runLater() is used to prevent thread conflicts
		Platform.runLater(new Runnable() {
			public void run() {
				// Setup Space
				space.setStyle("-fx-border-color: gold;"); // Change border to gold to highlight entrance on display
			}
		});
	}
	
	/**
	 * 
	 * Set the type of a parking bay to be displayed
	 * 
	 * @param String type [The character that represents the parking bay's type i.e. N, D or E]
	 * @param int y [The Y coordinate of the parking bay]
	 * @param int x [The X coordinate of the parking bay]
	 */
	public void setSpaceType(String type, int y, int x) {
		// Get Space
		Button space = carpark[y][x];
		
		// Platform.runLater() is used to prevent thread conflicts
		Platform.runLater(new Runnable() {
			public void run() {
				// Setup Space
				space.setText(type);
				space.setStyle(space.getStyle() + "-fx-background-color: #000000;");
				space.setTextFill(Color.GREEN);
			}
		});
	}
	
	/**
	 * 
	 * Update the type and availability of a parking bay to be displayed
	 * 
	 * @param String type [The character that represents the parking bay's type i.e. N, D or E]
	 * @param int y [The Y coordinate of the parking bay]
	 * @param int x [The X coordinate of the parking bay]
	 * @param boolean available [Whether or not the parking bay is available]
	 */
	public void updateSpace(String type, int y, int x, boolean available) {
		// Get Space
		Button space = carpark[y][x];
		
		// Platform.runLater() is used to prevent thread conflicts
		Platform.runLater(new Runnable() {
			public void run() {
				// Setup Space
				space.setText(type);
				
				if (available) {
					space.setTextFill(Color.GREEN);
				} else {
					space.setTextFill(Color.RED);
				}
			}
		});
	}
	
	// Create the graphs flow pane
	public FlowPane simulationGraphs() {
		// Create FlowPane Of Graphs
		FlowPane graphs = new FlowPane(queueLengthGraph, queueTimeGraph, carparkUtilisationGraph);
	    
		// Setup Pane
		graphs.setMinSize(1000, 200);
		
		return graphs;
	}
	
	/**
	 * 
	 * Create a line chart from supplied data
	 * Adapted from: https://www.tutorialspoint.com/how-to-add-multiple-linecharts-into-one-scene-stage-in-javafx
	 * Accessed: 26/08/2020
	 *
	 * @param String xLabel [The x axis label]
	 * @param String yLabel [The y axis label]
	 * @param XYChart.Series series [The series associated with the chart]
	 * @return LineChart<String, Number> chart [The chart to be returned]
	 */
	public LineChart<String, Number> createChart(String xLabel, String yLabel, XYChart.Series series) {
		// Create Axis
		CategoryAxis xAxis = new CategoryAxis();
		NumberAxis yAxis = new NumberAxis();
		
		// Setup Axis
	    xAxis.setLabel(xLabel);
	    xAxis.setAnimated(false);
	    yAxis.setLabel(yLabel);
	    yAxis.setAnimated(false);
	    
	    // Create Chart
	    LineChart<String, Number> chart = new LineChart<String, Number>(xAxis, yAxis);
	    
	    // Setup Chart
	    chart.setPrefSize(333, 200);
	    chart.getData().add(series);
	    chart.setLegendVisible(false);
	    chart.setAnimated(false);

	    return chart;
	}
	
	/**
	 * 
	 * Update line chart with supplied data
	 * Adapted from: https://levelup.gitconnected.com/realtime-charts-with-javafx-ed33c46b9c8d
	 * Accessed: 26/08/2020
	 * 
	 * @param String graph [The name of the graph to be updated]
	 * @param String x [The x value of the series to be added]
	 * @param double y [The y value of the series to be added]
	 */
	public void updateChart(String graph, String x, double y) {
		// Platform.runLater() is used to prevent thread conflicts
		Platform.runLater(new Runnable(){
			public void run() {
				XYChart.Series series;
				
				// Find the associated graph series
				switch(graph) {
					case "queue-length":
						series = queueLengthSeries;
						break;
					case "queue-duration":
						series = queueTimeSeries;
						break;
					case "carpark-utilisation":
						series = carparkUtilisationSeries;
						break;
					default:
						return;
				}
				
				// Update the chart
		        series.getData().add(new XYChart.Data<>(x, y));
		        
		        // If there are more than 10 points, remove the first point to keep size manageable 
		        if (series.getData().size() > 15) {
		            series.getData().remove(0);
		        }
			}
		});
	}
}