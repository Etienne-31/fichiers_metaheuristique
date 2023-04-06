package jobshop.encodings;

import jobshop.Instance;

import java.util.Optional;

/** Common class for all encodings.
 *
 * The only requirement for this class is to provide a conversion from the encoding into a Schedule.
 */
public abstract class Encoding {

    /** Problem instance of which this is the solution. */
    public final Instance instance;

    /** Constructor, that initializes the instance field. */
    public Encoding(Instance instance) {
        this.instance = instance;
    }

    /** Attempts to convert this solution into a schedule.
     *
     * @return A empty optional if the solution is not valid. Otherwise the optional will contain a valid schedule of
     *         the solution.
     */
    public abstract Optional<Schedule> toSchedule();
}
