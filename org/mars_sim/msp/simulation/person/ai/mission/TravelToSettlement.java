/**
 * Mars Simulation Project
 * TravelToSettlement.java
 * @version 2.80 2006-09-08
 * @author Scott Davis
 */

package org.mars_sim.msp.simulation.person.ai.mission;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.mars_sim.msp.simulation.RandomUtil;
import org.mars_sim.msp.simulation.Simulation;
import org.mars_sim.msp.simulation.UnitManager;
import org.mars_sim.msp.simulation.person.Person;
import org.mars_sim.msp.simulation.person.PersonCollection;
import org.mars_sim.msp.simulation.person.PersonIterator;
import org.mars_sim.msp.simulation.person.ai.job.Driver;
import org.mars_sim.msp.simulation.person.ai.job.Job;
import org.mars_sim.msp.simulation.person.ai.job.JobManager;
import org.mars_sim.msp.simulation.person.ai.social.RelationshipManager;
import org.mars_sim.msp.simulation.structure.Settlement;
import org.mars_sim.msp.simulation.structure.SettlementCollection;
import org.mars_sim.msp.simulation.structure.SettlementIterator;
import org.mars_sim.msp.simulation.vehicle.Rover;
import org.mars_sim.msp.simulation.vehicle.Vehicle;

/** 
 * The TravelToSettlement class is a mission to travel from one settlement 
 * to another randomly selected one within range of an available rover.   
 */
public class TravelToSettlement extends RoverMission implements Serializable {
	
	// Mission event types
	public static final String DESTINATION_SETTLEMENT = "destination settlement";
	
	// Static members
	private static final double BASE_MISSION_WEIGHT = 1D;
	private static final double RELATIONSHIP_MODIFIER = 10D;
	private static final double JOB_MODIFIER = 1D;
	private static final double CROWDING_MODIFIER = 5D;
	private static final double RANGE_BUFFER = .8D;
	
    // Data members
    private Settlement destinationSettlement;

    /** 
     * Constructs a TravelToSettlement object with destination settlement
     * randomly determined.
     * @param startingPerson the person starting the mission.
     * @throws MissionException if error constructing mission.
     */
    public TravelToSettlement(Person startingPerson) throws MissionException {
    	// Use RoverMission constructor
        super("Travel To Settlement", startingPerson);
        
        if (!isDone()) {
        	
        	// Initialize data members
        	setStartingSettlement(startingPerson.getSettlement());

        	// Set mission capacity.
        	if (hasVehicle()) setMissionCapacity(getRover().getCrewCapacity());
        	
        	// Choose destination settlement.
        	setDestinationSettlement(getRandomDestinationSettlement(startingPerson, getStartingSettlement()));
        	if (getDestinationSettlement() != null) { 
        		addNavpoint(new NavPoint(getDestinationSettlement().getCoordinates(), getDestinationSettlement(), 
        				getDestinationSettlement().getName()));
        		setDescription("Travel To " + getDestinationSettlement().getName());
        	}
        	else endMission();
        	
        	// Recruit additional people to mission.
        	if (!isDone()) recruitPeopleForMission(startingPerson);
        	
        	// Check if vehicle can carry enough supplies for the mission.
        	try {
        		if (hasVehicle() && !isVehicleLoadable()) endMission();
        	}
        	catch (Exception e) {
        		throw new MissionException(null, e);
        	}
        }
        
        // Set initial phase
        setPhase(VehicleMission.EMBARKING);
        setPhaseDescription("Embarking from " + getStartingSettlement().getName());
        
        // System.out.println("Travel to Settlement mission");
    }

    /** 
     * Gets the weighted probability that a given person would start this mission.
     * @param person the given person
     * @return the weighted probability
     */
    public static double getNewMissionProbability(Person person) {

    	double missionProbability = 0D;
    	
        if (person.getLocationSituation().equals(Person.INSETTLEMENT)) {
        	
        	// Check if mission is possible for person based on their circumstance.
        	boolean missionPossible = true;
            Settlement settlement = person.getSettlement();
	    
	    	// Check if available rover.
	    	if (!areVehiclesAvailable(settlement)) missionPossible = false;
            
			// Check if minimum number of people are available at the settlement.
			// Plus one to hold down the fort.
			if (!minAvailablePeopleAtSettlement(settlement, (MIN_PEOPLE + 1))) missionPossible = false;
	    	
	    	// Check if there are any desirable settlements within range.
	    	try {
	    		Vehicle vehicle = getVehicleWithGreatestRange(settlement);
	    		if (vehicle != null) {
	    			Map desirableSettlements = getDestinationSettlements(person, settlement, vehicle.getRange());
	    			if (desirableSettlements.size() == 0) missionPossible = false;
	    		}
	    	}
	    	catch (Exception e) {
	    		System.err.println("Error finding vehicles at settlement.");
	    		e.printStackTrace(System.err);
	    	}
	    	
	    	// Determine mission probability.
	        if (missionPossible) {
	        	missionProbability = BASE_MISSION_WEIGHT;
	            
	            // Crowding modifier.
	            int crowding = settlement.getCurrentPopulationNum() - settlement.getPopulationCapacity();
	            if (crowding > 0) missionProbability *= (crowding + 1);
	        	
	    		// Job modifier.
	        	Job job = person.getMind().getJob();
	        	if (job != null) missionProbability *= job.getStartMissionProbabilityModifier(TravelToSettlement.class);	
	        }
        }

        return missionProbability;
    }
    
    /**
     * Determines a new phase for the mission when the current phase has ended.
     * @throws MissionException if problem setting a new phase.
     */
    protected void determineNewPhase() throws MissionException {
    	if (EMBARKING.equals(getPhase())) {
    		startTravelToNextNode();
    		setPhase(VehicleMission.TRAVELLING);
    		setPhaseDescription("Driving to " + getNextNavpoint().getDescription());
    		associateAllMembersWithSettlement(destinationSettlement);
    	}
		else if (TRAVELLING.equals(getPhase())) {
			if (getCurrentNavpoint().isSettlementAtNavpoint()) {
				setPhase(VehicleMission.DISEMBARKING);
				setPhaseDescription("Disembarking at " + getCurrentNavpoint().getDescription());
			}
		}
		else if (DISEMBARKING.equals(getPhase())) endMission();
    }
    
    /**
     * Sets the destination settlement.
     * @param destinationSettlement the new destination settlement.
     */
    private void setDestinationSettlement(Settlement destinationSettlement) {
    	this.destinationSettlement = destinationSettlement;
    	fireMissionUpdate(DESTINATION_SETTLEMENT);
    }
    
    /**
     * Gets the destination settlement.
     * @return destination settlement
     */
    public final Settlement getDestinationSettlement() {
    	return destinationSettlement;
    }

    /** 
     * Determines a random destination settlement other than current one.
     * @param person the person searching for a settlement.
     * @param startingSettlement the settlement the mission is starting at.
     * @return randomly determined settlement
     * @throws MissionException if problem determining destination settlement.
     */
    private Settlement getRandomDestinationSettlement(Person person, Settlement startingSettlement) throws MissionException {
    	
    	try {
    		double range = getVehicle().getRange();
    		Settlement result = null;
        
    		// Find all desirable destination settlements.
    		Map desirableSettlements = getDestinationSettlements(person, startingSettlement, range);
        
    		// Randomly select a desirable settlement.
    		if (desirableSettlements.size() > 0) result = (Settlement) RandomUtil.getWeightedRandomObject(desirableSettlements);
    
    		return result;
    	}
    	catch (Exception e) {
    		throw new MissionException(VehicleMission.EMBARKING, e);
    	}
    }
    
    /**
     * Gets all possible and desirable destination settlements.
     * @param person the person searching for a settlement.
     * @param startingSettlement the settlement the mission is starting at.
     * @param range the range (km) that can be travelled.
     * @return map of destination settlements.
     */
    private static Map getDestinationSettlements(Person person, Settlement startingSettlement, double range) {
    	Map result = new HashMap();
    	
    	UnitManager unitManager = startingSettlement.getUnitManager();
    	SettlementIterator i = new SettlementCollection(unitManager.getSettlements()).iterator();
		while (i.hasNext()) {
			Settlement settlement = i.next();
			double distance = startingSettlement.getCoordinates().getDistance(settlement.getCoordinates());
			if ((startingSettlement != settlement) && (distance <= (range * RANGE_BUFFER))) {
				double desirability = getDestinationSettlementDesirability(person, startingSettlement, settlement);
				if (desirability > 0D) result.put(settlement, new Double(desirability));
			}
		}
    	
    	return result;
    }
	
	/**
	 * Gets the desirability of the destination settlement.
	 * @param person the person looking at the settlement.
	 * @param startingSettlement the settlement the person is already at.
	 * @param destinationSettlement the new settlement.
	 * @return negative or positive desirability weight value.
	 */
	private static double getDestinationSettlementDesirability(Person person, Settlement startingSettlement, 
			Settlement destinationSettlement) {
			
		// Determine relationship factor in destination settlement relative to starting settlement.
		RelationshipManager relationshipManager = Simulation.instance().getRelationshipManager();
		double currentOpinion = relationshipManager.getAverageOpinionOfPeople(person, 
				startingSettlement.getAllAssociatedPeople());
		double destinationOpinion = relationshipManager.getAverageOpinionOfPeople(person, 
				destinationSettlement.getAllAssociatedPeople());
		double relationshipFactor = (destinationOpinion - currentOpinion) / 100D;
			
		// Determine job opportunities in destination settlement relative to starting settlement.
		Job currentJob = person.getMind().getJob();
		double currentJobProspect = JobManager.getJobProspect(person, currentJob, startingSettlement, true);
		double destinationJobProspect = 0D;
		if (person.getMind().getJobLock()) 
			destinationJobProspect = JobManager.getJobProspect(person, currentJob, destinationSettlement, false);
		else destinationJobProspect = JobManager.getBestJobProspect(person, destinationSettlement, false);
		double jobFactor = 0D;
		if (destinationJobProspect > currentJobProspect) jobFactor = 1D;
		else if (destinationJobProspect < currentJobProspect) jobFactor = -1D;
			
		// Determine available space in destination settlement relative to starting settlement.
		int startingCrowding = startingSettlement.getPopulationCapacity() - 
				startingSettlement.getAllAssociatedPeople().size() - 1;
		int destinationCrowding = destinationSettlement.getPopulationCapacity() - 
				destinationSettlement.getAllAssociatedPeople().size();
		double crowdingFactor = destinationCrowding - startingCrowding;
		
		// Return the sum of the factors with modifiers.
		return (relationshipFactor * RELATIONSHIP_MODIFIER) + (jobFactor * JOB_MODIFIER) + (crowdingFactor * CROWDING_MODIFIER);
	}
	
	/**
	 * Checks to see if a person is capable of joining a mission.
	 * @param person the person to check.
	 * @return true if person could join mission.
	 */
	protected boolean isCapableOfMission(Person person) {
		if (super.isCapableOfMission(person)) {
			if (person.getLocationSituation().equals(Person.INSETTLEMENT)) {
				if (person.getSettlement() == getStartingSettlement()) return true;
			}
		}
		return false;
	}
	
	/**
	 * Gets the mission qualification value for the person.
	 * Person is qualified and interested in joining the mission if the value is larger than 0.
	 * The larger the qualification value, the more likely the person will be picked for the mission.
	 * Qualification values of zero or negative will not join missions.
	 * @param person the person to check.
	 * @return mission qualification value.
	 * @throws MissionException if problem finding mission qualification.
	 */
	protected double getMissionQualification(Person person) throws MissionException {
		double result = 0D;
		
		if (isCapableOfMission(person)) {
			result = super.getMissionQualification(person);
			RelationshipManager relationshipManager = Simulation.instance().getRelationshipManager();
			
			// Add modifier for average relationship with inhabitants of destination settlement.
			if (getDestinationSettlement() != null) {
				PersonCollection destinationInhabitants = getDestinationSettlement().getAllAssociatedPeople();
				double destinationSocialModifier = (relationshipManager.getAverageOpinionOfPeople(person, 
						destinationInhabitants) - 50D) / 50D;
				result += destinationSocialModifier;
			}
			
			// Subtract modifier for average relationship with non-mission inhabitants of starting settlement.
			if (getStartingSettlement() != null) {
				PersonCollection startingInhabitants = getStartingSettlement().getAllAssociatedPeople();
				PersonIterator i = startingInhabitants.iterator();
				while (i.hasNext()) {
					if (hasPerson(i.next())) i.remove();
				}
				double startingSocialModifier = (relationshipManager.getAverageOpinionOfPeople(person, 
						startingInhabitants) - 50D) / 50D;
				result -= startingSocialModifier;
			}
			
			// If person has the "Driver" job, add 1 to their qualification.
			if (person.getMind().getJob() instanceof Driver) result += 1D;
		}
		
		return result;
	}
	
	/**
	 * Recruits new people into the mission.
	 * @param startingPerson the person starting the mission.
	 */
	protected void recruitPeopleForMission(Person startingPerson) {
		super.recruitPeopleForMission(startingPerson);
		
		// Make sure there is at least one person left at the starting settlement.
		if (!atLeastOnePersonRemainingAtSettlement(getStartingSettlement(), startingPerson)) {
			// Remove last person added to the mission.
			Person lastPerson = (Person) getPeople().get(getPeopleNumber() - 1);
			if (lastPerson != null) {
				lastPerson.getMind().setMission(null);
				if (getPeopleNumber() < getMinPeople()) endMission();
			}
		}
	}
	
	/**
	 * Gets the settlement associated with the mission.
	 * @return settlement or null if none.
	 */
	public Settlement getAssociatedSettlement() {
		return getDestinationSettlement();
	}
	
    /**
     * Gets the number and types of equipment needed for the mission.
     * @param useBuffer use time buffer in estimation if true.
     * @return map of equipment class and Integer number.
     * @throws Exception if error determining needed equipment.
     */
    public Map getEquipmentNeededForRemainingMission(boolean useBuffer) throws Exception {
    	if (equipmentNeededCache != null) return equipmentNeededCache;
    	else {
    		Map result = super.getEquipmentNeededForRemainingMission(useBuffer);
    		equipmentNeededCache = result;
    		return result;
    	}
    }
    
	/**
	 * Compares the quality of two vehicles for use in this mission.
	 * (This method should be added to by children)
	 * @param firstVehicle the first vehicle to compare
	 * @param secondVehicle the second vehicle to compare
	 * @return -1 if the second vehicle is better than the first vehicle, 
	 * 0 if vehicle are equal in quality,
	 * and 1 if the first vehicle is better than the second vehicle.
	 * @throws IllegalArgumentException if firstVehicle or secondVehicle is null.
	 * @throws Exception if error comparing vehicles.
	 */
	protected int compareVehicles(Vehicle firstVehicle, Vehicle secondVehicle) throws Exception {
		int result = super.compareVehicles(firstVehicle, secondVehicle);
		
		if ((result == 0) && isUsableVehicle(firstVehicle) && isUsableVehicle(secondVehicle)) {
			// Check if one can hold more crew than the other.
			if (((Rover) firstVehicle).getCrewCapacity() > ((Rover) secondVehicle).getCrewCapacity()) result = 1;
			else if (((Rover) firstVehicle).getCrewCapacity() < ((Rover) secondVehicle).getCrewCapacity()) result = -1;
				
			// Vehicle with superior range should be ranked higher.
			if (result == 0) {
				if (firstVehicle.getRange() > secondVehicle.getRange()) result = 1;
				else if (firstVehicle.getRange() < secondVehicle.getRange()) result = -1;
			}
		}
		
		return result;
	}
}