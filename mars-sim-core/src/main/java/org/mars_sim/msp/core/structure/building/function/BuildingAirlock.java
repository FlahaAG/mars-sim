/**
 * Mars Simulation Project
 * BuildingAirlock.java
 * @version 3.06 2014-03-02
 * @author Scott Davis
 */

package org.mars_sim.msp.core.structure.building.function;

import java.awt.geom.Point2D;
import java.util.logging.Logger;

import org.mars_sim.msp.core.Airlock;
import org.mars_sim.msp.core.Inventory;
import org.mars_sim.msp.core.LocalAreaUtil;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.BuildingManager;

/** 
 * The BuildingAirlock class represents an airlock for a building.
 */
public class BuildingAirlock extends Airlock {

    /** default serial id. */
    private static final long serialVersionUID = 1L;
    
    private static Logger logger = Logger.getLogger(BuildingAirlock.class.getName());

    // Data members.
    private Building building; // The building this airlock is for.
    private Point2D airlockInsidePos;
    private Point2D airlockInteriorPos;
    private Point2D airlockExteriorPos;

    /**
     * Constructor
     * 
     * @param building the building this airlock of for.
     * @param capacity number of people airlock can hold.
     * @throws IllegalArgumentException if building is not valid or if 
     * capacity is less than one.
     */
    public BuildingAirlock(Building building, int capacity) {
        // User Airlock constructor
        super(capacity);

        this.building = building;
        
        if (building == null) {
            throw new IllegalArgumentException("building is null.");
        }
        
        // Determine airlock interior position.
        // TODO Replace with configured airlock interior position.
        Point2D relativeInteriorPos = LocalAreaUtil.getRandomInteriorLocation(building);
        airlockInteriorPos = LocalAreaUtil.getLocalRelativeLocation(relativeInteriorPos.getX(), 
                relativeInteriorPos.getY(), building);
        
        // Determine airlock exterior position.
        // TODO Replace with configured airlock exterior position.
        Point2D relativeExteriorPos = LocalAreaUtil.getRandomExteriorLocation(building, 1D);
        airlockExteriorPos = LocalAreaUtil.getLocalRelativeLocation(relativeExteriorPos.getX(), 
                relativeExteriorPos.getY(), building);
        
        // Determine airlock inside position.
        // TODO Replace with configured airlock inside position.
        Point2D relativeAirlockInsidePos = LocalAreaUtil.getRandomInteriorLocation(building);
        airlockInsidePos = LocalAreaUtil.getLocalRelativeLocation(relativeAirlockInsidePos.getX(), 
                relativeAirlockInsidePos.getY(), building);
    }

    /**
     * Enters a person into the airlock from either the inside or the outside.
     * Inner or outer door (respectively) must be open for person to enter.
     * @param person the person to enter the airlock
     * @param inside true if person is entering from inside
     *               false if person is entering from outside
     * @return true if person entered the airlock successfully
     */
    public boolean enterAirlock(Person person, boolean inside) {
        boolean result = super.enterAirlock(person, inside);

        // Check if person is entering airlock from inside.
        if (result && inside) {
            // Add person to the building.
            BuildingManager.addPersonToBuildingSameLocation(person, building);
        }

        return result;
    }         

    /**
     * Causes a person within the airlock to exit either inside or outside.
     *
     * @param person the person to exit.
     * @throws Exception if person is not in the airlock.
     */
    protected void exitAirlock(Person person) {
        Inventory inv = building.getInventory();

        if (inAirlock(person)) {

            if (PRESSURIZED.equals(getState())) {
                // Exit person to inside building.
                BuildingManager.addPersonToBuildingSameLocation(person, building);
                inv.storeUnit(person);
            }
            else if (DEPRESSURIZED.equals(getState())) {
                // Exit person to outside building.
                BuildingManager.removePersonFromBuilding(person, building);
                inv.retrieveUnit(person);
            }
            else {
                logger.severe("Building airlock in incorrect state for exiting: " + getState());
            }
        }
        else throw new IllegalStateException(person.getName() + " not in airlock of " + getEntityName());
    }

    /**
     * Gets the name of the entity this airlock is attached to.
     *
     * @return name
     */
    public String getEntityName() {
        Settlement settlement = building.getBuildingManager().getSettlement();
        return settlement.getName() + ": " + building.getName();
    }

    /**
     * Gets the inventory of the entity this airlock is attached to.
     *
     * @return inventory
     */
    public Inventory getEntityInventory() {
        return building.getInventory();
    }
    
    @Override
    public Object getEntity() {
        return building;
    }

    @Override
    public Point2D getAvailableInteriorPosition() {
        return airlockInteriorPos;
    }

    @Override
    public Point2D getAvailableExteriorPosition() {
        return airlockExteriorPos;
    }

    @Override
    public Point2D getAvailableAirlockPosition() {
        return airlockInsidePos;
    }
}