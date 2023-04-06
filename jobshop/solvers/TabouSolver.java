package jobshop.solvers;

import jobshop.Instance;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Schedule;
import jobshop.encodings.Task;
import jobshop.solvers.neighborhood.Neighborhood;
import jobshop.solvers.neighborhood.Nowicki;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.LinkedList;

public class TabouSolver implements Solver{

    final Neighborhood neighborhood;
    final Solver baseSolver;

    /** Creates a new descent solver with a given neighborhood and a solver for the initial solution.
     *
     * @param neighborhood Neighborhood object that should be used to generates neighbor solutions to the current candidate.
     * @param baseSolver A solver to provide the initial solution.
     */
    public TabouSolver(Neighborhood neighborhood, Solver baseSolver) {
        this.neighborhood = neighborhood;
        this.baseSolver = baseSolver;
    }


        @Override
        public Optional<Schedule> solve(Instance instance,long deadline){

            Optional<Schedule> return_schedule = this.baseSolver.solve(instance, deadline);
            Nowicki now = new Nowicki();
            LinkedList<Nowicki.Swap> listTaboo = new LinkedList<
                    Nowicki.Swap>();
            ResourceOrder RO = new ResourceOrder(return_schedule.get());

            List<ResourceOrder> list_RO;


            ResourceOrder best_RO = RO;
            int best_makespan = best_RO.toSchedule().get().makespan();



            long exec_time = 0;
            boolean recherche = true;
            ResourceOrder previousRO;

            while ((exec_time < deadline) && (recherche)) {
                previousRO = best_RO;
                List<Nowicki.Swap> listSwapRO = now.allSwaps(best_RO);
                ArrayList<Nowicki.Swap> nonTabooSwaps = new ArrayList<Nowicki.Swap>();
                Nowicki.Swap best_Swap = null;


                for(Nowicki.Swap ite : listSwapRO){

                    boolean isTaboo = false;

                    for(Nowicki.Swap BestIterator : listTaboo){
                        if(ite.equals(BestIterator)){
                            isTaboo = true;
                            break;
                        }
                    }
                    if (!isTaboo){
                        nonTabooSwaps.add(ite);
                    }
                }



                for(Nowicki.Swap swapIterator : nonTabooSwaps){
                    ResourceOrder RO_Iterator = swapIterator.generateFrom(best_RO);

                    if ((RO_Iterator.toSchedule().isPresent()) && (RO_Iterator.toSchedule().get().isValid())) {
                        int makespan_iterator = RO_Iterator.toSchedule().get().makespan();
                        if (makespan_iterator < best_makespan) {
                            best_makespan = makespan_iterator;
                            best_RO = RO_Iterator;
                            best_Swap = swapIterator;
                        }
                    }
                }

                if(best_Swap != null){

                    listTaboo.addLast(best_Swap);
                    if(listTaboo.size() > 10 ){
                        listTaboo.removeFirst();
                    }
                }



                if (previousRO == best_RO) {
                    recherche = false;
                }
                exec_time = System.currentTimeMillis();
            }

            return best_RO.toSchedule();
        }



    }
