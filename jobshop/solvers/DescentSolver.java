package jobshop.solvers;

import jobshop.Instance;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Schedule;
import jobshop.solvers.neighborhood.Neighborhood;
import jobshop.solvers.neighborhood.Nowicki;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** An empty shell to implement a descent solver. */
public class DescentSolver implements Solver {

    final Neighborhood neighborhood;
    final Solver baseSolver;

    /** Creates a new descent solver with a given neighborhood and a solver for the initial solution.
     *
     * @param neighborhood Neighborhood object that should be used to generates neighbor solutions to the current candidate.
     * @param baseSolver A solver to provide the initial solution.
     */
    public DescentSolver(Neighborhood neighborhood, Solver baseSolver) {
        this.neighborhood = neighborhood;
        this.baseSolver = baseSolver;
    }

    @Override
    public Optional<Schedule> solve(Instance instance, long deadline) {

        Optional<Schedule> return_schedule = this.baseSolver.solve(instance,deadline);
        Nowicki now = new Nowicki();

        ResourceOrder RO = new ResourceOrder(return_schedule.get());

        List<ResourceOrder> list_RO ;

        ResourceOrder best_RO = RO;
        int best_makespan = best_RO.toSchedule().get().makespan();


        long exec_time = 0;
        boolean recherche = true;
        ResourceOrder previousRO;

        while((exec_time < deadline)&&(recherche)){
            previousRO = best_RO;
            list_RO = now.generateNeighbors(best_RO);
            for(ResourceOrder RO_Iterator : list_RO){
                //verifier si RO est valide
                // verifier RO_Iterator.toSchedule().ispresent()/empty() check doc optional

                if((RO_Iterator.toSchedule().isPresent())&&(RO_Iterator.toSchedule().get().isValid())){
                    int makespan_iterator = RO_Iterator.toSchedule().get().makespan();
                    if(makespan_iterator < best_makespan){
                        best_makespan = makespan_iterator;
                        best_RO = RO_Iterator;
                    }
                }


            }

            if(previousRO == best_RO){
                recherche = false;
            }
            exec_time = System.currentTimeMillis();
        }

        return best_RO.toSchedule();
    }

}
