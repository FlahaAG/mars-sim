/**
 * Mars Simulation Project
 * Botanist.java
 * @version 2.76 2004-06-10
 * @author Scott Davis
 */
package org.mars_sim.msp.simulation.person.ai.job;

import java.io.Serializable;
import java.util.*;
import org.mars_sim.msp.simulation.person.*;
import org.mars_sim.msp.simulation.person.ai.mission.*;
import org.mars_sim.msp.simulation.person.ai.task.*;
import org.mars_sim.msp.simulation.structure.Settlement;
import org.mars_sim.msp.simulation.structure.building.*;
import org.mars_sim.msp.simulation.structure.building.function.*;

/** 
 * The Botanist class represents a job for a botanist.
 */
public class Botanist extends Job implements Serializable {

	/**
	 * Constructor
	 */
	public Botanist() {
		// Use Job constructor
		super("Botanist");
		
		// Add botany-related tasks.
		jobTasks.add(ResearchBotany.class);
		jobTasks.add(TendGreenhouse.class);
		
		// Add botanist-related missions.
		jobMissionStarts.add(TravelToSettlement.class);
		jobMissionJoins.add(TravelToSettlement.class);		
	}

	/**
	 * Gets a person's capability to perform this job.
	 * @param person the person to check.
	 * @return capability (min 0.0).
	 */
	public double getCapability(Person person) {
		
		double result = 0D;
		
		int botanySkill = person.getSkillManager().getSkillLevel(Skill.BOTANY);
		result = botanySkill;
		
		NaturalAttributeManager attributes = person.getNaturalAttributeManager();
		int academicAptitude = attributes.getAttribute("Academic Aptitude");
		int experienceAptitude = attributes.getAttribute("Experience Aptitude");
		double averageAptitude = (academicAptitude + experienceAptitude) / 2D;
		result+= result * ((averageAptitude - 50D) / 100D);
		
		return result;
	}
	
	/**
	 * Gets the base settlement need for this job.
	 * @param settlement the settlement in need.
	 * @return the base need >= 0
	 */
	public double getSettlementNeed(Settlement settlement) {
		double result = 0D;
		
		// Add (labspace * tech level) for all labs with botany specialities.
		List laboratoryBuildings = settlement.getBuildingManager().getBuildings(Research.NAME);
		Iterator i = laboratoryBuildings.iterator();
		while (i.hasNext()) {
			Building building = (Building) i.next();
			try {
				Research lab = (Research) building.getFunction(Research.NAME);
				if (lab.hasSpeciality(Skill.BOTANY)) 
					result += (lab.getResearcherNum() * lab.getTechnologyLevel());
			}
			catch (BuildingException e) {
				System.err.println("Botanist.getSettlementNeed(): " + e.getMessage());
			}
		}
		
		// Add (growing area in greenhouses) / 15
		List greenhouseBuildings = settlement.getBuildingManager().getBuildings(Farming.NAME);
		Iterator j = greenhouseBuildings.iterator();
		while (j.hasNext()) {
			Building building = (Building) j.next();
			try {
				Farming farm = (Farming) building.getFunction(Farming.NAME);
				result += (farm.getGrowingArea() / 15D);
			}
			catch (BuildingException e) {
				System.err.println("Botanist.getSetltementNeed(): " + e.getMessage());
			}
		}
		
		return result;	
	}	
}