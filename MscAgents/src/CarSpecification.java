/**
 * 
 * Serialized class to be able to send parameters within agent communication.
 * Specifically to send car specifications to parking bay for smart allocation and analytics.
 *
 */

class CarSpecification implements java.io.Serializable  {
     private String numberPlate; 
     private String type;
     private double mpg;
     private double carbonEmissions;
     
     // Return the car's number plate
     public String getNumberPlate() {
    	 return numberPlate;
     }
     
     // Return the cars type
     public String getType() {
    	 return type;
     }
     
     // Return the cars miles per gallon
     public double getMpg() {
    	 return mpg;
     }
     
     // Return the cars carbon emissions per kilometre
     public double getCarbonEmissions() {
    	 return carbonEmissions;
     }
     
     /**
      * 
      * Set the car's number plate
      * 
      * @param String numberPlate [The number plate of the car]
      */
     public void setNumberPlate(String numberPlate) {
    	 this.numberPlate = numberPlate;
     }

     /**
      * 
      * Set the car's type
      * 
      * @param String type [Character representing the car's type i.e. N, D, E]
      */
     public void setType(String type) {
         this.type = type;
     }
     
     /**
      * 
      * Set the car's miles per gallon.
      * 
      * @param double mpg [The miles the car can travel per gallon of fuel]
      */
     public void setMpg(double mpg) {
    	 this.mpg = mpg;
     }
     
     /**
      * 
      * Set the car's carbon emissions.
      * 
      * @param double carbonEmissions [The carbon emissions produced per kilometre]
      */
     public void setCarbonEmissions(double carbonEmissions) {
    	 this.carbonEmissions = carbonEmissions;
     }
} 