/**
 * Mars Simulation Project
 * ExitVehicle.java
 * @version 2.74 2002-01-29
 * @author Scott Davis
 */

package org.mars_sim.msp.simulation.person.ai;

import org.mars_sim.msp.simulation.*;
import org.mars_sim.msp.simulation.person.*;
import org.mars_sim.msp.simulation.structure.*;
import org.mars_sim.msp.simulation.vehicle.*;
import java.io.Serializable;

/** The ExitVehicle class is a task for exiting a vehicle.
 *  The duration of the task is 20 millisols.
 */
class ExitVehicle extends Task implements Serializable {

    // Data members
    private double duration = 20D; // The predetermined duration of task in millisols
    private Settlement destinationSettlement; // The settlement being exited to.
    private Vehicle vehicle; // The vehicle to be exited.

    /** Constructs a ExitVehicle object
     *  @param person the person to perform the task
     *  @param mars the virtual Mars
     *  @param destinationSettlement the settlement being exited to
     */
    public ExitVehicle(Person person, VirtualMars mars, Vehicle vehicle, Settlement destinationSettlement) {
        super("Exiting " + vehicle.getName(), person, false, mars);

        // Initialize data members
        this.vehicle = vehicle;
        this.destinationSettlement = destinationSettlement;
    }

    /** Performs this task for the given amount of time.
     *  @param time the amount of time to perform this task (in millisols)
     *  @return amount of time remaining after finishing with task (in millisols)
     */
    double performTask(double time) {
        double timeLeft = super.performTask(time);
        if (subTask != null) return timeLeft;

        timeCompleted += time;
        if (timeCompleted > duration) {
            person.setLocationSituation(Person.INSETTLEMENT);
	    vehicle.getInventory().takeUnit(person, destinationSettlement);
            done = true;
            return timeCompleted - duration;
        }
        else return 0;
    }
}
