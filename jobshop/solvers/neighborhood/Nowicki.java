package jobshop.solvers.neighborhood;

import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import java.util.HashMap;

/** Implementation of the Nowicki and Smutnicki neighborhood.
 *
 * It works on the ResourceOrder encoding by generating two neighbors for each block
 * of the critical path.
 * For each block, two neighbors should be generated that respectively swap the first two and
 * last two tasks of the block.
 */
public class Nowicki extends Neighborhood {

    /** A block represents a subsequence of the critical path such that all tasks in it execute on the same machine.
     * This class identifies a block in a ResourceOrder representation.
     *
     * Consider the solution in ResourceOrder representation
     * machine 0 : (0,1) (1,2) (2,2)
     * machine 1 : (0,2) (2,1) (1,1)
     * machine 2 : ...
     *
     * The block with : machine = 1, firstTask= 0 and lastTask = 1
     * Represent the task sequence : [(0,2) (2,1)]
     *
     * */
    public static class Block {
        /** machine on which the block is identified */
        public final int machine;
        /** index of the first task of the block */
        public  int firstTask;
        /** index of the last task of the block */
        public  int lastTask;

        /** Creates a new block. */
        Block(int machine, int firstTask, int lastTask) {
            this.machine = machine;
            this.firstTask = firstTask;
            this.lastTask = lastTask;
        }

        public int getFirstTask(){return this.firstTask;}
        public int getLastTask(){return this.lastTask;}

        public void setFirstTask(int indexFirstTask){
            this.firstTask = indexFirstTask;
        }

        public void setLastTask(int indexLastTask){
            this.lastTask = indexLastTask;
        }

        public void printBlock(){
            System.out.println("Block pour machine :"+this.machine+ " index First Task : "+firstTask+ " index Last Task"+ lastTask);
        }
    }

    /**
     * Represents a swap of two tasks on the same machine in a ResourceOrder encoding.
     *
     * Consider the solution in ResourceOrder representation
     * machine 0 : (0,1) (1,2) (2,2)
     * machine 1 : (1,1) (0,2) (2,1)
     * machine 2 : ...
     *
     * The swap with : machine = 1, t1= 0 and t2 = 1
     * Represent inversion of the two tasks : (0,2) and (2,1)
     * Applying this swap on the above resource order should result in the following one :
     * machine 0 : (0,1) (1,2) (2,2)
     * machine 1 : (2,1) (0,2) (1,1)
     * machine 2 : ...
     */
    public static class Swap {
        /** machine on which to perform the swap */
        public final int machine;

        /** index of one task to be swapped (in the resource order encoding).
         * t1 should appear earlier than t2 in the resource order. */
        public final int t1;

        /** index of the other task to be swapped (in the resource order encoding) */
        public final int t2;

        /** Creates a new swap of two tasks. */
        Swap(int machine, int t1, int t2) {
            this.machine = machine;
            if (t1 < t2) {
                this.t1 = t1;
                this.t2 = t2;
            } else {
                this.t1 = t2;
                this.t2 = t1;
            }
        }


        /** Creates a new ResourceOrder order that is the result of performing the swap in the original ResourceOrder.
         *  The original ResourceOrder MUST NOT be modified by this operation.
         */
        public ResourceOrder generateFrom(ResourceOrder original) {
            ResourceOrder new_RO = original.copy();
//            Task task_t1 = new_RO.getTaskFromMatrix(machine,t1);
//            Task task_t2 = new_RO.getTaskFromMatrix(machine,t2);

            new_RO.swapTasks(machine,t1,t2);
//            new_RO.setTaskinMatrix(machine,t2,task_t1);

            return new_RO;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Swap swap = (Swap) o;
            return machine == swap.machine && t1 == swap.t1 && t2 == swap.t2;
        }

        @Override
        public int hashCode() {
            return Objects.hash(machine, t1, t2);
        }
    }


    @Override
    public List<ResourceOrder> generateNeighbors(ResourceOrder current) {
        // convert the list of swaps into a list of neighbors (function programming FTW)
        return allSwaps(current).stream().map(swap -> swap.generateFrom(current)).collect(Collectors.toList());

    }

    /** Generates all swaps of the given ResourceOrder.
     * This method can be used if one wants to access the inner fields of a neighbors. */
    public List<Swap> allSwaps(ResourceOrder current) {
        List<Swap> neighbors = new ArrayList<>();
        // iterate over all blocks of the critical path
        for(var block : blocksOfCriticalPath(current)) {
            // for this block, compute all neighbors and add them to the list of neighbors
            neighbors.addAll(neighbors(block));
        }
        return neighbors;
    }

    /** Returns a list of all the blocks of the critical path. */
    public static List<Block> blocksOfCriticalPath(ResourceOrder order) {

        ArrayList<Block> list_blocks = new ArrayList<Block>();
        List<Task> criticalPath = order.toSchedule().get().criticalPath();
        boolean block_is_created = false;
        Block block_a_ajouter = null;
        int previousNumMachine = -1;
        int previousIndexonMachine = -1;

        for(Task task_iterator : criticalPath){
           int num_machine_task = order.instance.machine(task_iterator);

            if(num_machine_task == previousNumMachine){

                if(block_is_created){
                    block_a_ajouter.setLastTask(order.getIndexOnMachine(task_iterator,num_machine_task));
                }
                else{
                    block_a_ajouter = new Block(num_machine_task,previousIndexonMachine,order.getIndexOnMachine(task_iterator,num_machine_task));
                    block_is_created = true;
                }
            }
            else{

                if(block_is_created){
                    list_blocks.add(block_a_ajouter);
                    block_is_created = false;
                    block_a_ajouter = null;
                }
            }
            previousIndexonMachine = order.getIndexOnMachine(task_iterator,num_machine_task);
            previousNumMachine = num_machine_task;
        }

        if(block_a_ajouter != null){
            list_blocks.add(block_a_ajouter);
        }


       /* for(Block blockIterator : list_blocks){
            blockIterator.printBlock();
        }*/
        return list_blocks;
    }

    /** For a given block, return the possible swaps for the Nowicki and Smutnicki neighborhood */
    List<Swap> neighbors(Block block) {
        //Les indexes se suivent
        ArrayList<Swap> listSwap = new ArrayList<Swap>();
        boolean two_element = false;

        if(block.getFirstTask()+ 1 == block.getLastTask()){
            two_element = true;
        }
        if(two_element){
            Swap new_swap = new Swap(block.machine, block.getFirstTask(), block.getLastTask());
            listSwap.add(new_swap);
        }
        else{
            Swap new_swap1 = new Swap(block.machine, block.getFirstTask(), block.getFirstTask()+1);
            Swap new_swap2 = new Swap(block.machine, block.getLastTask() - 1, block.getLastTask());
            listSwap.add(new_swap1);
            listSwap.add(new_swap2);
        }
        return listSwap;
    }

}
