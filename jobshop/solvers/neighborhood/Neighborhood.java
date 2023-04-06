package jobshop.solvers.neighborhood;

import jobshop.encodings.ResourceOrder;

import java.util.List;

/** For a particular solution in the ResourceOrder encoding, a neighborhood allows the generation of its neighbors:
 * a set of closely related solutions.
 */
public abstract class Neighborhood {

    /** Generates all neighbors for the current solution.  */
    public abstract List<ResourceOrder> generateNeighbors(ResourceOrder current);

}
